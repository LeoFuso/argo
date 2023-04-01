package io.github.leofuso.argo.plugin.navis.security.credentials

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig.USER_INFO_CONFIG
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal

/**
 * A username/password credentials that can be used to log in to something protected by a username and password.
 *
 */
abstract class UserInfoCredentials : BasicAuthCredentials {

    /**
     * Returns the username to use when authenticating.
     *
     * @return The username property.
     */
    abstract fun getUsername(): Property<String>

    /**
     * Sets the username to use when authenticating.
     *
     * @param username The username.
     */
    fun username(username: String) = getUsername().set(username)

    /**
     * Returns the password to use when authenticating.
     *
     * @return The password property.
     */
    abstract fun getPassword(): Property<String>

    /**
     * Sets the password to use when authenticating.
     *
     * @param password The password.
     */
    fun password(password: String) = getPassword().set(password)

    @Internal
    override fun getAlias(): String = "USER_INFO"

    @Internal
    override fun toProperties(): MutableMap<String, String> {
        val source = super.toProperties()
        getUsername()
            .zip(getPassword()) { first, second -> "'$first:$second'" }
            .required(USER_INFO_CONFIG)
            .let(source::plusAssign)

        return source
    }

    override fun toString(): String {
        return "[ Hidden ]"
    }

}
