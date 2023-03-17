package io.github.leofuso.columba.cli.runner

import io.github.leofuso.columba.cli.IDL_EXTENSION
import io.github.leofuso.columba.cli.PROTOCOL_EXTENSION
import io.github.leofuso.columba.cli.SCHEMA_EXTENSION
import io.github.leofuso.columba.cli.command.CompileCommand
import java.io.File

class DefaultSchemaFileClassifier(runner: CompileCommand) : SchemaFileClassifier {

    private val logger = runner.logger

    private val sources = runner.source
    private val schemaQueue = mutableListOf<File>()
    private val protocolQueue = mutableListOf<File>()

    override fun dequeue(): Map<SchemaFileClassifier.FileClassification, Collection<File>> {

        sources.forEach(::visitFile)

        val queue = mapOf(
            SchemaFileClassifier.FileClassification.Schema to schemaQueue.toList(),
            SchemaFileClassifier.FileClassification.Protocol to protocolQueue.toList()
        )
        schemaQueue.clear()
        protocolQueue.clear()

        return queue
    }

    private fun visitFile(source: File) {
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
}
