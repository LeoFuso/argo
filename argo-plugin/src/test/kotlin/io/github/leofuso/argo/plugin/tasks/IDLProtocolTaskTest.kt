package io.github.leofuso.argo.plugin.tasks

import io.github.leofuso.argo.plugin.append
import io.github.leofuso.argo.plugin.fixtures.loadResource
import io.github.leofuso.argo.plugin.tmkdirs
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.Path

@DisplayName("IDL: Functional tests related to IDLProtocolTask.")
class IDLProtocolTaskTest {

    @TempDir
    private lateinit var rootDir: File

    private lateinit var build: File

    @BeforeEach
    fun setUp() {
        build = File(rootDir, "build.gradle")
        build.createNewFile()
    }

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
        build append """
            
            import io.github.leofuso.argo.plugin.tasks.IDLProtocolTask

            plugins {
                id 'java'
                id 'io.github.leofuso.argo' apply false
            }
            
            configurations {
                shared {
                    canBeConsumed = false
                    canBeResolved = true
                }
            }
            
            def sharedIDLJar = tasks.register('sharedIDLJar', Jar) {
                from 'src/shared'
            }.get()
            
            dependencies {
                shared sharedIDLJar.outputs.files
            }
            
            tasks.register('generateProtocol', IDLProtocolTask) {
                source(file('src/dependent'))
                classpath = configurations.shared
                outputDir = file('build/protocol')
            }
            
        """
            .trimIndent()

        val shared = rootDir tmkdirs "src/shared/shared.avdl"
        shared append loadResource("tasks/protocol/shared.avdl").readText()

        val dependent = rootDir tmkdirs "src/dependent/dependent.avdl"
        dependent append loadResource("tasks/protocol/dependent.avdl").readText()

        /* When */
        val result = GradleRunner.create()
            .withProjectDir(rootDir)
            .withPluginClasspath()
            .withArguments("build", "sharedIDLJar", "generateProtocol", "--stacktrace")
            .build()

        /* Then */
        val generateProtocol = result.task(":generateProtocol")
        assertThat(generateProtocol)
            .isNotNull
            .extracting { it?.outcome }
            .isSameAs(TaskOutcome.SUCCESS)

        assertThat(Path(rootDir.absolutePath + "/build/protocol/com/example/dependent/DependentProtocol.avpr")).exists()

    }

    @Test
    @DisplayName(
"""
 Given a build with IDLs with runtime classpath input,
 when building,
 then should produce the necessary Protocol files.
"""
    )
    fun t1() {

        /* Given */
        build append """
            
            import io.github.leofuso.argo.plugin.tasks.IDLProtocolTask

            plugins {
                id 'java'
                id 'io.github.leofuso.argo' apply false
            }
            
            def sharedIDLJar = tasks.register('sharedIDLJar', Jar) {
                from 'src/shared'
            }.get()
            
            dependencies {
                runtimeOnly sharedIDLJar.outputs.files
            }
            
            tasks.register('generateProtocol', IDLProtocolTask) {
                source(file('src/main/avro'))
                classpath = configurations.runtimeClasspath
                outputDir = file('build/protocol')
            }
            
        """
            .trimIndent()

        val shared = rootDir tmkdirs "src/shared/shared.avdl"
        shared append loadResource("tasks/protocol/shared.avdl").readText()

        val dependent = rootDir tmkdirs "src/main/avro/dependent.avdl"
        dependent append loadResource("tasks/protocol/dependent.avdl").readText()

        /* When */
        val result = GradleRunner.create()
            .withProjectDir(rootDir)
            .withPluginClasspath()
            .withArguments("build", "sharedIDLJar", "generateProtocol", "--stacktrace")
            .build()

        /* Then */
        val generateProtocol = result.task(":generateProtocol")
        assertThat(generateProtocol)
            .isNotNull
            .extracting { it?.outcome }
            .isSameAs(TaskOutcome.SUCCESS)

        assertThat(Path(rootDir.absolutePath + "/build/protocol/com/example/dependent/DependentProtocol.avpr")).exists()

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
        build append """
            
            import io.github.leofuso.argo.plugin.tasks.IDLProtocolTask

            plugins {
                id 'java'
                id 'io.github.leofuso.argo' apply false
            }
            
            dependencies {
            
            }
            
            tasks.register('generateProtocol', IDLProtocolTask) {
                source(file('src/main/avro'))
                classpath = configurations.runtimeClasspath
                outputDir = file('build/protocol')
            }
            
        """
            .trimIndent()

        val v1 = rootDir tmkdirs "src/main/avro/v1/test.avdl"
        v1 append loadResource("tasks/protocol/namespace/v1.avdl").readText()

        val v2 = rootDir tmkdirs "src/main/avro/v2/test.avdl"
        v2 append loadResource("tasks/protocol/namespace/v2.avdl").readText()

        /* When */
        val result = GradleRunner.create()
            .withProjectDir(rootDir)
            .withPluginClasspath()
            .withArguments("build", "generateProtocol", "--stacktrace")
            .build()

        /* Then */
        val generateProtocol = result.task(":generateProtocol")
        assertThat(generateProtocol)
            .isNotNull
            .extracting { it?.outcome }
            .isSameAs(TaskOutcome.SUCCESS)

        assertThat(Path(rootDir.absolutePath + "/build/protocol/org/example/v1/TestProtocol.avpr")).exists()
        assertThat(Path(rootDir.absolutePath + "/build/protocol/org/example/v2/TestProtocol.avpr")).exists()

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
        build append """
            
            import io.github.leofuso.argo.plugin.tasks.IDLProtocolTask

            plugins {
                id 'java'
                id 'io.github.leofuso.argo' apply false
            }
            
            dependencies {
            
            }
            
            tasks.register('generateProtocol', IDLProtocolTask) {
                source(file('src/main/avro'))
                classpath = configurations.runtimeClasspath
                outputDir = file('build/protocol')
            }
            
        """
            .trimIndent()

        val v1 = rootDir tmkdirs "src/main/avro/v1/test.avdl"
        v1 append loadResource("tasks/protocol/namespace/v1.avdl").readText()

        val v2 = rootDir tmkdirs "src/main/avro/v1/test_same_protocol.avdl"
        v2 append loadResource("tasks/protocol/namespace/v1.avdl").readText()

        /* When */
        val result = GradleRunner.create()
            .withProjectDir(rootDir)
            .withPluginClasspath()
            .withArguments("build", "generateProtocol", "--stacktrace")
            .buildAndFail()

        /* Then */
        val generateProtocol = result.task(":generateProtocol")
        assertThat(generateProtocol)
            .isNotNull
            .extracting { it?.outcome }
            .isSameAs(TaskOutcome.FAILED)

        assertThat(result.output)
            .contains("There's already another Protocol defined in the classpath with the same name.")
    }

}
