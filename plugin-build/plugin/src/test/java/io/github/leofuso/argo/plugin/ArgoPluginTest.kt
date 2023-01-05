package io.github.leofuso.argo.plugin

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.File

class ArgoPluginTest {

    @Test
    fun `plugin is applied correctly to the project`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("io.github.leofuso.argo")

        assert(project.tasks.getByName("templateExample") is TemplateExampleTask)
    }

    @Test
    fun `extension templateExampleConfig is created correctly`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("io.github.leofuso.argo")

        assertNotNull(project.extensions.getByName("templateExampleConfig"))
    }

    @Test
    fun `parameters are passed correctly from extension to task`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("io.github.leofuso.argo")
        val aFile = File(project.projectDir, ".tmp")
        (project.extensions.getByName(ARGO_EXTENSION) as ArgoExtension).apply {
//            tag.set("a-sample-tag")
//            message.set("just-a-message")
//            outputFile.set(aFile)
        }

        val task = project.tasks.getByName("templateExample") as TemplateExampleTask

        assertEquals("a-sample-tag", task.tag.get())
        assertEquals("just-a-message", task.message.get())
        assertEquals(aFile, task.outputFile.get().asFile)
    }
}
