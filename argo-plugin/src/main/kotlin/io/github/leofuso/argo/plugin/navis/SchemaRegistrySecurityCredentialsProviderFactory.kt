package io.github.leofuso.argo.plugin.navis

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig.*
import io.github.leofuso.argo.plugin.navis.security.JAASCredentials
import org.apache.kafka.common.config.SaslConfigs.*
import org.gradle.api.Action
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
import org.gradle.internal.reflect.Instantiator
import org.gradle.kotlin.dsl.invoke
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.security.auth.spi.LoginModule

abstract class SchemaRegistrySecurityCredentialsProviderFactory(
    @Inject private val instantiator: Instantiator,
    @Inject private val providerFactory: ProviderFactory
) : TaskExecutionGraphListener {

    private val missingProviderErrors: MutableSet<String> = ConcurrentHashMap.newKeySet()

    @Suppress("UNCHECKED_CAST")
    fun <T : Credentials> provide(type: Class<T>): Provider<T> {
        return when {
            PasswordCredentials::class.java.isAssignableFrom(type) -> evaluateAtConfigurationTime(PasswordCredentialsProvider())
            JAASCredentials::class.java.isAssignableFrom(type) -> evaluateAtConfigurationTime(JAASCredentialsProvider())
            else -> error("Unsupported credentials type: $type.")
        } as Provider<T>
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Credentials> provide(type: Class<out Credentials>, action: Action<in Credentials>): Provider<T> {
        return when {
            PasswordCredentials::class.java.isAssignableFrom(type) -> evaluateAtConfigurationTime(PasswordCredentialsProvider(action))
            JAASCredentials::class.java.isAssignableFrom(type) -> evaluateAtConfigurationTime(JAASCredentialsProvider(action))
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
    private abstract inner class CredentialsProvider<T : Credentials>(
        protected val identity: String,
        private val action: Action<in T>? = null
    ) : Callable<T> {

        private val missingProperties: MutableSet<String> = LinkedHashSet()

        override fun call(): T = if (action != null) {
            val type = getProvidedType()
            val credentials: T = instantiator.newInstance(type)
            action.invoke(credentials)
            credentials
        } else {
            provide()
        }

        abstract fun provide(): T
        abstract fun getProvidedType(): Class<T>

        fun getRequiredProperty(property: String): String? {
            val identityProperty = identityProperty(property)
            val propertyProvider = providerFactory.gradleProperty(identityProperty)
            val absent = !propertyProvider.isPresent
            val isBlank = propertyProvider.isPresent && propertyProvider.get().isBlank()
            if (absent || isBlank) {
                missingProperties.add(identityProperty)
            }
            return propertyProvider.orNull
        }

        fun getOptionalProperty(property: String? = null): Provider<String> {
            val identityProperty = identityProperty(property)
            return providerFactory.gradleProperty(identityProperty)
        }

        fun getOptionalProperties(prefix: String): Map<String, String> {
            val identityProperty = identityProperty(prefix)
            val propertyProvider = providerFactory.gradlePropertiesPrefixedBy(identityProperty)
                .map { properties ->
                    properties.filterValues { it.isNotBlank() }
                        .mapKeys { (key, _) ->
                            key.removePrefix(identityProperty)
                        }
                }

            return propertyProvider.getOrElse(emptyMap())
        }

        fun assertRequiredValuesPresence() {
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

    private inner class PasswordCredentialsProvider(action: Action<in Credentials>? = null) :
        CredentialsProvider<PasswordCredentials>("$CLIENT_NAMESPACE.$USER_INFO_CONFIG", action) {

        @Synchronized
        override fun provide(): PasswordCredentials {
            val username = getRequiredProperty("username")
            val password = getRequiredProperty("password")
            assertRequiredValuesPresence()
            return DefaultPasswordCredentials(username, password)
        }

        override fun getProvidedType() = PasswordCredentials::class.java
    }

    private inner class JAASCredentialsProvider(action: Action<in Credentials>? = null) :
        CredentialsProvider<JAASCredentials>("$CLIENT_NAMESPACE.$SASL_JAAS_CONFIG", action) {

        @Synchronized
        override fun provide(): JAASCredentials {

            val rootConfig = getOptionalProperty()
            if (rootConfig.isPresent && rootConfig.get().isNotBlank()) {
                val credentials = instantiator.newInstance(JAASCredentials::class.java)
                credentials.saslJaasConfig(rootConfig.get())
                return credentials
            }

            val loginModuleProperty = getRequiredProperty("login.module")
            val controlFlagProperty = getRequiredProperty("control.flag")
            assertRequiredValuesPresence()

            val loginModule = Class.forName(loginModuleProperty)
                .let { clazz ->
                    if (clazz.isAssignableFrom(LoginModule::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        clazz as Class<out LoginModule>
                    } else {
                        error("Unsupported 'LoginModule' property. Class [$loginModuleProperty] does not implement LoginModule.")
                    }
                }

            checkNotNull(controlFlagProperty) { "After 'assertRequiredValuesPresence' this variable cannot be null." }
            val loginModuleControlFlag = when (controlFlagProperty.uppercase()) {
                "REQUIRED" -> JAASCredentials.LoginModuleControlFlag.REQUIRED
                "REQUISITE" -> JAASCredentials.LoginModuleControlFlag.REQUISITE
                "SUFFICIENT" -> JAASCredentials.LoginModuleControlFlag.SUFFICIENT
                "OPTIONAL" -> JAASCredentials.LoginModuleControlFlag.OPTIONAL
                else -> error("Invalid login module control flag '$controlFlagProperty' in JAAS config.")
            }

            val credentials = instantiator.newInstance(JAASCredentials::class.java)
            credentials.loginModule(loginModule)
            credentials.controlFlag(loginModuleControlFlag)

            val options = getOptionalProperties("option")
            credentials.options(options)

            return credentials
        }

        override fun getProvidedType() = JAASCredentials::class.java
    }
}
