package io.github.leofuso.argo.plugin

import java.io.File
import java.nio.file.Files

infix fun File.append(content: String) = this.writeText(content)
    .let { this }

infix fun File.tmkdirs(child: String) = run {
    val file = File(this, child)
    Files.createDirectories(file.parentFile.toPath())
    Files.createFile(file.toPath())
    file
}

fun applyPlugin(name: String, build: File) = build append "plugins { id $name } \n"
