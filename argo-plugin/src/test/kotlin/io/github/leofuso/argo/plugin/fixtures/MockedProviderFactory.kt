package io.github.leofuso.argo.plugin.fixtures

import org.gradle.api.internal.provider.DefaultProviderFactory
import org.gradle.api.internal.provider.Providers
import org.gradle.api.provider.Provider

/**
 * There's no way of adding 'gradle.properties' as a variable for 'ProjectBuilder'.
 * See [issue-17638](https://github.com/gradle/gradle/issues/17638)
 */
class MockedProviderFactory(private val properties: Map<String, String>) : DefaultProviderFactory() {

    override fun gradleProperty(propertyName: String): Provider<String> {
        val value = properties[propertyName] ?: error("Property '$propertyName' not found.")
        return Providers.of(value)
    }

    override fun gradlePropertiesPrefixedBy(variableNamePrefix: String): Provider<MutableMap<String, String>> {
        val value = properties.filter { (key, _) -> key.startsWith(variableNamePrefix) }.toMutableMap()
        return Providers.of(value)
    }

}
