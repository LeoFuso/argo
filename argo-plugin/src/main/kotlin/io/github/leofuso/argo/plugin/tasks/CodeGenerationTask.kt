package io.github.leofuso.argo.plugin.tasks

import io.github.leofuso.argo.plugin.JAR_EXTENSION
import io.github.leofuso.argo.plugin.ZIP_EXTENSION
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.Incremental
import java.io.File

@DisableCachingByDefault(because = "Gradle would require more information to cache this task")
abstract class CodeGenerationTask : DefaultTask() {

    private val _pattern = PatternSet()
    private val _classpath: ConfigurableFileCollection = project.objects.fileCollection()
    private val _sources: ConfigurableFileCollection = project.objects.fileCollection()

    @get:Internal
    var pattern: PatternFilterable
        get() = _pattern
        set(value) {
            _pattern.copyFrom(value)
        }

    @get:Classpath
    @get:InputFiles
    var classpath: FileCollection
        get() = _classpath
        set(value) = _classpath.setFrom(value)

    @get:Internal
    internal val configurableClasspath: ConfigurableFileCollection
        get() = _classpath

    @InputFiles
    @Incremental
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getSources() = _sources
        .asFileTree
        .matching(_pattern)

    @OutputDirectory
    abstract fun getOutputDir(): DirectoryProperty

    /**
     * Adds some source to this task. The given source objects will be evaluated in accordance with [org.gradle.api.Project.files].
     *
     * @param sources The source to add
     */
    open fun source(vararg sources: Any): ConfigurableFileCollection = _sources.from(*sources)

    open fun source(source: Configuration): ConfigurableFileCollection = source.asFileTree
        .matching(
            PatternSet()
                .include(
                    "**${File.separator}*.$JAR_EXTENSION",
                    "**${File.separator}*.$ZIP_EXTENSION"
                )
        )
        .map {
            runCatching {
                project.zipTree(it)
            }.onFailure { throwable ->
                logger.error("Task '${this.name}' is unable to extract sources from File[${it.path}].", throwable)
            }
        }
        .filter { it.isSuccess }
        .flatMap { it.getOrThrow() }
        .let { _sources.from(it) }
}
