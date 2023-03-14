package io.github.leofuso.columba.compiler.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.unique
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.unique
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import io.github.leofuso.columba.compiler.cli.runner.SpecificCompilerRunner
import org.apache.avro.compiler.specific.SpecificCompiler.FieldVisibility
import org.apache.avro.generic.GenericData.StringType
import org.slf4j.helpers.MessageFormatter

class FieldOptions : OptionGroup(
    name = "Field Options",
    help = "Options related to Java field generation."
) {
    val visibility by option(
        "--field-visibility",
        help = "Java class property visibility."
    )
        .enum<FieldVisibility>(ignoreCase = true)
        .default(FieldVisibility.PRIVATE)

    val useDecimalType by option(
        "--use-decimal-type",
        help = "Use BigDecimal as the decimal type, instead of ByteArray class."
    ).flag(default = false)

    val stringType by option(
        "--string-type",
        help = "String property implementing class, either CharSequence, String, Utf8."
    )
        .enum<StringType>(ignoreCase = true)
        .default(StringType.CharSequence)
}

class AccessorOptions : OptionGroup(
    name = "Acessor Options",
    help = "Options related to Java field-accessor methods."
) {
    val allowSetters by option(
        "--allow-setters",
        help = "Whether the properties are final."
    ).flag(default = false)

    val addExtraOptionalGetters by option(
        "--add-extra-optional-getters",
        help = "Add extra optional-wrapped getters on top of default ones."
    ).flag(default = false)

    val useOptionalGetters by option(
        "--use-optional-getters-only",
        help = "Use optional-wrapped getters instead of default ones."
    ).flag(default = false)

    val useOptionalGettersForNullableFieldsOnly by option(
        "--use-optional-getters-for-nullable-fields-only",
        help = "Use optional-wrapped getters instead of default ones, but only for fields that are nullable."
    ).flag(default = false)
}

class CompileCommandRunner : CliktCommand(
    name = "compile",
    help = """
    Source code generation.
    
    Generates SpecificRecord Java source files from Schema(.$SCHEMA_EXTENSION) 
    and Protocol(.$PROTOCOL_EXTENSION) definition files.
    
    Compile 'SOURCE'(s) to 'DEST'.
    
    """,
    printHelpOnEmptyArgs = true
) {

    val logger = ConsoleLogger()

    init {
        context {
            helpFormatter = CliktHelpFormatter(showRequiredTag = true, showDefaultValues = true)
        }
    }

    val outputEncoding by option(
        "-o",
        "--output-enconding",
        help = "Encoding for the generated classes."
    )
        .default("UTF-8")

    val fields by FieldOptions()
    val accessors by AccessorOptions()

    val velocityTemplate by option(
        "--velocity-template",
        "-t",
        help = """
           Velocity is a Java-based templating engine. It is used by the compiler to generate Java source code.
           
           You can costumize its behavior by providing additional velocity-tools alongside a corresponding
           template.
           
           Use this option to locate the custom Velocity templates by providing its path.
           
        """
    )
        .path(
            mustBeReadable = true,
            mustExist = true,
            canBeFile = false
        )

    val additionalVelocityTools by option(
        "--velocity-tools",
        "-v",
        help = """
           Velocity is a Java-based templating engine. It is used by the compiler to generate Java source code.
           
           You can costumize its behavior by providing additional velocity-tools alongside a corresponding
           template.
           
           Use this option to add additional Velocity tool-classes by 
           passing the fully qualified class names, separated by ';'.
           
        """
    )
        .split(";")
        .default(listOf())
        .unique()

    val additionalLogicalTypeFactories by option(
        "--logical-type-factories",
        "-f",
        help = """
            Conversion between generic and logical type values.
            
            Instances of these classes are added to a LogicalTypeFactory pool to convert logical types to
            a particular representation.
          
            Use this option to add LogicalTypeFactories by 
            passing its name, and the fully qualified class name. 
           
           Example:
           
            ```
            ---logical-type-factories timezone=io.github.leofuso.argo.custom.TimeZoneLogicalTypeFactory
            ---logical-type-factories other=io.github.leofuso.argo.custom.OtherLogicalTypeFactory
            -f other-one=io.github.leofuso.argo.custom.OtherOneLogicalTypeFactory
            ```
            
        """
    )
        .associate()

    val additionalConverters by option(
        "--converters",
        "-c",
        help = """
            Conversion between generic and logical type values.
            
            Instances of these classes are added to GenericData to convert logical types to
            a particular representation.
          
            Use this option to add additional converter classes by 
            passing the fully qualified class names, separated by ';'.
            
        """
    )
        .split(";")
        .default(listOf())
        .unique()

    private val infoMode by option(
        "--info",
        "-i",
        help = "info mode."
    ).flag(default = false)

    val source by argument().file(mustExist = true, mustBeReadable = true, canBeDir = false).multiple().unique()
    val dest by argument().file(canBeFile = false)

    override fun run() {

        logger.lifecycle("Compiling $source to $dest.")
        SpecificCompilerRunner(this)
            .run()

    }

    /**
     * Exposes a limited Logger-like instance.
     */
    inner class ConsoleLogger {

        private val runner = this@CompileCommandRunner

        private fun format(message: String?, vararg args: Any?): String? {
            if(args.size > 1) {
                return MessageFormatter.arrayFormat(message, args).message
            }
            return MessageFormatter.format(message, args[0]).message
        }

        fun isInfoEnabled() = runner.infoMode

        fun lifecycle(message: String?, vararg arguments: Any?) = lifecycle(format(message, *arguments))

        fun info(message: String?, vararg arguments: Any?) = info(format(message, *arguments))

        fun error(message: String?, vararg arguments: Any?) = error(format(message, *arguments))

        fun lifecycle(
            message: String?,
            trailingNewline: Boolean = true,
            lineSeparator: String = runner.currentContext.console.lineSeparator
        ) = runner.echo(message, trailingNewline, false, lineSeparator)

        fun info(message: String?, trailingNewline: Boolean = true, lineSeparator: String = runner.currentContext.console.lineSeparator) =
            if (runner.infoMode) {
                runner.echo(message, trailingNewline, false, lineSeparator)
            } else {
                /* Ignored: Not in info mode. */
            }

        fun error(message: String?, trailingNewline: Boolean = true, lineSeparator: String = runner.currentContext.console.lineSeparator) =
            runner.echo(message, trailingNewline, true, lineSeparator)
    }

}

fun main(args: Array<String>) = CompileCommandRunner().main(args)
