package io.github.leofuso.argo.plugin.navis

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig.*
import io.github.leofuso.argo.plugin.navis.security.JAASCredentials
import io.github.leofuso.argo.plugin.navis.security.UserInfoCredentials
import org.apache.kafka.common.config.SaslConfigs.*
import org.gradle.api.Action
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.credentials.Credentials
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.execution.TaskExecutionGraphListener
import org.gradle.api.internal.provider.DefaultProvider
import org.gradle.api.internal.provider.MissingValueException
import org.gradle.api.internal.provider.ValueSupplier
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.internal.logging.text.TreeFormatter
import org.gradle.kotlin.dsl.invoke
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.security.auth.spi.LoginModule

open class CredentialsProviderFactory @Inject constructor(
    private val objectsFactory: ObjectFactory,
    private val providerFactory: ProviderFactory
) : TaskExecutionGraphListener {

    private val missingProviderErrors: MutableSet<String> = ConcurrentHashMap.newKeySet()

    @Suppress("UNCHECKED_CAST")
    fun <T : Credentials> provide(type: Class<out T>, action: Action<in T>? = null): Provider<T> {
        return when {
            UserInfoCredentials::class.java.isAssignableFrom(type) ->
                evaluateAtConfigurationTime(
                    UserInfoCredentialsProvider(action as Action<UserInfoCredentials>)
                )
            JAASCredentials::class.java.isAssignableFrom(type) ->
                evaluateAtConfigurationTime(
                    JAASCredentialsProvider(action as Action<JAASCredentials>)
                )

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

    @Suppress("UnstableApiUsage", "UNCHECKED_CAST")
    private abstract inner class CredentialsProvider<T : Credentials>(
        protected val identity: String,
        private val action: Action<in T>? = null
    ) : Callable<T> {

        private val missingProperties: MutableSet<String> = LinkedHashSet()

        override fun call(): T {
            val type = getProvidedType()
            val credentials: T = objectsFactory.newInstance(type)
            action?.invoke(credentials)
            return mergeProperties(credentials)
        }

        /**
         * A user may configure non-sensitive properties using the DSL, and sensitive properties
         * via Gradle properties.
         * This method assures that no property is lost.
         *
         *  For each property in [credentials]:
         * * checks if it was configured via DSL or via Gradle properties;
         * * Merge them, prioritazing the ones configured via DSL.
         *
         */
        abstract fun mergeProperties(credentials: T): T

        abstract fun getProvidedType(): Class<T>

        @Suppress("ReturnCount")
        fun <T : Any> getRequiredProperty(key: String, methodAccessor: () -> Property<T>, transformer: (String) -> T = { it as T }): T? {

            val primary = methodAccessor.invoke()
            if (primary.isPresent) {
                val value = primary.get()
                if (value is String && value.isNotBlank()) {
                    return value
                }
            }

            val identityProperty = identityProperty(key)
            val gradleProperty = providerFactory.gradleProperty(identityProperty)
            val absent = !gradleProperty.isPresent
            val isBlank = gradleProperty.isPresent && gradleProperty.get().isBlank()
            if (absent || isBlank) {
                missingProperties.add(identityProperty)
                return null
            }

            return gradleProperty.map(transformer).get()
        }

        fun <T : Any> getOptionalProperty(
            key: String = identity,
            methodAccessor: () -> Property<T>,
            transformer: (String) -> T = { it as T }
        ): T? {

            val primary = methodAccessor.invoke()
            if (primary.isPresent) {
                primary.get()
            }

            val identityProperty = identityProperty(key)
            return providerFactory.gradleProperty(identityProperty)
                .map(transformer)
                .orNull
        }

        fun <K, V> getOptionalProperties(
            prefix: String,
            methodAccessor: () -> MapProperty<K, V>,
            transformer: (Map<String, String>) -> Map<K, V> = { it as Map<K, V> }
        ): Map<K, V> {

            val identityProperty = identityProperty(prefix)
            val gradleProperty = providerFactory.gradlePropertiesPrefixedBy(identityProperty)
                .map { properties ->
                    properties.filterValues { it.isNotBlank() }
                        .mapKeys { (key, _) ->
                            key.removePrefix(identityProperty)
                        }
                }
                .map(transformer)

            return methodAccessor.invoke()
                .convention(gradleProperty)
                .getOrElse(emptyMap())
        }

        fun assertRequiredValuesPresence() {
            if (missingProperties.isNotEmpty()) {
                val errorBuilder = TreeFormatter()
                errorBuilder.node("The following Gradle properties are missing for '$identity' credentials")
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

    private inner class UserInfoCredentialsProvider(action: Action<UserInfoCredentials>? = null) :
        CredentialsProvider<UserInfoCredentials>("$CLIENT_NAMESPACE$USER_INFO_CONFIG.", action) {

        @Synchronized
        override fun mergeProperties(credentials: UserInfoCredentials): UserInfoCredentials {
            val username = getRequiredProperty("username", credentials::getUsername)
            val password = getRequiredProperty("password", credentials::getPassword)
            assertRequiredValuesPresence()

            credentials.username(checkNotNull(username) { "At this stage it can't be null." })
            credentials.password(checkNotNull(password) { "At this stage it can't be null." })
            return credentials
        }

        override fun getProvidedType() = UserInfoCredentials::class.java
    }

    private inner class JAASCredentialsProvider(action: Action<JAASCredentials>? = null) :
        CredentialsProvider<JAASCredentials>("$CLIENT_NAMESPACE$SASL_JAAS_CONFIG.", action) {

        @Synchronized
        override fun mergeProperties(credentials: JAASCredentials): JAASCredentials {

            val rootConfig = getOptionalProperty(methodAccessor = credentials::getSaslJaasConfig)
            if (rootConfig != null) {
                credentials.saslJaasConfig(rootConfig)
                return credentials
            }

            val loginModule = getRequiredProperty(
                "login.module",
                credentials::getLoginModule
            ) { property ->
                Class.forName(property)
                    .let { clazz ->
                        if (clazz.isAssignableFrom(LoginModule::class.java)) {
                            @Suppress("UNCHECKED_CAST")
                            clazz as Class<out LoginModule>
                        } else {
                            error("Unsupported 'LoginModule' property. Class [$property] does not implement LoginModule.")
                        }
                    }
            }

            val loginModuleControlFlag = getRequiredProperty(
                "control.flag",
                credentials::getLoginModuleControlFlag
            ) { property ->
                when (property.uppercase()) {
                    "REQUIRED" -> JAASCredentials.LoginModuleControlFlag.REQUIRED
                    "REQUISITE" -> JAASCredentials.LoginModuleControlFlag.REQUISITE
                    "SUFFICIENT" -> JAASCredentials.LoginModuleControlFlag.SUFFICIENT
                    "OPTIONAL" -> JAASCredentials.LoginModuleControlFlag.OPTIONAL
                    else -> error("Invalid login module control flag '$property' in JAAS config.")
                }
            }
            assertRequiredValuesPresence()

            credentials.loginModule(checkNotNull(loginModule) { "At this stage it can't be null." })
            credentials.controlFlag(checkNotNull(loginModuleControlFlag) { "At this stage it can't be null." })

            val options = getOptionalProperties("option", credentials::getOptions)
            credentials.options(options)

            return credentials
        }

        override fun getProvidedType() = JAASCredentials::class.java
    }
}
