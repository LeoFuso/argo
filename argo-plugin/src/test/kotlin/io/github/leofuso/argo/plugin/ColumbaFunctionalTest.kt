package io.github.leofuso.argo.plugin

import io.github.leofuso.argo.plugin.fixtures.loadResource
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.readText

@DisabledOnOs(OS.WINDOWS)
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

    @Test
    @DisplayName(
        """
 Given a default Argo 'gradle.build',
 when building,
 then should produce the necessary Java files, with correct caracteristics.
"""
    )
    fun t1() {

        /* Given */
        build append """
            
            plugins {
                id 'java'
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
            
        """
            .trimIndent()

        rootDir tmkdirs "src/main/avro/user.avsc" append
            loadResource("tasks/compiler/user.avsc").readText()

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
        val compile = result.task(":compileApacheAvroJava")
        assertThat(compile)
            .isNotNull
            .extracting { it?.outcome }
            .isSameAs(TaskOutcome.SUCCESS)

        assertThat(Path("${rootDir.absolutePath}/build/generated-main-specific-record/example/avro/User.java"))
            .exists()
            .content()
            .asString()
            .contains("public java.lang.CharSequence getName()")
            .contains("private java.lang.CharSequence name;")
            .doesNotContain("Custom template")
            .doesNotContain("public void setName(java.lang.CharSequence value)")
            .contains("public Optional<java.lang.Integer> getFavoriteNumber()")
            .contains("public java.math.BigDecimal getSalary()")
            .containsOnlyOnce("public java.nio.ByteBuffer")
            .contains("public java.time.LocalDate getBirthDate()")
            .doesNotContain("public Optional<java.time.LocalDate>")

    }

    @DisplayName(
        """
 Given an Argo 'gradle.build' with StringType config,
 when building,
 then should produce the necessary Java files, with correct caracteristics.
"""
    )
    @ParameterizedTest(name = "{index} ==> StringType ''{0}''")
    @ValueSource(strings = ["CharSequence", "String", "Utf8"])
    fun t2(config: String) {

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
                    fields {
                        stringType = GenericData.StringType.$config
                    }
                }
            }
            
        """
            .trimIndent()

        rootDir tmkdirs "src/main/avro/user.avsc" append
            loadResource("tasks/compiler/user.avsc").readText()

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
        val compile = result.task(":compileApacheAvroJava")
        assertThat(compile)
            .isNotNull
            .extracting { it?.outcome }
            .isSameAs(TaskOutcome.SUCCESS)

        val stringClass = when (config) {
            "String" -> "java.lang.String"
            "CharSequence" -> "java.lang.CharSequence"
            "Utf8" -> "org.apache.avro.util.Utf8"
            else -> "???"
        }

        assertThat(Path("${rootDir.absolutePath}/build/generated-main-specific-record/example/avro/User.java"))
            .exists()
            .content()
            .asString()
            .contains("public $stringClass getName()")
    }

    @DisplayName(
        """
 Given an Argo 'gradle.build' with field visibility config,
 when building,
 then should produce the necessary Java files, with correct caracteristics.
"""
    )
    @ParameterizedTest(name = "{index} ==> FieldVisibility ''{0}''")
    @ValueSource(strings = ["PUBLIC", "PRIVATE"])
    fun t3(config: String) {

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
                    fields {
                        visibility = SpecificCompiler.FieldVisibility.$config
                    }
                }
            }
            
        """
            .trimIndent()

        rootDir tmkdirs "src/main/avro/user.avsc" append
            loadResource("tasks/compiler/user.avsc").readText()

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
        val compile = result.task(":compileApacheAvroJava")
        assertThat(compile)
            .isNotNull
            .extracting { it?.outcome }
            .isSameAs(TaskOutcome.SUCCESS)

        assertThat(Path("${rootDir.absolutePath}/build/generated-main-specific-record/example/avro/User.java"))
            .exists()
            .content()
            .asString()
            .contains("${config.lowercase()} java.lang.CharSequence name;")
    }

    @DisplayName(
        """
 Given an Argo 'gradle.build' with field immutability config,
 when building,
 then should produce the necessary Java files, with correct caracteristics.
"""
    )
    @ParameterizedTest(name = "{index} ==> Immutability config ''{0}''. Accessible? {1}")
    @CsvSource(
        "Boolean.TRUE, false",
        "Boolean.FALSE, true",
        "'true', false",
        "'false', true",
        "true, false",
        "false, true"
    )
    fun t4(config: String, accessible: Boolean) {

        /* Given */
        build append """
            
            plugins {
                id 'java'
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
                    accessors {
                        noSetters = $config
                    }
                }
            }
            
        """
            .trimIndent()

        rootDir tmkdirs "src/main/avro/user.avsc" append
            loadResource("tasks/compiler/user.avsc").readText()

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
        val compile = result.task(":compileApacheAvroJava")
        assertThat(compile)
            .isNotNull
            .extracting { it?.outcome }
            .isSameAs(TaskOutcome.SUCCESS)

        if (accessible) {
            assertThat(Path("${rootDir.absolutePath}/build/generated-main-specific-record/example/avro/User.java"))
                .exists()
                .content()
                .asString()
                .contains("public void setName(java.lang.CharSequence value)")
        } else {
            assertThat(Path("${rootDir.absolutePath}/build/generated-main-specific-record/example/avro/User.java"))
                .exists()
                .content()
                .asString()
                .doesNotContain("public void setName(java.lang.CharSequence value)")
        }

    }

    @DisplayName(
        """
 Given an Argo 'gradle.build' with extra optional fields config,
 when building,
 then should produce the necessary Java files, with correct caracteristics.
"""
    )
    @ParameterizedTest(name = "{index} ==> Extra optional config ''{0}''. Contains extra optional? {1}")
    @CsvSource(
        "Boolean.TRUE, true",
        "Boolean.FALSE, false",
        "'true', true",
        "'false', false",
        "true, true",
        "false, false"
    )
    fun t5(config: String, shouldContainExtraOptional: Boolean) {

        /* Given */
        build append """
            
            plugins {
                id 'java'
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
                    accessors {
                        addExtraOptionalGetters = $config
                    }
                }
            }
            
        """
            .trimIndent()

        rootDir tmkdirs "src/main/avro/user.avsc" append
            loadResource("tasks/compiler/user.avsc").readText()

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
        val compile = result.task(":compileApacheAvroJava")
        assertThat(compile)
            .isNotNull
            .extracting { it?.outcome }
            .isSameAs(TaskOutcome.SUCCESS)

        if (shouldContainExtraOptional) {
            assertThat(Path("${rootDir.absolutePath}/build/generated-main-specific-record/example/avro/User.java"))
                .exists()
                .content()
                .asString()
                .contains("public Optional<java.lang.Integer> getOptionalFavoriteNumber()")
        } else {
            assertThat(Path("${rootDir.absolutePath}/build/generated-main-specific-record/example/avro/User.java"))
                .exists()
                .content()
                .asString()
                .doesNotContain("public Optional<java.lang.Integer> getOptionalFavoriteNumber()")
        }

    }

    @DisplayName(
        """
 Given an Argo 'gradle.build' with optional field config,
 when building,
 then should produce the necessary Java files, with correct caracteristics.
"""
    )
    @ParameterizedTest(name = "{index} ==> Optional Getters? ''{0}''. Only Nullable? {1}")
    @CsvSource(
        "Boolean.TRUE, Boolean.TRUE, true, true",
        "Boolean.TRUE, Boolean.FALSE, true, false",
        "Boolean.FALSE, Boolean.TRUE, false, true",
        "Boolean.FALSE, Boolean.FALSE, false, false",
        "true, true, true, true",
        "true, false, true, false",
        "false, true, false, true",
        "false, false, false, false",
        "'true', 'true', true, true",
        "'true', 'false', true, false",
        "'false', 'true', false, true",
        "'false', 'false', false, false"
    )
    fun t6(getters: String, nullableOnly: String, hasGetter: Boolean, onlyNullable: Boolean) {

        /* Given */
        build append """
            
            plugins {
                id 'java'
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
                    accessors {
                        useOptionalGetters = $getters
                        optionalGettersForNullableFieldsOnly = $nullableOnly
                    }
                }
            }
            
        """
            .trimIndent()

        rootDir tmkdirs "src/main/avro/user.avsc" append
            loadResource("tasks/compiler/user.avsc").readText()

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
        val compile = result.task(":compileApacheAvroJava")
        assertThat(compile)
            .isNotNull
            .extracting { it?.outcome }
            .isSameAs(TaskOutcome.SUCCESS)

        val sourceCode = Path("${rootDir.absolutePath}/build/generated-main-specific-record/example/avro/User.java").readText()

        if (hasGetter && onlyNullable) {
            assertThat(sourceCode)
                .contains("public Optional<java.lang.Integer> getFavoriteNumber()")
                .doesNotContain("public Optional<java.lang.CharSequence> getName()")
        }

        if (hasGetter && !onlyNullable) {
            assertThat(sourceCode)
                .contains("public Optional<java.lang.Integer> getFavoriteNumber()")
                .contains("public Optional<java.lang.CharSequence> getName()")
        }

        if (!hasGetter && !onlyNullable || !hasGetter) {
            assertThat(sourceCode)
                .doesNotContain("public Optional<java.lang.Integer> getFavoriteNumber()")
                .contains("public java.lang.Integer getFavoriteNumber()")
                .doesNotContain("public Optional<java.lang.CharSequence> getName()")
        }
    }

}
