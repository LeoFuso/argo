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

        rootDir tmkdirs ("src" sh "main" sh "avro" sh "protocol" sh "interop.avdl") append
            loadResource("parser" sh "scenarios" sh "protocol" sh "interop.avdl").readText()

        rootDir tmkdirs ("src" sh "main" sh "avro" sh "receipt" sh "obs.receipt.avsc") append
            loadResource("parser" sh "scenarios" sh "reference" sh "chain" sh "obs.receipt.avsc").readText()

        rootDir tmkdirs ("src" sh "main" sh "avro" sh "receipt" sh "obs.receipt-line.avsc") append
            loadResource("parser" sh "scenarios" sh "reference" sh "chain" sh "obs.receipt-line.avsc").readText()

        rootDir tmkdirs ("src" sh "main" sh "avro" sh "obs.statement-line.avsc") append
            loadResource("parser" sh "scenarios" sh "reference" sh "chain" sh "obs.statement-line.avsc").readText()

        /* When */
        val result = DefaultGradleRunner.create()
            .withProjectDir(rootDir)
            .withPluginClasspath()
            .withArguments(
                "build",
                "--stacktrace",
                // GradleRunner was throwing SunCertPathBuilderException... idk
                "-Djavax.net.ssl.trustStore=${System.getenv("JAVA_HOME")}" sh "lib" sh "security" sh "cacerts"
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

        val buildPath = rootDir.absolutePath sh "build" sh "generated-main-specific-record"
        val eventsPath = buildPath sh "io" sh "github" sh "leofuso" sh "obs" sh "demo" sh "events"
        val apachePath = buildPath sh "org" sh "apache" sh "avro"
        assertThat(
            listOf(
                Path(eventsPath sh "Details.java"),
                Path(eventsPath sh "Ratio.java"),
                Path(eventsPath sh "ReceiptLine.java"),
                Path(eventsPath sh "Source.java"),
                Path(eventsPath sh "StatementLine.java"),
                Path(eventsPath sh "Department.java"),
                Path(eventsPath sh "Operation.java"),
                Path(eventsPath sh "Receipt.java"),
                Path(apachePath sh "Node.java"),
                Path(apachePath sh "InteropProtocol.java"),
                Path(apachePath sh "Interop.java"),
                Path(apachePath sh "Kind.java"),
                Path(apachePath sh "Foo.java"),
                Path(apachePath sh "MD5.java")
            )
        ).allSatisfy { assertThat(it).exists() }
    }

}
