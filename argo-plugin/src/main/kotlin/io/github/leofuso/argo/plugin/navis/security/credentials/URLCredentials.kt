package io.github.leofuso.argo.plugin.navis.security.credentials

abstract class URLCredentials : BasicAuthCredentials {

    override fun getAlias(): String = "URL"

}
