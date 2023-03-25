package io.github.leofuso.argo.plugin.navis

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig.*
import io.github.leofuso.argo.plugin.unsuported
import org.apache.kafka.common.config.SaslConfigs.*
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.credentials.Credentials
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.execution.TaskExecutionGraphListener
import org.gradle.api.internal.provider.DefaultProvider
import org.gradle.api.internal.provider.MissingValueException
import org.gradle.api.internal.provider.ValueSupplier
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.internal.credentials.DefaultPasswordCredentials
import org.gradle.internal.logging.text.TreeFormatter
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import javax.security.auth.login.AppConfigurationEntry
import javax.security.auth.spi.LoginModule

class BasicAuthCredentialProviderFactory(private val factory: ProviderFactory) : TaskExecutionGraphListener {

    private val missingProviderErrors: MutableSet<String> = ConcurrentHashMap.newKeySet()

    @Suppress("UNCHECKED_CAST")
    fun <T : Credentials> provide(type: Class<T>): Provider<T> {
        return when {
            PasswordCredentials::class.java.isAssignableFrom(type) -> evaluateAtConfigurationTime(PasswordCredentialsProvider())
            JAASCredentials::class.java.isAssignableFrom(type) -> evaluateAtConfigurationTime(JAASCredentialsProvider())
            else -> error("Unsupported credentials type: $type.")
        } as Provider<T>
    }

    override fun graphPopulated(graph: TaskExecutionGraph) {
        if (missingProviderErrors.isNotEmpty()) {
            val exceptions = missingProviderErrors.map { message: String -> MissingValueException(message) }.toList()
            throw ProjectConfigurationException("Credentials required for this build could not be resolved.", exceptions)
        }
    }

    private fun <T : Credentials?> evaluateAtConfigurationTime(provider: Callable<T>): Provider<T> {
        return InterceptingProvider(provider)
    }

    private inner class InterceptingProvider<T>(value: Callable<out T>) : DefaultProvider<T>(value) {

        override fun getProducer(): ValueSupplier.ValueProducer {
            calculatePresence(ValueSupplier.ValueConsumer.IgnoreUnsafeRead)
            return super.getProducer()
        }

        override fun calculatePresence(consumer: ValueSupplier.ValueConsumer): Boolean {
            return try {
                super.calculatePresence(consumer)
            } catch (e: MissingValueException) {
                e.message?.let(missingProviderErrors::add)
                if (consumer == ValueSupplier.ValueConsumer.IgnoreUnsafeRead) {
                    return false
                }
                throw e
            }
        }

        override fun calculateOwnValue(consumer: ValueSupplier.ValueConsumer): ValueSupplier.Value<out T> {
            return try {
                super.calculateOwnValue(consumer)
            } catch (e: MissingValueException) {
                e.message?.let(missingProviderErrors::add)
                throw e
            }
        }
    }

    @Suppress("UnstableApiUsage")
    private abstract inner class CredentialsProvider<T : Credentials>(protected val identity: String) : Callable<T> {

        private val missingProperties: MutableSet<String> = LinkedHashSet()

        fun getRequiredProperty(property: String): String? {
            val identityProperty = identityProperty(property)
            val propertyProvider = factory
                .environmentVariable(identityProperty)
                .orElse(factory.gradleProperty(identityProperty))

            val absent = !propertyProvider.isPresent
            val isBlank = propertyProvider.isPresent && propertyProvider.get().isBlank()
            if (absent || isBlank) {
                missingProperties.add(identityProperty)
            }
            return propertyProvider.orNull
        }

        fun getOptionalProperty(property: String? = null): Provider<String> {
            val identityProperty = identityProperty(property)
            return factory
                .environmentVariable(identityProperty)
                .orElse(factory.gradleProperty(identityProperty))
        }

        fun getOptionalProperties(prefix: String): Map<String, String> {
            val identityProperty = identityProperty(prefix)
            val propertyProvider = factory
                .environmentVariablesPrefixedBy(identityProperty)
                .orElse(factory.gradlePropertiesPrefixedBy(identityProperty))
                .map { properties ->
                    properties.filterValues { it.isNotBlank() }
                        .mapKeys { (key, _) ->
                            key.removePrefix(identityProperty)
                        }
                }

            return if (propertyProvider.isPresent) {
                propertyProvider.get()
            } else {
                emptyMap()
            }
        }

        fun assertRequiredValuesPresent() {
            if (missingProperties.isNotEmpty()) {
                val errorBuilder = TreeFormatter()
                errorBuilder.node("The following Gradle properties are missing for '").append(identity).append("' credentials")
                errorBuilder.startChildren()
                for (missingProperty in missingProperties) {
                    errorBuilder.node(missingProperty)
                }
                errorBuilder.endChildren()
                missingProperties.clear()
                throw MissingValueException(errorBuilder.toString())
            }
        }

        private fun identityProperty(property: String?): String {
            return identity + property
        }
    }

    private inner class PasswordCredentialsProvider : CredentialsProvider<PasswordCredentials>("$CLIENT_NAMESPACE.$USER_INFO_CONFIG") {
        @Synchronized
        override fun call(): PasswordCredentials {
            val username = getRequiredProperty("username")
            val password = getRequiredProperty("password")
            assertRequiredValuesPresent()
            return DefaultPasswordCredentials(username, password)
        }
    }

    private inner class JAASCredentialsProvider : CredentialsProvider<JAASCredentials>("$CLIENT_NAMESPACE.$SASL_JAAS_CONFIG") {
        override fun call(): JAASCredentials {

            val rootConfig = getOptionalProperty()
            if (rootConfig.isPresent && rootConfig.get().isNotBlank()) {
                return object : JAASCredentials {
                    override fun getSaslJaasConfig() = rootConfig.get()
                    override fun setSaslJaasConfig(config: String) = unsuported("Shouldn't be called.")
                    override fun loginModule(module: Class<out LoginModule>) = unsuported("Shouldn't be called.")
                    override fun controlFlag(flag: AppConfigurationEntry.LoginModuleControlFlag) = unsuported("Shouldn't be called.")
                    override fun options(options: Map<String, String>) = unsuported("Shouldn't be called.")
                    override fun option(key: String, value: String) = unsuported("Shouldn't be called.")
                }
            }

            val loginModule = getRequiredProperty("login.module")
            val controlFlag = getRequiredProperty("control.flag")
            assertRequiredValuesPresent()

            val credential = DefaultJAASCredentials(loginModule!!, controlFlag!!)

            val options = getOptionalProperties("option")
            credential.options(options)

            return credential
        }
    }
}
