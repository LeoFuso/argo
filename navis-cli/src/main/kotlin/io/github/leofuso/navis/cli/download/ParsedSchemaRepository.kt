package io.github.leofuso.navis.cli.download

import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.github.leofuso.navis.cli.ConsoleLogger
import java.util.regex.Pattern

object ParsedSchemaSupplier {
    fun get(client: SchemaRegistryClient, logger: ConsoleLogger): ParsedSchemaRepository = DefaultParsedSchemaRepository(client, logger)
}

interface ParsedSchemaRepository {

    fun findLatestBySubject(subject: String) : ParsedSchema?

    fun findBySubjectAndVersion(subject: String, version: Int) : ParsedSchema?

    fun fetchByPattern(pattern: Pattern) : Set<ParsedSchema>

}
