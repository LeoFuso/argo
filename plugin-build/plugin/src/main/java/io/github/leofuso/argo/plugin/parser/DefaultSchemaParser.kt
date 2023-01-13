package io.github.leofuso.argo.plugin.parser

import io.github.leofuso.argo.plugin.SCHEMA_EXTENSION
import io.github.leofuso.argo.plugin.parser.DependencyGraphAwareSchemaParser.Schemas
import org.apache.avro.Schema
import org.apache.avro.SchemaParseException
import org.gradle.api.logging.Logger
import java.io.File
import java.util.regex.Pattern

class DefaultSchemaParser(private val logger: Logger) : DependencyGraphAwareSchemaParser {

    private val undefinedPattern = Pattern.compile("(?i).*(undefined name|not a defined name|type not supported).*")
    private val duplicatedPattern = Pattern.compile("Can't redefine: (.*)")

    override fun doParse(schemas: Schemas): Map<String, Schema> {

        val queue = ArrayDeque(schemas.elements)
        val iterator = queue.listIterator()

        val definitions = mutableMapOf<String, Schema>()

        do {

            val initialDefinitions = definitions.size
            while (iterator.hasNext()) {

                val source = iterator.next()
                val types = definitions.toMap()
                val tempParser = Schema.Parser()
                    .addTypes(types)

                /* Either resolved or re-enqueued */
                iterator.remove()
                val definition = findSchema(tempParser, source, queue)
                if (definition != null) {
                    definitions += definition
                }
            }
            val foundDefinitions = definitions.size

            /* Either finished or there are unresolveable types remaining. */
        } while (queue.isNotEmpty() && initialDefinitions != foundDefinitions)

        if (queue.isNotEmpty()) {
            val files = queue.map(File::getPath).reduce { acc, next -> "$acc; $next" }
            logger.lifecycle(
                "{} Schema(.{}) definition files remaining to be parsed. Files [{}].",
                queue.size,
                SCHEMA_EXTENSION,
                files
            )
            logger.lifecycle("Run in debug mode (-d or --debug) to more details.")
        }
        return definitions.toMap()
    }

    private fun findSchema(
        parser: Schema.Parser,
        source: File,
        queue: ArrayDeque<File>
    ): Pair<String, Schema>? {
        try {
            val schema = parser.parse(source)
            val key = schema.fullName
            return (key to schema)
        } catch (ex: SchemaParseException) {

            val errorMessage = ex.message ?: "unknown"
            val undefinedMatcher = undefinedPattern.matcher(errorMessage)
            val duplicatedMatcher = duplicatedPattern.matcher(errorMessage)

            val path = source.path
            when {

                undefinedMatcher.matches() -> {
                    if (logger.isDebugEnabled) {
                        logger.debug("Found undefined name at [{}] ({}); will try again.", path, errorMessage)
                    }
                    queue.addLast(source)
                }

                duplicatedMatcher.matches() -> {
                    if (logger.isDebugEnabled) {
                        logger.debug(
                            "Ignoring duplicated Schema definition [{}] at [{}].",
                            duplicatedMatcher.group(1),
                            path
                        )
                    }
                }

                else -> {
                    val unknownErrorMessage =
                        "Unexpected error while parsing Schema(.{}) definition at [{}]."
                    logger.error(unknownErrorMessage, SCHEMA_EXTENSION, path, ex)
                }
            }
            return null
        }
    }

    override fun getVisitor() = DefaultSchemaFileVisitor(logger)
}
