package io.github.leofuso.argo.plugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

fun String.asPlatformAgnosticPath() = this.replace("/", File.separator)

fun platformAgnosticPath(path: String): Path = Paths.get(path.asPlatformAgnosticPath())

infix fun File.append(content: String) = this.writeText(content)
    .let { this }

infix fun File.tmkdirs(child: String) = run {
    val file = File(this, child.asPlatformAgnosticPath())
    Files.createDirectories(file.parentFile.toPath())
    Files.createFile(file.toPath())
    file
}

infix fun String.sh(other: String) = this + File.separator + other

infix fun String.slash(target: String) = this.replace(target, File.separator)

inline fun <reified T> readTestClasspath(): String {
    val resource = T::class.java.getResourceAsStream("${File.separator}plugin-under-test-metadata.properties")
    checkNotNull(resource) {
        "Did not find plugin classpath resource, run `testClasses` build task."
    }

    val properties = Properties()
    properties.load(resource)

    /* So that there's no need to assume the classpath */
    return properties.getProperty("implementation-classpath")
        .split(File.pathSeparator)
        .map { it.replace("\\", "\\\\") }
        .filter { it.endsWith("java/main") }
        .map { it.replace("java/main", "java/test") }
        .joinToString(", ") { "\"$it\"" }
        .asPlatformAgnosticPath()
}

inline fun <reified T : Any> T.buildGradleRunner(vararg args: String = arrayOf("build")): BuildResult = DefaultGradleRunner.create()
    .withProjectDir(
        T::class.memberProperties.find { it.name == "rootDir" }
            ?.apply { isAccessible = true }
            ?.let { it.get(this) as File }
    )
    .apply {
        val gradleVersion = System.getenv("GRADLE_VERSION")
        if (gradleVersion != null) {
            this.withGradleVersion(gradleVersion)
        }
    }
    .withPluginClasspath()
    .withArguments(
        *args,
        "--stacktrace",
        "--info"
    )
    .forwardOutput()
    .withDebug(true)
    .build()

inline fun <reified T : Any> T.buildGradleRunnerAndFail(vararg args: String = arrayOf("build")): BuildResult = DefaultGradleRunner.create()
    .withProjectDir(
        T::class.memberProperties.find { it.name == "rootDir" }
            ?.apply { isAccessible = true }
            ?.let { it.get(this) as File }
    )
    .apply {
        val gradleVersion = System.getenv("GRADLE_VERSION")
        if (gradleVersion != null) {
            this.withGradleVersion(gradleVersion)
        }
    }
    .withPluginClasspath()
    .withArguments(
        *args,
        "--stacktrace",
        "--info"
    )
    .forwardOutput()
    .withDebug(true)
    .buildAndFail()
