package io.github.leofuso.argo.plugin.fixture

import java.io.File

object ResourceFactory {

    fun load(location: String): ByteArray {
        val resource = ResourceFactory::class.java
            .classLoader
            .getResource(location)
            ?: throw NullPointerException("ResourceFactory was unnable to locate [$location]")

        resource.openStream().use { stream -> return stream.readAllBytes() }
    }

    fun asFile(location: String): File {
        val resource = ResourceFactory::class.java
            .classLoader
            .getResource(location)
            ?: throw NullPointerException("ResourceFactory was unnable to locate [$location]")

        return File(resource.toURI())
    }
}
