package io.github.leofuso.argo.plugin.parser

import io.github.leofuso.argo.plugin.compiler.parser.DefaultSchemaParser
import io.github.leofuso.argo.plugin.compiler.parser.DependencyGraphAwareSchemaParser
import io.github.leofuso.argo.plugin.fixtures.FileTreeParameterResolver
import io.github.leofuso.argo.plugin.fixtures.loadResource
import org.apache.avro.Protocol
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import java.io.File

@DisplayName("Unit tests related to handling Protocol type definitions.")
@Extensions(ExtendWith(FileTreeParameterResolver::class))
class ProtocolParserTest {

    @TempDir
    private lateinit var rootDir: File

    private lateinit var project: Project
    private lateinit var subject: DependencyGraphAwareSchemaParser

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder
            .builder()
            .withProjectDir(rootDir)
            .withName("project-fixture")
            .build()
        subject = DefaultSchemaParser(project.logger)
    }

    @Test
    @DisplayName(
        """
 Given a Protocol,
 when parsing,
 then should resolve into a valid Protocol.
"""
    )
    fun t0() {
        /* Given */
        val resource = loadResource("parser/scenarios/protocol/mail.avpr")
        val source = project.objects.fileCollection().from(resource.absolutePath).asFileTree

        /* When */
        val resolution = subject.parse(source)

        /* Then */
        assertThat(resolution)
            .extracting("protocol")
            .asInstanceOf(InstanceOfAssertFactories.map(String::class.java, Protocol::class.java))
            .containsOnlyKeys("Mail")
    }
}
