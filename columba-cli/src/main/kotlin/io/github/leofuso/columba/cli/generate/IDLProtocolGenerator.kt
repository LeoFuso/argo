package io.github.leofuso.columba.cli.generate

import io.github.leofuso.columba.cli.ConsoleLogger
import io.github.leofuso.columba.cli.PROTOCOL_EXTENSION
import io.github.leofuso.columba.cli.path
import org.apache.avro.compiler.idl.Idl
import java.io.File
import java.nio.file.Files

class IDLProtocolGenerator(classpath: Set<File>, private val logger: ConsoleLogger) : AutoCloseable {

    private val classLoader = GlobalClassLoaderFactory.produce(classpath)

    @Suppress("UseRequire")
    fun generate(sources: Set<File>, dest: File) {
        val parsed = mutableSetOf<String>()
        sources.forEach {

            val idl = Idl(it, classLoader)
            val protocol = idl.CompilationUnit()
            val content = protocol.toString(true)
            val path = protocol.path()

            if (parsed.contains(path)) {
                throw IllegalArgumentException(
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

    override fun close() = classLoader.close()

}
