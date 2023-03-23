package io.github.leofuso.columba.cli.parser

import io.github.leofuso.columba.cli.ConsoleLogger
import io.github.leofuso.columba.cli.exceptions.NonDeterministicSchemaResolutionException
import io.github.leofuso.columba.cli.fixtures.loadResource
import io.github.leofuso.columba.cli.fixtures.permutations
import io.github.leofuso.columba.cli.fixtures.scenarios
import org.apache.avro.Schema
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.InstanceOfAssertFactories
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * This situation is generally encountered when schema files define records with inline record/enum definitions, and those inline types
 * are used in more than one file.
 */
@DisplayName("SchemaParser: Unit tests related to handling of duplicate type definitions.")
class DuplicatedSchemaParserTest {

    private val subject = DefaultSchemaParser(object : ConsoleLogger {
        override fun getLogLevel() = ConsoleLogger.LogLevel.INFO
        override fun lifecycle(message: String?) = println(message)
        override fun info(message: String?) = println(message)
        override fun warn(message: String?) = println(message)
        override fun error(message: String?) = println(message)
    })

    @Test
    @DisplayName(
        """
 Given repeated Schema definitions within a single file,
 when parsing,
 then raise an exception.
"""
    )
    fun t0() {

        /* Given */
        val source = listOf(loadResource("scenarios/repeated/SingleFile.avsc"))

        /* When then */
        assertThatThrownBy { subject.parse(source) }
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
    fun t1() {

        /* Given */
        val source = listOf(loadResource("scenarios/repeated/SingleFileDifferent.avsc"))

        /* When then */
        assertThatThrownBy { subject.parse(source) }
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
    fun t2() = scenarios("scenarios/repeated/equal") {

        /* When */
        val resolution = subject.parse(it)

        /* Then */
        assertThat(resolution)
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
    fun t3() = scenarios("scenarios/repeated/different") {
        /* When then */
        assertThatThrownBy { subject.parse(it) }
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
    fun t4() = scenarios("scenarios/repeated/nested/equal") {

        /* When */
        val resolution = subject.parse(it)

        /* Then */
        assertThat(resolution)
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
    fun t5() = scenarios("scenarios/repeated/nested/different") {
        /* When Then */
        assertThatThrownBy { subject.parse(it) }
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
            loadResource("scenarios/repeated/multiple/ContainsFixed1.avsc"),
            loadResource("scenarios/repeated/multiple/ContainsFixed2.avsc")
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
                            "example.ContainsFixed1",
                            "example.ContainsFixed2",
                            "example.Picture"
                        )
                }
            }
            .toList()
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
            loadResource("scenarios/repeated/multiple/ContainsFixed1.avsc"),
            loadResource("scenarios/repeated/multiple/ContainsFixed3.avsc")
        )
            .map { permutation ->

                DynamicTest.dynamicTest("Scenario ${permutation.key}") {
                    /* When then */
                    assertThatThrownBy { subject.parse(permutation.value) }
                        .isInstanceOf(NonDeterministicSchemaResolutionException::class.java)
                        .hasMessageContaining("[example.Picture]")
                }
            }
            .toList()
    }
}
