package io.github.leofuso.argo.plugin.columba.invoker

import org.gradle.api.file.FileCollection
import java.io.PrintStream

interface ColumbaInvoker {

    fun invoke(arguments: List<String>, classpath: FileCollection)

}

internal class DefaultColumbaInvoker : ColumbaInvoker {

    override fun invoke(arguments: List<String>, classpath: FileCollection) {

        // val classloader = classloaderFactory.produce(classpath)
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

internal class NoopColumbaInvoker : ColumbaInvoker {

    override fun invoke(arguments: List<String>, classpath: FileCollection) {
        println("NO-OP cli invokation.")
        println(
            "\tArgs: ${
            arguments.joinToString(
                "\n",
                "[\n",
                "\n\t ]",
                transform = { "\t\t$it" }
            )
            }"
        )
        println(
            "\tClasspath: ${
            classpath.files.joinToString(
                "\n",
                "[\n",
                "\n\t ]",
                transform = { "\t\t${it.path}" }
            )
            }"
        )
    }
}
