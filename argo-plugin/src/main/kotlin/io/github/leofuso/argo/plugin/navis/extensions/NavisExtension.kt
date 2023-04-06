@file:Suppress("unused")

package io.github.leofuso.argo.plugin.navis.extensions

import io.github.leofuso.argo.plugin.columba.extensions.ColumbaOptions
import io.github.leofuso.argo.plugin.navis.security.SecurityProviderFactory
import io.github.leofuso.argo.plugin.navis.security.credentials.Credentials
import io.github.leofuso.argo.plugin.navis.security.credentials.SSLCredentials
import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import java.io.File
import java.net.URI
import java.net.URL
import javax.inject.Inject

abstract class NavisOptions @Inject constructor(private val objectFactory: ObjectFactory) {

    @Nested
    abstract fun getSecurity(): SecurityNavisOptions

    abstract fun getDownloadSubjects(): ListProperty<DownloadSubjectOptions>

    abstract fun getURL(): Property<URI>

    fun url(url: String) = getURL().set(URI(url))

    fun url(url: URL) = getURL().set(url.toURI())

    fun url(uri: URI) = getURL().set(uri)

    fun security(action: Action<in SecurityNavisOptions>) = action.invoke(getSecurity())

    fun download(action: Action<in DownloadSubjectOptions>) {
        val subject = objectFactory.newInstance(DownloadSubjectOptions::class.java)
        action.invoke(subject)
        getDownloadSubjects().add(subject)
    }

    @Internal
    fun applyConventions(): NavisOptions {
        getURL().convention(URI("localhost:8081"))
        return this
    }

}

abstract class SecurityNavisOptions @Inject constructor(objectFactory: ObjectFactory) {

    private val factory: SecurityProviderFactory = objectFactory.newInstance()
    private val _credentials: Property<Credentials> = objectFactory.property()

    @get:Internal
    internal val credentials: Property<Credentials>
        get() = _credentials

    abstract fun getSSL(): Property<SSLCredentials>

    fun ssl(type: Class<SSLCredentials>) = getSSL().set(factory.provide(type))

    fun ssl(type: Class<SSLCredentials>, action: Action<SSLCredentials>) = getSSL().set(factory.provide(type, action))

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

abstract class DownloadSubjectOptions @Inject constructor(objectFactory: ObjectFactory) {

    private val _name: Property<String> = objectFactory.property()
    private val _regex: Property<Regex> = objectFactory.property()
    private val _outputDir: DirectoryProperty = objectFactory.directoryProperty()
    private val _version: Property<Int> = objectFactory.property()

    @get:Internal
    var name: String
        get() = _name.get()
        set(value) = _name.set(value)

    @get:Internal
    var regex: Regex
        get() = _regex.get()
        set(value) = _regex.set(value)

    @get:Internal
    var outputDir: File
        get() = _outputDir.asFile.get()
        set(value) = _outputDir.set(value)

    @get:Internal
    var version: Int
        get() = _version.get()
        set(value) = _version.set(value)

    @Internal
    fun getName() = _name

    @Internal
    fun getRegex() = _regex

    @Internal
    fun getOutputDir() = _outputDir

    @Internal
    fun getVersion() = _version

}
