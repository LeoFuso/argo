package io.github.leofuso.argo.plugin.tasks

import org.apache.avro.SchemaParseException
import org.apache.avro.compiler.specific.SpecificCompiler
import org.gradle.execution.commandline.TaskConfigurationException
import java.io.File
import java.util.regex.Pattern


class SchemaCaretaker(private val factory: (File) -> SpecificCompiler, collection: Collection<File>) :
    Iterator<Pair<File, SpecificCompiler>> {

    val undefinedType = Pattern.compile("(?i).*(undefined name|not a defined name|type not supported).*")
    val duplicatedType = Pattern.compile("Can't redefine: (.*)")

    private val queue: ArrayDeque<File>
    private val iterator: MutableListIterator<File>

    private val retries: Map<String, Int>

    init {
        queue = ArrayDeque(collection.sortedBy { it.name })
        iterator = queue.listIterator()
        retries = mutableMapOf()
    }

    override fun hasNext() = iterator.hasNext()
    override fun next(): Pair<File, SpecificCompiler> {

        val source = iterator.next()
        return runCatching { factory.invoke(source) }
            .fold(

                onFailure = { throwable ->

                    expectedCatch(throwable)

                    val currentSize = queue.size
                    retries.getOrDefault(source.name, 0)
                        .let { retries + (source.name to it + 1) }

                    if(currentSize < retries.getOrDefault(source.name, 0)) {
                        TODO("Throw unresolved value exception... or maybe just ignore?")
                    }

                    val hasNext = iterator.hasNext()
                    if (hasNext) {
                        queue.addLast(source)
                        return next()
                    } else {
                        throw throwable
                    }
                },
                onSuccess = { compiler ->
                    iterator.remove()
                    source to compiler
                }
            )
    }

    private fun expectedCatch(ex: Throwable) {
        when {
            ex is TaskConfigurationException
                && ex.cause is SchemaParseException
                && undefinedType.asPredicate().test((ex.cause as SchemaParseException).message) -> Unit
            else -> throw ex
        }
    }
}
