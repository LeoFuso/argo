@file:Suppress("JUnitMalformedDeclaration")

package io.github.leofuso.argo.plugin

import com.speedment.common.combinatorics.Permutation
import io.github.leofuso.argo.plugin.compiler.parser.DefaultSchemaParser
import io.github.leofuso.argo.plugin.compiler.parser.DependencyGraphAwareSchemaParser
import io.github.leofuso.argo.plugin.fixtures.FileTreeParameterResolver
import io.github.leofuso.argo.plugin.fixtures.annotation.SchemaParameter
import io.github.leofuso.argo.plugin.fixtures.loadResource
import org.apache.avro.Schema
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.streams.toList

@DisplayName("SchemaParser Unit tests")
@Extensions(ExtendWith(FileTreeParameterResolver::class))
class SchemaParserTest {

    @TempDir
    private lateinit var rootDir: File

    private lateinit var project: Project
    private lateinit var subject: DependencyGraphAwareSchemaParser

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder
            .builder()
            .withProjectDir(rootDir)
            .withName("project-fixture")
            .build()

        subject = DefaultSchemaParser(project.logger)
    }

    @DisplayName("Simple parsings.")
    @ParameterizedTest(
        name = """
 {index}: Given a {1},
 when parsing,
 then should resolve into a valid Schema.
"""
    )
    @MethodSource("simpleParsingMethodFactory")
    fun cabacc37b86b4baea31aaa461e8b7cdb(expected: Array<String>, source: FileTree) {
        /* When */
        val resolution = subject.parse(source)

        /* Then */
        assertThat(resolution)
            .extracting("schemas")
            .asInstanceOf(InstanceOfAssertFactories.map(String::class.java, Schema::class.java))
            .containsOnlyKeys(*expected)
    }

    @TestFactory
    fun permutedMethodFactory(): List<DynamicTest> {

        val record = loadResource<SchemaParserTest>("parser/scenarios/common/Record.avsc")
        val enum = loadResource<SchemaParserTest>("parser/scenarios/common/Enum.avsc")
        val fixed = loadResource<SchemaParserTest>("parser/scenarios/common/Fixed.avsc")
        val recordWithArray = loadResource<SchemaParserTest>("parser/scenarios/common/RecordWithArray.avsc")

        return Permutation.of(record, enum, fixed, recordWithArray)
            .map { permutation ->

                val directory = project.mkdir("permutation-${UUID.randomUUID()}").toPath().toFile()

                val permutationRef = permutation.map { file ->
                    file.copyTo(File(directory, file.name))
                    file.name
                }.collect(Collectors.joining(", ", "[ ", " ]"))

                val graph = project.objects.fileCollection().from(directory).asFileTree

                DynamicTest.dynamicTest("Permutation: $permutationRef") {

                    val resolution = subject.parse(graph)
                    assertThat(resolution)
                        .extracting("schemas")
                        .asInstanceOf(InstanceOfAssertFactories.map(String::class.java, Schema::class.java))
                        .containsOnlyKeys(
                            "io.github.leofuso.argo.plugin.parser.Enum",
                            "io.github.leofuso.argo.plugin.parser.Fixed",
                            "io.github.leofuso.argo.plugin.parser.Record",
                            "io.github.leofuso.argo.plugin.parser.RecordWithArray"
                        )
                }
            }.toList()
    }

    companion object {

        @JvmStatic
        fun simpleParsingMethodFactory(
            @SchemaParameter(location = "parser/scenarios/common/Record.avsc") record: FileTree,
            @SchemaParameter(location = "parser/scenarios/common/Enum.avsc") enum: FileTree,
            @SchemaParameter(location = "parser/scenarios/common/Fixed.avsc") fixed: FileTree,
            @SchemaParameter(location = "parser/scenarios/common") array: FileTree
        ): Stream<Arguments> {
            return Stream.of(
                Arguments.of(arrayOf("io.github.leofuso.argo.plugin.parser.Record"), named("Record.avsc", record)),
                Arguments.of(arrayOf("io.github.leofuso.argo.plugin.parser.Fixed"), named("Fixed.avsc", fixed)),
                Arguments.of(arrayOf("io.github.leofuso.argo.plugin.parser.Enum"), named("Enum.avsc", enum)),
                Arguments.of(
                    arrayOf(
                        "io.github.leofuso.argo.plugin.parser.Enum",
                        "io.github.leofuso.argo.plugin.parser.Fixed",
                        "io.github.leofuso.argo.plugin.parser.Record",
                        "io.github.leofuso.argo.plugin.parser.RecordWithArray"
                    ), named("RecordWithArray.avsc", array)
                )
            )
        }
    }
}
