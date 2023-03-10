package io.github.leofuso.argo.plugin

import org.apache.avro.Conversion
import org.apache.avro.Protocol
import org.apache.avro.compiler.specific.SpecificCompiler
import org.apache.avro.generic.GenericData.StringType
import org.apache.avro.specific.SpecificData
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import java.io.File

/**
 * Throws an [AssertionError] calculated by [lazyMessage] if the [value] is false.
 */
inline fun assertTrue(value: Boolean, lazyMessage: () -> Any = { "Failed Assertion." }) {
    if (!value) {
        val message = lazyMessage()
        throw AssertionError(message)
    }
}

fun SpecificCompiler.getCharacterEncoding(): String {
    val field = SpecificCompiler::class.java.getDeclaredField("outputCharacterEncoding")
    field.isAccessible = true
    return field.get(this) as String
}

fun SpecificCompiler.getStringType(): String {
    val field = SpecificCompiler::class.java.getDeclaredField("stringType")
    field.isAccessible = true
    return (field.get(this) as StringType).name
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

fun Protocol.path(): String =
    namespace.replace(NAMESPACE_SEPARATOR, File.separator) + File.separator + name + EXTENSION_SEPARATOR + PROTOCOL_EXTENSION

fun PatternSet.anti(other: PatternFilterable) = setExcludes(other.includes).setIncludes(other.excludes)

fun Project.addCompileOnlyConfiguration(name: String, description: String, source: SourceSet) =
    this.configurations.findByName(source.compileOnlyConfigurationName)?.let {
        val configuration = project.configurations.create(name) { config ->
            config.isVisible = true
            config.isCanBeResolved = true
            config.isCanBeConsumed = false
            config.description = description
        }
        configuration.extendsFrom(it)
    }
