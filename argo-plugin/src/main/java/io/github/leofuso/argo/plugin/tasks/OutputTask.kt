package io.github.leofuso.argo.plugin.tasks

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceTask
import org.gradle.work.Incremental

/**
 * An [OutputTask] produces source files as output.
 */
abstract class OutputTask : SourceTask() {

    private val outputDirectory: DirectoryProperty = project.objects.directoryProperty()

    @OutputDirectory
    fun getOutputDir() = outputDirectory

    @InputFiles
    @Incremental
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource(): FileTree {
        return super.getSource()
    }

    /**
     * Sets the output directory for this task.
     *
     * @param dir The directory.
     */
    @Internal
    fun setOutputDir(dir: Provider<out Directory>) {
        getOutputDir().set(dir)
    }
}
