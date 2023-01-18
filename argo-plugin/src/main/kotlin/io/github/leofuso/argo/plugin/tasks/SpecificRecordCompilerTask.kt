package io.github.leofuso.argo.plugin.tasks

import io.github.leofuso.argo.plugin.ColumbaOptions
import io.github.leofuso.argo.plugin.GROUP_SOURCE_GENERATION
import io.github.leofuso.argo.plugin.OptionalGettersStrategy
import io.github.leofuso.argo.plugin.PROTOCOL_EXTENSION
import io.github.leofuso.argo.plugin.SCHEMA_EXTENSION
import io.github.leofuso.argo.plugin.compiler.configure
import io.github.leofuso.argo.plugin.compiler.fromProtocol
import io.github.leofuso.argo.plugin.compiler.fromSchema
import io.github.leofuso.argo.plugin.compiler.parser.DefaultSchemaParser
import org.apache.avro.Conversion
import org.apache.avro.LogicalTypes
import org.apache.avro.compiler.specific.SpecificCompiler.FieldVisibility
import org.apache.avro.generic.GenericData.StringType
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.kotlin.dsl.property
import org.gradle.work.InputChanges
import kotlin.io.path.Path

fun getSpecificRecordCompileBuildDirectory(project: Project, source: SourceSet): Provider<Directory> =
    project.layout.buildDirectory.dir("generated-${source.name}-specific-record")

// @CacheableTask
@Suppress("TooManyFunctions")
abstract class SpecificRecordCompilerTask : OutputTask() {

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

    @Input
    @Optional
    abstract fun getEncoding(): Property<String>

    @Input
    @Optional
    abstract fun getAdditionalVelocityTools(): ListProperty<Class<*>>

    @Optional
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
    abstract fun getOptionalGettersStrategy(): Property<OptionalGettersStrategy>

    @Input
    @Optional
    abstract fun getAdditionalLogicalTypeFactories(): ListProperty<Class<out LogicalTypes.LogicalTypeFactory>>

    @Input
    @Optional
    abstract fun getAdditionalConverters(): ListProperty<Class<out Conversion<*>>>

    @TaskAction
    fun process(inputChanges: InputChanges) {
        val parser = DefaultSchemaParser(logger)
        val resolution = parser.parse(source)

        resolution.schemas
            .forEach { (_, schema) ->
                fromSchema(schema, configure()) { cause -> TaskExecutionException(this, cause) }
            }

        resolution.protocol
            .forEach { (_, protocol) ->
                fromProtocol(protocol, configure()) { cause -> TaskExecutionException(this, cause) }
            }

        didWork = true
    }

    fun withExtension(options: ColumbaOptions) {
        include("**/*.$SCHEMA_EXTENSION", "**/*.$PROTOCOL_EXTENSION")
        exclude(options.getExcluded().get())

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
        getOptionalGettersStrategy().set(accessors.getOptionalGettersStrategy())
    }

    private fun getSourceDirectory(source: SourceSet): () -> ConfigurableFileCollection = {
        val classpath = "src/${source.name}/avro"
        val path = Path(classpath)
        project.files(path)
    }

    private fun getBuildDirectory(source: SourceSet) = getSpecificRecordCompileBuildDirectory(project, source)

    fun configureSourceSet(source: SourceSet) {
        val buildDirectory = getBuildDirectory(source)
        setOutputDir(buildDirectory)
        source.java { it.srcDir(buildDirectory) }

        val sourceDirectory = getSourceDirectory(source)
        source(sourceDirectory)
    }
}
