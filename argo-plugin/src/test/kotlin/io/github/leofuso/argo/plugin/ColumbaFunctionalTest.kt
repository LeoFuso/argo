package io.github.leofuso.argo.plugin

import io.github.leofuso.argo.plugin.fixtures.loadResource
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
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
 Given a build with IDLs with runtime classpath input having the same type, in different namespaces,
 when building,
 then should produce the necessary Protocol files.
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
                implementation 'org.apache.avro:avro:1.11.1'
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
        val result = GradleRunner.create()
            .withProjectDir(rootDir)
            .withPluginClasspath()
            .withArguments("build", "compileApacheAvroJava", "--stacktrace")
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

        val rootPath = rootDir.absolutePath + "/build/generated-main-specific-record/io/github/leofuso/obs/demo/events"
        assertThat(
            listOf(
                Path("$rootPath/Details.java"),
                Path("$rootPath/Ratio.java"),
                Path("$rootPath/ReceiptLine.java"),
                Path("$rootPath/Source.java"),
                Path("$rootPath/StatementLine.java"),
                Path("$rootPath/Department.java"),
                Path("$rootPath/Operation.java"),
                Path("$rootPath/Receipt.java")
            )
        ).allSatisfy { assertThat(it).exists() }
    }

}
