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

        rootDir tmkdirs "src/main/avro/protocol/interop.avdl" append
            loadResource("parser/scenarios/protocol/interop.avdl").readText()

        rootDir tmkdirs "src/main/avro/receipt/obs.receipt.avsc" append
            loadResource("parser/scenarios/reference/chain/obs.receipt.avsc").readText()

        rootDir tmkdirs "src/main/avro/receipt/obs.receipt-line.avsc" append
            loadResource("parser/scenarios/reference/chain/obs.receipt-line.avsc").readText()

        rootDir tmkdirs "src/main/avro/obs.statement-line.avsc" append
            loadResource("parser/scenarios/reference/chain/obs.statement-line.avsc").readText()

        /* When */
        val result = DefaultGradleRunner.create()
            .withProjectDir(rootDir)
            .withPluginClasspath()
            .withArguments(
                "build",
                "--stacktrace",
                // GradleRunner was throwing SunCertPathBuilderException... idk
                "-Djavax.net.ssl.trustStore=${System.getenv("JAVA_HOME")}/lib/security/cacerts"
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

        val buildPath = "${rootDir.absolutePath}/build/generated-main-specific-record"
        assertThat(
            listOf(
                Path("$buildPath/io/github/leofuso/obs/demo/events/Details.java"),
                Path("$buildPath/io/github/leofuso/obs/demo/events/Ratio.java"),
                Path("$buildPath/io/github/leofuso/obs/demo/events/ReceiptLine.java"),
                Path("$buildPath/io/github/leofuso/obs/demo/events/Source.java"),
                Path("$buildPath/io/github/leofuso/obs/demo/events/StatementLine.java"),
                Path("$buildPath/io/github/leofuso/obs/demo/events/Department.java"),
                Path("$buildPath/io/github/leofuso/obs/demo/events/Operation.java"),
                Path("$buildPath/io/github/leofuso/obs/demo/events/Receipt.java"),
                Path("$buildPath/org/apache/avro/Node.java"),
                Path("$buildPath/org/apache/avro/InteropProtocol.java"),
                Path("$buildPath/org/apache/avro/Interop.java"),
                Path("$buildPath/org/apache/avro/Kind.java"),
                Path("$buildPath/org/apache/avro/Foo.java"),
                Path("$buildPath/org/apache/avro/MD5.java")
            )
        ).allSatisfy { assertThat(it).exists() }
    }

}
