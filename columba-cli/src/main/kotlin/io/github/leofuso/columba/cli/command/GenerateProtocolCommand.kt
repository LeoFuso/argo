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
import io.github.leofuso.columba.cli.*
import io.github.leofuso.columba.cli.parser.SpecificCompilerRunner
import org.apache.avro.compiler.idl.Idl
import java.io.File
import java.nio.file.Files

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

    private val dest by argument()
        .file(canBeFile = false)

    override fun run() {
        logger.lifecycle("Generating protocol from $sources on $dest.")

        val parsed = mutableSetOf<String>()
        sources.forEach {

            val idl = Idl(it)
            val protocol = idl.CompilationUnit()
            val content = protocol.toString(true)
            val path = protocol.path()

            if (parsed.contains(path)) {
                throw
                    IllegalStateException(
                        "Invalid Protocol [$path]. There's already another Protocol defined in the classpath with the same name."
                    )
            }

            logger.lifecycle("Writing Protocol($PROTOCOL_EXTENSION) to ['$path'].")
            val output = File(dest, path)
            Files.createDirectories(output.parentFile.toPath())
            Files.createFile(output.toPath())
            output.writeText(content)
            parsed.add(path)
        }
    }
}
