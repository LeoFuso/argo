package io.github.leofuso.argo.plugin.navis.extensions

import io.github.leofuso.argo.plugin.navis.SchemaRegistrySecurityCredentialsProviderFactory
import org.gradle.api.Action
import org.gradle.api.credentials.Credentials
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import java.net.URI
import javax.inject.Inject

abstract class NavisOptions {

    abstract fun getURL(): Property<URI>

    @Nested
    abstract fun getSecurity(): NavisSecurityOptions

    fun security(action: Action<in NavisSecurityOptions>) = action.invoke(getSecurity())

    fun url(url: String) = getURL().set(URI(url))

}

abstract class NavisSecurityOptions {

    @Nested
    abstract fun getBasicAuth(): SecurityBasicAuthOptions

    fun basicAuth(action: Action<in SecurityBasicAuthOptions>) = action.invoke(getBasicAuth())

}

abstract class SecurityBasicAuthOptions @Inject constructor(objectFactory: ObjectFactory) {

    private val factory: SchemaRegistrySecurityCredentialsProviderFactory = objectFactory.newInstance()
    private val _credentials: Property<Credentials> = objectFactory.property()

    @get:Internal
    internal val credentials: Property<Credentials>
        get() = _credentials

    fun credentials(type: Class<out Credentials>) {
        if (_credentials.isPresent) {
            error("Multi credentials are unsupported. Credentials already defined of Class [${_credentials.get()::class.java}].")
        }
        _credentials.set(factory.provide(type))
    }

    fun credentials(type: Class<out Credentials>, action: Action<in Credentials>) {
        if (_credentials.isPresent) {
            error("Multi credentials are unsupported. Credentials already defined of Class [${_credentials.get()::class.java}].")
        }
        _credentials.set(factory.provide(type, action))
    }

}
