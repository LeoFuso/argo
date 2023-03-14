package io.github.leofuso.argo.plugin.columba.invoker

import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException

interface CliInvoker {

    fun invoke(
        arguments: List<String>,
        classpath: FileCollection
    )

}

internal class DefaultCliInvoker(
    private val classloaderFactory: ClasspathClassLoaderFactory = GlobalClassLoaderFactory
) : CliInvoker {

    override fun invoke(arguments: List<String>, classpath: FileCollection) {
        try {
            val classloader = classloaderFactory.produce(classpath)
            val main = classloader.loadClass("io.github.leofuso.columba.cli.MainKt")
            val mainMethod = main.getMethod(
                "main",
                Array<String>::class.java,
                PrintStream::class.java,
                PrintStream::class.java
            )
            mainMethod.invoke(null, arguments.toTypedArray(), System.out, System.err)
        } catch (ex: InvocationTargetException) {
            val message = ex.targetException.message
            if (message != null) {
                return
            }
            throw GradleException(message ?: "There was a problem running columba.", ex)
        }
    }
}

internal class NoopCliInvoker(private val logger: Logger) : CliInvoker {

    override fun invoke(arguments: List<String>, classpath: FileCollection) {
        logger.lifecycle("NO-OP cli invokation.")
        logger.lifecycle(
            "\tArgs: {}", arguments.joinToString(
                ",\n",
                "[\n",
                "\n\t ]",
                transform = { "\t\t$it" }
            )
        )
        logger.lifecycle(
            "\tClasspath: {}", classpath.files.joinToString(
                ",\n",
                "[\n",
                "\n\t ]",
                transform = { "\t\t${it.path}" }
            )
        )
    }
}
