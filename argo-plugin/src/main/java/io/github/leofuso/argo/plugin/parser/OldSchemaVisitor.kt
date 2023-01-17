package io.github.leofuso.argo.plugin.parser

import io.github.leofuso.argo.plugin.SCHEMA_EXTENSION
import io.github.leofuso.argo.plugin.assertTrue
import org.apache.avro.Schema
import org.apache.avro.SchemaParseException
import org.apache.avro.compiler.specific.SpecificCompiler
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
import java.io.File
import java.util.regex.Pattern

data class SchemaDefinition(val source: File, val schema: Schema) {
    init {
        assertTrue(schema.type == Schema.Type.RECORD)
    }
}

class OldSchemaVisitor(private val logger: Logger) {

    private val undefinedPattern = Pattern.compile("(?i).*(undefined name|not a defined name|type not supported).*")
    private val duplicatedPattern = Pattern.compile("Can't redefine: (.*)")

    fun visit(tree: FileTree): Map<String, SchemaDefinition> {

        val queue = ArrayDeque<File>()
        val iterator = queue.listIterator()

        tree.matching { pattern -> pattern.include("**/*.$SCHEMA_EXTENSION") }
            .visit { details ->
                val isDirectory = details.isDirectory
                if (isDirectory) {
                    return@visit
                }
                logger.lifecycle("Enqueue [{}] for Schema resolution.", details.name)
                queue.addFirst(details.file)
            }

        val definitions = mutableMapOf<String, SchemaDefinition>()

        do {

            val initialDefinitions = definitions.size
            while (iterator.hasNext()) {

                val source = iterator.next()
                val types = definitions.map { it.key to it.value.schema }.toMap()
                val tempParser = Schema.Parser()
                    .addTypes(types)

                iterator.remove()
                val definition = maybeParse(tempParser, source, queue)
                if (definition != null) {
                    definitions += definition
                }
            }
            val foundDefinitions = definitions.size
        } while (queue.isNotEmpty() && initialDefinitions != foundDefinitions)
        return definitions.toMap()
    }

    private fun maybeParse(
        parser: Schema.Parser,
        source: File,
        queue: ArrayDeque<File>
    ): Pair<String, SchemaDefinition>? {
        try {

            val schema = parser.parse(source)
            val key = schema.fullName

            val definition = SchemaDefinition(source, schema)
            return (key to definition)
        } catch (ex: SchemaParseException) {

            val errorMessage = ex.message ?: "unknown"
            val undefinedMatcher = undefinedPattern.matcher(errorMessage)
            val duplicatedMatcher = duplicatedPattern.matcher(errorMessage)

            when {
                undefinedMatcher.matches() -> queue.addLast(source)
                duplicatedMatcher.matches() -> {
                    logger.warn("Ignoring Schema duplicated definition [${duplicatedMatcher.group(1)}] at ${source.name}")
                }

                else -> {
                    logger.error("Unexpected error while parsing Schema(.$SCHEMA_EXTENSION) file: ${source.name}", ex)
                }
            }
            return null
        }
    }

    fun doCompile(
        definitions: Map<String, SchemaDefinition> = mapOf(),
        destination: File,
        config: (SpecificCompiler) -> Unit
    ) {
        definitions.forEach { (key, definition) ->
            logger.lifecycle("Running SpecificCompiler [{}].", key)
            val source = definition.source
            val schema = definition.schema
            val compiler = SpecificCompiler(schema)
            config.invoke(compiler)
            compiler.compileToDestination(source, destination)
        }
    }
}
