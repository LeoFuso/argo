package io.github.leofuso.argo.plugin.tasks

import io.github.leofuso.argo.plugin.GROUP_SOURCE_GENERATION
import io.github.leofuso.argo.plugin.SCHEMA_EXTENSION
import io.github.leofuso.argo.plugin.options.OptionalGettersStrategy
import org.apache.avro.Conversion
import org.apache.avro.LogicalTypes
import org.apache.avro.Schema
import org.apache.avro.compiler.specific.SpecificCompiler
import org.apache.avro.compiler.specific.SpecificCompiler.FieldVisibility
import org.apache.avro.generic.GenericData.StringType
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.nio.charset.StandardCharsets

@CacheableTask
abstract class SpecificRecordCompileTask : OutputTask() {

    init {
        description = "Generates SpecificRecord Java source files from Schema(.avsc) and Protocol(.avpr) definition files."
        group = GROUP_SOURCE_GENERATION
    }

    @get:Input
    abstract val encoding: Property<String>

    @get:Input
    abstract val additionalVelocityTools: ListProperty<Class<*>>

    @get:Optional
    @get:InputDirectory
    abstract val templateDirectory: DirectoryProperty

    @get:Input
    abstract val stringType: Property<StringType>

    @get:Input
    abstract val fieldVisibility: Property<FieldVisibility>

    @get:Input
    abstract val useBigDecimal: Property<Boolean>

    @get:Input
    abstract val noSetters: Property<Boolean>

    @get:Input
    abstract val addExtraOptionalGetters: Property<Boolean>

    @get:Input
    abstract val optionalGetters: Property<OptionalGettersStrategy>

    @get:Input
    abstract val logicalTypeFactories: MapProperty<String, Class<out LogicalTypes.LogicalTypeFactory>>

    @get:Input
    abstract val additionalConverters: ListProperty<Class<out Conversion<*>>>

    private fun conventions() {
        encoding.convention(StandardCharsets.UTF_8.name())
        additionalVelocityTools.convention(listOf())
        stringType.convention(StringType.CharSequence)
        fieldVisibility.convention(FieldVisibility.PRIVATE)
        useBigDecimal.convention(true)
        noSetters.convention(true)
        addExtraOptionalGetters.convention(false)
        optionalGetters.convention(OptionalGettersStrategy.ONLY_NULLABLE_FIELDS)
        logicalTypeFactories.convention(mapOf())
        additionalConverters.convention(listOf())
    }

    @TaskAction
    fun process() {
        conventions()
        compile(source.singleFile, destination.map(Directory::getAsFile).get())
    }

    private fun compile(source: File, destination: File) {
        val parser = Schema.Parser()
        source.listFiles { f ->
            f.isFile && SCHEMA_EXTENSION == f.extension
        }?.forEach { rawSchema ->
            val schema = parser.parse(rawSchema)
            val compiler = SpecificCompiler(schema)
            doCompile(compiler, rawSchema, destination)
        }
    }

    private fun doCompile(compiler: SpecificCompiler, source: File, destination: File) {

        compiler.setStringType(stringType.get())
        compiler.isCreateSetters = noSetters.map(Boolean::not).get()
        compiler.isGettersReturnOptional = true
        compiler.isOptionalGettersForNullableFieldsOnly = true
        compiler.isCreateOptionalGetters = false
        compiler.setEnableDecimalLogicalType(useBigDecimal.get())
        compiler.setOutputCharacterEncoding(encoding.get())
        compiler.setFieldVisibility(fieldVisibility.get())

        compiler.compileToDestination(source, destination)
    }
}
