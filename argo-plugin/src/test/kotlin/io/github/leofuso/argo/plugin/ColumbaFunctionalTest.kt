package io.github.leofuso.argo.plugin

import io.github.leofuso.argo.plugin.fixtures.loadResource
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.Path

@DisplayName("Columba: Functional tests related to Columba Plugin.")
class ColumbaFunctionalTest {

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
 Given a complete Argo 'gradle.build',
 when building,
 then should produce the necessary IDL, Protocol and Java files.
"""
    )
    fun t0() {

        /* Given */
        build append """
            
            import org.apache.avro.compiler.specific.SpecificCompiler
            import org.apache.avro.generic.GenericData
            
            buildscript {
                repositories {
                    mavenCentral()
                }
                dependencies {
                    classpath group: 'org.apache.avro', name: 'avro', version: '1.11.0'
                }
            }
            
            plugins {
                id 'java'
                id 'idea'
                id 'io.github.leofuso.argo'
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(11)
                }
            }
            
            argo {
                columba {
                    compiler = 'org.apache.avro:avro-compiler:1.11.0'
                    outputEncoding = 'UTF-8'
                    fields {
                        visibility = SpecificCompiler.FieldVisibility.PRIVATE
                        useDecimalType = true
                        stringType = GenericData.StringType.CharSequence
                    }
                    accessors {
                        noSetters = false
                        addExtraOptionalGetters = false
                        useOptionalGetters = true
                        optionalGettersForNullableFieldsOnly = true
                    }
                }
            }

        """
            .trimIndent()

        val slash = File.separator

        rootDir tmkdirs "src${slash}main${slash}avro${slash}protocol${slash}interop.avdl" append
            loadResource("parser${slash}scenarios${slash}protocol${slash}interop.avdl").readText()

        rootDir tmkdirs "src${slash}main${slash}avro${slash}receipt${slash}obs.receipt.avsc" append
            loadResource(
                "parser${slash}scenarios${slash}reference${slash}chain${slash}obs.receipt.avsc"
            ).readText()

        rootDir tmkdirs "src${slash}main${slash}avro${slash}receipt${slash}obs.receipt-line.avsc" append
            loadResource(
                "parser${slash}scenarios${slash}reference${slash}chain${slash}obs.receipt-line.avsc"
            ).readText()

        rootDir tmkdirs "src${slash}main${slash}avro${slash}obs.statement-line.avsc" append
            loadResource(
                "parser${slash}scenarios${slash}reference${slash}chain${slash}obs.statement-line.avsc"
            ).readText()

        /* When */
        val result = DefaultGradleRunner.create()
            .withProjectDir(rootDir)
            .withPluginClasspath()
            .withArguments(
                "build",
                "--stacktrace",
                // GradleRunner was throwing SunCertPathBuilderException... idk
                "-Djavax.net.ssl.trustStore=${System.getenv("JAVA_HOME")}${slash}lib${slash}security${slash}cacerts"
            )
            .forwardOutput()
            .withDebug(true)
            .build()

        /* Then */
        val generateProtocol = result.task(":generateApacheAvroProtocol")
        assertThat(generateProtocol)
            .isNotNull
            .extracting { it?.outcome }
            .isSameAs(TaskOutcome.SUCCESS)

        val compile = result.task(":compileApacheAvroJava")
        assertThat(compile)
            .isNotNull
            .extracting { it?.outcome }
            .isSameAs(TaskOutcome.SUCCESS)

        val buildPath = "${rootDir.absolutePath}${slash}build${slash}generated-main-specific-record"
        val eventsPath = "$buildPath${slash}io${slash}github${slash}leofuso${slash}obs${slash}demo${slash}events${slash}"
        val apachePath = "$buildPath${slash}org${slash}apache${slash}avro${slash}"
        assertThat(
            listOf(
                Path("${eventsPath}Details.java"),
                Path("${eventsPath}Ratio.java"),
                Path("${eventsPath}ReceiptLine.java"),
                Path("${eventsPath}Source.java"),
                Path("${eventsPath}StatementLine.java"),
                Path("${eventsPath}Department.java"),
                Path("${eventsPath}Operation.java"),
                Path("${eventsPath}Receipt.java"),
                Path("${apachePath}Node.java"),
                Path("${apachePath}InteropProtocol.java"),
                Path("${apachePath}Interop.java"),
                Path("${apachePath}Kind.java"),
                Path("${apachePath}Foo.java"),
                Path("${apachePath}MD5.java")
            )
        ).allSatisfy { assertThat(it).exists() }
    }

}
