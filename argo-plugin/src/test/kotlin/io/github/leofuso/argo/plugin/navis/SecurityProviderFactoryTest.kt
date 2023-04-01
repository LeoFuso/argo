package io.github.leofuso.argo.plugin.navis

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig.*
import io.github.leofuso.argo.plugin.fixtures.MockedProviderFactory
import io.github.leofuso.argo.plugin.navis.security.SecurityProviderFactory
import io.github.leofuso.argo.plugin.navis.security.credentials.*
import org.apache.kafka.common.config.SaslConfigs.*
import org.gradle.api.Project
import org.gradle.api.internal.provider.MissingValueException
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.*
import java.io.File
import java.net.URL
import java.time.Duration

@DisplayName("Navis: Unit tests related to 'SecurityProviderFactory'.")
class SecurityProviderFactoryTest {

    @TempDir
    private lateinit var rootDir: File
    private lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder
            .builder()
            .withProjectDir(rootDir)
            .withName("security-provider-factory-test")
            .build()
    }

    @Test
    @DisplayName(
        """
        Given a 'UserInfoCredentials' entirely configured using 'gradle.properties',
        when provided,
        then all values should match.
        """
    )
    fun id1680110508973() {

        /* Given */
        val expectedUsername = "LeoFuso"
        val expectedPassword = "@!&!S#\$qdL2zS@@t#q6"

        val gradleProperties = mapOf(
            "schema.registry.basic.auth.user.info.username" to expectedUsername,
            "schema.registry.basic.auth.user.info.password" to expectedPassword
        )

        val subject = SecurityProviderFactory(project.objects, MockedProviderFactory(gradleProperties))

        /* When */
        val credentials = subject.provide(UserInfoCredentials::class.java)

        /* Then */
        expectThat(credentials) {
            isA<Provider<UserInfoCredentials>>()
            get(Provider<UserInfoCredentials>::isPresent)
                .isTrue()
            get(Provider<UserInfoCredentials>::get)
                .isA<UserInfoCredentials>()
                .and {
                    get { getUsername().get() }
                        .isEqualTo(expectedUsername)
                    get { getPassword().get() }
                        .isEqualTo(expectedPassword)

                    get(UserInfoCredentials::toProperties)
                        .hasEntry(BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")
                        .hasEntry(USER_INFO_CONFIG, "'$expectedUsername:$expectedPassword'")

                    get(UserInfoCredentials::toString)
                        .isEqualTo("[ Hidden ]")
                }
        }
    }

    @Test
    @DisplayName(
        """
        Given a 'UserInfoCredentials' configured using both 'gradle.properties' and 'Kotlin DSL',
        when provided,
        then all values should match.
        """
    )
    fun id1680111884409() {

        /* Given */
        val expectedUsername = "LeoFuso"
        val expectedPassword = "@!&!S#\$qdL2zS@@t#q6"

        val gradleProperties = mapOf(
            "schema.registry.basic.auth.user.info.username" to "Unknown",
            "schema.registry.basic.auth.user.info.password" to expectedPassword
        )

        val subject = SecurityProviderFactory(project.objects, MockedProviderFactory(gradleProperties))

        /* When */
        val credentials = subject
            .provide(UserInfoCredentials::class.java) { it.username(expectedUsername) }

        /* Then */
        expectThat(credentials) {
            isA<Provider<UserInfoCredentials>>()
            get(Provider<UserInfoCredentials>::isPresent)
                .isTrue()
            get(Provider<UserInfoCredentials>::get)
                .isA<UserInfoCredentials>()
                .and {
                    get { getUsername().get() }
                        .isEqualTo(expectedUsername)
                    get { getPassword().get() }
                        .isEqualTo(expectedPassword)

                    get(UserInfoCredentials::toProperties)
                        .hasEntry(BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")
                        .hasEntry(USER_INFO_CONFIG, "'$expectedUsername:$expectedPassword'")

                    get(UserInfoCredentials::toString)
                        .isEqualTo("[ Hidden ]")
                }
        }
    }

    @Test
    @DisplayName(
        """
        Given a 'UserInfoCredentials' entirely configured using 'Kotlin DSL',
        when provided,
        then all values should match.
        """
    )
    fun id1680112323081() {

        /* Given */
        val expectedUsername = "LeoFuso"
        val expectedPassword = "secret"

        val subject = SecurityProviderFactory(project.objects, MockedProviderFactory(emptyMap()))

        /* When */
        val credentials = subject
            .provide(UserInfoCredentials::class.java) {
                it.username(expectedUsername)
                it.password(expectedPassword)
            }

        /* Then */
        expectThat(credentials) {
            isA<Provider<UserInfoCredentials>>()
            get(Provider<UserInfoCredentials>::isPresent)
                .isTrue()
            get(Provider<UserInfoCredentials>::get)
                .isA<UserInfoCredentials>()
                .and {
                    get { getUsername().get() }
                        .isEqualTo(expectedUsername)
                    get { getPassword().get() }
                        .isEqualTo(expectedPassword)

                    get(UserInfoCredentials::toProperties)
                        .hasEntry(BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")
                        .hasEntry(USER_INFO_CONFIG, "'$expectedUsername:$expectedPassword'")

                    get(UserInfoCredentials::toString)
                        .isEqualTo("[ Hidden ]")
                }
        }
    }

    @Test
    @DisplayName(
        """
        Given a 'UserInfoCredentials' with missing required properties,
        when provided,
        then fail, accusing 'missing properties'.
        """
    )
    fun id1680126911895() {

        /* Given */
        val expectedPassword = "secret"

        val gradleProperties = mapOf(
            "schema.registry.basic.auth.user.info.password" to expectedPassword
        )

        val subject = SecurityProviderFactory(project.objects, MockedProviderFactory(gradleProperties))

        /* When */
        val credentials = subject.provide(UserInfoCredentials::class.java)

        /* Then */
        expectThat(credentials) {
            isA<Provider<UserInfoCredentials>>()

            expectCatching { credentials.get() }
                .isFailure()
                .isA<MissingValueException>()
        }
    }

    @Test
    @DisplayName(
        """
        Given a 'StaticTokenCredentials' configured using the individual 'gradle.properties',
        when provided,
        then all values should match.
        """
    )
    fun id1680112676357() {

        /* Given */
        val expectedToken = "26%sU*#@LP#4Zgr!@!o"

        val gradleProperties = mapOf(
            "schema.registry.$BEARER_AUTH_TOKEN_CONFIG" to expectedToken
        )

        val subject = SecurityProviderFactory(project.objects, MockedProviderFactory(gradleProperties))

        /* When */
        val credentials = subject.provide(StaticTokenCredentials::class.java)

        /* Then */
        expectThat(credentials) {
            isA<Provider<BearerAuthCredentials>>()
            get(Provider<StaticTokenCredentials>::isPresent)
                .isTrue()
            get(Provider<StaticTokenCredentials>::get)
                .isA<BearerAuthCredentials>()
                .and {
                    get(BearerAuthCredentials::toProperties)
                        .hasEntry(BEARER_AUTH_CREDENTIALS_SOURCE, "STATIC_TOKEN")
                        .hasEntry(BEARER_AUTH_TOKEN_CONFIG, expectedToken)
                }
        }
    }

    @Test
    @DisplayName(
        """
        Given a 'StaticTokenCredentials' configured using 'Kotlin DSL',
        when provided,
        then all values should match.
        """
    )
    fun id1680126508274() {

        /* Given */
        val expectedToken = "26%sU*#@LP#4Zgr!@!o"

        val subject = SecurityProviderFactory(project.objects, MockedProviderFactory(emptyMap()))

        /* When */
        val credentials = subject.provide(StaticTokenCredentials::class.java) {
            it.token(expectedToken)
        }

        /* Then */
        expectThat(credentials) {
            isA<Provider<StaticTokenCredentials>>()
            get(Provider<StaticTokenCredentials>::isPresent)
                .isTrue()
            get(Provider<StaticTokenCredentials>::get)
                .isA<BearerAuthCredentials>()
                .and {
                    get(BearerAuthCredentials::toProperties)
                        .hasEntry(BEARER_AUTH_CREDENTIALS_SOURCE, "STATIC_TOKEN")
                        .hasEntry(BEARER_AUTH_TOKEN_CONFIG, expectedToken)
                }
        }
    }

    @Test
    @DisplayName(
        """
        Given a 'StaticTokenCredentials' with missing required properties,
        when provided,
        then fail, accusing 'missing properties'.
        """
    )
    fun id1680379228942() {

        /* Given */
        val subject = SecurityProviderFactory(project.objects, MockedProviderFactory(emptyMap()))

        /* When */
        val credentials = subject.provide(StaticTokenCredentials::class.java)

        /* Then */
        expectThat(credentials) {
            isA<Provider<StaticTokenCredentials>>()

            expectCatching { credentials.get() }
                .isFailure()
                .isA<MissingValueException>()
        }

    }

    @Test
    @DisplayName(
        """
        Given an 'OAuthCredentials' entirely configured using the individual 'gradle.properties',
        when provided,
        then all values should match.
        """
    )
    fun id1680128156467() {

        /* Given */
        @Suppress("SpellCheckingInspection")
        val gradleProperties = mapOf(
            "$CLIENT_NAMESPACE$BEARER_AUTH_LOGICAL_CLUSTER" to "LSRC-XXXXX",
            "$CLIENT_NAMESPACE$BEARER_AUTH_IDENTITY_POOL_ID" to "5bed8df5-75a7-4f98-9879-5118629e5b1f",
            "$CLIENT_NAMESPACE$BEARER_AUTH_CLIENT_ID" to "LeoFuso",
            "$CLIENT_NAMESPACE$BEARER_AUTH_CLIENT_SECRET" to "26%sU*#@LP#4Zgr!@!o",
            "$CLIENT_NAMESPACE$BEARER_AUTH_SCOPE" to "schemas",
            "$CLIENT_NAMESPACE$BEARER_AUTH_ISSUER_ENDPOINT_URL" to "https://openid.io",
            "$CLIENT_NAMESPACE$BEARER_AUTH_CACHE_EXPIRY_BUFFER_SECONDS" to "180",
            "$CLIENT_NAMESPACE$BEARER_AUTH_SCOPE_CLAIM_NAME" to "email",
            "$CLIENT_NAMESPACE$BEARER_AUTH_SUB_CLAIM_NAME" to "auth0|USER-ID"
        )

        val subject = SecurityProviderFactory(project.objects, MockedProviderFactory(gradleProperties))

        /* When */
        val credentials = subject.provide(OAuthCredentials::class.java)

        /* Then */
        expectThat(credentials) {
            isA<Provider<OAuthCredentials>>()
            get(Provider<OAuthCredentials>::isPresent)
                .isTrue()
            get(Provider<OAuthCredentials>::get)
                .isA<OAuthCredentials>()
                .and {

                    get(OAuthCredentials::toProperties)
                        .hasEntry(BEARER_AUTH_CREDENTIALS_SOURCE, "OAUTHBEARER")
                        .hasEntry(BEARER_AUTH_LOGICAL_CLUSTER, gradleProperties["$CLIENT_NAMESPACE$BEARER_AUTH_LOGICAL_CLUSTER"])
                        .hasEntry(BEARER_AUTH_IDENTITY_POOL_ID, gradleProperties["$CLIENT_NAMESPACE$BEARER_AUTH_IDENTITY_POOL_ID"])
                        .hasEntry(BEARER_AUTH_CLIENT_ID, gradleProperties["$CLIENT_NAMESPACE$BEARER_AUTH_CLIENT_ID"])
                        .hasEntry(BEARER_AUTH_CLIENT_SECRET, gradleProperties["$CLIENT_NAMESPACE$BEARER_AUTH_CLIENT_SECRET"])
                        .hasEntry(BEARER_AUTH_SCOPE, gradleProperties["$CLIENT_NAMESPACE$BEARER_AUTH_SCOPE"])
                        .hasEntry(BEARER_AUTH_ISSUER_ENDPOINT_URL, gradleProperties["$CLIENT_NAMESPACE$BEARER_AUTH_ISSUER_ENDPOINT_URL"])
                        .hasEntry(
                            BEARER_AUTH_CACHE_EXPIRY_BUFFER_SECONDS,
                            gradleProperties["$CLIENT_NAMESPACE$BEARER_AUTH_CACHE_EXPIRY_BUFFER_SECONDS"]
                        )
                        .hasEntry(BEARER_AUTH_SCOPE_CLAIM_NAME, gradleProperties["$CLIENT_NAMESPACE$BEARER_AUTH_SCOPE_CLAIM_NAME"])
                        .hasEntry(BEARER_AUTH_SUB_CLAIM_NAME, gradleProperties["$CLIENT_NAMESPACE$BEARER_AUTH_SUB_CLAIM_NAME"])
                }
        }
    }

    @Test
    @DisplayName(
        """
        Given an 'OAuthCredentials' entirely configured using 'Kotlin DSL',
        when provided,
        then all values should match.
        """
    )
    fun id1680128523149() {

        /* Given */
        @Suppress("SpellCheckingInspection")
        val gradleProperties = mapOf(
            BEARER_AUTH_LOGICAL_CLUSTER to "LSRC-XXXXX",
            BEARER_AUTH_IDENTITY_POOL_ID to "5bed8df5-75a7-4f98-9879-5118629e5b1f",
            BEARER_AUTH_CLIENT_ID to "LeoFuso",
            BEARER_AUTH_CLIENT_SECRET to "26%sU*#@LP#4Zgr!@!o",
            BEARER_AUTH_SCOPE to "schemas",
            BEARER_AUTH_ISSUER_ENDPOINT_URL to "https://openid.io",
            BEARER_AUTH_CACHE_EXPIRY_BUFFER_SECONDS to "180",
            BEARER_AUTH_SCOPE_CLAIM_NAME to "email",
            BEARER_AUTH_SUB_CLAIM_NAME to "auth0|USER-ID"
        )

        val subject = SecurityProviderFactory(project.objects, MockedProviderFactory(emptyMap()))

        /* When */
        val credentials = subject.provide(OAuthCredentials::class.java) {
            it.logicalCluster(gradleProperties[BEARER_AUTH_LOGICAL_CLUSTER]!!)
            it.identityPoolId(gradleProperties[BEARER_AUTH_IDENTITY_POOL_ID]!!)
            it.clientId(gradleProperties[BEARER_AUTH_CLIENT_ID]!!)
            it.clientSecret(gradleProperties[BEARER_AUTH_CLIENT_SECRET]!!)
            it.scope(gradleProperties[BEARER_AUTH_SCOPE]!!)
            it.issuerEndpointUrl(URL(gradleProperties[BEARER_AUTH_ISSUER_ENDPOINT_URL]))
            it.cacheExpireBuffer(Duration.ofSeconds(gradleProperties[BEARER_AUTH_CACHE_EXPIRY_BUFFER_SECONDS]!!.toLong()))
            it.scopeClaimName(gradleProperties[BEARER_AUTH_SCOPE_CLAIM_NAME]!!)
            it.subClaimName(gradleProperties[BEARER_AUTH_SUB_CLAIM_NAME]!!)
        }

        /* Then */
        expectThat(credentials) {
            isA<Provider<OAuthCredentials>>()
            get(Provider<OAuthCredentials>::isPresent)
                .isTrue()
            get(Provider<OAuthCredentials>::get)
                .isA<OAuthCredentials>()
                .and {

                    get(OAuthCredentials::toProperties)
                        .hasEntry(BEARER_AUTH_CREDENTIALS_SOURCE, "OAUTHBEARER")
                        .hasEntry(BEARER_AUTH_LOGICAL_CLUSTER, gradleProperties[BEARER_AUTH_LOGICAL_CLUSTER])
                        .hasEntry(BEARER_AUTH_IDENTITY_POOL_ID, gradleProperties[BEARER_AUTH_IDENTITY_POOL_ID])
                        .hasEntry(BEARER_AUTH_CLIENT_ID, gradleProperties[BEARER_AUTH_CLIENT_ID])
                        .hasEntry(BEARER_AUTH_CLIENT_SECRET, gradleProperties[BEARER_AUTH_CLIENT_SECRET])
                        .hasEntry(BEARER_AUTH_SCOPE, gradleProperties[BEARER_AUTH_SCOPE])
                        .hasEntry(BEARER_AUTH_ISSUER_ENDPOINT_URL, gradleProperties[BEARER_AUTH_ISSUER_ENDPOINT_URL])
                        .hasEntry(BEARER_AUTH_CACHE_EXPIRY_BUFFER_SECONDS, gradleProperties[BEARER_AUTH_CACHE_EXPIRY_BUFFER_SECONDS])
                        .hasEntry(BEARER_AUTH_SCOPE_CLAIM_NAME, gradleProperties[BEARER_AUTH_SCOPE_CLAIM_NAME])
                        .hasEntry(BEARER_AUTH_SUB_CLAIM_NAME, gradleProperties[BEARER_AUTH_SUB_CLAIM_NAME])
                }
        }
    }

    @Test
    @DisplayName(
        """
        Given an 'OAuthCredentials' configured using both 'gradle.properties' and 'Kotlin DSL',
        when provided,
        then all values should match.
        """
    )
    fun id1680128844102() {

        /* Given */
        @Suppress("SpellCheckingInspection")
        val propFixture = mapOf(
            "$CLIENT_NAMESPACE$BEARER_AUTH_LOGICAL_CLUSTER" to "LSRC-XXXXX",
            "$CLIENT_NAMESPACE$BEARER_AUTH_IDENTITY_POOL_ID" to "5bed8df5-75a7-4f98-9879-5118629e5b1f",
            "$CLIENT_NAMESPACE$BEARER_AUTH_CLIENT_ID" to "LeoFuso",
            "$CLIENT_NAMESPACE$BEARER_AUTH_CLIENT_SECRET" to "26%sU*#@LP#4Zgr!@!o",
            "$CLIENT_NAMESPACE$BEARER_AUTH_SCOPE" to "schemas",
            "$CLIENT_NAMESPACE$BEARER_AUTH_ISSUER_ENDPOINT_URL" to "https://openid.io",
            "$CLIENT_NAMESPACE$BEARER_AUTH_CACHE_EXPIRY_BUFFER_SECONDS" to "180",
            "$CLIENT_NAMESPACE$BEARER_AUTH_SCOPE_CLAIM_NAME" to "email",
            "$CLIENT_NAMESPACE$BEARER_AUTH_SUB_CLAIM_NAME" to "auth0|USER-ID"
        )

        val subject = SecurityProviderFactory(
            project.objects,
            MockedProviderFactory(
                propFixture.filterNot { (key, _) ->
                    key.contains(BEARER_AUTH_CLIENT_ID) || key.contains(BEARER_AUTH_CLIENT_SECRET)
                }
            )
        )

        /* When */
        val credentials = subject.provide(OAuthCredentials::class.java) {
            it.clientId(propFixture["$CLIENT_NAMESPACE$BEARER_AUTH_CLIENT_ID"]!!)
            it.clientSecret(propFixture["$CLIENT_NAMESPACE$BEARER_AUTH_CLIENT_SECRET"]!!)
        }

        /* Then */
        expectThat(credentials) {
            isA<Provider<OAuthCredentials>>()
            get(Provider<OAuthCredentials>::isPresent)
                .isTrue()
            get(Provider<OAuthCredentials>::get)
                .isA<OAuthCredentials>()
                .and {

                    get(OAuthCredentials::toProperties)
                        .hasEntry(BEARER_AUTH_CREDENTIALS_SOURCE, "OAUTHBEARER")
                        .hasEntry(BEARER_AUTH_LOGICAL_CLUSTER, propFixture["$CLIENT_NAMESPACE$BEARER_AUTH_LOGICAL_CLUSTER"])
                        .hasEntry(BEARER_AUTH_IDENTITY_POOL_ID, propFixture["$CLIENT_NAMESPACE$BEARER_AUTH_IDENTITY_POOL_ID"])
                        .hasEntry(BEARER_AUTH_CLIENT_ID, propFixture["$CLIENT_NAMESPACE$BEARER_AUTH_CLIENT_ID"])
                        .hasEntry(BEARER_AUTH_CLIENT_SECRET, propFixture["$CLIENT_NAMESPACE$BEARER_AUTH_CLIENT_SECRET"])
                        .hasEntry(BEARER_AUTH_SCOPE, propFixture["$CLIENT_NAMESPACE$BEARER_AUTH_SCOPE"])
                        .hasEntry(BEARER_AUTH_ISSUER_ENDPOINT_URL, propFixture["$CLIENT_NAMESPACE$BEARER_AUTH_ISSUER_ENDPOINT_URL"])
                        .hasEntry(
                            BEARER_AUTH_CACHE_EXPIRY_BUFFER_SECONDS,
                            propFixture["$CLIENT_NAMESPACE$BEARER_AUTH_CACHE_EXPIRY_BUFFER_SECONDS"]
                        )
                        .hasEntry(BEARER_AUTH_SCOPE_CLAIM_NAME, propFixture["$CLIENT_NAMESPACE$BEARER_AUTH_SCOPE_CLAIM_NAME"])
                        .hasEntry(BEARER_AUTH_SUB_CLAIM_NAME, propFixture["$CLIENT_NAMESPACE$BEARER_AUTH_SUB_CLAIM_NAME"])
                }
        }
    }

    @Test
    @DisplayName(
        """
        Given an 'OAuthCredentials' with missing required properties,
        when provided,
        then fail, accusing 'missing properties'.
        """
    )
    fun id1680131032529() {

        /* Given */
        @Suppress("SpellCheckingInspection")
        val gradleProperties = mapOf(
            "$CLIENT_NAMESPACE$BEARER_AUTH_LOGICAL_CLUSTER" to "LSRC-XXXXX",
            "$CLIENT_NAMESPACE$BEARER_AUTH_IDENTITY_POOL_ID" to "5bed8df5-75a7-4f98-9879-5118629e5b1f",
            "$CLIENT_NAMESPACE$BEARER_AUTH_SCOPE" to "schemas",
            "$CLIENT_NAMESPACE$BEARER_AUTH_ISSUER_ENDPOINT_URL" to "https://openid.io",
            "$CLIENT_NAMESPACE$BEARER_AUTH_CACHE_EXPIRY_BUFFER_SECONDS" to "180",
            "$CLIENT_NAMESPACE$BEARER_AUTH_SCOPE_CLAIM_NAME" to "email",
            "$CLIENT_NAMESPACE$BEARER_AUTH_SUB_CLAIM_NAME" to "auth0|USER-ID"
        )

        val subject = SecurityProviderFactory(project.objects, MockedProviderFactory(gradleProperties))

        /* When */
        val credentials = subject.provide(OAuthCredentials::class.java)

        /* Then */
        expectThat(credentials) {
            isA<Provider<OAuthCredentials>>()

            expectCatching { credentials.get() }
                .isFailure()
                .isA<MissingValueException>()
        }
    }

    @Test
    @DisplayName(
        """
        Given an 'OAuthCredentials' with missing optional properties,
        when provided,
        then all values should match.
        """
    )
    fun id1680131032252() {

        /* Given */
        @Suppress("SpellCheckingInspection")
        val gradleProperties = mapOf(
            "$CLIENT_NAMESPACE$BEARER_AUTH_LOGICAL_CLUSTER" to "LSRC-XXXXX",
            "$CLIENT_NAMESPACE$BEARER_AUTH_IDENTITY_POOL_ID" to "5bed8df5-75a7-4f98-9879-5118629e5b1f",
            "$CLIENT_NAMESPACE$BEARER_AUTH_CLIENT_ID" to "LeoFuso",
            "$CLIENT_NAMESPACE$BEARER_AUTH_CLIENT_SECRET" to "26%sU*#@LP#4Zgr!@!o",
            "$CLIENT_NAMESPACE$BEARER_AUTH_ISSUER_ENDPOINT_URL" to "https://openid.io"
        )

        val subject = SecurityProviderFactory(project.objects, MockedProviderFactory(gradleProperties))

        /* When */
        val credentials = subject.provide(OAuthCredentials::class.java)

        /* Then */
        expectThat(credentials) {
            isA<Provider<OAuthCredentials>>()
            get(Provider<OAuthCredentials>::isPresent)
                .isTrue()
            get(Provider<OAuthCredentials>::get)
                .isA<OAuthCredentials>()
                .and {

                    get(OAuthCredentials::toProperties)
                        .hasEntry(BEARER_AUTH_CREDENTIALS_SOURCE, "OAUTHBEARER")
                        .hasEntry(BEARER_AUTH_LOGICAL_CLUSTER, gradleProperties["$CLIENT_NAMESPACE$BEARER_AUTH_LOGICAL_CLUSTER"])
                        .hasEntry(BEARER_AUTH_IDENTITY_POOL_ID, gradleProperties["$CLIENT_NAMESPACE$BEARER_AUTH_IDENTITY_POOL_ID"])
                        .hasEntry(BEARER_AUTH_CLIENT_ID, gradleProperties["$CLIENT_NAMESPACE$BEARER_AUTH_CLIENT_ID"])
                        .hasEntry(BEARER_AUTH_CLIENT_SECRET, gradleProperties["$CLIENT_NAMESPACE$BEARER_AUTH_CLIENT_SECRET"])
                        .doesNotContainKey(BEARER_AUTH_SCOPE)
                        .hasEntry(BEARER_AUTH_ISSUER_ENDPOINT_URL, gradleProperties["$CLIENT_NAMESPACE$BEARER_AUTH_ISSUER_ENDPOINT_URL"])
                        .doesNotContainKey(BEARER_AUTH_CACHE_EXPIRY_BUFFER_SECONDS)
                        .doesNotContainKey(BEARER_AUTH_SCOPE_CLAIM_NAME)
                        .doesNotContainKey(BEARER_AUTH_SUB_CLAIM_NAME)
                }
        }
    }
}
