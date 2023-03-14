package io.github.leofuso.columba.cli.command

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import org.apache.avro.compiler.specific.SpecificCompiler
import org.apache.avro.generic.GenericData

class FieldOptions : OptionGroup(
    name = "Field Options",
    help = "Options related to Java field generation."
) {
    val visibility by option(
        "--field-visibility",
        help = "Java class property visibility."
    )
        .enum<SpecificCompiler.FieldVisibility>(ignoreCase = true)
        .default(SpecificCompiler.FieldVisibility.PRIVATE)

    val useDecimalType by option(
        "--use-decimal-type",
        help = "Use BigDecimal as the decimal type, instead of ByteArray class."
    ).flag(default = false)

    val stringType by option(
        "--string-type",
        help = "String property implementing class, either CharSequence, String, Utf8."
    )
        .enum<GenericData.StringType>(ignoreCase = true)
        .default(GenericData.StringType.CharSequence)
}
