@file:Suppress("JUnitMalformedDeclaration")

package io.github.leofuso.columba.cli.parser

import io.github.leofuso.columba.cli.ConsoleLogger
import io.github.leofuso.columba.cli.fixtures.loadResource
import io.github.leofuso.columba.cli.fixtures.permutations
import io.github.leofuso.columba.cli.fixtures.scenarios
import org.apache.avro.Schema
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.*

@DisplayName("SchemaParser: Unit tests related to dependency resolution of type definitions.")
class DependencyResolutionParserTest {

    private val logger = object : ConsoleLogger {
        override fun getLogLevel() = ConsoleLogger.LogLevel.INFO
        override fun lifecycle(message: String?) = println(message)
        override fun info(message: String?) = println(message)
        override fun warn(message: String?) = println(message)
        override fun error(message: String?) = println(message)
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
        val resource = loadResource("scenarios/common/Record.avsc")
        val subject = DefaultSchemaParser(listOf(resource), logger)

        /* When */
        val resolution = subject.parse()

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
            loadResource("scenarios/common/Record.avsc"),
            loadResource("scenarios/common/Enum.avsc"),
            loadResource("scenarios/common/Fixed.avsc"),
            loadResource("scenarios/common/UseRecordWithArray.avsc"),
            loadResource("scenarios/common/UseRecordWithType.avsc")
        )
            .map { permutation ->

                DynamicTest.dynamicTest("Scenario ${permutation.key}") {

                    /* Given */
                    val subject = DefaultSchemaParser(permutation.value, logger)

                    /* When */
                    val resolution = subject.parse()

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
    fun t2() = scenarios("scenarios/missing/leaf") {

        /* Given */
        val subject = DefaultSchemaParser(it, logger)

        /* When */
        val resolution = subject.parse()

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
    fun t3() = scenarios("scenarios/missing/root") {

        /* Given */
        val subject = DefaultSchemaParser(it, logger)

        /* When */
        val resolution = subject.parse()

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
    fun t4() = scenarios("scenarios/missing/middle") {

        /* Given */
        val subject = DefaultSchemaParser(it, logger)

        /* When */
        val resolution = subject.parse()

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
    fun t5() {

        /* Given */
        val source = listOf(loadResource("scenarios/inline/obs.receipt.avsc"))
        val subject = DefaultSchemaParser(source, logger)

        /* When */
        val resolution = subject.parse()

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
    fun t6() = scenarios("scenarios/reference/chain") {

        /* Given */
        val subject = DefaultSchemaParser(it, logger)

        /* When */
        val resolution = subject.parse()

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
