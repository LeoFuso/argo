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
import io.github.leofuso.columba.cli.command.CompileCommand
import org.slf4j.helpers.MessageFormatter
import java.io.PrintStream

class CommandRunner(private val out: PrintStream, private val err: PrintStream) : CliktCommand(
    name = "Columba",
    printHelpOnEmptyArgs = true
) {

    /**
     * The log levels supported by Gradle.
     */
    enum class LogLevel {
        DEBUG, INFO, LIFECYCLE, WARN, QUIET, ERROR
    }

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
        ).enum()
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
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    inner class SimpleLogger {

        private val command = this@CommandRunner

        private fun format(message: String?, vararg args: Any?): String? {
            if (args.size > 1) {
                return MessageFormatter.arrayFormat(message, args).message
            }
            return MessageFormatter.format(message, args[0]).message
        }

        fun getLogLevel(): LogLevel = command.logLevel

        private fun isLogEnabled(target: LogLevel) = command.logLevel.ordinal <= target.ordinal

        fun isInfoEnabled() = isLogEnabled(LogLevel.INFO)

        fun isLifecycleEnabled() = isLogEnabled(LogLevel.LIFECYCLE)

        fun isWarnEnabled() = isLogEnabled(LogLevel.WARN)

        fun isQuietEnabled() = isLogEnabled(LogLevel.QUIET)

        fun lifecycle(message: String?, vararg arguments: Any?) = lifecycle(format(message, *arguments))

        fun info(message: String?, vararg arguments: Any?) = info(format(message, *arguments))

        fun warn(message: String?, vararg arguments: Any?) = warn(format(message, *arguments))

        fun error(message: String?, vararg arguments: Any?) = error(format(message, *arguments))

        fun lifecycle(
            message: String?,
            trailingNewline: Boolean = true,
            lineSeparator: String = command.currentContext.console.lineSeparator
        ) = if (isLifecycleEnabled()) command.echo(message, trailingNewline, false, lineSeparator) else Unit

        fun info(message: String?, trailingNewline: Boolean = true, lineSeparator: String = command.currentContext.console.lineSeparator) =
            if (isInfoEnabled()) command.echo(message, trailingNewline, false, lineSeparator) else Unit

        fun warn(message: String?, trailingNewline: Boolean = true, lineSeparator: String = command.currentContext.console.lineSeparator) =
            if (isWarnEnabled()) command.echo("\u001B[33m$message\u001B[0m", trailingNewline, false, lineSeparator) else Unit

        fun error(message: String?, trailingNewline: Boolean = true, lineSeparator: String = command.currentContext.console.lineSeparator) =
            command.echo(message, trailingNewline, true, lineSeparator)
    }

}

fun main(args: Array<String>) = main(args, System.out, System.err)

fun main(args: Array<String>, out: PrintStream, err: PrintStream) = CommandRunner(out, err)
    .subcommands(CompileCommand())
    .main(args)
