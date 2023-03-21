package io.github.leofuso.argo.plugin.fixtures

import com.speedment.common.combinatorics.Permutation
import io.github.leofuso.argo.plugin.asPlatformAgnosticPath
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.junit.jupiter.api.DynamicTest
import java.io.File
import java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE
import java.util.*
import java.util.stream.Collectors

fun loadResource(path: String): File {
    val walker = StackWalker.getInstance(RETAIN_CLASS_REFERENCE)
    val callerClass = walker.callerClass
    val classLoader = callerClass.classLoader

    val sysPath = path.asPlatformAgnosticPath()
    val resource = classLoader.getResource(sysPath)
        ?: throw IllegalArgumentException("Unnable to resolve the resource at [$sysPath]")

    val uri = resource.toURI()
    return File(uri)
}

fun permutations(project: Project, vararg files: File): Map<String, FileTree> {
    return Permutation.of(*files)
        .map { permutation ->

            val uuid = UUID.randomUUID()
            val directory = project.mkdir("permutation-$uuid").toPath().toFile()

            val key = permutation.map { file ->
                val name = file.name
                val target = File(directory, name)
                file.copyTo(target)
                name
            }.collect(Collectors.joining(", ", "[ ", " ]"))

            key to project.objects.fileCollection().from(directory).asFileTree
        }
        .toList()
        .toMap()
}

fun permutations(project: Project, tree: FileTree): Map<String, FileTree> {
    return permutations(project, *tree.files.toTypedArray())
}

fun scenarios(project: Project, tree: FileTree, executable: (FileTree) -> Unit) = permutations(project, tree)
    .map {
        DynamicTest.dynamicTest("Scenario ${it.key}") {
            executable.invoke(it.value)
        }
    }
    .toList()
