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
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import java.io.File
import kotlin.io.path.Path

@CacheableTask
abstract class IDLProtocolTask : CodeGenerationTask() {

    init {
        description = "Generates Avro Protocol(.$PROTOCOL_EXTENSION) source files from Avro IDL(.$IDL_EXTENSION) source files."
        group = GROUP_SOURCE_GENERATION
        pattern.include("**${File.separator}*.$IDL_EXTENSION")
    }

    @get:Internal
    override val arguments
        get() = listOf(
            LoggerArgument(logger),
            GenerateProtocolArgument,
            SourceArgument(getSources()),
            OutputArgument(getOutputDir()),
            ClasspathArgument(classpath)
        )
            .flatMap(CliArgument::args)

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
    fun process() {
        val sources = getSources()
        logger.lifecycle("Generating Avro Protocol(.$PROTOCOL_EXTENSION) from {} source files.", sources.files.size)

        val exclusion = pattern.excludes
        if (exclusion.isNotEmpty()) {
            logger.lifecycle("Excluding sources from {}", exclusion)
        }

        doRunInIsolation()
        didWork = true
    }
}
