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

        rootDir tmkdirs ("src/main/avro/protocol/interop.avdl" slash "/") append
            loadResource("parser/scenarios/protocol/interop.avdl" slash "/").readText()

        rootDir tmkdirs ("src/main/avro/receipt/obs.receipt.avsc" slash "/") append
            loadResource("parser/scenarios/reference/chain/obs.receipt.avsc" slash "/").readText()

        rootDir tmkdirs ("src/main/avro/receipt/obs.receipt-line.avsc" slash "/") append
            loadResource("parser/scenarios/reference/chain/obs.receipt-line.avsc" slash "/").readText()

        rootDir tmkdirs ("src/main/avro/obs.statement-line.avsc" slash "/") append
            loadResource("parser/scenarios/reference/chain/obs.statement-line.avsc" slash "/").readText()

        /* When */
        val result = DefaultGradleRunner.create()
            .withProjectDir(rootDir)
            .withPluginClasspath()
            .withArguments(
                "build",
                "--stacktrace",
                // GradleRunner was throwing SunCertPathBuilderException... idk
                "-Djavax.net.ssl.trustStore=${(System.getenv("JAVA_HOME") + "/lib/security/cacerts") slash "/"}"
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

        val buildPath = "${rootDir.absolutePath}/build/generated-main-specific-record" slash "/"
        val eventsPath = "$buildPath/io/github/leofuso/obs/demo/events" slash "/"
        val apachePath = "$buildPath/org/apache/avro" slash "/"
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
