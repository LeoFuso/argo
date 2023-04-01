package io.github.leofuso.argo.plugin.navis.security

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig.*
import io.github.leofuso.argo.plugin.navis.security.credentials.*
import org.apache.kafka.common.config.SaslConfigs.*
import org.gradle.api.Action
import org.gradle.api.ProjectConfigurationException
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
import org.gradle.kotlin.dsl.mapProperty
import java.lang.reflect.ParameterizedType
import java.net.URL
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.security.auth.spi.LoginModule

/**
 * A SecurityProviderFactory is able to produce the following security-related providers:
 *  * [Credentials] to Authenticate against the Schema Registry API, both Basic Auth and Bearer Token strategies.
 *  * SSL related constructs used to communicate with the Schema Registry API.
 *
 *  Note: SSL strategies are **not** implemented yet.
 *
 */
open class SecurityProviderFactory @Inject constructor(
    private val objectsFactory: ObjectFactory,
    private val providerFactory: ProviderFactory
) : TaskExecutionGraphListener {

    private val missingProviderErrors: MutableSet<String> = ConcurrentHashMap.newKeySet()

    @Suppress("UNCHECKED_CAST")
    fun <T : Credentials> provide(type: Class<out T>, action: Action<in T>? = null): Provider<T> {
        return when {

            UserInfoCredentials::class.java.isAssignableFrom(type) ->
                evaluateAtConfigurationTime(
                    UserInfoCredentialsProvider(action as? Action<UserInfoCredentials>)
                )

            URLCredentials::class.java.isAssignableFrom(type) ->
                evaluateAtConfigurationTime(
                    URLCredentialsProvider()
                )

            SaslBasicAuthCredentials::class.java.isAssignableFrom(type) ->
                evaluateAtConfigurationTime(
                    JAASBasicAuthCredentialsProvider(action as? Action<SaslBasicAuthCredentials>)
                )

            OAuthCredentials::class.java.isAssignableFrom(type) ->
                evaluateAtConfigurationTime(
                    OAuthCredentialsProvider(action as? Action<OAuthCredentials>)
                )

            StaticTokenCredentials::class.java.isAssignableFrom(type) ->
                evaluateAtConfigurationTime(
                    StaticTokenCredentialsProvider(action as? Action<StaticTokenCredentials>)
                )

            else -> throw IllegalArgumentException("Unsupported credentials type: $type.")

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
            val type: Class<T> = getProvidedType()
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
         * * Merge them, prioritizing the ones configured via DSL.
         *
         */
        open fun mergeProperties(credentials: T): T = credentials

        open fun getProvidedType(): Class<T> = this.javaClass.genericSuperclass
            .let { it as ParameterizedType }
            .actualTypeArguments
            .first()
            .let { it as Class<T> }

        @Suppress("ReturnCount")
        fun <T : Any> getRequiredProperty(key: String, methodAccessor: () -> Property<T>, transformer: (String) -> T = { it as T }): T? {

            val identityProperty = identityProperty(key)
            val primary = methodAccessor.invoke()
            if (primary.isPresent) {
                val value = primary.get()
                return if (value is String && value.isNotBlank()) {
                    value
                } else if (value is String && value.isBlank()) {
                    missingProperties.add(identityProperty)
                    return null
                } else {
                    value
                }
            }

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
            key: String? = null,
            methodAccessor: () -> Property<T>,
            transformer: (String) -> T = { it as T }
        ): T? {

            val primary = methodAccessor.invoke()
            if (primary.isPresent) {
                return primary.get()
            }

            val identityProperty = identityProperty(key)
            return providerFactory.gradleProperty(identityProperty)
                .map(transformer)
                .orNull
        }

        inline fun <reified K, reified V> getOptionalProperties(
            prefix: String? = null,
            methodAccessor: () -> MapProperty<K, V>,
            noinline transformer: (Map<String, String>) -> Map<K, V> = { it as Map<K, V> }
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

            val mapProperty = objectsFactory.mapProperty<K, V>()
            mapProperty.putAll(gradleProperty)
            mapProperty.putAll(methodAccessor.invoke())

            return mapProperty.getOrElse(emptyMap())
        }

        fun assertRequiredValuesPresence() {
            if (missingProperties.isNotEmpty()) {
                val errorBuilder = TreeFormatter()
                errorBuilder.node("The following Gradle properties are missing for '${getProvidedType().simpleName}' credentials")
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
            return property?.let { identity + it } ?: identity
        }
    }

    private inner class UserInfoCredentialsProvider(action: Action<UserInfoCredentials>? = null) :
        CredentialsProvider<UserInfoCredentials>("$CLIENT_NAMESPACE$USER_INFO_CONFIG", action) {

        @Synchronized
        override fun mergeProperties(credentials: UserInfoCredentials): UserInfoCredentials {

            getRequiredProperty(
                ".username",
                credentials::getUsername
            )?.let(credentials::username)

            getRequiredProperty(
                ".password",
                credentials::getPassword
            )?.let(credentials::password)

            assertRequiredValuesPresence()

            return credentials
        }

    }

    private inner class URLCredentialsProvider(action: Action<URLCredentials>? = null) :
        CredentialsProvider<URLCredentials>("DEFAULT", action)

    private inner class JAASBasicAuthCredentialsProvider(action: Action<SaslBasicAuthCredentials>? = null) :
        CredentialsProvider<SaslBasicAuthCredentials>("$CLIENT_NAMESPACE$SASL_JAAS_CONFIG", action) {

        @Synchronized
        override fun mergeProperties(credentials: SaslBasicAuthCredentials): SaslBasicAuthCredentials {

            val rootConfig = getOptionalProperty(methodAccessor = credentials::getSaslJaasConfig)
            if (rootConfig != null) {
                credentials.saslJaasConfig(rootConfig)
                return credentials
            }

            val loginModule = getRequiredProperty(
                ".login.module",
                credentials::getLoginModule
            ) { property ->
                Class.forName(property)
                    .let { clazz ->
                        if (LoginModule::class.java.isAssignableFrom(clazz)) {
                            @Suppress("UNCHECKED_CAST")
                            clazz as Class<out LoginModule>
                        } else {
                            error("Unsupported 'LoginModule' property. Class [$property] does not implement LoginModule.")
                        }
                    }
            }

            val loginModuleControlFlag = getRequiredProperty(
                ".control.flag",
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

            credentials.loginModule(checkNotNull(loginModule) { "At this stage, it can't be null." })
            credentials.controlFlag(checkNotNull(loginModuleControlFlag) { "At this stage, it can't be null." })

            val options = getOptionalProperties(".option.", credentials::getOptions)
            credentials.options(options)

            return credentials
        }

    }

    private inner class OAuthCredentialsProvider(action: Action<OAuthCredentials>? = null) :
        CredentialsProvider<OAuthCredentials>(CLIENT_NAMESPACE, action) {

        @Synchronized
        override fun mergeProperties(credentials: OAuthCredentials): OAuthCredentials {

            getRequiredProperty(
                BEARER_AUTH_LOGICAL_CLUSTER,
                credentials::getLogicalCluster
            )?.let(credentials::logicalCluster)

            getRequiredProperty(
                BEARER_AUTH_IDENTITY_POOL_ID,
                credentials::getIdentityPoolId
            )?.let(credentials::identityPoolId)

            getRequiredProperty(
                BEARER_AUTH_CLIENT_ID,
                credentials::getClientId
            )?.let(credentials::clientId)

            getRequiredProperty(
                BEARER_AUTH_CLIENT_SECRET,
                credentials::getClientSecret
            )?.let(credentials::clientSecret)

            getOptionalProperty(
                BEARER_AUTH_SCOPE,
                credentials::getScope
            )?.let(credentials::scope)

            getRequiredProperty(
                BEARER_AUTH_ISSUER_ENDPOINT_URL,
                credentials::getIssuerEndpointUrl,
                ::URL
            )?.let(credentials::issuerEndpointUrl)

            getOptionalProperty(
                BEARER_AUTH_CACHE_EXPIRY_BUFFER_SECONDS,
                credentials::getCacheExpireBuffer
            ) {
                val seconds = it.toLong()
                Duration.ofSeconds(seconds)
            }?.let(credentials::cacheExpireBuffer)

            getOptionalProperty(
                BEARER_AUTH_SCOPE_CLAIM_NAME,
                credentials::getScopeClaimName
            )?.let(credentials::scopeClaimName)

            getOptionalProperty(
                BEARER_AUTH_SUB_CLAIM_NAME,
                credentials::getSubClaimName
            )?.let(credentials::subClaimName)

            assertRequiredValuesPresence()

            return credentials
        }

    }

    private inner class StaticTokenCredentialsProvider(action: Action<StaticTokenCredentials>? = null) :
        CredentialsProvider<StaticTokenCredentials>(CLIENT_NAMESPACE, action) {

        @Synchronized
        override fun mergeProperties(credentials: StaticTokenCredentials): StaticTokenCredentials {

            getRequiredProperty(
                BEARER_AUTH_TOKEN_CONFIG,
                credentials::getToken
            )?.let(credentials::token)

            assertRequiredValuesPresence()

            return credentials
        }
    }
}
