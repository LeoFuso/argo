package io.github.leofuso.argo.plugin.navis.security.credentials

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal

/**
 * A static token used to authenticate against an external OAuth service.
 *
 * Expected properties:
 *
 * &nbsp;
 *
 * `schema.registry.bearer.auth.token = ` [token][StaticTokenCredentials.getToken]
 *
 * &nbsp;
 *
 */
abstract class StaticTokenCredentials : BearerAuthCredentials {

    @Internal
    abstract fun getToken(): Property<String>

    fun token(value: String) = getToken().set(value)

    override fun toProperties(): MutableMap<String, String> {
        val source = super.toProperties()
        getToken().required(SchemaRegistryClientConfig.BEARER_AUTH_TOKEN_CONFIG).let(source::plusAssign)
        return source
    }

    @Internal
    override fun getAlias() = "STATIC_TOKEN"

}
