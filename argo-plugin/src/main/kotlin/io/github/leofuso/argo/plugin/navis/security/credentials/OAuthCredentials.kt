package io.github.leofuso.argo.plugin.navis.security.credentials

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig.*
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import java.net.URL
import java.time.Duration

/**
 * Used to authenticate against an external OAuth service.
 *
 * Expected properties:
 *
 * &nbsp;
 *
 * `schema.registry.bearer.auth.logical.cluster = ` [cluster][OAuthCredentials.getLogicalCluster]
 *
 * `schema.registry.bearer.auth.identity.pool.id = ` [poolId][OAuthCredentials.getIdentityPoolId]
 *
 * `schema.registry.bearer.auth.client.id = ` [clientId][OAuthCredentials.getClientId]
 *
 * `schema.registry.bearer.auth.client.secret = ` [clientSecret][OAuthCredentials.getClientSecret]
 *
 * `schema.registry.bearer.auth.scope = ` [scope][OAuthCredentials.getScope] (optional)
 *
 * `schema.registry.bearer.auth.issuer.endpoint.url = ` [issuer][OAuthCredentials.getIssuerEndpointUrl]
 *
 * `schema.registry.bearer.auth.cache.expiry.buffer.seconds = ` [cacheExpire][OAuthCredentials.getCacheExpireBuffer] (optional)
 *
 * `schema.registry.bearer.auth.scope.claim.name = ` [scopeClaimName][OAuthCredentials.getScopeClaimName] (optional)
 *
 * `schema.registry.bearer.auth.sub.claim.name = ` [subClaimName][OAuthCredentials.getSubClaimName] (optional)
 *
 * &nbsp;
 *
 */
abstract class OAuthCredentials : BearerAuthCredentials {

    @Internal
    abstract fun getLogicalCluster(): Property<String>

    @Internal
    abstract fun getIdentityPoolId(): Property<String>

    @Internal
    abstract fun getClientId(): Property<String>

    @Internal
    abstract fun getClientSecret(): Property<String>

    @Internal
    abstract fun getScope(): Property<String>

    @Internal
    abstract fun getIssuerEndpointUrl(): Property<URL>

    @Internal
    abstract fun getCacheExpireBuffer(): Property<Duration>

    @Internal
    abstract fun getScopeClaimName(): Property<String>

    @Internal
    abstract fun getSubClaimName(): Property<String>

    fun logicalCluster(value: String) = getLogicalCluster().set(value)

    fun identityPoolId(value: String) = getIdentityPoolId().set(value)

    fun clientId(value: String) = getClientId().set(value)

    fun clientSecret(value: String) = getClientSecret().set(value)

    fun scope(value: String) = getScope().set(value)

    fun issuerEndpointUrl(value: URL) = getIssuerEndpointUrl().set(value)

    fun cacheExpireBuffer(value: Duration) = getCacheExpireBuffer().set(value)

    fun scopeClaimName(value: String) = getScopeClaimName().set(value)

    fun subClaimName(value: String) = getSubClaimName().set(value)

    @Internal
    override fun toProperties(): MutableMap<String, String> {
        val source = super.toProperties()

        getLogicalCluster().required(BEARER_AUTH_LOGICAL_CLUSTER).let(source::plusAssign)
        getIdentityPoolId().required(BEARER_AUTH_IDENTITY_POOL_ID).let(source::plusAssign)
        getClientId().required(BEARER_AUTH_CLIENT_ID).let(source::plusAssign)
        getClientSecret().required(BEARER_AUTH_CLIENT_SECRET).let(source::plusAssign)
        getScope().nonRequired(BEARER_AUTH_SCOPE)?.let(source::plusAssign)

        getIssuerEndpointUrl()
            .map(URL::toString)
            .required(BEARER_AUTH_ISSUER_ENDPOINT_URL)
            .let(source::plusAssign)

        getCacheExpireBuffer()
            .map(Duration::getSeconds)
            .map(Long::toString)
            .nonRequired(BEARER_AUTH_CACHE_EXPIRY_BUFFER_SECONDS)
            ?.let(source::plusAssign)

        getScopeClaimName().nonRequired(BEARER_AUTH_SCOPE_CLAIM_NAME)?.let(source::plusAssign)
        getSubClaimName().nonRequired(BEARER_AUTH_SUB_CLAIM_NAME)?.let(source::plusAssign)

        return source
    }

    @Internal
    override fun getAlias(): String = "OAUTHBEARER"

}
