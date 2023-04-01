package io.github.leofuso.argo.plugin.navis.security.credentials

/**
 *
 * Retrieves the credentials from Schema Registry url, e.g. `https://foo.bar@192.168.0.1:443`.
 *
 * No expected properties.
 */
abstract class URLCredentials : BasicAuthCredentials {

    override fun getAlias(): String = "URL"

}
