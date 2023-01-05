package io.github.leofuso.argo.plugin

import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.SourceSet

fun Project.avroSourceDir(source: SourceSet) = file("src/${source.name}/${source.AVRO_SOURCE_SET_NAME}")

fun ProjectLayout.getSpecificRecordBuildDirectory(source: SourceSet) = buildDirectory.dir("generate-${source.name}-specific-record")

val SourceSet.AVRO_SOURCE_SET_NAME: String
    get() = "avro"

