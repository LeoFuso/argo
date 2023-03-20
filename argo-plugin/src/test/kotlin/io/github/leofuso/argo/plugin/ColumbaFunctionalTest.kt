package io.github.leofuso.argo.plugin

import io.github.leofuso.argo.custom.CommentGenerator
import io.github.leofuso.argo.custom.TimestampGenerator
import io.github.leofuso.argo.plugin.fixtures.loadResource
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
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
                        
            plugins {
                id 'java'
                id 'idea'
                id 'io.github.leofuso.argo'
            }
            
            repositories {
                mavenLocal()
                mavenCentral()
            }
            
            dependencies {
                
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            argo {
                columba {
                    compiler = 'org.apache.avro:avro-compiler:1.11.0'
                    outputEncoding = 'UTF-8'
                    fields {
                        visibility = 'PRIVATE'
                        useDecimalType = true
                        stringType = 'CharSequence'
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
        val result = buildGradleRunner()

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
                id 'idea'
                id 'io.github.leofuso.argo'
            }
            
            repositories {
                mavenLocal()
                mavenCentral()
            }
            
            dependencies {
                
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
        """
            .trimIndent()

        rootDir tmkdirs "src/main/avro/user.avsc" append
            loadResource("tasks/compiler/user.avsc").readText()

        /* When */
        val result = buildGradleRunner()

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
    @ValueSource(strings = ["CharSequence", "charsequence", "String", "Utf8", "utf8", "string", "CHARSEQUENCE"])
    fun t2(config: String) {

        /* Given */
        build append """
            
            import org.apache.avro.compiler.specific.SpecificCompiler
            import org.apache.avro.generic.GenericData

            plugins {
                id 'java'
                id 'idea'
                id 'io.github.leofuso.argo'
            }
            
            repositories {
                mavenLocal()
                mavenCentral()
            }
            
            dependencies {
                
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            argo {
                columba {
                    fields {
                        stringType = '$config'
                    }
                }
            }
            
        """
            .trimIndent()

        rootDir tmkdirs "src/main/avro/user.avsc" append
            loadResource("tasks/compiler/user.avsc").readText()

        /* When */
        val result = buildGradleRunner()

        /* Then */
        val compile = result.task(":compileApacheAvroJava")
        assertThat(compile)
            .isNotNull
            .extracting { it?.outcome }
            .isSameAs(TaskOutcome.SUCCESS)

        val stringClass = when (config) {
            "String" -> "java.lang.String"
            "string" -> "java.lang.String"
            "CharSequence" -> "java.lang.CharSequence"
            "charsequence" -> "java.lang.CharSequence"
            "CHARSEQUENCE" -> "java.lang.CharSequence"
            "Utf8" -> "org.apache.avro.util.Utf8"
            "utf8" -> "org.apache.avro.util.Utf8"
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
    @ValueSource(strings = ["PUBLIC", "public", "PRIVATE", "private"])
    fun t3(config: String) {

        /* Given */
        build append """
            
            import org.apache.avro.compiler.specific.SpecificCompiler
            import org.apache.avro.generic.GenericData

            plugins {
                id 'java'
                id 'idea'
                id 'io.github.leofuso.argo'
            }
            
            repositories {
                mavenLocal()
                mavenCentral()
            }
            
            dependencies {
                
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            argo {
                columba {
                    fields {
                        visibility = '$config'
                    }
                }
            }
            
        """
            .trimIndent()

        rootDir tmkdirs "src/main/avro/user.avsc" append
            loadResource("tasks/compiler/user.avsc").readText()

        /* When */
        val result = buildGradleRunner()

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
                id 'idea'
                id 'io.github.leofuso.argo'
            }
            
            repositories {
                mavenLocal()
                mavenCentral()
            }
            
            dependencies {
                
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
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
        val result = buildGradleRunner()

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
                id 'idea'
                id 'io.github.leofuso.argo'
            }
            
            repositories {
                mavenLocal()
                mavenCentral()
            }
            
            dependencies {
                
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
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
        val result = buildGradleRunner()

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
                id 'idea'                
                id 'io.github.leofuso.argo'
            }
            
            repositories {
                mavenLocal()
                mavenCentral()
            }
            
            dependencies {
                
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
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
        val result = buildGradleRunner()

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

    @DisplayName(
        """
 Given an Argo 'gradle.build' with useDecimalType config,
 when building,
 then should produce the necessary Java files, with correct caracteristics.
"""
    )
    @ParameterizedTest(name = "{index} ==> Use decimal type ''{0}''")
    @CsvSource(
        "Boolean.TRUE, true",
        "Boolean.FALSE, false",
        "'true', true",
        "'false', false",
        "true, true",
        "false, false"
    )
    fun t6(config: String, useDecimalType: Boolean) {

        /* Given */
        build append """
            
            plugins {
                id 'java'
                id 'idea'                
                id 'io.github.leofuso.argo'
            }
            
            repositories {
                mavenLocal()
                mavenCentral()
            }
            
            dependencies {
                
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            argo {
                columba {
                    fields {
                        useDecimalType = $config
                    }
                }
            }
            
        """
            .trimIndent()

        rootDir tmkdirs "src/main/avro/user.avsc" append
            loadResource("tasks/compiler/user.avsc").readText()

        /* When */
        val result = buildGradleRunner()

        /* Then */
        val compile = result.task(":compileApacheAvroJava")
        assertThat(compile)
            .isNotNull
            .extracting { it?.outcome }
            .isSameAs(TaskOutcome.SUCCESS)

        if (useDecimalType) {
            assertThat(Path("${rootDir.absolutePath}/build/generated-main-specific-record/example/avro/User.java"))
                .exists()
                .content()
                .asString()
                .contains("public java.math.BigDecimal getSalary()")
                .doesNotContain("public java.nio.ByteBuffer getSalary()")
        } else {
            assertThat(Path("${rootDir.absolutePath}/build/generated-main-specific-record/example/avro/User.java"))
                .exists()
                .content()
                .asString()
                .contains("public java.nio.ByteBuffer getSalary()")
                .doesNotContain("public java.math.BigDecimal getSalary()")
        }
    }

    @Test
    @DisplayName(
        """
 Given an Argo 'gradle.build' with a custom template directory,
 when building,
 then should produce the necessary Java files, with correct caracteristics.
"""
    )
    fun t7() {

        /* Given */
        build append """
            
            plugins {
                id 'java'
                id 'idea'                
                id 'io.github.leofuso.argo'
            }
            
            repositories {
                mavenLocal()
                mavenCentral()
            }
            
            dependencies {
                
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            argo {
                columba {
                    velocityTemplateDirectory = file('templates/custom/')
                }
            }
            
        """
            .trimIndent()

        rootDir tmkdirs "src/main/avro/user.avsc" append
            loadResource("tasks/compiler/user.avsc").readText()

        rootDir tmkdirs "templates/custom/record.vm" append
            loadResource("tasks/compiler/record.vm").readText()

        /* When */
        val result = buildGradleRunner()

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
            .contains("Custom template")
    }

    @Test
    @DisplayName(
        """
 Given an Argo 'gradle.build' with custom velocity classes,
 when building,
 then should produce the necessary Java files, with correct caracteristics.
"""
    )
    fun t8() {

        val classpath = readPluginClasspath<ColumbaFunctionalTest>()

        /* Given */
        build append """
             
                                  
            plugins {
                id 'java'
                id 'idea'                
                id 'io.github.leofuso.argo'
            }
            
            repositories {
                mavenLocal()
                mavenCentral()
            }
            
            dependencies {
                implementation 'org.apache.avro:avro:1.11.1'
                compileApacheAvroJava files($classpath)
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            argo {
                columba {
                    velocityTemplateDirectory = file('templates/custom/')
                    additionalVelocityTools = [
                            'io.github.leofuso.argo.custom.TimestampGenerator',
                            'io.github.leofuso.argo.custom.CommentGenerator'
                    ]
                }
            }
            
        """
            .trimIndent()

        rootDir tmkdirs "src/main/avro/user.avsc" append
            loadResource("tasks/compiler/user.avsc").readText()

        rootDir tmkdirs "templates/custom/record.vm" append
            loadResource("tasks/compiler/record-tools.vm").readText()

        /* When */
        val result = buildGradleRunner()

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
            .contains(CommentGenerator.CUSTOM_COMMENT)
            .contains(TimestampGenerator.MESSAGE_PREFIX)
    }

    @ParameterizedTest(name = "{index} ==> Use String type ''{0}''")
    @DisplayName(
        """
 Given an Argo 'gradle.build' with custom converter classes, and multiple String types,
 when building,
 then should produce the necessary Java files, with correct caracteristics.
"""
    )
    @ValueSource(strings = ["String", "CharSequence", "Utf8"])
    fun t9(stringType: String) {

        val classpath = readPluginClasspath<ColumbaFunctionalTest>()

        /* Given */
        build append """
                                  
            plugins {
                id 'java'
                id 'idea'                
                id 'io.github.leofuso.argo'
            }
            
            repositories {
                mavenLocal()
                mavenCentral()
            }
            
            dependencies {
                implementation 'org.apache.avro:avro:1.11.1'
                compileApacheAvroJava files($classpath)
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            argo {
                columba {
                    additionalLogicalTypeFactories.put('timezone', 'io.github.leofuso.argo.custom.TimeZoneLogicalTypeFactory')
                    additionalConverters.add('io.github.leofuso.argo.custom.TimeZoneConversion')
                    fields {
                        stringType = '$stringType'
                    }
                }
            }
            
        """
            .trimIndent()

        rootDir tmkdirs "src/main/avro/custom-conversion.avsc" append
            loadResource("tasks/compiler/custom-conversion.avsc").readText()

        /* When */
        val result = buildGradleRunner()

        /* Then */
        val compile = result.task(":compileApacheAvroJava")
        assertThat(compile)
            .isNotNull
            .extracting { it?.outcome }
            .isSameAs(TaskOutcome.SUCCESS)

        assertThat(Path("${rootDir.absolutePath}/build/generated-main-specific-record/test/Event.java"))
            .exists()
            .content()
            .asString()
            .contains("java.time.Instant start;")
            .contains("java.util.TimeZone timezone;")
    }

    @ParameterizedTest(name = "{index} ==> Use String type ''{0}''")
    @DisplayName(
        """
 Given an Argo 'gradle.build' with custom converter classes, and multiple String types,
 when building from a Protocol,
 then should produce the necessary Java files, with correct caracteristics.
"""
    )
    @ValueSource(strings = ["String", "CharSequence", "Utf8"])
    fun t10(stringType: String) {

        val classpath = readPluginClasspath<ColumbaFunctionalTest>()

        /* Given */
        build append """
                                  
            plugins {
                id 'java'
                id 'idea'                
                id 'io.github.leofuso.argo'
            }
            
            repositories {
                mavenLocal()
                mavenCentral()
            }
            
            dependencies {
                implementation 'org.apache.avro:avro:1.11.1'
                compileApacheAvroJava files($classpath)
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            argo {
                columba {
                    additionalLogicalTypeFactories.put('timezone', 'io.github.leofuso.argo.custom.TimeZoneLogicalTypeFactory')
                    additionalConverters.add('io.github.leofuso.argo.custom.TimeZoneConversion')
                    fields {
                        stringType = '$stringType'
                    }
                }
            }
            
        """
            .trimIndent()

        rootDir tmkdirs "src/main/avro/custom-conversion.avpr" append
            loadResource("tasks/compiler/custom-conversion.avpr").readText()

        /* When */
        val result = buildGradleRunner()

        /* Then */
        val compile = result.task(":compileApacheAvroJava")
        assertThat(compile)
            .isNotNull
            .extracting { it?.outcome }
            .isSameAs(TaskOutcome.SUCCESS)

        assertThat(Path("${rootDir.absolutePath}/build/generated-main-specific-record/test/Event.java"))
            .exists()
            .content()
            .asString()
            .contains("java.time.Instant start;")
            .contains("java.util.TimeZone timezone;")
    }
}
