package io.github.leofuso.argo.plugin.tasks

import io.github.leofuso.argo.plugin.GROUP_SOURCE_GENERATION
import io.github.leofuso.argo.plugin.PROTOCOL_EXTENSION
import io.github.leofuso.argo.plugin.SCHEMA_EXTENSION
import io.github.leofuso.argo.plugin.columba.arguments.*
import io.github.leofuso.argo.plugin.columba.extensions.ColumbaOptions
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.property
import java.io.File
import kotlin.io.path.Path

fun getSpecificRecordCompileBuildDirectory(project: Project, source: SourceSet): Provider<Directory> =
    project.layout.buildDirectory.dir("generated-${source.name}-specific-record")

@CacheableTask
@Suppress("TooManyFunctions")
abstract class SpecificRecordCompilerTask : CodeGenerationTask() {

    init {

        description = """
            |Generates SpecificRecord Java source files from Schema(.$SCHEMA_EXTENSION) 
            |and Protocol(.$PROTOCOL_EXTENSION) definition files.
        """.trimMargin().replace("\n", "")

        group = GROUP_SOURCE_GENERATION
    }

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
    override val arguments
        get() = listOf(
            LoggerArgument(logger),
            CompileArgument,
            SourceArgument(getSources()),
            OutputArgument(getOutputDir()),
            OutputEncodingArgument(getEncoding()),
            VelocityTemplateArgument(getVelocityTemplateDirectory()),
            VelocityToolsArgument(getAdditionalVelocityTools()),
            ConverterArgument(getAdditionalConverters()),
            LogicalTypeFactoryArgument(getAdditionalLogicalTypeFactories()),
            StringTypeArgument(getStringType()),
            FieldVisibilityArgument(getFieldVisibility()),
            AllowSettersArgument(noSetters.map { it.not() }),
            UseDecimalTypeArgument(useDecimalType),
            ExtraOptionalGettersArgument(addExtraOptionalGetters),
            UseOptionalGettersOnlyArgument(useOptionalGetters),
            UseOptionalGettersForNullableFieldsOnlyArgument(optionalGettersForNullableFieldsOnly)
        )
            .flatMap(CliArgument::args)

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
    abstract fun getStringType(): Property<String>

    @Input
    @Optional
    abstract fun getFieldVisibility(): Property<String>

    @Input
    @Optional
    abstract fun getAdditionalLogicalTypeFactories(): MapProperty<String, String>

    @Input
    @Optional
    abstract fun getAdditionalConverters(): ListProperty<String>

    internal fun configureAt(source: SourceSet) {
        val buildDirectory = getSpecificRecordCompileBuildDirectory(project, source)
        getOutputDir().set(buildDirectory)
        source.java { it.srcDir(buildDirectory) }

        val sourceDirectory = run {
            val classpath = "src${File.separator}${source.name}${File.separator}avro"
            val path = Path(classpath)
            project.files(path).asPath
        }
        source(sourceDirectory)
    }

    internal fun dependsOn(task: TaskProvider<IDLProtocolTask>) {
        source(task)
        super.dependsOn(task)
    }

    @TaskAction
    fun process() {
        val sources = getSources()
        logger.lifecycle("Generating SpecificRecord Java sources from {} definition files.", sources.files.size)

        val exclusion = pattern.excludes
        if (exclusion.isNotEmpty()) {
            logger.lifecycle("Excluded sources {}", exclusion)
        }

        doProcessInIsolation()
        didWork = true
    }

    fun withExtension(options: ColumbaOptions) {
        pattern.include(
            "**${File.separator}*.$SCHEMA_EXTENSION",
            "**${File.separator}*.$PROTOCOL_EXTENSION"
        )
        pattern.exclude(options.getExcluded().get())
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
}
