package io.github.leofuso.argo.plugin.fixtures

import java.io.File

inline fun <reified T> loadResource(path: String): File {
    val classLoader = T::class.java.classLoader
    val resource = classLoader.getResource(path)
        ?: throw IllegalArgumentException("Unnable to resolve the resource at [$path]")
    return File(resource.toURI())
}
