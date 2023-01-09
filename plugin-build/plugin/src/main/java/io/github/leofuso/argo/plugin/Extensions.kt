package io.github.leofuso.argo.plugin

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import java.io.File

fun Project.avroSourceDir(source: SourceSet): File = file("src/${source.name}/${source.AVRO_SOURCE_SET_NAME}")

fun ProjectLayout.getSpecificRecordBuildDirectory(source: SourceSet): Provider<Directory> =
    buildDirectory.dir("generate-${source.name}-specific-record")

val SourceSet.AVRO_SOURCE_SET_NAME: String
    get() = "avro"

/**
 * Throws an [AssertionError] calculated by [lazyMessage] if the [value] is false.
 */
inline fun assertTrue(value: Boolean, lazyMessage: () -> Any = { "Failed Assertion." }) {
    if (!value) {
        val message = lazyMessage()
        throw AssertionError(message)
    }
}
