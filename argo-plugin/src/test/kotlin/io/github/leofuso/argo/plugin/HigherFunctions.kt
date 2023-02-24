package io.github.leofuso.argo.plugin

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

infix fun File.append(content: String) = this.writeText(content)
    .let { this }

infix fun File.tmkdirs(child: String) = run {
    val file = File(this, child)
    Files.createDirectories(file.parentFile.toPath())
    Files.createFile(file.toPath())
    file
}

infix fun String.sh(other: String) = this + File.separator + other

infix fun String.slash(target: String) = this.replace(target, File.separator)

inline fun <reified T> readPluginClasspath(): String {
    val resource = T::class.java.classLoader.getResource("plugin-classpath.txt")
    val path = checkNotNull(resource) {
        "Did not find plugin classpath resource, run `testClasses` build task."
    }.path

    return Files.readAllLines(Paths.get(path))
        .map { it.replace("\\", "\\\\") }
        .filter { it.endsWith("java/test") }
        .joinToString(", ") { "\"$it\"" }
}
