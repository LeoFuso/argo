package io.github.leofuso.argo.plugin.navis.security.credentials

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig.*
import org.apache.kafka.common.config.ConfigException
import org.gradle.api.NonExtensible
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal

/**
 * Base interface for credentials used for different authentication strategies against the Schema Registry API.
 *
 * A user can populate this credential by any of these strategies:
 *
 *  1. Populating a `gradle.config` file, either in the project or in the Gradle directory, with the needed properties;
 *  2. Passing the needed properties as project variables to Gradle, e.g. `--project-prop schema.registry.bearer.auth.token=token`;
 *  2. Using the DSL to manually populate this credential;
 *
 * &nbsp;
 *
 * Each [Credentials] has its own required parameters.
 * To check how to configure it, please refer to [child's][Credentials] documentation.
 *
 * &nbsp;
 * &nbsp;
 *
 * All child [Credentials] properties are expected to be prefixed like so:
 *
 * `schema.registry.____ = `[props][Credentials.toProperties]
 *
 *  &nbsp;
 *  &nbsp;
 *
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

/**
 * Supports 'basic auth' [Credentials].
 */
interface BasicAuthCredentials : Credentials {

    @Internal
    override fun toProperties(): MutableMap<String, String> = mutableMapOf(BASIC_AUTH_CREDENTIALS_SOURCE to getAlias())

}

/**
 * Supports 'bearer auth' [Credentials].
 */
interface BearerAuthCredentials : Credentials {

    @Internal
    override fun toProperties(): MutableMap<String, String> = mutableMapOf(BEARER_AUTH_CREDENTIALS_SOURCE to getAlias())

}
