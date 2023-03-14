package io.github.leofuso.argo.plugin.columba

import org.gradle.internal.classloader.VisitableURLClassLoader
import java.io.File
import java.net.URLClassLoader

inline fun <reified T> T.urlClassLoader(files: Set<File>): URLClassLoader = files.map { it.toURI().toURL() }
    .toTypedArray()
    .let {
        URLClassLoader.newInstance(it, T::class.java.classLoader)
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
