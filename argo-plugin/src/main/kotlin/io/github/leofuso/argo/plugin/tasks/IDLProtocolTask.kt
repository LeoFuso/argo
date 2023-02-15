package io.github.leofuso.argo.plugin.tasks

import io.github.leofuso.argo.plugin.GROUP_SOURCE_GENERATION
import io.github.leofuso.argo.plugin.IDL_EXTENSION
import io.github.leofuso.argo.plugin.PROTOCOL_EXTENSION
import io.github.leofuso.argo.plugin.path
import org.apache.avro.compiler.idl.Idl
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileType
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File
import java.net.MalformedURLException
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.*
import kotlin.io.path.Path

fun getAvroProtocolBuildDirectory(project: Project, source: SourceSet): Provider<Directory> =
    project.layout.buildDirectory.dir("generated-${source.name}-avro-protocol")

@CacheableTask
abstract class IDLProtocolTask : DefaultTask() {

    init {

        description = """
            |Generates Avro Protocol(.$PROTOCOL_EXTENSION) source files from Avro IDL(.$IDL_EXTENSION) source files.
        """.trimMargin().replace("\n", "")

        group = GROUP_SOURCE_GENERATION
    }

    private val _sources: ConfigurableFileCollection = project.objects.fileCollection()
    private val _pattern: PatternFilterable = PatternSet()
    private val _classpath: ConfigurableFileCollection = project.objects.fileCollection()

    @get:Classpath
    @get:InputFiles
    var classpath: FileCollection
        get() = _classpath
        set(value) = _classpath.setFrom(value)

    @OutputDirectory
    abstract fun getOutputDir(): DirectoryProperty

    @InputFiles
    @Incremental
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getSources() = _sources.asFileTree.matching(_pattern)

    @Internal
    fun getSourcePattern() = _pattern

    /**
     * Adds some source to this task. The given source objects will be evaluated as per [org.gradle.api.Project.files].
     *
     * @param sources The source to add
     */
    open fun source(vararg sources: Any) = _sources.from(*sources)

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

        val exclusion = getSourcePattern().excludes
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
                val exception =
                    IllegalStateException(
                        "Invalid Protocol [$path]. There's already another Protocol defined in the classpath with the same name."
                    )
                throw TaskExecutionException(this, exception)
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

    private fun assembleClassLoader() = _classpath.files.mapNotNull {
        try {
            val uri = it.toURI()
            uri.toURL()
        } catch (ex: MalformedURLException) {
            logger.error("Unnable to extract URL from file at [{}]", it.path, ex)
            null
        }
    }.toTypedArray().let { URLClassLoader(it, null) }

    fun configureSourceSet(source: SourceSet) {
        val buildDirectory = getAvroProtocolBuildDirectory(project, source)
        getOutputDir().set(buildDirectory)
        val sourceDirectory = run {
            val classpath = "src/${source.name}/avro"
            val path = Path(classpath)
            project.files(path).asPath
        }
        _sources.from(sourceDirectory)
        getSourcePattern().include("**/*.$IDL_EXTENSION")
    }

}
