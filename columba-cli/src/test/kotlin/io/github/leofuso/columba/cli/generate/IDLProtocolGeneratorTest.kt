package io.github.leofuso.columba.cli.generate

import io.github.leofuso.columba.cli.ConsoleLogger
import io.github.leofuso.columba.cli.fixtures.loadResource
import io.github.leofuso.columba.cli.fixtures.toSysPath
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.Path

@DisplayName("IDL: ProtocolGenerator unit tests.")
class IDLProtocolGeneratorTest {

    private val logger = object : ConsoleLogger {
        override fun getLogLevel() = ConsoleLogger.LogLevel.INFO
        override fun lifecycle(message: String?) = println(message)
        override fun info(message: String?) = println(message)
        override fun warn(message: String?) = println(message)
        override fun error(message: String?) = println(message)
    }

    @TempDir
    private lateinit var dest: File

    @Test
    @DisplayName(
"""
 Given a build with IDLs with custom classpath input,
 when building,
 then should produce the necessary Protocol files.
"""
    )
    fun t0() {

        /* Given */
        val classpathElement = loadResource("scenarios/protocol/shared.avdl")
        val sourceElement = loadResource("scenarios/protocol/dependent.avdl")

        val subject = IDLProtocolGenerator(setOf(classpathElement), logger)

        /* When */
        subject.generate(setOf(sourceElement), dest)

        /* Then */
        val path = "${dest.absolutePath}/com/example/dependent/DependentProtocol.avpr".toSysPath()
        assertThat(Path(path)).exists()
    }

    @Test
    @DisplayName(
"""
 Given a build with IDLs with runtime classpath input having the same type, in different namespaces,
 when building,
 then should produce the necessary Protocol files.
"""
    )
    fun t2() {

        /* Given */
        val sources = setOf(
            loadResource("scenarios/protocol/namespace/v1.avdl"),
            loadResource("scenarios/protocol/namespace/v2.avdl")
        )

        val subject = IDLProtocolGenerator(setOf(), logger)

        /* When */
        subject.generate(sources, dest)

        /* Then */
        val pathV1 = "${dest.absolutePath}/org/example/v1/TestProtocol.avpr".toSysPath()
        assertThat(Path(pathV1)).exists()

        val pathV2 = "${dest.absolutePath}/org/example/v2/TestProtocol.avpr".toSysPath()
        assertThat(Path(pathV2)).exists()
    }

    @Test
    @DisplayName(
        """
 Given a build with IDLs with runtime classpath input having the same type, in the same namespace,
 when building,
 then should fail with correct output.
"""
    )
    fun t3() {

        /* Given */
        val sources = setOf(
            loadResource("scenarios/protocol/namespace/v1.avdl"),
            loadResource("scenarios/protocol/namespace/v1-copy.avdl")
        )

        val subject = IDLProtocolGenerator(setOf(), logger)

        /* When then */
        assertThatThrownBy { subject.generate(sources, dest) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("There's already another Protocol defined in the classpath with the same name.")
    }
}
