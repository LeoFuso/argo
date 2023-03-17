package io.github.leofuso.argo.plugin.columba.invoker

import org.apache.logging.log4j.io.IoBuilder
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import java.io.PrintStream

interface ColumbaInvoker {

    fun invoke(
        arguments: List<String>,
        classpath: FileCollection
    )

}

internal class DefaultColumbaInvoker(
    private val classloaderFactory: ClasspathClassLoaderFactory = GlobalClassLoaderFactory
) : ColumbaInvoker {


    override fun invoke(arguments: List<String>, classpath: FileCollection) {

        //val classloader = classloaderFactory.produce(classpath)
        val main = Class.forName("io.github.leofuso.columba.cli.MainKt")
        val mainMethod = main.getMethod(
            "main",
            Array<String>::class.java,
            PrintStream::class.java,
            PrintStream::class.java
        )

        val logger = IoBuilder
            .forLogger(this::class.java)
            .buildPrintStream()

        mainMethod.invoke(null, arguments.toTypedArray(), logger, logger)
    }
}

internal class NoopColumbaInvoker(private val logger: Logger) : ColumbaInvoker {

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
