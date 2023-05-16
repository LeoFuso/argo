package io.github.leofuso.columba.cli.parser

import io.github.leofuso.columba.cli.ConsoleLogger
import io.github.leofuso.columba.cli.SCHEMA_EXTENSION
import org.apache.avro.Schema
import org.apache.avro.SchemaParseException
import java.io.File
import java.util.regex.Pattern

class DefaultSchemaParser(private val logger: ConsoleLogger) : DependencyGraphAwareSchemaParser {

    private val undefinedPattern = Pattern.compile(
        "(?i)(?<message>.*(undefined name|not a defined name|type not supported)*:)(?<subject>.*)"
    )
    private val duplicatedPattern = Pattern.compile("Can't redefine: (.*)")

    private val classifier = DefaultSchemaFileClassifier(logger)

    override fun doParse(schemas: DependencyGraphAwareSchemaParser.Schemas): Map<String, Schema> {

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
            logger.info(
                "{} Schema(.{}) definition files with unresolved dependencies. Files [{}].",
                unresolved.size,
                SCHEMA_EXTENSION,
                files
            )
            if (!logger.isInfoEnabled()) {
                logger.info(
                    "There are some Schema definition files remaining to be parsed. Run in info mode (-i or --info) to get more details."
                )
            }
        }
        return definitions.toMap()
    }

    override fun getClassifier() = classifier

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
                    logger.info("Found undefined name at [{}] ({}); enqueued to another try.", path, errorMessage)
                    val notEnqueued = queue.contains(source).not()
                    if (notEnqueued) {
                        queue.addLast(source)
                    }
                    null
                }

                else -> {
                    val unknownErrorMessage =
                        "Unexpected error while parsing Schema(.{}) definition at [{}]. {}"
                    logger.error(unknownErrorMessage, SCHEMA_EXTENSION, path, ex.message)
                    null
                }
            }
        }
    }

    override fun logger() = logger
}
