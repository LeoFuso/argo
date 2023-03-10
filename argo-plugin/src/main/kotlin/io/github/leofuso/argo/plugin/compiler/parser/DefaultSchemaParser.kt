package io.github.leofuso.argo.plugin.compiler.parser

import io.github.leofuso.argo.plugin.SCHEMA_EXTENSION
import io.github.leofuso.argo.plugin.compiler.parser.DependencyGraphAwareSchemaParser.Schemas
import org.apache.avro.Schema
import org.apache.avro.SchemaParseException
import org.gradle.api.logging.Logger
import java.io.File
import java.util.regex.Pattern

class DefaultSchemaParser(private val logger: Logger) : DependencyGraphAwareSchemaParser {

    private val undefinedPattern = Pattern.compile("(?i).*(undefined name|not a defined name|type not supported).*")
    private val duplicatedPattern = Pattern.compile("Can't redefine: (.*)")

    override fun doParse(schemas: Schemas): Map<String, Schema> {

        /* Holding all unresolved resolutions between iterations */
        val unresolved = ArrayDeque<File>(schemas.elements.size)

        /* A temporary queue used within the iteration */
        val queue = ArrayDeque(schemas.elements)
        val iterator = queue.listIterator()
        val definitions = mutableMapOf<String, Schema>()

        do {
            val initialDefinitions = definitions.size

            queue.addAll(unresolved)
            while (iterator.hasNext()) {
                val source = iterator.next()
                val types = definitions.toMap()
                val tempParser = Schema.Parser()
                    .addTypes(types)

                /* Either resolved or re-enqueued */
                iterator.remove()
                val definition = findSchema(tempParser, source, unresolved)
                if (definition != null) {
                    unresolved.remove(source)
                    definitions += definition
                }
            }

            /*
             * Either finished or there are non-resolvable types remaining.
             * If definition number doesn't change between iterations, there's nothing more to be done.
             */
            val foundDefinitions = definitions.size

        } while (unresolved.isNotEmpty() && initialDefinitions != foundDefinitions)

        if (unresolved.isNotEmpty()) {
            val files = unresolved.map(File::getPath).reduce { acc, next -> "$acc; $next" }
            logger.lifecycle(
                "{} Schema(.{}) definition files remaining to be parsed. Files [{}].",
                unresolved.size,
                SCHEMA_EXTENSION,
                files
            )
            logger.lifecycle("Run in debug mode (-d or --debug) to get more details.")
        }
        return definitions.toMap()
    }

    private fun findSchema(parser: Schema.Parser, source: File, queue: ArrayDeque<File>): Map<String, Schema>? {
        val conflictResolution = SchemaConflictResolution(logger, duplicatedPattern, parser.types)

        try {
            parser.parse(source)
            return parser.types
        } catch (ex: SchemaParseException) {
            val errorMessage = ex.message ?: "unknown"
            val undefinedMatcher = undefinedPattern.matcher(errorMessage)
            val duplicatedMatcher = duplicatedPattern.matcher(errorMessage)

            val path = source.path
            return when {
                duplicatedMatcher.matches() -> conflictResolution.resolve(duplicatedMatcher.group(1), source)
                undefinedMatcher.matches() -> {
                    if (logger.isDebugEnabled) {
                        logger.debug("Found undefined name at [{}] ({}); will try again.", path, errorMessage)
                    }
                    val notEnqueued = queue.contains(source).not()
                    if (notEnqueued) {
                        queue.addLast(source)
                    }
                    null
                }

                else -> {
                    val unknownErrorMessage =
                        "Unexpected error while parsing Schema(.{}) definition at [{}]."
                    logger.error(unknownErrorMessage, SCHEMA_EXTENSION, path, ex)
                    null
                }
            }
        }
    }

    override fun getVisitor() = DefaultSchemaFileVisitor(logger)
    override fun logger() = logger
}
