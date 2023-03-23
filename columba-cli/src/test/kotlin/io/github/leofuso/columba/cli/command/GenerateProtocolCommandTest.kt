package io.github.leofuso.columba.cli.command

import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.subcommands
import io.github.leofuso.columba.cli.CommandRunner
import io.github.leofuso.columba.cli.IDL_EXTENSION
import io.github.leofuso.columba.cli.JAR_EXTENSION
import io.github.leofuso.columba.cli.fixtures.loadResource
import org.assertj.core.api.Assertions.assertThatNoException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@DisplayName("GenerateProtocolCommand: Functional tests related to Command parsing.")
class GenerateProtocolCommandTest {

    @TempDir
    private lateinit var dest: File

    private val invalidSource = loadResource("scenarios/common/Record.avsc")
    private val source = loadResource("scenarios/protocol/dependent.avdl")
    private val classpath = loadResource("scenarios/protocol/shared.avdl")
    private val lib = loadResource("scenarios/protocol/shared.jar")

    private val subject =
        CommandRunner(System.out, System.err)
            .subcommands(GenerateProtocolCommand())

    @Test
    @DisplayName(
        """
 Given an empty command run,
 when parsing,
 then should raise a help exception.
"""
    )
    fun t0() {
        /* When Then */
        assertThatThrownBy { subject.parse(emptyList()) }
            .isInstanceOf(PrintHelpMessage::class.java)
    }

    @Test
    @DisplayName(
        """
 Given an empty 'generate-protocol' command run,
 when parsing,
 then should raise a help exception.
"""
    )
    fun t1() {
        /* When Then */
        assertThatThrownBy { subject.parse(arrayOf("generate-protocol")) }
            .isInstanceOf(PrintHelpMessage::class.java)
    }

    @Test
    @DisplayName(
        """
 Given a bad parameter value for SOURCES,
 when parsing,
 then should raise a 'bad parameter' exception.
"""
    )
    fun t2() {
        /* When Then */
        assertThatThrownBy { subject.parse(arrayOf("generate-protocol", "${File.separator}arbitrary", "aaa")) }
            .isInstanceOf(BadParameterValue::class.java)
            .hasMessageContaining("\"${File.separator}arbitrary\" does not exist.")
    }

    @Test
    @DisplayName(
        """
 Given a bad parameter value for DEST,
 when parsing,
 then should raise a 'bad parameter' exception.
"""
    )
    fun t3() {

        /* When Then */
        assertThatThrownBy { subject.parse(arrayOf("generate-protocol", source.absolutePath, "${File.separator}arbitrary")) }
            .isInstanceOf(BadParameterValue::class.java)
            .hasMessageContaining("\"${File.separator}arbitrary\" does not exist.")
    }

    @Test
    @DisplayName(
        """
 Given a bad parameter value for SOURCES,
 when parsing,
 then should raise a 'bad parameter' exception.
"""
    )
    fun t4() {
        /* When Then */
        assertThatThrownBy {
            subject.parse(
                arrayOf(
                    "generate-protocol",
                    source.absolutePath,
                    invalidSource.absolutePath,
                    dest.absolutePath
                )
            )
        }
            .isInstanceOf(BadParameterValue::class.java)
            .hasMessageContaining("Accepts only Avro IDL(.avdl) source files as input.")
    }

    @Test
    @DisplayName(
        """
 Given a bad parameter value for '--classpath',
 when parsing,
 then should raise a 'bad parameter' exception.
"""
    )
    fun t5() {

        /* When Then */
        assertThatThrownBy {
            subject.parse(
                arrayOf(
                    "generate-protocol",
                    source.absolutePath,
                    dest.absolutePath,
                    "--classpath",
                    invalidSource.absolutePath
                )
            )
        }
            .isInstanceOf(BadParameterValue::class.java)
            .hasMessageContaining("Accepts only Avro IDL(.$IDL_EXTENSION) source files or Jar(.$JAR_EXTENSION) files as classpath.")
    }

    @Test
    @DisplayName(
        """
 Given a bad parameter value for '--classpath',
 when parsing,
 then should raise a 'bad parameter' exception.
"""
    )
    fun t6() {

        /* When Then */
        assertThatThrownBy {
            subject.parse(
                arrayOf(
                    "generate-protocol",
                    source.absolutePath,
                    dest.absolutePath,
                    "--classpath",
                    "${File.separator}arbitrary.avdl"
                )
            )
        }
            .isInstanceOf(BadParameterValue::class.java)
            .hasMessageContaining("does not exist.")
    }

    @Test
    @DisplayName(
        """
 Given a valid parameter value for '--classpath',
 when parsing,
 then should 'generate-protocol'.
"""
    )
    fun t7() {

        /* When Then */
        assertThatNoException()
            .isThrownBy {
                subject.parse(arrayOf("generate-protocol", source.absolutePath, dest.absolutePath, "--classpath", classpath.absolutePath))
            }
    }

    @Test
    @DisplayName(
        """
 Given a valid parameter value for '--classpath',
 when parsing,
 then should 'generate-protocol'.
"""
    )
    fun t77() {

        /* When Then */
        assertThatNoException()
            .isThrownBy {
                subject.parse(arrayOf("generate-protocol", source.absolutePath, dest.absolutePath, "--classpath", lib.absolutePath))
            }
    }

    @Test
    @DisplayName(
        """
 Given a no parameter value for all options,
 when parsing,
 then should 'generate-protocol' with default options.
"""
    )
    fun t8() {

        /* When Then */
        assertThatNoException()
            .isThrownBy {
                subject.parse(arrayOf("generate-protocol", classpath.absolutePath, dest.absolutePath))
            }
    }
}
