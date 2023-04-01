package io.github.leofuso.argo.plugin.navis.security.credentials

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig.*
import org.apache.kafka.common.config.ConfigException
import org.gradle.api.NonExtensible
import org.gradle.api.provider.Provider
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

    fun Provider<String>.required(name: String): Pair<String, String> = orNull
        ?.let {
            if (it.isBlank()) {
                throw ConfigException("The ${javaClass.simpleName} configuration option $it value must not be blank.")
            }
            name to it
        }
        ?: throw ConfigException("The ${javaClass.simpleName} configuration option $name value is required.")

    fun Provider<String>.nonRequired(name: String): Pair<String, String>? = orNull
        ?.let {
            if (it.isBlank()) {
                throw ConfigException("The ${javaClass.simpleName} configuration option $name value must not be blank.")
            }
            return name to it
        }

}

interface BasicAuthCredentials : Credentials {

    @Internal
    override fun toProperties(): MutableMap<String, String> = mutableMapOf(BASIC_AUTH_CREDENTIALS_SOURCE to getAlias())

}

interface BearerAuthCredentials : Credentials {

    @Internal
    override fun toProperties(): MutableMap<String, String> = mutableMapOf(BEARER_AUTH_CREDENTIALS_SOURCE to getAlias())

}
