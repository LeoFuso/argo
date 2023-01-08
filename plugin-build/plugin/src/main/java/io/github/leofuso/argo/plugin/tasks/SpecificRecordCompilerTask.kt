package io.github.leofuso.argo.plugin.tasks

import io.github.leofuso.argo.plugin.GROUP_SOURCE_GENERATION
import io.github.leofuso.argo.plugin.OptionalGettersStrategy
import io.github.leofuso.argo.plugin.PROTOCOL_EXTENSION
import io.github.leofuso.argo.plugin.SCHEMA_EXTENSION
import org.apache.avro.Conversion
import org.apache.avro.LogicalTypes
import org.apache.avro.Schema
import org.apache.avro.compiler.specific.SpecificCompiler
import org.apache.avro.compiler.specific.SpecificCompiler.FieldVisibility
import org.apache.avro.generic.GenericData.StringType
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import org.gradle.execution.commandline.TaskConfigurationException
import org.gradle.kotlin.dsl.property
import org.gradle.work.InputChanges
import java.io.File

//@CacheableTask
abstract class SpecificRecordCompilerTask : OutputTask() {

    init {
        description =
            "Generates SpecificRecord Java source files from Schema(.avsc) and Protocol(.avpr) definition files."
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
    var useDecimalType = project.objects.property<Boolean>()

    @get:Input
    var noSetters = project.objects.property<Boolean>()

    @get:Input
    abstract val addExtraOptionalGetters: Property<Boolean>

    @get:Input
    @get:Optional
    abstract var useOptionalGetters: Property<Boolean>

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
        compileSchemas()
    }

    private fun doCompile(factory: (File) -> SpecificCompiler, spec: Spec<in File>) {
        source.filter(spec)
            .forEach { source ->
                val compiler = factory.invoke(source)
                runCatching {
                    compiler.compileToDestination(source, getOutputDir().get().asFile)
                }.onFailure { throwable ->
                    throw TaskExecutionException(this, throwable as Exception)
                }
            }
    }

    private fun compileSchemas() {
        val parser = Schema.Parser()
        val factory = fromSchema(parser, configure()) { source, throwable ->
            TaskConfigurationException(
                name,
                "Unexpected error while parsing Schema(.avsc) definition file [${source.name}]",
                throwable as Exception
            )
        }
        doCompile(factory) { f ->
            f.isFile && SCHEMA_EXTENSION == f.extension
        }
    }

    private fun compileProtocol() {
        val factory = fromProtocol(configure()) { source, throwable ->
            TaskConfigurationException(
                name,
                "Unexpected error while parsing Protocol(.avpr) definition file [${source.name}]",
                throwable as Exception
            )
        }
        doCompile(factory) { f ->
            f.isFile && PROTOCOL_EXTENSION == f.extension
        }
    }
}

