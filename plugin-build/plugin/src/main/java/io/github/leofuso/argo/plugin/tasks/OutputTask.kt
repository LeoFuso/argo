package io.github.leofuso.argo.plugin.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceTask

abstract class OutputTask : SourceTask() {

    @get:OutputDirectory
    abstract val destination: DirectoryProperty

    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource(): FileTree {
        return super.getSource()
    }
}
