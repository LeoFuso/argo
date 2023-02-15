package io.github.leofuso.argo.plugin.compiler.parser

import org.apache.avro.Protocol
import org.apache.avro.Schema
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitor
import org.gradle.api.logging.Logger
import java.io.File

/**
 * A **DependencyGraphAwareSchemaParser** is a [Parser][org.apache.avro.Schema.Parser] capable of producing
 * [Schemas][org.apache.avro.Schema] and interpreting [Protocols][org.apache.avro.Protocol] by resolving the dependency
 * graph up to *N* degrees without the need of **pre-inlining** them. This parser achieves this by transversing the
 * file structure in depth-first order, resolving Schemas with O(n²) iterations, where (n) is the number of Schema
 * definition files.
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

        logger().lifecycle("Commencing Source file parsing.")

        val visitor = getVisitor()
        graph.visit(visitor)

        val emptyResolution = Resolution(schemas = mapOf(), protocol = mapOf())

        val queue = visitor.dequeue()
        return queue.map { entry ->
            when (entry.key) {
                SchemaFileVisitor.FileClassification.Schema -> {
                    logger().lifecycle("Found {} Schema's definition files to be resolved.", entry.value.size)
                    val schemas = Schemas(entry.value)
                    val resolution = doParse(schemas)
                    SchemaResolution(resolution)
                }

                SchemaFileVisitor.FileClassification.Protocol -> {
                    logger().lifecycle("Found {} Protocol's definition files to be resolved.", entry.value.size)
                    val protocols = Protocols(entry.value)
                    val resolution = doParse(protocols)
                    ProtocolResolution(resolution)
                }
            }
        }.fold(emptyResolution) { res, some ->
            when (some) {
                is ProtocolResolution -> res.copy(protocol = some.protocol)
                is SchemaResolution -> res.copy(schemas = some.schema)
            }
        }
    }

    fun doParse(schemas: Schemas): Map<String, Schema>

    fun doParse(protocols: Protocols): Map<String, Protocol> = protocols.elements.associate {
        val protocol = Protocol.parse(it)
        protocol.name to protocol
    }

    /**
     * @return A [SchemaFileVisitor].
     */
    fun getVisitor(): SchemaFileVisitor

    /**
     * Accessor to underlying [logger] implementation.
     */
    fun logger(): Logger

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
data class Resolution(val schemas: Map<String, Schema>, val protocol: Map<String, Protocol>)

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
