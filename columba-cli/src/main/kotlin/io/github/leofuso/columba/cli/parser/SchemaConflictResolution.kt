package io.github.leofuso.columba.cli.parser

import io.github.leofuso.columba.cli.ConsoleLogger
import io.github.leofuso.columba.cli.SCHEMA_EXTENSION
import io.github.leofuso.columba.cli.exceptions.NonDeterministicSchemaResolutionException
import org.apache.avro.Schema
import org.apache.avro.SchemaParseException
import java.io.File
import java.util.regex.Pattern

class SchemaConflictResolution(private val logger: ConsoleLogger, private val pattern: Pattern, discoveredTypes: Map<String, Schema>) {

    private val types: MutableMap<String, Schema> = mutableMapOf()

    init {
        types += discoveredTypes
    }

    fun resolve(namespace: String, source: File): Map<String, Schema> {
        logger.warn("Found duplicated Schema definitions. Run in info mode (-i or --info) to get more information.")
        val conflictResolution = doResolve(namespace, source, types)
        types.forEach { (name, schema) ->
            val undefined = conflictResolution[name] != schema
            if (undefined) {
                val conflictingMessage = "Found conflicting Schema definitions [$name] at [${source.path}]."
                throw NonDeterministicSchemaResolutionException(conflictingMessage)
            }
            logger.info(
                "Ignoring duplicated Schema definition [{}] at [{}].",
                name,
                source.path
            )
        }
        return types + conflictResolution
    }

    private fun doResolve(namespace: String, source: File, accTypes: Map<String, Schema>): MutableMap<String, Schema> {
        val discoveredTypes = mutableMapOf<String, Schema>() + accTypes - namespace
        val auxiliarParser = Schema.Parser()
            .addTypes(discoveredTypes)

        try {
            auxiliarParser.parse(source)
            return auxiliarParser.types
        } catch (ex: SchemaParseException) {
            val matcher = pattern.matcher(ex.message ?: "unknown")
            return when {
                matcher.matches() && matcher.group(1) == namespace -> {
                    val duplicatedMessage = "Duplicated Schema definition in a single file [$namespace] at [${source.path}]."
                    throw NonDeterministicSchemaResolutionException(duplicatedMessage)
                }

                matcher.matches() -> doResolve(matcher.group(1), source, discoveredTypes)
                else -> {
                    val unknownErrorMessage =
                        "Unexpected error while parsing Schema(.$SCHEMA_EXTENSION) definition at [${source.path}]."
                    throw NonDeterministicSchemaResolutionException(unknownErrorMessage, ex)
                }
            }
        }
    }
}
