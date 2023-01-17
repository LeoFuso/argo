package io.github.leofuso.argo.plugin;

import io.github.leofuso.argo.plugin.parser.DefaultSchemaParser;
import io.github.leofuso.argo.plugin.parser.DependencyGraphAwareSchemaParser;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SchemaParserUnitTest {

    private DependencyGraphAwareSchemaParser subject;

    @BeforeEach
    void setUp() {
        final Project project = ProjectBuilder.builder().build();
        subject = new DefaultSchemaParser(project.getLogger());
    }

    @Test
    void stuff() {
        Assertions.assertTrue(true);
    }
}
