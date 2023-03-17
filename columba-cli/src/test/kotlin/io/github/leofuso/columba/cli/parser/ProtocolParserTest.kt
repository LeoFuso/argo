package io.github.leofuso.columba.cli.parser

import io.github.leofuso.columba.cli.ConsoleLogger
import io.github.leofuso.columba.cli.fixtures.loadResource
import org.apache.avro.Protocol
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ProtocolParser: Unit tests related to handling Protocol type definitions.")
class ProtocolParserTest {

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
 Given a Protocol,
 when parsing,
 then should resolve into a valid Protocol.
"""
    )
    fun t0() {
        /* Given */
        val resource = loadResource("scenarios/protocol/mail.avpr")
        val source = listOf(resource)
        val subject = DefaultSchemaParser(
            source,
            logger
        )
        /* When */
        val resolution = subject.parse()

        /* Then */
        assertThat(resolution)
            .extracting("protocol")
            .asInstanceOf(InstanceOfAssertFactories.map(String::class.java, Protocol::class.java))
            .containsOnlyKeys("Mail")
    }
}
