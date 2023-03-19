package io.github.leofuso.argo.plugin.tasks

import io.github.leofuso.argo.plugin.GROUP_SOURCE_GENERATION
import io.github.leofuso.argo.plugin.IDL_EXTENSION
import io.github.leofuso.argo.plugin.PROTOCOL_EXTENSION
import io.github.leofuso.argo.plugin.columba.arguments.ClasspathArgument
import io.github.leofuso.argo.plugin.columba.arguments.CliArgument
import io.github.leofuso.argo.plugin.columba.arguments.GenerateProtocolArgument
import io.github.leofuso.argo.plugin.columba.arguments.LoggerArgument
import io.github.leofuso.argo.plugin.columba.arguments.OutputArgument
import io.github.leofuso.argo.plugin.columba.arguments.SourceArgument
import io.github.leofuso.argo.plugin.columba.invoker.ColumbaWorkAction
import org.gradle.api.file.FileType
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject
import kotlin.io.path.Path

@CacheableTask
abstract class IDLProtocolTask @Inject constructor(private val executor: WorkerExecutor) : CodeGenerationTask() {

    init {
        description = "Generates Avro Protocol(.$PROTOCOL_EXTENSION) source files from Avro IDL(.$IDL_EXTENSION) source files."
        group = GROUP_SOURCE_GENERATION
        pattern.include("**${File.separator}*.$IDL_EXTENSION")
    }

    @get:Internal
    internal val arguments
        get() = listOf(
            LoggerArgument(logger),
            GenerateProtocolArgument,
            SourceArgument(getSources()),
            OutputArgument(getOutputDir()),
            ClasspathArgument(classpath)
        )
            .flatMap(CliArgument::args)

//    @get:Nested
//    abstract val launcher: Property<JavaLauncher>

    open fun configureAt(sourceSet: SourceSet) {
        val buildDirectory = project.layout.buildDirectory.dir("generated-${sourceSet.name}-avro-protocol")
        getOutputDir().set(buildDirectory)
        val sourceDirectory = run {
            val classpath = "src${File.separator}${sourceSet.name}${File.separator}avro"
            val path = Path(classpath)
            project.files(path).asPath
        }
        source(sourceDirectory)
    }

    @TaskAction
    fun process(inputChanges: InputChanges) {

        val sources = getSources()
        if (inputChanges.isIncremental) {

            val changes = inputChanges.getFileChanges(sources)
                .filter { it.fileType != FileType.DIRECTORY }
                .filter { it.file.extension == IDL_EXTENSION }

            val anyChanges = changes.any()
            if (!anyChanges) {
                return
            }

            logger.lifecycle("Generating Avro Protocol(.$PROTOCOL_EXTENSION) from:")
            changes.forEach { change -> logger.lifecycle("\t{}", change.normalizedPath) }
        }

        val exclusion = pattern.excludes
        if (exclusion.isNotEmpty()) {
            logger.lifecycle("Excluding sources from {}", exclusion)
        }

        val queue = executor.processIsolation { spec ->
            spec.classpath.from(classpath)
            spec.forkOptions { options ->
                options.maxHeapSize = "64m"
            }
        }

        project.logging.captureStandardOutput(LogLevel.LIFECYCLE)
        queue.submit(ColumbaWorkAction::class.java) { parameters ->
            parameters.arguments.set(arguments)
            parameters.noop.set(false)
        }

        didWork = true
    }

}
