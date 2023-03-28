package io.github.leofuso.argo.plugin.navis

import io.github.leofuso.argo.plugin.fixtures.MockedProviderFactory
import io.github.leofuso.argo.plugin.navis.security.UserInfoCredentials
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CredentialsProviderFactoryTest {

    @TempDir
    private lateinit var rootDir: File
    private lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder
            .builder()
            .withProjectDir(rootDir)
            .withName("credentials-provider-factory-test")
            .build()
    }

    @Test
    fun stuff() {

        val subject = CredentialsProviderFactory(
            project.objects,
            MockedProviderFactory(
                mapOf(
                    "schema.registry.basic.auth.user.info.username" to "Unknown",
                    "schema.registry.basic.auth.user.info.password" to "secret"
                )
            )
        )

        val credentials = subject.provide(UserInfoCredentials::class.java) { it.username("LeoFuso") }
        assertThat(credentials.get().getUsername().get()).isEqualTo("LeoFuso")
    }
}
