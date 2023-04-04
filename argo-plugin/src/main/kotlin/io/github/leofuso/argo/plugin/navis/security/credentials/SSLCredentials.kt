package io.github.leofuso.argo.plugin.navis.security.credentials

import org.apache.kafka.common.config.ConfigDef
import org.gradle.api.provider.MapProperty

@Suppress("unused")
abstract class SSLCredentials : Credentials {

    private val configDef = ConfigDef().withClientSslSupport()

    abstract fun getOptions(): MapProperty<String, String>

    fun option(key: String, value: String) = getOptions().put(key, value)

    fun options(value: Map<String, String>) = getOptions().putAll(value)

    override fun toProperties(): MutableMap<String, String> {
        val property = getOptions()
        val values = configDef.parse(property.get())

        @Suppress("UnnecessaryVariable")
        val properties = ConfigDef.convertToStringMapWithPasswordValues(values)
        return properties
    }

    override fun getAlias() = "SSL_CREDENTIALS"

}
