package io.github.leofuso.argo.plugin.columba.invoker

import org.gradle.api.file.FileCollection
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap

interface ClasspathClassLoaderFactory {

    fun produce(classpath: FileCollection): URLClassLoader

}

internal class CachedClassLoaderFactory : ClasspathClassLoaderFactory {

    private val cache = ConcurrentHashMap<Int, URLClassLoader>()

    override fun produce(classpath: FileCollection): URLClassLoader {
        val hashCode = HashSet(classpath.files).hashCode()
        return cache.getOrPut(hashCode) {
            URLClassLoader(
                classpath.files.map { it.toURI().toURL() }.toTypedArray(),
                null
            )
        }
    }
}

object GlobalClassLoaderFactory : ClasspathClassLoaderFactory by CachedClassLoaderFactory()
