package io.github.leofuso.columba.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktConsole
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.output.defaultCliktConsole
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.leofuso.columba.cli.command.CompileCommand
import org.slf4j.helpers.MessageFormatter
import java.io.PrintStream

class CommandRunner(private val out: PrintStream, private val err: PrintStream) : CliktCommand(
    name = "Columba Cli",
    printHelpOnEmptyArgs = true
) {

    val logger = ConsoleLogger()

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
        currentContext.findOrSetObject { logger }
    }

    private val info by option(
        "--info",
        "-i",
        help = "info mode."
    ).flag(default = false)

    override fun run() {
        /* NOOP */
    }

    /**
     * Exposes a limited Logger-like instance.
     */
    inner class ConsoleLogger {

        private val main = this@CommandRunner

        private fun format(message: String?, vararg args: Any?): String? {
            if (args.size > 1) {
                return MessageFormatter.arrayFormat(message, args).message
            }
            return MessageFormatter.format(message, args[0]).message
        }

        fun isInfoEnabled() = main.info

        fun lifecycle(message: String?, vararg arguments: Any?) = lifecycle(format(message, *arguments))

        fun info(message: String?, vararg arguments: Any?) = info(format(message, *arguments))

        fun error(message: String?, vararg arguments: Any?) = error(format(message, *arguments))

        fun lifecycle(
            message: String?,
            trailingNewline: Boolean = true,
            lineSeparator: String = main.currentContext.console.lineSeparator
        ) = main.echo(message, trailingNewline, false, lineSeparator)

        fun info(message: String?, trailingNewline: Boolean = true, lineSeparator: String = main.currentContext.console.lineSeparator) =
            if (isInfoEnabled()) {
                main.echo(message, trailingNewline, false, lineSeparator)
            } else {
                /* Ignored: Not in info mode. */
            }

        fun error(message: String?, trailingNewline: Boolean = true, lineSeparator: String = main.currentContext.console.lineSeparator) =
            main.echo(message, trailingNewline, true, lineSeparator)
    }

}

fun main(args: Array<String>) = main(args, System.out, System.err)

fun main(args: Array<String>, out: PrintStream, err: PrintStream) = CommandRunner(out, err)
    .subcommands(CompileCommand())
    .main(args)
