package io.github.leofuso.argo.plugin.parser

import io.github.leofuso.argo.plugin.PROTOCOL_EXTENSION
import io.github.leofuso.argo.plugin.SCHEMA_EXTENSION
import io.github.leofuso.argo.plugin.assertTrue
import org.apache.avro.Schema
import org.apache.avro.SchemaParseException
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.logging.Logger
import java.io.File
import java.nio.file.Path
import java.util.regex.Pattern

class ParserFactory(val logger: Logger) {

    private val queue: ArrayDeque<File> = ArrayDeque()
    private val iterator = queue.listIterator()

    private val undefinedType = Pattern.compile("(?i).*(undefined name|not a defined name|type not supported).*")
    private val duplicatedType = Pattern.compile("Can't redefine: (.*)")

    private val types = mutableMapOf<String, RecordSchemaType>()

    private fun flushQueue(): Boolean {

        val hasNext = iterator.hasNext()
        iteration@ while(hasNext) {

            val initialSize = types.size
            val source = iterator.next()
            configSchemaType(source)
            val newSize = types.size

            if(initialSize == newSize) {
                break@iteration
            } else {
                iterator.remove()
            }
        }
        return queue.isEmpty()
    }

    private fun doGetParser() = Schema.Parser().addTypes(types.map { it.key to it.value.schema }.toMap())

    fun getParser(): Schema.Parser {
        flushQueue()
        return doGetParser()
    }

    private fun configSchemaType(source: File) {

        try {

            val parser = doGetParser()
            val schema = parser.parse(source)

            val key = schema.fullName
            val path = source.toPath()

            val recordSchemaType = RecordSchemaType(path, schema)
            types + (key to recordSchemaType)

        } catch (ex: SchemaParseException) {

            val errorMessage = ex.message ?: throw ex
            val undefinedMatcher = undefinedType.matcher(errorMessage)
            val duplicatedMatcher = duplicatedType.matcher(errorMessage)

            when {
                undefinedMatcher.matches() -> queue.addLast(source)
                duplicatedMatcher.matches() -> {
                    logger.warn("Ignoring duplicated Schema definition [${duplicatedMatcher.group(1)}] at ${source.name}")
                }
            }
        }
    }

    fun getVisitor() = object : FileVisitor {

        override fun visitDir(ignored: FileVisitDetails) {}

        override fun visitFile(details: FileVisitDetails) {
            when (val extension = details.file.extension) {
                SCHEMA_EXTENSION -> configSchemaType(details.file)
                PROTOCOL_EXTENSION -> logger.warn("Unsupported extension ($extension).")
                else -> logger.lifecycle("Visiting unknown source-file: $extension.")
            }
        }
    }
}

data class RecordSchemaType(val location: Path, val schema: Schema) {
    init {
        assertTrue(schema.type == Schema.Type.RECORD)
    }
}
