package io.github.leofuso.argo.plugin.options

import org.apache.avro.compiler.specific.SpecificCompiler.FieldVisibility
import org.apache.avro.generic.GenericData.StringType
import java.nio.charset.Charset

data class CompilerOptions(
    val encoding: Charset?,
    val stringType: StringType?,
    val fieldVisibility: FieldVisibility?
)

/**
 * Used to specify the strategy for nullable fields for generated code.
 */
public enum class OptionalGettersStrategy {
    ALL_FIELDS, ONLY_NULLABLE_FIELDS
}
