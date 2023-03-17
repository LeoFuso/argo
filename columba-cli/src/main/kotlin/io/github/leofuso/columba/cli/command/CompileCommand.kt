package io.github.leofuso.columba.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.unique
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import io.github.leofuso.columba.cli.ConsoleLogger
import io.github.leofuso.columba.cli.PROTOCOL_EXTENSION
import io.github.leofuso.columba.cli.SCHEMA_EXTENSION
import io.github.leofuso.columba.cli.parser.SpecificCompilerRunner

class CompileCommand : CliktCommand(
    name = "compile",
    help = """
    Source code generation.
    
    Generates SpecificRecord Java source files from Schema(.$SCHEMA_EXTENSION) 
    and Protocol(.$PROTOCOL_EXTENSION) definition files.
    
    Compile 'SOURCE'(s) to 'DEST'.
    
    """,
    printHelpOnEmptyArgs = true
) {

    init {
        context {
            helpFormatter = CliktHelpFormatter(showRequiredTag = true, showDefaultValues = true)
        }
    }

    val logger by requireObject<ConsoleLogger>()

    val fields by FieldOptions()
    val accessors by AccessorOptions()
    val velocity by VelocityOptions()
    val converter by ConverterOptions()

    val outputEncoding by option(
        "-o",
        "--output-encoding",
        help = "Encoding for the generated classes."
    )
        .default("UTF-8")

    val source by argument()
        .file(mustExist = true, mustBeReadable = true, canBeDir = false)
        .multiple()
        .unique()

    val dest by argument()
        .file(canBeFile = false)

    override fun run() {
        logger.lifecycle("Compiling $source to $dest.")
        SpecificCompilerRunner(this)
            .run()

    }

}
