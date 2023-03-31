package io.github.leofuso.argo.plugin.navis.security.credentials

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig.*
import org.gradle.api.NonExtensible
import org.gradle.api.tasks.Internal

/**
 * Base interface for credentials used for different authentication strategies against the Schema Registry API.
 */
@NonExtensible
interface Credentials {

    @Internal
    fun getAlias(): String

    @Internal
    fun toProperties(): MutableMap<String, String>

}

interface BasicAuthCredentials : Credentials {

    @Internal
    override fun toProperties(): MutableMap<String, String> = mutableMapOf(BASIC_AUTH_CREDENTIALS_SOURCE to getAlias())

}

interface BearerAuthCredentials : Credentials {

    @Internal
    override fun toProperties(): MutableMap<String, String> = mutableMapOf(BEARER_AUTH_CREDENTIALS_SOURCE to getAlias())

}
