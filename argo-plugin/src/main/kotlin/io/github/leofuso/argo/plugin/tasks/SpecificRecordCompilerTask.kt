package io.github.leofuso.argo.plugin.tasks

import io.github.leofuso.argo.plugin.ColumbaOptions
import io.github.leofuso.argo.plugin.GROUP_SOURCE_GENERATION
import io.github.leofuso.argo.plugin.JAR_EXTENSION
import io.github.leofuso.argo.plugin.PROTOCOL_EXTENSION
import io.github.leofuso.argo.plugin.SCHEMA_EXTENSION
import io.github.leofuso.argo.plugin.ZIP_EXTENSION
import io.github.leofuso.argo.plugin.compiler.SpecificCompilerTaskDelegate
import io.github.leofuso.argo.plugin.compiler.parser.DefaultSchemaParser
import org.apache.avro.compiler.specific.SpecificCompiler.FieldVisibility
import org.apache.avro.generic.GenericData.StringType
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileType
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.gradle.kotlin.dsl.property
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File
import kotlin.io.path.Path

fun getSpecificRecordCompileBuildDirectory(project: Project, source: SourceSet): Provider<Directory> =
    project.layout.buildDirectory.dir("generated-${source.name}-specific-record")

@CacheableTask
@Suppress("TooManyFunctions")
abstract class SpecificRecordCompilerTask : DefaultTask() {

    init {

        description = """
            |Generates SpecificRecord Java source files from Schema(.$SCHEMA_EXTENSION) 
            |and Protocol(.$PROTOCOL_EXTENSION) definition files.
        """.trimMargin().replace("\n", "")

        group = GROUP_SOURCE_GENERATION
    }

    private val _sources: ConfigurableFileCollection = project.objects.fileCollection()
    private val _pattern: PatternFilterable = PatternSet()
    private val _compressedPattern: PatternFilterable = PatternSet()
        .include(
            "**${File.separator}*.$JAR_EXTENSION",
            "**${File.separator}*.$ZIP_EXTENSION"
        )

    private val _classpath: ConfigurableFileCollection = project.objects.fileCollection()

    @get:Input
    val useDecimalType = project.objects.property<Boolean>()

    @get:Input
    val noSetters = project.objects.property<Boolean>()

    @get:Input
    abstract val addExtraOptionalGetters: Property<Boolean>

    @get:Input
    abstract val useOptionalGetters: Property<Boolean>

    @get:Input
    abstract val optionalGettersForNullableFieldsOnly: Property<Boolean>

    @get:Internal
    var pattern: PatternFilterable
        get() = _pattern
        set(value) = run {
            _pattern.setIncludes(value.includes)
            _pattern.setExcludes(value.excludes)
        }

    @get:Classpath
    @get:InputFiles
    var classpath: FileCollection
        get() = _classpath
        set(value) = _classpath.setFrom(value)

    @get:Internal
    val configurableClasspath: ConfigurableFileCollection
        get() = _classpath

    @OutputDirectory
    abstract fun getOutputDir(): DirectoryProperty

    @InputFiles
    @Incremental
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getSources(): FileCollection = _sources.asFileTree
        .matching(_compressedPattern)
        .map {
            runCatching {
                project.zipTree(it)
                    .asFileTree
                    .matching(_pattern)
                    .files
            }.onFailure { throwable ->
                logger.error("Task '${this.name}' is unable to extract sources from File[${it.path}].", throwable)
            }
        }
        .filter { it.isSuccess }
        .flatMap { it.getOrThrow() }
        .let {

            if (it.isNotEmpty()) {
                logger.lifecycle("Found '${it.size}' definition files in the classpath.")
                if (logger.isInfoEnabled) {
                    logger.info("Applying source(${it.joinToString(", ") {file -> "'${File.separator}${file.name}'" }}).")
                }
            }

            _sources.from(it)
                .asFileTree
                .matching(_pattern)
        }

    /**
     * Adds some source to this task. The given source objects will be evaluated in accordance with [org.gradle.api.Project.files].
     *
     * @param sources The source to add
     */
    open fun source(vararg sources: Any): ConfigurableFileCollection = _sources.from(*sources)

    @Input
    @Optional
    abstract fun getEncoding(): Property<String>

    @Input
    @Optional
    abstract fun getAdditionalVelocityTools(): ListProperty<String>

    @Optional
    @Classpath
    @InputDirectory
    abstract fun getVelocityTemplateDirectory(): DirectoryProperty

    @Input
    @Optional
    abstract fun getStringType(): Property<StringType>

    @Input
    @Optional
    abstract fun getFieldVisibility(): Property<FieldVisibility>

    @Input
    @Optional
    abstract fun getAdditionalLogicalTypeFactories(): MapProperty<String, String>

    @Input
    @Optional
    abstract fun getAdditionalConverters(): ListProperty<String>

    @TaskAction
    fun process(inputChanges: InputChanges) {

        val sources = getSources()
        if (sources.files.isEmpty()) {
            logger.lifecycle("Skipping task '${this.name}': No sources.")
            return
        }

        if (inputChanges.isIncremental) {

            val changes = inputChanges.getFileChanges(sources)
                .filter { it.fileType != FileType.DIRECTORY }

            val anyChanges = changes.any()
            if (!anyChanges) {
                return
            }

            logger.lifecycle("Generating SpecificRecord Java sources from:")
            changes.forEach { change -> logger.lifecycle("\t{}", change.normalizedPath) }
        }

        val exclusion = _pattern.excludes
        if (exclusion.isNotEmpty()) {
            logger.lifecycle("Excluded sources {}", exclusion)
        }

        if (inputChanges.isIncremental.not()) {
            logger.lifecycle("Generating SpecificRecord Java sources from all sources.")
        }

        try {

            SpecificCompilerTaskDelegate(this)
                .use { delegate ->

                    val parser = DefaultSchemaParser(logger)
                    val resolution = parser.parse(sources.asFileTree)

                    delegate.run(resolution, getOutputDir().asFile.get())
                }

            didWork = true

        } catch (ex: Throwable) {
            throw TaskExecutionException(this, ex)
        }
    }

    fun withExtension(options: ColumbaOptions) {
        _pattern.include(
            "**${File.separator}*.$SCHEMA_EXTENSION",
            "**${File.separator}*.$PROTOCOL_EXTENSION"
        )
        _pattern.exclude(options.getExcluded().get())
        getEncoding().set(options.getOutputEncoding())
        getAdditionalVelocityTools().set(options.getAdditionalVelocityTools())
        getAdditionalLogicalTypeFactories().set(options.getAdditionalLogicalTypeFactories())
        getAdditionalConverters().set(options.getAdditionalConverters())
        getVelocityTemplateDirectory().set(options.getVelocityTemplateDirectory())

        val fields = options.getFields()
        getStringType().set(fields.getStringType())
        getFieldVisibility().set(fields.getVisibility())
        useDecimalType.set(fields.useDecimalTypeProperty)

        val accessors = options.getAccessors()
        noSetters.set(accessors.noSetterProperty)
        addExtraOptionalGetters.set(accessors.addExtraOptionalGettersProperty)
        useOptionalGetters.set(accessors.useOptionalGettersProperty)
        optionalGettersForNullableFieldsOnly.set(accessors.optionalGettersForNullableFieldsOnlyProperty)
    }

    fun configureSourceSet(source: SourceSet) {
        val buildDirectory = getSpecificRecordCompileBuildDirectory(project, source)
        getOutputDir().set(buildDirectory)
        source.java { it.srcDir(buildDirectory) }

        val sourceDirectory = run {
            val classpath = "src${File.separator}${source.name}${File.separator}avro"
            val path = Path(classpath)
            project.files(path).asPath
        }
        _sources.from(sourceDirectory)
    }

    fun dependsOn(task: TaskProvider<IDLProtocolTask>) {
        _sources.from(task)
        super.dependsOn(task)
    }
}
