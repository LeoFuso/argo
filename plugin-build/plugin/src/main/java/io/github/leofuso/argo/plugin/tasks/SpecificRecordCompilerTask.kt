package io.github.leofuso.argo.plugin.tasks

import io.github.leofuso.argo.plugin.GROUP_SOURCE_GENERATION
import io.github.leofuso.argo.plugin.OptionalGettersStrategy
import io.github.leofuso.argo.plugin.PROTOCOL_EXTENSION
import io.github.leofuso.argo.plugin.SCHEMA_EXTENSION
import io.github.leofuso.argo.plugin.parser.SchemaVisitor
import org.apache.avro.Conversion
import org.apache.avro.LogicalTypes
import org.apache.avro.compiler.specific.SpecificCompiler
import org.apache.avro.compiler.specific.SpecificCompiler.FieldVisibility
import org.apache.avro.generic.GenericData.StringType
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property
import org.gradle.work.InputChanges
import java.io.File

//@CacheableTask
abstract class SpecificRecordCompilerTask : OutputTask() {

    init {
        description =
            "Generates SpecificRecord Java source files from Schema(.$SCHEMA_EXTENSION) and Protocol(.$PROTOCOL_EXTENSION) definition files."
        group = GROUP_SOURCE_GENERATION
    }

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
    abstract fun getOptionalGettersStrategy(): Property<OptionalGettersStrategy>

    @Input
    @Optional
    abstract fun getAdditionalLogicalTypeFactories(): ListProperty<Class<out LogicalTypes.LogicalTypeFactory>>

    @Input
    @Optional
    abstract fun getAdditionalConverters(): ListProperty<Class<out Conversion<*>>>

    @TaskAction
    fun process(inputChanges: InputChanges) {
        val visitor = SchemaVisitor(logger)
        val definitions = visitor.getDefinitions(source)
        visitor.compileToDisk(definitions, getOutputDir().get().asFile, configure())
    }

    private fun doCompile(factory: (File) -> SpecificCompiler, spec: Spec<in File>) {

        val sources = source.filter(spec).toList()
        SchemaCaretaker(factory, sources)
            .forEach {
                runCatching {
                    it.second.compileToDestination(it.first, getOutputDir().get().asFile)
                }.onFailure { throwable ->
                    throw TaskExecutionException(this, throwable as Exception)
                }
            }
    }
}

