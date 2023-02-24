package io.github.leofuso.argo.plugin.compiler

import java.io.File
import java.net.URLClassLoader

fun urlClassLoader(files: Set<File>) = files.mapNotNull {
    val uri = it.toURI()
    uri.toURL()
}.toTypedArray().let { URLClassLoader(it) }
