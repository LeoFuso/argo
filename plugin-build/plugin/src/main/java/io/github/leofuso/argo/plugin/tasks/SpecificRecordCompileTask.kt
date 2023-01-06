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

//@CacheableTask
abstract class SpecificRecordCompileTask : OutputTask() {

    init {
        description = "Generates SpecificRecord Java source files from Schema(.avsc) and Protocol(.avpr) definition files."
        group = GROUP_SOURCE_GENERATION
    }

    @get:Input
    @get:Optional
    abstract val encoding: Property<String>

    @get:Input
    @get:Optional
    abstract val additionalVelocityTools: ListProperty<Class<*>>

    @get:Optional
    @get:InputDirectory
    abstract val templateDirectory: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val stringType: Property<StringType>

    @get:Input
    @get:Optional
    abstract val fieldVisibility: Property<FieldVisibility>

    @get:Input
    @get:Optional
    abstract val useBigDecimal: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val noSetters: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val addExtraOptionalGetters: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val optionalGetters: Property<OptionalGettersStrategy>

    @get:Input
    @get:Optional
    abstract val logicalTypeFactories: MapProperty<String, Class<out LogicalTypes.LogicalTypeFactory>>

    @get:Input
    @get:Optional
    abstract val additionalConverters: ListProperty<Class<out Conversion<*>>>

    @TaskAction
    fun process() {
        compile()
    }

    private fun compile() {
        val parser = Schema.Parser()
        source.filter { f ->
            f.isFile && SCHEMA_EXTENSION == f.extension
        }.forEach { rawSchema ->
            val schema = parser.parse(rawSchema)
            val compiler = SpecificCompiler(schema)
            doCompile(compiler, rawSchema)
        }
    }

    private fun doCompile(compiler: SpecificCompiler, source: File) {

        compiler.setStringType(stringType.get())
        compiler.isCreateSetters = noSetters.map(Boolean::not).get()
        compiler.isGettersReturnOptional = optionalGetters.isPresent
        compiler.isOptionalGettersForNullableFieldsOnly = optionalGetters.get() == OptionalGettersStrategy.ONLY_NULLABLE_FIELDS
        compiler.isCreateOptionalGetters = addExtraOptionalGetters.get()
        compiler.setEnableDecimalLogicalType(useBigDecimal.get())
        encoding.orNull?.let { compiler.setOutputCharacterEncoding(it) }
        compiler.setFieldVisibility(fieldVisibility.get())
        compiler.compileToDestination(source, destination.get().asFile)
    }
}
