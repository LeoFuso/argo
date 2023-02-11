@file:Suppress("JUnitMalformedDeclaration")

package io.github.leofuso.argo.plugin.parser

import io.github.leofuso.argo.plugin.compiler.parser.DefaultSchemaParser
import io.github.leofuso.argo.plugin.compiler.parser.DependencyGraphAwareSchemaParser
import io.github.leofuso.argo.plugin.fixtures.FileTreeParameterResolver
import io.github.leofuso.argo.plugin.fixtures.annotation.SchemaParameter
import io.github.leofuso.argo.plugin.fixtures.loadResource
import io.github.leofuso.argo.plugin.fixtures.permutations
import io.github.leofuso.argo.plugin.fixtures.scenarios
import org.apache.avro.Schema
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.*

@DisplayName("SchemaParser: Unit tests related to dependency resolution of type definitions.")
@Extensions(ExtendWith(FileTreeParameterResolver::class))
class DependencyResolutionParserTest {

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
 Given a Schema with no dependencies,
 when parsing,
 then should resolve into a valid Schema.
"""
    )
    fun t0() {
        /* Given */
        val resource = loadResource("parser/scenarios/common/Record.avsc")
        val source = project.objects.fileCollection().from(resource.absolutePath).asFileTree

        /* When */
        val resolution = subject.parse(source)

        /* Then */
        assertThat(resolution)
            .extracting("schemas")
            .asInstanceOf(InstanceOfAssertFactories.map(String::class.java, Schema::class.java))
            .containsOnlyKeys("io.github.leofuso.argo.plugin.parser.Record")
    }

    @TestFactory
    @DisplayName(
        """
 Given a Schema containing dependencies from somewhere up the chain,
 when parsing,
 then should resolve into a valid Schema.
"""
    )
    fun t1(): List<DynamicTest> {
        /* Given */
        return permutations(
            project,
            loadResource("parser/scenarios/common/Record.avsc"),
            loadResource("parser/scenarios/common/Enum.avsc"),
            loadResource("parser/scenarios/common/Fixed.avsc"),
            loadResource("parser/scenarios/common/UseRecordWithArray.avsc"),
            loadResource("parser/scenarios/common/UseRecordWithType.avsc")
        )
            .map { permutation ->

                DynamicTest.dynamicTest("Scenario ${permutation.key}") {
                    /* When */
                    val resolution = subject.parse(permutation.value)

                    /* Then */
                    assertThat(resolution)
                        .extracting("schemas")
                        .asInstanceOf(InstanceOfAssertFactories.map(String::class.java, Schema::class.java))
                        .containsOnlyKeys(
                            "io.github.leofuso.argo.plugin.parser.Enum",
                            "io.github.leofuso.argo.plugin.parser.Fixed",
                            "io.github.leofuso.argo.plugin.parser.Record",
                            "io.github.leofuso.argo.plugin.parser.UseRecordWithArray",
                            "io.github.leofuso.argo.plugin.parser.UseRecordWithType"
                        )
                }
            }
            .toList()
    }

    @TestFactory
    @DisplayName(
        """
 Given a Schema with missing leaf dependency,
 when parsing,
 then should not resolve into any valid Schemas.
"""
    )
    fun t2(@SchemaParameter(location = "parser/scenarios/missing/leaf") graph: FileTree) = scenarios(project, graph) {
        /* When */
        val resolution = subject.parse(it)

        /* Then */
        assertThat(resolution)
            .extracting("schemas")
            .asInstanceOf(InstanceOfAssertFactories.map(String::class.java, Schema::class.java))
            .isEmpty()
    }

    @TestFactory
    @DisplayName(
        """
 Given a Schema with missing root dependency,
 when parsing,
 then should resolve only leaf dependencies into a valid Schema.
"""
    )
    fun t3(@SchemaParameter(location = "parser/scenarios/missing/root") graph: FileTree) = scenarios(project, graph) {
        /* When */
        val resolution = subject.parse(it)

        /* Then */
        assertThat(resolution)
            .extracting("schemas")
            .asInstanceOf(InstanceOfAssertFactories.map(String::class.java, Schema::class.java))
            .containsOnlyKeys(
                "io.github.leofuso.obs.demo.events.StatementLine",
                "io.github.leofuso.obs.demo.events.Source",
                "io.github.leofuso.obs.demo.events.Details",
                "io.github.leofuso.obs.demo.events.Department",
                "io.github.leofuso.obs.demo.events.Operation",
                "io.github.leofuso.obs.demo.events.Ratio",
                "io.github.leofuso.obs.demo.events.ReceiptLine"
            )
    }

    @TestFactory
    @DisplayName(
        """
 Given a Schema with missing middle dependency,
 when parsing,
 then should resolve only available leaf dependencies into a valid Schema.
"""
    )
    fun t4(@SchemaParameter(location = "parser/scenarios/missing/middle") graph: FileTree) = scenarios(project, graph) {
        /* When */
        val resolution = subject.parse(it)

        /* Then */
        assertThat(resolution)
            .extracting("schemas")
            .asInstanceOf(InstanceOfAssertFactories.map(String::class.java, Schema::class.java))
            .containsOnlyKeys(
                "io.github.leofuso.obs.demo.events.StatementLine",
                "io.github.leofuso.obs.demo.events.Source",
                "io.github.leofuso.obs.demo.events.Details",
                "io.github.leofuso.obs.demo.events.Department",
                "io.github.leofuso.obs.demo.events.Operation"
            )
    }

    @Test
    @DisplayName(
        """
 Given an inlined Schema with two dependencies,
 when parsing,
 then should resolve into three self-contained valid Schemas.
"""
    )
    fun t5(@SchemaParameter(location = "parser/scenarios/inline") graph: FileTree) {
        /* When */
        val resolution = subject.parse(graph)

        /* Then */
        assertThat(resolution)
            .extracting("schemas")
            .asInstanceOf(InstanceOfAssertFactories.map(String::class.java, Schema::class.java))
            .containsOnlyKeys(
                "io.github.leofuso.obs.demo.events.StatementLine",
                "io.github.leofuso.obs.demo.events.Source",
                "io.github.leofuso.obs.demo.events.Details",
                "io.github.leofuso.obs.demo.events.Department",
                "io.github.leofuso.obs.demo.events.Operation",
                "io.github.leofuso.obs.demo.events.Ratio",
                "io.github.leofuso.obs.demo.events.Receipt",
                "io.github.leofuso.obs.demo.events.ReceiptLine"
            )
    }

    @TestFactory
    @DisplayName(
        """
 Given a Schema with a more-than-one dependency down the chain,
 when parsing,
 then should resolve all three available Schemas.
"""
    )
    fun t6(@SchemaParameter(location = "parser/scenarios/reference/chain") graph: FileTree) = scenarios(project, graph) {
        /* When */
        val resolution = subject.parse(it)

        /* Then */
        assertThat(resolution)
            .extracting("schemas")
            .asInstanceOf(InstanceOfAssertFactories.map(String::class.java, Schema::class.java))
            .containsOnlyKeys(
                "io.github.leofuso.obs.demo.events.StatementLine",
                "io.github.leofuso.obs.demo.events.Source",
                "io.github.leofuso.obs.demo.events.Details",
                "io.github.leofuso.obs.demo.events.Department",
                "io.github.leofuso.obs.demo.events.Operation",
                "io.github.leofuso.obs.demo.events.Ratio",
                "io.github.leofuso.obs.demo.events.Receipt",
                "io.github.leofuso.obs.demo.events.ReceiptLine"
            )
    }
}
