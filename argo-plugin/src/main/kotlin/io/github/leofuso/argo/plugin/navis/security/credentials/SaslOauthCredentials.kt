package io.github.leofuso.argo.plugin.navis.security.credentials

import org.gradle.api.tasks.Internal

@Suppress("unused")
abstract class SaslOauthCredentials : JAASCredentials, BearerAuthCredentials {

    @Internal
    override fun toProperties(): MutableMap<String, String> {
        val source = super<BearerAuthCredentials>.toProperties()
        val jaas = super<JAASCredentials>.toProperties()
        source += jaas
        return source
    }

    @Internal
    override fun getAlias(): String = "SASL_OAUTHBEARER_INHERIT"

}
