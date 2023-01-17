package io.github.leofuso.argo.plugin.parser

import io.github.leofuso.argo.plugin.parser.SchemaFileVisitor.FileClassification.*
import org.apache.avro.Protocol
import org.apache.avro.Schema
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitor
import java.io.File

/**
 * A **DependencyGraphAwareSchemaParser** is a [Parser][org.apache.avro.Schema.Parser] capable of producing
 * [Schemas][org.apache.avro.Schema] and interpreting [Protocols][org.apache.avro.Protocol] by resolving the dependency
 * graph up to *N* degrees without the need of **pre-inlining** them. This parser achieves this by transversing the
 * file structure in depth-first order, and resolving any found Schemas lazily.
 *
 * **Notes:**
 *
 * 1. Repeated declarations do not imply runtime errors. They are solved by whoever appears first.
 * 2. It is not an ideal strategy in any way, it doesn't need to be. This parser should be used only
 * in the context of this application.
 * 3. The name is a mouthful.
 */
interface DependencyGraphAwareSchemaParser {

    /**
     * Parse all definition schemas from the provided [graph], returning all successful resolutions.
     */
    fun parse(graph: FileTree): Resolution {

        val visitor = getVisitor()
        graph.visit(visitor)

        val emptyResolution = Resolution(schema = mapOf(), protocol = mapOf())

        val queue = visitor.dequeue()
        return queue.map { entry ->
            when (entry.key) {
                Schema -> {
                    val schemas = Schemas(entry.value)
                    val resolution = doParse(schemas)
                    SchemaResolution(resolution)
                }

                Protocol -> {
                    val protocols = Protocols(entry.value)
                    val resolution = doParse(protocols)
                    ProtocolResolution(resolution)
                }
            }
        }.fold(emptyResolution) { res, some ->
            when (some) {
                is ProtocolResolution -> res.copy(protocol = some.protocol)
                is SchemaResolution -> res.copy(schema = some.schema)
            }
        }
    }

    fun doParse(schemas: Schemas): Map<String, Schema>

    fun doParse(protocols: Protocols): Map<String, Protocol> =
        protocols.elements.associate {
            val protocol = Protocol.parse(it)
            protocol.name to protocol
        }

    /**
     * @return A [SchemaFileVisitor].
     */
    fun getVisitor(): SchemaFileVisitor

    /**
     * A wrapper Schema(.avsc) files.
     */
    data class Schemas(val elements: Collection<File>)

    /**
     * A wrapper Protocol(.avpr) files.
     */
    data class Protocols(val elements: Collection<File>)
}

/**
 * A Resolution containing all successfully parsed definitions.
 */
data class Resolution(val schema: Map<String, Schema>, val protocol: Map<String, Protocol>)

/**
 * A [SchemaFileVisitor] is used to visit each of the definition files in a [FileTree].
 */
interface SchemaFileVisitor : FileVisitor {

    enum class FileClassification { Schema, Protocol }

    /**
     * Dequeue all found definitions.
     */
    fun dequeue(): Map<FileClassification, Collection<File>>
}

private sealed class SomeResolution(val schema: Map<String, Schema>, val protocol: Map<String, Protocol>)
private class ProtocolResolution(value: Map<String, Protocol>) : SomeResolution(schema = mapOf(), value)
private class SchemaResolution(value: Map<String, Schema>) : SomeResolution(value, protocol = mapOf())
