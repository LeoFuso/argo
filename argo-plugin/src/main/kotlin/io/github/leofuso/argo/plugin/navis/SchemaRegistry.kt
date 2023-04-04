package io.github.leofuso.argo.plugin.navis

import io.confluent.kafka.schemaregistry.SchemaProvider
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import org.gradle.kotlin.dsl.provideDelegate

interface SchemaRegistry {

    fun client(): CachedSchemaRegistryClient

    fun configure(schemaConfig: SchemaConfig)

    data class SchemaConfig(
        val url: String = "localhost:8081",
        val cacheCapacity: Int = 100,
        val providers: List<SchemaProvider> = listOf(AvroSchemaProvider()),
        val configs: Map<String, Any?> = mapOf(),
        val httpHeaders: Map<String, String> = mapOf()
    )
}

internal class SchemaRegistrySingleton : SchemaRegistry {

    private var _schemaConfig = SchemaRegistry.SchemaConfig()

    private val _client: CachedSchemaRegistryClient by lazy {
        CachedSchemaRegistryClient(
            listOf(_schemaConfig.url),
            _schemaConfig.cacheCapacity,
            _schemaConfig.providers,
            _schemaConfig.configs,
            _schemaConfig.httpHeaders
        )
    }

    override fun client() = _client

    override fun configure(schemaConfig: SchemaRegistry.SchemaConfig) = run { _schemaConfig = schemaConfig }

}

object GlobalSchemaRegistry: SchemaRegistry by SchemaRegistrySingleton()
