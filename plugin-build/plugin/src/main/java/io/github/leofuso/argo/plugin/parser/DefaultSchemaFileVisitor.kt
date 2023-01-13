package io.github.leofuso.argo.plugin.parser

import io.github.leofuso.argo.plugin.PROTOCOL_EXTENSION
import io.github.leofuso.argo.plugin.SCHEMA_EXTENSION
import io.github.leofuso.argo.plugin.parser.SchemaFileVisitor.FileClassification
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.logging.Logger
import java.io.File

class DefaultSchemaFileVisitor(private val logger: Logger) : SchemaFileVisitor {

    private val schemaQueue = mutableListOf<File>()
    private val protocolQueue = mutableListOf<File>()

    override fun dequeue(): Map<FileClassification, Collection<File>> {
        val queue = mapOf(
            FileClassification.Schema to schemaQueue.toList(),
            FileClassification.Protocol to protocolQueue.toList()
        )
        schemaQueue.clear()
        protocolQueue.clear()
        return queue
    }

    /**
     * Visits a directory.
     *
     * @param details Meta-info about the directory.
     */
    override fun visitDir(details: FileVisitDetails) {}

    /**
     * Visits a file.
     *
     * @param details Meta-info about the file.
     */
    override fun visitFile(details: FileVisitDetails) {
        val source = details.file

        val doesntExists = source.exists().not()
        if (doesntExists) {
            logger.warn("Ignoring inexistent definition file at [${source.path}].")
            return
        }

        val notReadable = source.canRead().not()
        if (notReadable) {
            logger.warn("Ignoring unreadable definition file at [${source.path}].")
            return
        }

        when (source.extension) {
            SCHEMA_EXTENSION -> {
                if (logger.isDebugEnabled) {
                    logger.debug("Adding ${source.name} to Schema queue. Path[${source.path}]")
                }
                schemaQueue.add(source)
            }

            PROTOCOL_EXTENSION -> {
                if (logger.isDebugEnabled) {
                    logger.debug("Adding ${source.name} to Protocol queue. Path[${source.path}]")
                }
                protocolQueue.add(source)
            }

            else -> logger.quiet("Ignoring a potential defitinion file having an unknown extension at [${source.path}].")
        }
    }
}
