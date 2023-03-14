package io.github.leofuso.columba.compiler.cli

import org.apache.avro.Conversion
import org.apache.avro.compiler.specific.SpecificCompiler
import org.apache.avro.generic.GenericData
import org.apache.avro.specific.SpecificData

fun SpecificCompiler.getCharacterEncoding(): String {
    val field = SpecificCompiler::class.java.getDeclaredField("outputCharacterEncoding")
    field.isAccessible = true
    return field.get(this) as String
}

fun SpecificCompiler.getStringType(): String {
    val field = SpecificCompiler::class.java.getDeclaredField("stringType")
    field.isAccessible = true
    return (field.get(this) as GenericData.StringType).name
}

fun SpecificCompiler.enableDecimalLogicalType(): Boolean {
    val field = SpecificCompiler::class.java.getDeclaredField("enableDecimalLogicalType")
    field.isAccessible = true
    return field.getBoolean(this)
}

fun SpecificCompiler.getAdditionalVelocityTools(): List<Any?> {
    val field = SpecificCompiler::class.java.getDeclaredField("additionalVelocityTools")
    field.isAccessible = true
    return field.get(this) as List<Any?>
}

fun SpecificCompiler.getTemplateDirectory(): String {
    val field = SpecificCompiler::class.java.getDeclaredField("templateDir")
    field.isAccessible = true
    return field.get(this) as String
}

fun SpecificCompiler.getConverters(): List<Conversion<*>> {
    val field = SpecificCompiler::class.java.getDeclaredField("specificData")
    field.isAccessible = true
    val data = field.get(this) as SpecificData
    return data.conversions.map { it }
}
