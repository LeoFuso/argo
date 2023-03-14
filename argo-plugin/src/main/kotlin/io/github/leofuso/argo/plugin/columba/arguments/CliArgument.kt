package io.github.leofuso.argo.plugin.columba.arguments

import org.apache.avro.compiler.specific.SpecificCompiler.FieldVisibility
import org.apache.avro.generic.GenericData.StringType
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/**
 * Velocity Tools
 */
const val VELOCITY_TEMPLATE_ARGS = "--velocity-template"
const val VELOCITY_TOOLS_ARGS = "--velocity-tools"

/**
 * Converter
 */
const val LOGICAL_TYPE_FACTORIES_ARGS = "--logical-type-factories"
const val CONVERTER_ARGS = "--converters"

/**
 * Fields
 */
const val FIELD_VISIBILITY_ARGS = "--field-visibility"
const val DECIMAL_TYPE_ARGS = "--use-decimal-type"
const val STRING_TYPE_ARGS = "--string-type"

/**
 * Acessors
 */
const val ALLOW_SETTERS_ARGS = "--allow-setters"
const val EXTRA_OPTIONAL_GETTERS_ARGS = "--add-extra-optional-getters"
const val USE_OPTIONAL_GETTERS_ONLY_ARGS = "--use-optional-getters-only"
const val USE_OPTIONAL_GETTERS_NULL_ONLY_ARGS = "--use-optional-getters-for-nullable-fields-only"

/**
 * Miscellaneous
 */
const val OUTPUT_ENCODING_ARG = "--output-enconding"

internal sealed interface CliArgument {

    fun args(): List<String>

}

internal data class OutputEncodingArgument(val encoding: Property<String>) : CliArgument {
    override fun args() =
        if (encoding.isPresent) {
            listOf("$OUTPUT_ENCODING_ARG ${encoding.get()}")
        } else {
            emptyList()
        }
}

internal data class UseOptionalGettersForNullableFieldsOnlyArgument(val active: Property<Boolean>) : CliArgument {
    override fun args() =
        if (active.isPresent && active.get()) {
            listOf(USE_OPTIONAL_GETTERS_NULL_ONLY_ARGS)
        } else {
            emptyList()
        }
}

internal data class UseOptionalGettersOnlyArgument(val active: Property<Boolean>) : CliArgument {
    override fun args() =
        if (active.isPresent && active.get()) {
            listOf(USE_OPTIONAL_GETTERS_ONLY_ARGS)
        } else {
            emptyList()
        }
}

internal data class AllowSettersArgument(val active: Provider<Boolean>) : CliArgument {
    override fun args() =
        if (active.isPresent && active.get()) {
            listOf(ALLOW_SETTERS_ARGS)
        } else {
            emptyList()
        }
}

internal data class UseDecimalTypeArgument(val active: Property<Boolean>) : CliArgument {
    override fun args() =
        if (active.isPresent && active.get()) {
            listOf(DECIMAL_TYPE_ARGS)
        } else {
            emptyList()
        }
}

internal data class FieldVisibilityArgument(val visibility: Property<FieldVisibility>) : CliArgument {
    override fun args() =
        if (visibility.isPresent) {
            listOf("$FIELD_VISIBILITY_ARGS ${visibility.get().name}")
        } else {
            emptyList()
        }
}

internal data class StringTypeArgument(val type: Property<StringType>) : CliArgument {
    override fun args() =
        if (type.isPresent) {
            listOf("$STRING_TYPE_ARGS ${type.get().name}")
        } else {
            emptyList()
        }
}

internal data class ConverterArgument(val converters: ListProperty<String>) : CliArgument {
    override fun args() =
        if (converters.isPresent && converters.get().isNotEmpty()) {
            listOf("$CONVERTER_ARGS  ${converters.get().joinToString { ";" }}")
        } else {
            emptyList()
        }
}

internal data class LogicalTypeFactoryArgument(val factories: MapProperty<String, String>) : CliArgument {
    override fun args() =
        if (factories.isPresent && factories.get().isNotEmpty()) {
            val arg = factories.get()
                .map { (k, v) -> "$LOGICAL_TYPE_FACTORIES_ARGS $k=$v" }
                .joinToString(" ")
            listOf(arg)
        } else {
            emptyList()
        }
}

internal data class VelocityTemplateArgument(val directory: DirectoryProperty) : CliArgument {
    override fun args() =
        if (directory.isPresent) {
            listOf("$VELOCITY_TEMPLATE_ARGS ${directory.get().asFile.path}")
        } else {
            emptyList()
        }
}

internal data class VelocityToolsArgument(val tools: ListProperty<String>) : CliArgument {
    override fun args() =
        if (tools.isPresent && tools.get().isNotEmpty()) {
            listOf("$VELOCITY_TOOLS_ARGS ${tools.get().joinToString(";")}")
        } else {
            emptyList()
        }
}

internal data class ExtraOptionalGettersArgument(val active: Property<Boolean>) : CliArgument {
    override fun args() =
        if (active.isPresent && active.get()) {
            listOf(EXTRA_OPTIONAL_GETTERS_ARGS)
        } else {
            emptyList()
        }
}

internal data class SourceArgument(val files: FileCollection) : CliArgument {
    override fun args() = listOf(files.joinToString(" ") { it.absolutePath })
}

internal data class OutputArgument(val directory: DirectoryProperty) : CliArgument {
    override fun args() =
        if (directory.isPresent) {
            listOf(directory.asFile.get().path)
        } else {
            emptyList()
        }
}


