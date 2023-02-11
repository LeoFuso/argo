package io.github.leofuso.argo.plugin.tasks

import io.github.leofuso.argo.plugin.GROUP_SOURCE_GENERATION
import io.github.leofuso.argo.plugin.IDL_EXTENSION
import io.github.leofuso.argo.plugin.PROTOCOL_EXTENSION
import io.github.leofuso.argo.plugin.path
import org.apache.avro.compiler.idl.Idl
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File
import java.net.MalformedURLException
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.*

abstract class IDLProtocolTask : DefaultTask() {

    init {

        description = """
            |Generates Avro Protocol(.$PROTOCOL_EXTENSION) source files from Avro IDL(.$IDL_EXTENSION) source files.
        """.trimMargin().replace("\n", "")

        group = GROUP_SOURCE_GENERATION
    }

    @OutputDirectory
    abstract fun getOutputDir(): DirectoryProperty

    @InputFiles
    @Incremental
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract fun getSources(): ConfigurableFileTree

    @Classpath
    @InputFiles
    abstract fun getClasspath(): ConfigurableFileTree

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

        val exclusion = getSources().patterns.excludes
        if (exclusion.isNotEmpty()) {
            logger.lifecycle("Excluding sources from {}", exclusion)
        }

        val parsed = mutableSetOf<String>()
        val classLoader = assembleClassLoader()
        val outputDir = getOutputDir().asFile.get()
        sources.files.forEach {

            val idl = Idl(it, classLoader)
            val protocol = idl.CompilationUnit()
            val content = protocol.toString(true)
            val path = protocol.path()

            if (parsed.contains(path)) {
                throw TaskExecutionException(this, IllegalStateException("Invalid namespace [$path]. Protocol already exists."))
            }

            logger.lifecycle("Writing Protocol($PROTOCOL_EXTENSION) to ['$path'].")
            val output = File(outputDir, path)
            Files.createDirectories(output.parentFile.toPath())
            Files.createFile(output.toPath())
            output.writeText(content)
            parsed.add(path)
        }
        didWork = true
    }

    private fun assembleClassLoader() = getClasspath().files.mapNotNull {
        try {
            val uri = it.toURI()
            uri.toURL()
        } catch (ex: MalformedURLException) {
            logger.error("Unnable to extract URL from file at [{}]", it.path, ex)
            null
        }
    }.toTypedArray().let { URLClassLoader(it, null) }
}