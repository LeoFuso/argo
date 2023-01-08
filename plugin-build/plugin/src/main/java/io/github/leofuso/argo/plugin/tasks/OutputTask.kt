package io.github.leofuso.argo.plugin.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.*
import org.gradle.work.Incremental

abstract class OutputTask : SourceTask() {

    @OutputDirectory
    abstract fun getOutputDir(): DirectoryProperty

    @InputFiles
    @Incremental
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource(): FileTree {
        return super.getSource()
    }
}
