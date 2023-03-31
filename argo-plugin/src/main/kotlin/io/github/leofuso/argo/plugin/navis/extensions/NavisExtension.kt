@file:Suppress("unused")

package io.github.leofuso.argo.plugin.navis.extensions

import io.github.leofuso.argo.plugin.navis.security.SecurityProviderFactory
import io.github.leofuso.argo.plugin.navis.security.credentials.Credentials
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import java.net.URI
import java.net.URL
import javax.inject.Inject

abstract class NavisOptions {

    abstract fun getURL(): Property<URI>

    fun url(url: String) = getURL().set(URI(url))

    fun url(url: URL) = getURL().set(url.toURI())

    fun url(uri: URI) = getURL().set(uri)

    @Nested
    abstract fun getSecurity(): SecurityNavisOptions

    fun security(action: Action<in SecurityNavisOptions>) = action.invoke(getSecurity())

    @Nested
    abstract fun getDownload(): DownloadNavisOptions

    fun download(action: Action<in DownloadNavisOptions>) = action.invoke(getDownload())

}

abstract class SecurityNavisOptions @Inject constructor(objectFactory: ObjectFactory) {

    private val factory: SecurityProviderFactory = objectFactory.newInstance()
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

    fun <T : Credentials> credentials(type: Class<out T>, action: Action<in T>) {
        if (_credentials.isPresent) {
            error("Multi credentials are unsupported. Credentials already defined of Class [${_credentials.get()::class.java}].")
        }
        _credentials.set(factory.provide(type, action))
    }

}

abstract class DownloadNavisOptions
