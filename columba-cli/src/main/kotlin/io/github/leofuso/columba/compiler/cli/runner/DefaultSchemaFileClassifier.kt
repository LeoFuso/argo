package io.github.leofuso.columba.compiler.cli.runner

import io.github.leofuso.columba.compiler.cli.CompileCommandRunner
import io.github.leofuso.columba.compiler.cli.IDL_EXTENSION
import io.github.leofuso.columba.compiler.cli.PROTOCOL_EXTENSION
import io.github.leofuso.columba.compiler.cli.SCHEMA_EXTENSION
import java.io.File

class DefaultSchemaFileClassifier(runner: CompileCommandRunner) : SchemaFileClassifier {

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
            logger.lifecycle("Ignoring non-existent definition file at [${source.path}].")
            return
        }

        val notReadable = source.canRead().not()
        if (notReadable) {
            logger.lifecycle("Ignoring unreadable definition file at [${source.path}].")
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
                logger.lifecycle("Unsupported IDL files.")
            }

            else -> logger.lifecycle(
                "Ignoring a potential definition file having an unknown extension at [${source.path}]."
            )
        }
    }
}
