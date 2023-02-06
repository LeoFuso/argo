@file:Suppress("JUnitMalformedDeclaration")

package io.github.leofuso.argo.plugin

import io.github.leofuso.argo.plugin.compiler.parser.DefaultSchemaParser
import io.github.leofuso.argo.plugin.compiler.parser.DependencyGraphAwareSchemaParser
import io.github.leofuso.argo.plugin.fixtures.FileTreeParameterResolver
import io.github.leofuso.argo.plugin.fixtures.annotation.SchemaParameter
import io.github.leofuso.argo.plugin.fixtures.loadResource
import io.github.leofuso.argo.plugin.fixtures.permutations
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

@DisplayName("SchemaParser Unit tests")
@Extensions(ExtendWith(FileTreeParameterResolver::class))
class SchemaParserTest {

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
    fun cabacc37b86b4baea31aaa461e8b7cde() {

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
    fun cabacc37b86b4baea31aaa461e8b7cdf(): List<DynamicTest> {

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
            }.toList()
    }

    @TestFactory
    @DisplayName(
        """
        Given a Schema with missing leaf dependency,
 when parsing,
 then should not resolve into any valid Schemas.
        """
    )
    fun cabacc37b86b4baea31aaa461e8b7cdb(@SchemaParameter(location = "parser/scenarios/missing/leaf") graph: FileTree): List<DynamicTest> {
        return permutations(project, graph)
            .map { permutation ->

                DynamicTest.dynamicTest("Scenario ${permutation.key}") {

                    /* When */
                    val resolution = subject.parse(permutation.value)

                    /* Then */
                    assertThat(resolution)
                        .extracting("schemas")
                        .asInstanceOf(InstanceOfAssertFactories.map(String::class.java, Schema::class.java))
                        .isEmpty()
                }
            }.toList()
    }

    @TestFactory
    @DisplayName(
        """
        Given a Schema with missing root dependency,
 when parsing,
 then should resolve only leaf dependencies into a valid Schema.
        """
    )
    fun cabacc37b86b4baea31aaa461e8b7cdr(@SchemaParameter(location = "parser/scenarios/missing/root") graph: FileTree): List<DynamicTest> {
        return permutations(project, graph)
            .map { permutation ->

                DynamicTest.dynamicTest("Scenario ${permutation.key}") {

                    /* When */
                    val resolution = subject.parse(permutation.value)

                    /* Then */
                    assertThat(resolution)
                        .extracting("schemas")
                        .asInstanceOf(InstanceOfAssertFactories.map(String::class.java, Schema::class.java))
                        .containsOnlyKeys(
                            "io.github.leofuso.obs.demo.events.StatementLine",
                            "io.github.leofuso.obs.demo.events.ReceiptLine"
                        )
                }
            }.toList()
    }

    @TestFactory
    @DisplayName(
        """
        Given a Schema with missing middle dependency,
 when parsing,
 then should resolve only available leaf dependency into a valid Schema.
        """
    )
    fun cabacc37b86b4baea31aaa461e8b7cdf(@SchemaParameter(location = "parser/scenarios/missing/middle") graph: FileTree): List<DynamicTest> {
        return permutations(project, graph)
            .map { permutation ->

                DynamicTest.dynamicTest("Scenario ${permutation.key}") {

                    /* When */
                    val resolution = subject.parse(permutation.value)

                    /* Then */
                    assertThat(resolution)
                        .extracting("schemas")
                        .asInstanceOf(InstanceOfAssertFactories.map(String::class.java, Schema::class.java))
                        .containsOnlyKeys(
                            "io.github.leofuso.obs.demo.events.StatementLine"
                        )
                }
            }.toList()
    }

    @Test
    @DisplayName(
        """
        Given an inlined Schema with two dependencies,
 when parsing,
 then should resolve into three self-contained valid Schemas.
        """
    )
    fun cabacc37b86b4baea31aaa461e8b7cd2(@SchemaParameter(location = "parser/scenarios/inline") graph: FileTree) {

        /* When */
        val resolution = subject.parse(graph)

        /* Then */
        assertThat(resolution)
            .extracting("schemas")
            .asInstanceOf(InstanceOfAssertFactories.map(String::class.java, Schema::class.java))
            .containsOnlyKeys(
                "io.github.leofuso.obs.demo.events.Receipt"
            )
    }
}
