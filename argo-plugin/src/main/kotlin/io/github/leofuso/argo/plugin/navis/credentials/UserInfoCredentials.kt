package io.github.leofuso.argo.plugin.navis.credentials

import org.gradle.api.credentials.Credentials
import org.gradle.api.provider.Property

/**
 * A username/password credentials that can be used to log in to something protected by a username and password.
 *
 */
abstract class UserInfoCredentials : Credentials {

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
}
