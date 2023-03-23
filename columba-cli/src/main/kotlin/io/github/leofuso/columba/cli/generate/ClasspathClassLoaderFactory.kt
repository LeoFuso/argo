package io.github.leofuso.columba.cli.generate

import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap

interface ClasspathClassLoaderFactory {

    fun produce(classpath: Set<File>): URLClassLoader

}

internal class CachedClassLoaderFactory : ClasspathClassLoaderFactory {

    private val cache = ConcurrentHashMap<Int, URLClassLoader>()

    override fun produce(classpath: Set<File>): URLClassLoader {
        val hashCode = HashSet(classpath).hashCode()
        return cache.getOrPut(hashCode) {
            URLClassLoader(
                classpath.map { it.toURI().toURL() }.toTypedArray(),
                null
            )
        }
    }
}

object GlobalClassLoaderFactory : ClasspathClassLoaderFactory by CachedClassLoaderFactory()
