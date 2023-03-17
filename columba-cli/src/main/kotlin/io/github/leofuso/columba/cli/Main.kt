package io.github.leofuso.columba.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktConsole
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.output.defaultCliktConsole
import com.github.ajalt.clikt.parameters.groups.default
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import io.github.leofuso.columba.cli.ConsoleLogger.*
import io.github.leofuso.columba.cli.command.CompileCommand
import io.github.leofuso.columba.cli.command.GenerateProtocolCommand
import java.io.PrintStream

class CommandRunner(private val out: PrintStream, private val err: PrintStream) : CliktCommand(
    name = "Columba",
    printHelpOnEmptyArgs = true
) {

    private val logLevel by mutuallyExclusiveOptions(
        option(
            "--info",
            "-i",
            help = "Set LogLevel as Info or Quiet."
        )
            .flag("--quiet", "-q")
            .convert { op -> if (op) LogLevel.INFO else LogLevel.QUIET },
        option(
            "--log",
            "-l",
            help = "A general LogLevel configuration."
        ).enum(ignoreCase = true)
    )
        .single()
        .default(LogLevel.LIFECYCLE)

    private val logger by findOrSetObject { SimpleLogger() }

    init {
        context {
            helpFormatter = CliktHelpFormatter(showRequiredTag = true, showDefaultValues = true)
            console = object : CliktConsole {

                private val delegate = defaultCliktConsole()

                override val lineSeparator: String
                    get() = delegate.lineSeparator

                override fun print(text: String, error: Boolean) {
                    if (error) {
                        this@CommandRunner.err
                    } else {
                        this@CommandRunner.out
                    }.print(text)
                }

                override fun promptForLine(prompt: String, hideInput: Boolean) = delegate.promptForLine(prompt, hideInput)
            }
        }
    }

    override fun run() {
        /* Lazy property */
        check(logger.getLogLevel() == logLevel)
    }

    /**
     * Exposes a limited Logger-like instance.
     */
    inner class SimpleLogger : ConsoleLogger {

        private val command = this@CommandRunner

        private val lineSeparator
            get() = command.currentContext.console.lineSeparator

        override fun getLogLevel(): LogLevel = command.logLevel

        override fun lifecycle(message: String?) = if (isLifecycleEnabled()) {
            command.echo(message, true, false, lineSeparator)
        } else {
            Unit
        }

        override fun info(message: String?) = if (isInfoEnabled()) {
            command.echo(message, true, false, lineSeparator)
        } else {
            Unit
        }

        override fun warn(message: String?) = if (isWarnEnabled()) {
            command.echo("\u001B[33m$message\u001B[0m", true, false, lineSeparator)
        } else {
            Unit
        }

        override fun error(message: String?) = command.echo(message, true, true, lineSeparator)
    }

}

fun main(args: Array<String>) = main(args, System.out, System.err)

fun main(args: Array<String>, out: PrintStream, err: PrintStream) = CommandRunner(out, err)
    .subcommands(CompileCommand(), GenerateProtocolCommand())
    .main(args)
