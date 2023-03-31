package io.github.leofuso.argo.plugin.navis

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig.USER_INFO_CONFIG
import io.github.leofuso.argo.plugin.fixtures.MockedProviderFactory
import io.github.leofuso.argo.plugin.navis.security.SecurityProviderFactory
import io.github.leofuso.argo.plugin.navis.security.credentials.JAASCredentials
import io.github.leofuso.argo.plugin.navis.security.credentials.SaslBasicAuthCredentials
import io.github.leofuso.argo.plugin.navis.security.credentials.UserInfoCredentials
import org.apache.kafka.common.config.SaslConfigs.*
import org.apache.kafka.common.security.plain.PlainLoginModule
import org.apache.kafka.common.security.scram.ScramLoginModule
import org.eclipse.jetty.jaas.spi.JDBCLoginModule
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
import strikt.assertions.hasEntry
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.io.File
import javax.security.auth.spi.LoginModule

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
        Given a basic auth 'JAASCredentials' entirely configured using the individual 'gradle.properties',
        when provided,
        then all values should match.
        """
    )
    fun id1680112676357() {

        /* Given */
        val expectedLoginModule = ScramLoginModule::class.java
        val expectedControlFlag = JAASCredentials.LoginModuleControlFlag.REQUIRED
        val expectedUsername = "LeoFuso"
        val expectedPassword = "secret"

        val expectedConfig =
            "${expectedLoginModule.name} ${expectedControlFlag.flag} username='$expectedUsername' password='$expectedPassword';"

        val gradleProperties = mapOf(
            "schema.registry.sasl.jaas.config.login.module" to expectedLoginModule.name,
            "schema.registry.sasl.jaas.config.control.flag" to expectedControlFlag.flag,
            "schema.registry.sasl.jaas.config.option.username" to expectedUsername,
            "schema.registry.sasl.jaas.config.option.password" to expectedPassword
        )

        val subject = SecurityProviderFactory(project.objects, MockedProviderFactory(gradleProperties))

        /* When */
        val credentials = subject.provide(SaslBasicAuthCredentials::class.java)

        /* Then */
        expectThat(credentials) {
            isA<Provider<JAASCredentials>>()
            get(Provider<SaslBasicAuthCredentials>::isPresent)
                .isTrue()
            get(Provider<SaslBasicAuthCredentials>::get)
                .isA<JAASCredentials>()
                .and {

                    get { getLoginModule().get() }
                        .isEqualTo(expectedLoginModule)

                    get { getLoginModuleControlFlag().get() }
                        .isEqualTo(expectedControlFlag)

                    get { getOptions().get() }
                        .hasEntry("username", expectedUsername)
                        .hasEntry("password", expectedPassword)

                    get(JAASCredentials::toProperties)
                        .hasEntry(BASIC_AUTH_CREDENTIALS_SOURCE, "SASL_INHERIT")
                        .hasEntry(SASL_JAAS_CONFIG, expectedConfig)
                }
        }
    }

    @Test
    @DisplayName(
        """
        Given a basic auth 'JAASCredentials' entirely configured using root attribute with 'gradle.properties',
        when provided,
        then all values should match.
        """
    )
    fun id1680126508274() {

        /* Given */
        val expectedLoginModule = PlainLoginModule::class.java
        val expectedControlFlag = JAASCredentials.LoginModuleControlFlag.REQUIRED
        val expectedUsername = "LeoFuso"
        val expectedPassword = "secret"

        val expectedConfig =
            "${expectedLoginModule.name} ${expectedControlFlag.flag} username='$expectedUsername' password='$expectedPassword';"

        val gradleProperties = mapOf("schema.registry.sasl.jaas.config" to expectedConfig)

        val subject = SecurityProviderFactory(project.objects, MockedProviderFactory(gradleProperties))

        /* When */
        val credentials = subject.provide(SaslBasicAuthCredentials::class.java)

        /* Then */
        expectThat(credentials) {
            isA<Provider<SaslBasicAuthCredentials>>()
            get(Provider<SaslBasicAuthCredentials>::isPresent)
                .isTrue()
            get(Provider<SaslBasicAuthCredentials>::get)
                .isA<SaslBasicAuthCredentials>()
                .and {

                    get { getLoginModule().isPresent }
                        .isFalse()

                    get { getLoginModuleControlFlag().isPresent }
                        .isFalse()

                    get { getOptions().get() }
                        .isEmpty()

                    get(SaslBasicAuthCredentials::toProperties)
                        .hasEntry(BASIC_AUTH_CREDENTIALS_SOURCE, "SASL_INHERIT")
                        .hasEntry(SASL_JAAS_CONFIG, expectedConfig)
                }
        }
    }

    @Test
    @DisplayName(
        """
        Given a basic auth 'JAASCredentials' entirely configured using 'Kotlin DSL' in the individual properties,
        when provided,
        then all values should match.
        """
    )
    fun id1680128156467() {

        /* Given */
        val expectedLoginModule = ScramLoginModule::class.java
        val expectedControlFlag = JAASCredentials.LoginModuleControlFlag.REQUIRED
        val expectedUsername = "LeoFuso"
        val expectedPassword = "secret"

        val expectedConfig =
            "${expectedLoginModule.name} ${expectedControlFlag.flag} username='$expectedUsername' password='$expectedPassword';"

        val subject = SecurityProviderFactory(project.objects, MockedProviderFactory(emptyMap()))

        /* When */
        val credentials = subject.provide(SaslBasicAuthCredentials::class.java) {
            it.loginModule(expectedLoginModule)
            it.controlFlag(expectedControlFlag)
            it.option("username", expectedUsername)
            it.option("password", expectedPassword)
        }

        /* Then */
        expectThat(credentials) {
            isA<Provider<SaslBasicAuthCredentials>>()
            get(Provider<SaslBasicAuthCredentials>::isPresent)
                .isTrue()
            get(Provider<SaslBasicAuthCredentials>::get)
                .isA<SaslBasicAuthCredentials>()
                .and {

                    get { getLoginModule().get() }
                        .isEqualTo(expectedLoginModule)

                    get { getLoginModuleControlFlag().get() }
                        .isEqualTo(expectedControlFlag)

                    get { getOptions().get() }
                        .hasEntry("username", expectedUsername)
                        .hasEntry("password", expectedPassword)

                    get(SaslBasicAuthCredentials::toProperties)
                        .hasEntry(BASIC_AUTH_CREDENTIALS_SOURCE, "SASL_INHERIT")
                        .hasEntry(SASL_JAAS_CONFIG, expectedConfig)
                }
        }
    }

    @Test
    @DisplayName(
        """
        Given a basic auth 'JAASCredentials' entirely configured using 'Kotlin DSL' in the root property,
        when provided,
        then all values should match.
        """
    )
    fun id1680128523149() {

        /* Given */
        val expectedLoginModule = PlainLoginModule::class.java
        val expectedControlFlag = JAASCredentials.LoginModuleControlFlag.REQUIRED
        val expectedUsername = "LeoFuso"
        val expectedPassword = "secret"

        val expectedConfig =
            "${expectedLoginModule.name} ${expectedControlFlag.flag} username='$expectedUsername' password='$expectedPassword';"

        val subject = SecurityProviderFactory(project.objects, MockedProviderFactory(emptyMap()))

        /* When */
        val credentials = subject.provide(SaslBasicAuthCredentials::class.java) {
            it.saslJaasConfig(expectedConfig)
        }

        /* Then */
        expectThat(credentials) {
            isA<Provider<SaslBasicAuthCredentials>>()
            get(Provider<SaslBasicAuthCredentials>::isPresent)
                .isTrue()
            get(Provider<SaslBasicAuthCredentials>::get)
                .isA<SaslBasicAuthCredentials>()
                .and {

                    get(SaslBasicAuthCredentials::getLoginModule)
                        .get(Provider<Class<out LoginModule>>::isPresent)
                        .isFalse()

                    get(SaslBasicAuthCredentials::getLoginModuleControlFlag)
                        .get(Provider<JAASCredentials.LoginModuleControlFlag>::isPresent)
                        .isFalse()

                    get { getOptions().get() }
                        .isEmpty()

                    get(SaslBasicAuthCredentials::toProperties)
                        .hasEntry(BASIC_AUTH_CREDENTIALS_SOURCE, "SASL_INHERIT")
                        .hasEntry(SASL_JAAS_CONFIG, expectedConfig)
                }
        }
    }

    @Test
    @DisplayName(
        """
        Given a basic auth 'JAASCredentials' configured using both 'gradle.properties' and 'Kotlin DSL',
        when provided,
        then all values should match.
        """
    )
    fun id1680128844102() {

        /* Given */
        val expectedLoginModule = JDBCLoginModule::class.java
        val expectedControlFlag = JAASCredentials.LoginModuleControlFlag.REQUIRED
        val expectedDbDriver = "org.postgresql.Driver"
        val expectedDbUrl = "jdbc:postgresql://fake-dns:5432/vault-db"
        val expectedDbUsername = "LeoFuso"
        val expectedDbPassword = "secret"

        val expectedConfig =
            "${expectedLoginModule.name} ${expectedControlFlag.flag} " +
                "dbDriver='$expectedDbDriver' " +
                "dbUrl='$expectedDbUrl' " +
                "dbUserName='$expectedDbUsername' " +
                "dbPassword='$expectedDbPassword';"

        val gradleProperties = mapOf(
            "schema.registry.sasl.jaas.config.option.dbUrl" to "jdbc:postgresql://localhost:5432/vault-db",
            "schema.registry.sasl.jaas.config.option.dbPassword" to expectedDbPassword
        )

        val subject = SecurityProviderFactory(project.objects, MockedProviderFactory(gradleProperties))

        /* When */
        val credentials = subject.provide(SaslBasicAuthCredentials::class.java) {
            it.loginModule(expectedLoginModule)
            it.controlFlag(expectedControlFlag)
            it.option("dbDriver", expectedDbDriver)
            it.option("dbUrl", expectedDbUrl)
            it.option("dbUserName", expectedDbUsername)
        }

        /* Then */
        expectThat(credentials) {
            isA<Provider<SaslBasicAuthCredentials>>()
            get(Provider<SaslBasicAuthCredentials>::isPresent)
                .isTrue()
            get(Provider<SaslBasicAuthCredentials>::get)
                .isA<SaslBasicAuthCredentials>()
                .and {

                    get { getLoginModule().get() }
                        .isEqualTo(expectedLoginModule)

                    get { getLoginModuleControlFlag().get() }
                        .isEqualTo(expectedControlFlag)

                    get { getOptions().get() }
                        .hasEntry("dbDriver", expectedDbDriver)
                        .hasEntry("dbUrl", expectedDbUrl)
                        .hasEntry("dbUserName", expectedDbUsername)
                        .hasEntry("dbPassword", expectedDbPassword)

                    get(SaslBasicAuthCredentials::toProperties)
                        .hasEntry(BASIC_AUTH_CREDENTIALS_SOURCE, "SASL_INHERIT")
                        .hasEntry(SASL_JAAS_CONFIG, expectedConfig)
                }
        }
    }

    @Test
    @DisplayName(
        """
        Given a basic auth 'JAASCredentials' with missing required properties,
        when provided,
        then fail, accusing 'missing properties'.
        """
    )
    fun id1680131032529() {

        /* Given */
        val expectedDbUrl = "jdbc:postgresql://fake-dns:5432/vault-db"
        val expectedDbPassword = "secret"

        val gradleProperties = mapOf(
            "schema.registry.sasl.jaas.config.option.dbUrl" to expectedDbUrl,
            "schema.registry.sasl.jaas.config.option.dbPassword" to expectedDbPassword
        )

        val subject = SecurityProviderFactory(project.objects, MockedProviderFactory(gradleProperties))

        /* When */
        val credentials = subject.provide(SaslBasicAuthCredentials::class.java)

        /* Then */
        expectThat(credentials) {
            isA<Provider<SaslBasicAuthCredentials>>()

            expectCatching { credentials.get() }
                .isFailure()
                .isA<MissingValueException>()
        }
    }
}
