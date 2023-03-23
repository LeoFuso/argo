package io.github.leofuso.columba.cli

import org.slf4j.helpers.MessageFormatter

/**
 * A simplified ConsoleLogger.
 */
@Suppress("ComplexInterface")
interface ConsoleLogger {

    /**
     * The log levels supported by Gradle.
     */
    enum class LogLevel {
        DEBUG, INFO, LIFECYCLE, WARN, QUIET, ERROR
    }

    private fun format(message: String?, vararg args: Any?): String? {
        if (args.size > 1) {
            return MessageFormatter.arrayFormat(message, args).message
        }
        return MessageFormatter.format(message, args[0]).message
    }

    fun getLogLevel(): LogLevel

    fun isLogEnabled(target: LogLevel) = getLogLevel().ordinal <= target.ordinal

    fun isInfoEnabled() = isLogEnabled(LogLevel.INFO)

    fun isLifecycleEnabled() = isLogEnabled(LogLevel.LIFECYCLE)

    fun isWarnEnabled() = isLogEnabled(LogLevel.WARN)

    fun isQuietEnabled() = isLogEnabled(LogLevel.QUIET)

    fun lifecycle(message: String?)

    fun lifecycle(message: String?, vararg arguments: Any?) = lifecycle(format(message, *arguments))

    fun info(message: String?)

    fun info(message: String?, vararg arguments: Any?) = info(format(message, *arguments))

    fun warn(message: String?)

    fun warn(message: String?, vararg arguments: Any?) = warn(format(message, *arguments))

    fun error(message: String?)

    fun error(message: String?, vararg arguments: Any?) = error(format(message, *arguments))

}
