package io.github.leofuso.argo.plugin.tasks

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.github.leofuso.argo.plugin.*
import io.github.leofuso.argo.plugin.navis.GlobalSchemaRegistry
import io.github.leofuso.argo.plugin.navis.SchemaRegistry
import io.github.leofuso.argo.plugin.navis.extensions.DownloadSubjectOptions
import io.github.leofuso.argo.plugin.navis.extensions.NavisOptions
import io.github.leofuso.argo.plugin.navis.security.credentials.Credentials
import io.github.leofuso.argo.plugin.navis.security.credentials.SSLCredentials
import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.net.URI
import kotlin.io.path.Path

@CacheableTask
abstract class DownloadTask : DefaultTask() {

    init {
        description = "Downloads Schemas(.$SCHEMA_EXTENSION) from the Schema Registry."
        group = GROUP_SCHEMA_REGISTRY
    }

    @Input
    abstract fun getURL(): Property<URI>

    @Input
    abstract fun getSubjects(): ListProperty<DownloadSubjectOptions>

    @Input
    abstract fun getConfig(): MapProperty<String, String>

    @TaskAction
    fun process() {

        val url = getURL().map { it.toURL().toString() }.get()
        val configs = getConfig().get()
        val schemaConfig = SchemaRegistry.SchemaConfig(url = url, configs = configs)

        GlobalSchemaRegistry.configure(schemaConfig)
        val client = GlobalSchemaRegistry.client()
        val subjects = getSubjects().get()

        subjects.forEach { subject ->

            val name = subject.name
            val version = subject.getVersion().orNull
            val metadata = if (version != null) client.getSchemaMetadata(name, version) else client.getLatestSchemaMetadata(name)
            val schema = client.parseSchema(AvroSchema.TYPE, metadata.schema, metadata.references)
                .orElseThrow()

            subject.outputDir.mkdirs()
            val outputFile = File(subject.outputDir, "$name.$SCHEMA_EXTENSION")
            outputFile.createNewFile()
            logger.info("Writing to file $outputFile.")
            outputFile.printWriter().use { out ->
                out.println(schema.toString())
            }
        }

        TODO()
    }

    internal fun configureAt(source: SourceSet) {
        val output = run {
            val classpath = "src${File.separator}${source.name}${File.separator}avro"
            val path = Path(classpath)
            project.file(path)
        }

        val outputDirectory = project.objects.directoryProperty()
        outputDirectory.set(output)

        getSubjects()
            .map { list ->
                list.map { subject -> subject.getOutputDir().convention(outputDirectory) }
            }
    }

    fun withExtension(options: NavisOptions) {
        getURL().set(options.getURL())
        getSubjects().set(options.getDownloadSubjects())

        val ssl = options.getSecurity().getSSL().map(SSLCredentials::toProperties)
        val credentials = options.getSecurity().credentials.map(Credentials::toProperties)
        getConfig().putAll(ssl)
        getConfig().putAll(credentials)
    }
}
