package io.github.leofuso.argo.plugin.tasks

import io.github.leofuso.argo.plugin.append
import io.github.leofuso.argo.plugin.fixtures.loadResource
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class IDLProtocolTaskTest {

    @TempDir
    private lateinit var rootDir: File

    private lateinit var build: File

    @BeforeEach
    fun setUp() {
        build = File(rootDir, "build.gradle")
    }

    @Test
    fun stuff() {

        /* Given */
        //applyPlugin("java", build)

        val buildContent = """
            
            import io.github.leofuso.argo.plugin.tasks.IDLProtocolTask
            
            plugins {
                id 'java'
                id 'io.github.leofuso.argo' apply false
            }
            
            configurations.create("shared")
            tasks.register("sharedIDLJar", Jar) {
                from "src/shared"
            }
            
            dependencies {
                implementation 'org.apache.avro:avro:1.11.1'
                shared sharedIDLJar.outputs.files
            }

            tasks.register("generateProtocol", IDLProtocolTask) {
                sources.from file("src/dependent")
                classpath = configurations.shared
                outputDir = file("build/protocol")
            }
            
        """.trimIndent()
        append(buildContent, build)
        loadResource("tasks/protocol/shared.avdl").copyTo(File(rootDir, "src/shared"))
        loadResource("tasks/protocol/dependent.avdl").copyTo(File(rootDir, "src/dependent"))

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
    }

}
