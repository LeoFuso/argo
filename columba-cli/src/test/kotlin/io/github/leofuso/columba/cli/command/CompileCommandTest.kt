@file:Suppress("JUnitMalformedDeclaration")

package io.github.leofuso.columba.cli.command

import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.NoSuchOption
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.subcommands
import io.github.leofuso.columba.cli.CommandRunner
import io.github.leofuso.columba.cli.fixtures.loadResource
import org.assertj.core.api.Assertions.assertThatNoException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.*

@DisplayName("CompileCommand: Functional tests related to Command parsing.")
class CompileCommandTest {

    @TempDir
    private lateinit var dest: File

    private val source = loadResource("scenarios/common/Record.avsc")
    private val invalidSource = loadResource("scenarios/protocol/shared.avdl")

    private val subject =
        CommandRunner(System.out, System.err)
            .subcommands(CompileCommand())

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
 Given an empty compile command run,
 when parsing,
 then should raise a help exception.
"""
    )
    fun t1() {
        /* When Then */
        assertThatThrownBy { subject.parse(arrayOf("compile")) }
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
        assertThatThrownBy { subject.parse(arrayOf("compile", "/arbitrary", "aaa")) }
            .isInstanceOf(BadParameterValue::class.java)
            .hasMessageContaining("\"/arbitrary\" does not exist.")
    }

    @Test
    @DisplayName(
        """
 Given a bad parameter value for SOURCES,
 when parsing,
 then should raise a 'bad parameter' exception.
"""
    )
    fun t22() {
        /* When Then */
        assertThatThrownBy { subject.parse(arrayOf("compile", source.absolutePath, invalidSource.absolutePath, dest.absolutePath)) }
            .isInstanceOf(BadParameterValue::class.java)
            .hasMessageContaining("Accepts only Schema(.avsc) and Protocol(.avpr) definition files as input.")
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
        assertThatThrownBy { subject.parse(arrayOf("compile", source.absolutePath, "/arbitrary")) }
            .isInstanceOf(BadParameterValue::class.java)
            .hasMessageContaining("\"/arbitrary\" does not exist.")
    }

    @Test
    @DisplayName(
        """
 Given a bad parameter value for '--allow-setters',
 when parsing,
 then should raise a 'no such option' exception.
"""
    )
    fun t4() {

        /* When Then */
        assertThatThrownBy {
            subject.parse(
                arrayOf(
                    "compile",
                    source.absolutePath,
                    dest.absolutePath,
                    "--allow-setter"
                )
            )
        }
            .isInstanceOf(NoSuchOption::class.java)
            .hasMessageContaining("--allow-setters")
    }

    @Test
    @DisplayName(
        """
 Given a bad parameter value for '--field-visibility',
 when parsing,
 then should raise a 'bad parameter' exception.
"""
    )
    fun t5() {

        /* When Then */
        assertThatThrownBy {
            subject.parse(
                arrayOf(
                    "compile",
                    source.absolutePath,
                    dest.absolutePath,
                    "--field-visibility",
                    "INVALID_OPTION"
                )
            )
        }
            .isInstanceOf(BadParameterValue::class.java)
            .hasMessageContaining("(choose from PUBLIC, PRIVATE)")
    }

    @Test
    @DisplayName(
        """
 Given a bad parameter value for '--string-type',
 when parsing,
 then should raise a 'bad parameter' exception.
"""
    )
    fun t6() {

        /* When Then */
        assertThatThrownBy {
            subject.parse(
                arrayOf(
                    "compile",
                    source.absolutePath,
                    dest.absolutePath,
                    "--string-type",
                    "INVALID_OPTION"
                )
            )
        }
            .isInstanceOf(BadParameterValue::class.java)
            .hasMessageContaining("(choose from CharSequence, String, Utf8)")
    }

    @Test
    @DisplayName(
        """
 Given a bad parameter value for '--logical-type-factories',
 when parsing,
 then should raise a 'bad parameter' exception.
"""
    )
    fun t7() {

        /* When Then */
        assertThatThrownBy {
            subject.parse(
                arrayOf(
                    "compile",
                    source.absolutePath,
                    dest.absolutePath,
                    "--logical-type-factories",
                    "factory=com#SomeClass"
                )
            )
        }
            .isInstanceOf(BadParameterValue::class.java)
            .hasMessageContaining("the fully qualified form")
    }

    @Test
    @DisplayName(
        """
 Given a bad parameter value for '--logical-type-factories',
 when parsing,
 then should raise a 'bad parameter' exception.
"""
    )
    fun t8() {

        /* When Then */
        assertThatThrownBy {
            subject.parse(
                arrayOf(
                    "compile",
                    source.absolutePath,
                    dest.absolutePath,
                    "-f",
                    "factory=SomeClass",
                    "-f",
                    "=SomeOtherClass"
                )
            )
        }
            .isInstanceOf(BadParameterValue::class.java)
            .hasMessageContaining("the fully qualified form")
    }

    @Test
    @DisplayName(
        """
 Given a bad parameter value for '--converters',
 when parsing,
 then should raise a 'bad parameter' exception.
"""
    )
    fun t9() {

        /* When Then */
        assertThatThrownBy {
            subject.parse(
                arrayOf(
                    "compile",
                    source.absolutePath,
                    dest.absolutePath,
                    "-c",
                    "SomeClass;SomeOtherClass;Invalid/Class;AlsoValidClass.kt"
                )
            )
        }
            .isInstanceOf(BadParameterValue::class.java)
            .hasMessageContaining("the fully qualified form")
    }

    @Test
    @DisplayName(
        """
 Given a bad parameter value for '--velocity-template',
 when parsing,
 then should raise a 'bad parameter' exception.
"""
    )
    fun t10() {

        /* When Then */
        assertThatThrownBy {
            subject.parse(
                arrayOf(
                    "compile",
                    source.absolutePath,
                    dest.absolutePath,
                    "--velocity-template",
                    dest.absolutePath
                )
            )
        }
            .isInstanceOf(BadParameterValue::class.java)
            .hasMessageContaining("least some VelocityTemplates(.vm)")
    }

    @Test
    @DisplayName(
        """
 Given a bad parameter value for '--velocity-tools',
 when parsing,
 then should raise a 'bad parameter' exception.
"""
    )
    fun t11() {

        /* When Then */
        assertThatThrownBy {
            subject.parse(
                arrayOf(
                    "compile",
                    source.absolutePath,
                    dest.absolutePath,
                    "-v",
                    "SomeClass;SomeOtherClass;Invalid/Class;AlsoValidClass.kt"
                )
            )
        }
            .isInstanceOf(BadParameterValue::class.java)
            .hasMessageContaining("the fully qualified form.")
    }

    @Test
    @DisplayName(
        """
 Given a no parameter value for all options,
 when parsing,
 then should compile with default options.
"""
    )
    fun t12() {

        /* When Then */
        assertThatNoException()
            .isThrownBy {
                subject.parse(arrayOf("compile", source.absolutePath, dest.absolutePath))
            }
    }
}
