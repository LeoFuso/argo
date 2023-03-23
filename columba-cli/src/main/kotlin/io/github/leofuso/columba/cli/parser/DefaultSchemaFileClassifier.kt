package io.github.leofuso.columba.cli.parser

import io.github.leofuso.columba.cli.*
import java.io.File

class DefaultSchemaFileClassifier(private val logger: ConsoleLogger) : SchemaFileClassifier {

    override fun classify(files: Collection<File>): Map<SchemaFileClassifier.FileClassification, Collection<File>> {

        val schemaQueue = mutableListOf<File>()
        val protocolQueue = mutableListOf<File>()

        fun visitFile(source: File) {
            val doesntExists = source.exists().not()
            if (doesntExists) {
                logger.info("Ignoring non-existent definition file at [${source.path}].")
                return
            }

            val notReadable = source.canRead().not()
            if (notReadable) {
                logger.info("Ignoring unreadable definition file at [${source.path}].")
                return
            }

            when (source.extension) {
                SCHEMA_EXTENSION -> {
                    logger.info("Adding ${source.name} to Schema queue. Path[${source.path}]")
                    schemaQueue.add(source)
                }

                PROTOCOL_EXTENSION -> {
                    logger.info("Adding ${source.name} to Protocol queue. Path[${source.path}]")
                    protocolQueue.add(source)
                }

                IDL_EXTENSION -> {
                    logger.warn("Unsupported IDL files.")
                }

                else -> logger.warn(
                    "Ignoring a potential definition file having an unknown extension at [${source.path}]."
                )
            }
        }

        files.forEach(::visitFile)
        return mapOf(
            SchemaFileClassifier.FileClassification.Schema to schemaQueue.toList(),
            SchemaFileClassifier.FileClassification.Protocol to protocolQueue.toList()
        )
    }
}
