package io.github.leofuso.argo.plugin.navis.security.credentials

import org.gradle.api.tasks.Internal

abstract class SaslBasicAuthCredentials : JAASCredentials, BasicAuthCredentials {

    @Internal
    override fun toProperties(): MutableMap<String, String> {
        val source = super<BasicAuthCredentials>.toProperties()
        val jaas = super<JAASCredentials>.toProperties()
        source += jaas
        return source
    }

    @Internal
    override fun getAlias(): String = "SASL_INHERIT"

}
