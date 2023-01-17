package io.github.leofuso.argo.plugin.parser

import io.github.leofuso.argo.plugin.fixture.FileTreeParameterResolver
import io.github.leofuso.argo.plugin.fixture.annotation.SchemaParameter
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions

// @DisplayName("SchemaParser Unit tests")
@Extensions(
    ExtendWith(FileTreeParameterResolver::class)
)
class SchemaParserTest {

    private lateinit var project: Project
    private lateinit var subject: DependencyGraphAwareSchemaParser

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()
        subject = DefaultSchemaParser(project.logger)
    }

    @Test
    fun stuff(@SchemaParameter(location = "parser/scenarios/common/Enum.avsc") source: FileTree) {
        /* When */
        val resolution = subject.parse(source)

        /* Then */
        print("")
    }
}
