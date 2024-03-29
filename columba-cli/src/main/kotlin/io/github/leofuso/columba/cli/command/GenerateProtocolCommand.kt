package io.github.leofuso.columba.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.unique
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import io.github.leofuso.columba.cli.*
import io.github.leofuso.columba.cli.generate.IDLProtocolGenerator

class GenerateProtocolCommand : CliktCommand(
    name = "generate-protocol",
    help = """
    Protocol generation.
    
    Generates Avro Protocol(.$PROTOCOL_EXTENSION) source files from
    Avro IDL(.$IDL_EXTENSION) source files.
    
    Generate 'SOURCE'(s) to 'DEST'.
    
    """,
    printHelpOnEmptyArgs = true
) {

    init {
        context {
            helpFormatter = CliktHelpFormatter(showRequiredTag = true, showDefaultValues = true)
        }
    }

    private val logger by requireObject<ConsoleLogger>()

    private val sources by argument()
        .file(mustExist = true, mustBeReadable = true, canBeDir = false)
        .multiple()
        .unique()
        .check("Accepts only Avro IDL(.$IDL_EXTENSION) source files as input.") { input ->
            input.all { file -> (file.extension == IDL_EXTENSION) }
        }

    private val dest by argument()
        .file(mustExist = true, mustBeReadable = true, mustBeWritable = true, canBeFile = false)

    private val classpath by option(
        "--classpath",
        "-c",
        help = """
          
           Use this option to add additional IDLs to the classpath 
           by passing its paths, separated by ';'.
           
        """
    )
        .file(mustExist = true, mustBeReadable = true, canBeDir = false)
        .split(";")
        .default(listOf())
        .unique()
        .check("Accepts only Avro IDL(.$IDL_EXTENSION) source files or Jar(.$JAR_EXTENSION) files as classpath.") { input ->
            input.all { file -> file.extension == IDL_EXTENSION || file.extension == JAR_EXTENSION }
        }

    override fun run() {
        logger.lifecycle("Generating protocol from $sources on $dest.")
        IDLProtocolGenerator(classpath, logger)
            .use {
                it.generate(sources, dest)
            }
    }
}
