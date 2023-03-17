package io.github.leofuso.columba.cli.fixtures

import com.speedment.common.combinatorics.Permutation
import org.junit.jupiter.api.DynamicTest
import java.io.File
import java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE

fun loadResource(path: String): File {
    val walker = StackWalker.getInstance(RETAIN_CLASS_REFERENCE)
    val callerClass = walker.callerClass
    val classLoader = callerClass.classLoader

    val resource = classLoader.getResource(path)
        ?: throw IllegalArgumentException("Unnable to resolve the resource at [$path]")

    val uri = resource.toURI()
    return File(uri)
}

fun permutations(vararg files: File): Map<String, List<File>> {
    return Permutation.of(*files)
        .map { permutation ->

            val sources = permutation.toList()
            val key = sources.joinToString(", ", "[ ", " ]") { file -> file.name }

            key to sources
        }
        .toList()
        .toMap()
}

fun scenarios(path: String, executable: (List<File>) -> Unit) = scenarios(loadResource(path), executable)

fun scenarios(directory: File, executable: (List<File>) -> Unit) = permutations(*directory.listFiles() ?: emptyArray())
    .map {
        DynamicTest.dynamicTest("Scenario ${it.key}") {
            executable.invoke(it.value)
        }
    }
    .toList()
