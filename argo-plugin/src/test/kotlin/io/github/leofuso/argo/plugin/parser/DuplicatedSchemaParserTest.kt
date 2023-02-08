package io.github.leofuso.argo.plugin.parser

import io.github.leofuso.argo.plugin.compiler.exception.NonDeterministicSchemaResolutionException
import io.github.leofuso.argo.plugin.compiler.parser.DefaultSchemaParser
import io.github.leofuso.argo.plugin.compiler.parser.DependencyGraphAwareSchemaParser
import io.github.leofuso.argo.plugin.fixtures.FileTreeParameterResolver
import io.github.leofuso.argo.plugin.fixtures.annotation.SchemaParameter
import io.github.leofuso.argo.plugin.fixtures.loadResource
import io.github.leofuso.argo.plugin.fixtures.permutations
import io.github.leofuso.argo.plugin.fixtures.scenarios
import org.apache.avro.Schema
import org.assertj.core.api.Assertions
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

/**
 * This situation is generally encountered when schema files define records with inline record/enum definitions, and those inline types
 * are used in more than one file.
 */
@DisplayName("Unit tests related to handling of duplicate type definitions.")
@Extensions(ExtendWith(FileTreeParameterResolver::class))
class DuplicatedSchemaParserTest {

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
 Given repeated Schema definitions within a single file,
 when parsing,
 then raise an exception.
"""
    )
    fun t0(@SchemaParameter(location = "parser/scenarios/repeated/SingleFile.avsc") graph: FileTree) {
        /* When then */
        Assertions.assertThatThrownBy { subject.parse(graph) }
            .isInstanceOf(NonDeterministicSchemaResolutionException::class.java)
            .hasMessageContaining("[io.github.leofuso.argo.plugin.parser.Date]")
    }

    @Test
    @DisplayName(
        """
 Given repeated Schema definitions within a single file, with different contents,
 when parsing,
 then raise an exception.
"""
    )
    fun t1(@SchemaParameter(location = "parser/scenarios/repeated/SingleFileDifferent.avsc") graph: FileTree) {
        /* When then */
        Assertions.assertThatThrownBy { subject.parse(graph) }
            .isInstanceOf(NonDeterministicSchemaResolutionException::class.java)
            .hasMessageContaining("[io.github.leofuso.argo.plugin.parser.Date]")
    }

    @TestFactory
    @DisplayName(
        """
 Given repeated Schema definitions, with equal content in different def. files
 when parsing,
 then should resolve all Schema definitions normally.
"""
    )
    fun t2(@SchemaParameter(location = "parser/scenarios/repeated/equal") graph: FileTree) = scenarios(project, graph) {
        /* When */
        val resolution = subject.parse(it)

        /* Then */
        Assertions.assertThat(resolution)
            .extracting("schemas")
            .asInstanceOf(InstanceOfAssertFactories.map(String::class.java, Schema::class.java))
            .containsOnlyKeys(
                "io.github.leofuso.argo.plugin.parser.Breed",
                "io.github.leofuso.argo.plugin.parser.Cat"
            )
    }

    @TestFactory
    @DisplayName(
        """
 Given repeated Schema definitions, with different contents in different def. files
 when parsing,
 then raise an exception.
"""
    )
    fun t3(@SchemaParameter(location = "parser/scenarios/repeated/different") graph: FileTree) = scenarios(project, graph) {
        /* When then */
        Assertions.assertThatThrownBy { subject.parse(it) }
            .isInstanceOf(NonDeterministicSchemaResolutionException::class.java)
            .hasMessageContaining("[io.github.leofuso.argo.plugin.parser.Breed]")
    }

    @TestFactory
    @DisplayName(
        """
 Given multiple repeated and nested Schema definitions, with equal contents,
 when parsing,
 then should resolve all compatible Schema definitions.
"""
    )
    fun t4(@SchemaParameter(location = "parser/scenarios/repeated/nested/equal") graph: FileTree) = scenarios(project, graph) {
        /* When */
        val resolution = subject.parse(it)

        /* Then */
        Assertions.assertThat(resolution)
            .extracting("schemas")
            .asInstanceOf(InstanceOfAssertFactories.map(String::class.java, Schema::class.java))
            .containsOnlyKeys(
                "example.Person",
                "example.Gender",
                "example.Dog",
                "example.Order"
            )
    }

    @TestFactory
    @DisplayName(
        """
 Given multiple repeated and nested Schema definitions, with different contents,
 when parsing,
 then raise an exception.
"""
    )
    fun t5(@SchemaParameter(location = "parser/scenarios/repeated/nested/different") graph: FileTree) = scenarios(project, graph) {
        /* When Then */
        Assertions.assertThatThrownBy { subject.parse(it) }
            .isInstanceOf(NonDeterministicSchemaResolutionException::class.java)
            .hasMessageContaining("[example.Gender]")
    }

    @TestFactory
    @DisplayName(
        """
 Given repeated fixed definitions, with equal contents,
 when parsing,
 then should resolve all Schemas.
"""
    )
    fun t6(): List<DynamicTest> {
        /* Given */
        return permutations(
            project,
            loadResource("parser/scenarios/repeated/multiple/ContainsFixed1.avsc"),
            loadResource("parser/scenarios/repeated/multiple/ContainsFixed2.avsc")
        )
            .map { permutation ->

                DynamicTest.dynamicTest("Scenario ${permutation.key}") {
                    /* When */
                    val resolution = subject.parse(permutation.value)

                    /* Then */
                    Assertions.assertThat(resolution)
                        .extracting("schemas")
                        .asInstanceOf(InstanceOfAssertFactories.map(String::class.java, Schema::class.java))
                        .containsOnlyKeys(
                            "example.ContainsFixed1",
                            "example.ContainsFixed2",
                            "example.Picture"
                        )
                }
            }.toList()
    }

    @TestFactory
    @DisplayName(
        """
 Given repeated fixed definitions, with different contents,
 when parsing,
 then should fail.
"""
    )
    fun t7(): List<DynamicTest> {
        /* Given */
        return permutations(
            project,
            loadResource("parser/scenarios/repeated/multiple/ContainsFixed1.avsc"),
            loadResource("parser/scenarios/repeated/multiple/ContainsFixed3.avsc")
        )
            .map { permutation ->

                DynamicTest.dynamicTest("Scenario ${permutation.key}") {
                    /* When then */
                    Assertions.assertThatThrownBy { subject.parse(permutation.value) }
                        .isInstanceOf(NonDeterministicSchemaResolutionException::class.java)
                        .hasMessageContaining("[example.Picture]")
                }
            }.toList()
    }
}
