package io.github.leofuso.argo.plugin.navis.security.credentials

import io.confluent.kafka.schemaregistry.client.security.basicauth.BasicAuthCredentialProvider
import java.net.URL

class SaslBasicAuthCredentialProvider : BasicAuthCredentialProvider {

    override fun configure(configs: MutableMap<String, *>?) {
        TODO("Not yet implemented")
    }

    override fun alias() = "ENHANCED_SASL"

    override fun getUserInfo(url: URL?): String {
        TODO("Not yet implemented")
    }
}
