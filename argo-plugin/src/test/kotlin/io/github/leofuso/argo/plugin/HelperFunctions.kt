package io.github.leofuso.argo.plugin

import java.io.File
import java.nio.file.Files
import java.util.*

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
    val resource = T::class.java.getResourceAsStream("/plugin-under-test-metadata.properties")
    checkNotNull(resource) {
        "Did not find plugin classpath resource, run `testClasses` build task."
    }

    val properties = Properties()
    properties.load(resource)

    /* So that there's no need to assume the classpath */
    return properties.getProperty("implementation-classpath")
        .split(":")
        .map { it.replace("\\", "\\\\") }
        .filter { it.endsWith("java/main") }
        .map { it.replace("java/main", "java/test") }
        .joinToString(", ") { "\"$it\"" }
}
