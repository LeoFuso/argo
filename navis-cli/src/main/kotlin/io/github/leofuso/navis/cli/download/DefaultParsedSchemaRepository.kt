package io.github.leofuso.navis.cli.download

import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.client.SchemaMetadata
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.github.leofuso.navis.cli.ConsoleLogger
import java.util.regex.Pattern
import kotlin.jvm.optionals.getOrNull

class DefaultParsedSchemaRepository(private val client: SchemaRegistryClient, private val logger: ConsoleLogger) : ParsedSchemaRepository {

    private val cache : Set<String> = mutableSetOf()

    override fun findLatestBySubject(subject: String): ParsedSchema? = doDownload(subject, null)

    override fun findBySubjectAndVersion(subject: String, version: Int): ParsedSchema? = doDownload(subject, version)

    override fun fetchByPattern(pattern: Pattern): Set<ParsedSchema> {
        client.allSubjects.also { cache + it }
        return cache.mapNotNull { subject -> doDownload(subject, null) }.toSet()
    }

    private fun doDownload(subject: String, version: Int?): ParsedSchema? {

        fun fetchMetadata(subject: String, version: Int?): SchemaMetadata =
            if (version == null) {
                client.getLatestSchemaMetadata(subject)
            } else {
                client.getSchemaMetadata(subject, version)
            }

        return runCatching { fetchMetadata(subject, version) }
            .mapCatching {
                val parsedSchema = client.parseSchema(it.schemaType, it.schema, it.references)
                if(parsedSchema.isEmpty && logger.isInfoEnabled()) {
                    logger.info("Empty result while parsing Schema {} with version {}", it.id, it.version)
                }
                parsedSchema.getOrNull()
            }
            .recoverCatching { exception: Throwable ->
                logger.error("{}. Unable to find metadata for Subject {}: {}", exception.javaClass.simpleName, subject, exception.message)
                null
            }
            .getOrNull()
    }
}
