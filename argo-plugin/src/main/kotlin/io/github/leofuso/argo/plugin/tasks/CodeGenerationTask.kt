package io.github.leofuso.argo.plugin.tasks

import io.github.leofuso.argo.plugin.JAR_EXTENSION
import io.github.leofuso.argo.plugin.ZIP_EXTENSION
import io.github.leofuso.argo.plugin.columba.invoker.ColumbaWorkAction
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.kotlin.dsl.property
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "Gradle would require more information to cache this task")
abstract class CodeGenerationTask : DefaultTask() {

    private val _isNoop = project.objects.property<Boolean>().convention(false)
    private val _pattern = PatternSet()
    private val _classpath: ConfigurableFileCollection = project.objects.fileCollection()
    private val _sources: ConfigurableFileCollection = project.objects.fileCollection()

    @get:Nested
    @get:Optional
    abstract val launcher: Property<JavaLauncher>

    @get:Internal
    internal abstract val arguments: List<String>

    @get:Internal
    var isNoop: Boolean
        get() = _isNoop.get()
        set(value) = _isNoop.set(value)

    @get:Internal
    var pattern: PatternFilterable
        get() = _pattern
        set(value) {
            _pattern.copyFrom(value)
        }

    @get:Classpath
    var classpath: FileCollection
        get() = _classpath
        set(value) = _classpath.setFrom(value)

    @get:Internal
    internal val configurableClasspath: ConfigurableFileCollection
        get() = _classpath

    @InputFiles
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getSources(): FileTree {

        _sources.asFileTree
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

        return _sources
            .asFileTree
            .matching(_pattern)
    }

    @OutputDirectory
    abstract fun getOutputDir(): DirectoryProperty

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor

    /**
     * Adds some source to this task. The given source objects are evaluated in accordance with [org.gradle.api.Project.files].
     *
     * @param sources The source to add
     */
    open fun source(vararg sources: Any): ConfigurableFileCollection = _sources.from(*sources)

    protected fun doProcessInIsolation() {
        val executor = getWorkerExecutor()
        val queue = executor.processIsolation { spec ->
            spec.classpath.from(classpath)
            spec.forkOptions { options ->
                options.maxHeapSize = "64m"
                options.executable(launcher.get().executablePath.asFile.absolutePath)
            }
        }
        project.logging.captureStandardOutput(LogLevel.LIFECYCLE)
        queue.submit(ColumbaWorkAction::class.java) { parameters ->
            parameters.arguments.set(arguments)
            parameters.noop.set(isNoop)
        }
    }
}
