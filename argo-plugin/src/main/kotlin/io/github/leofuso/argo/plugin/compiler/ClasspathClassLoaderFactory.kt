package io.github.leofuso.argo.plugin.compiler

import org.gradle.internal.classloader.VisitableURLClassLoader
import java.io.File
import java.net.URLClassLoader

fun urlClassLoader(files: Set<File>, parent: ClassLoader? = null) = files.mapNotNull {
    val uri = it.toURI()
    uri.toURL()
}.toTypedArray()
    .let {
        if (parent != null) {
            URLClassLoader(it, parent)
        } else {
            URLClassLoader(it)
        }
    }

fun ClassLoader.loadURLs(files: Set<File>): ClassLoader = files.mapNotNull {
    val uri = it.toURI()
    uri.toURL()
}.fold(this) { loader, url ->
    if (loader is VisitableURLClassLoader) {
        loader.addURL(url)
    }
    return this
}
