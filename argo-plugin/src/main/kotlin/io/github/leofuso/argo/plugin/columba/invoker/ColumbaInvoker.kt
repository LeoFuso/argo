package io.github.leofuso.argo.plugin.columba.invoker

import org.gradle.api.file.FileCollection
import java.io.PrintStream

/**
 * A component in which delegates the task action by invoking a command-line-application.
 */
interface ColumbaInvoker {

    /**
     * Invoke a command based upon its [arguments].
     */
    fun invoke(arguments: List<String>)

}

internal class DefaultColumbaInvoker : ColumbaInvoker {

    override fun invoke(arguments: List<String>) {
        val main = Class.forName("io.github.leofuso.columba.cli.MainKt")
        val mainMethod = main.getMethod(
            "main",
            Array<String>::class.java,
            PrintStream::class.java,
            PrintStream::class.java
        )
        mainMethod.invoke(null, arguments.toTypedArray(), System.out, System.err)
    }
}

internal class NoopColumbaInvoker(private val classpath: FileCollection) : ColumbaInvoker {

    override fun invoke(arguments: List<String>) {
        println("NO-OP cli invokation.")
        println(
            "\tArgs: ${arguments.joinToString(
                "\n",
                "[\n",
                "\n\t ]",
                transform = { "\t\t$it" }
            )}"
        )
        println(
            "\tClasspath: ${classpath.files.joinToString(
                "\n",
                "[\n",
                "\n\t ]",
                transform = { "\t\t${it.path}" }
            )}"
        )
    }
}
