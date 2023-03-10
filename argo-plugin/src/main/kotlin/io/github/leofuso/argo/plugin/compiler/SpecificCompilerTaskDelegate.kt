package io.github.leofuso.argo.plugin.compiler

import io.github.leofuso.argo.plugin.compiler.parser.Resolution
import io.github.leofuso.argo.plugin.enableDecimalLogicalType
import io.github.leofuso.argo.plugin.getAdditionalVelocityTools
import io.github.leofuso.argo.plugin.getCharacterEncoding
import io.github.leofuso.argo.plugin.getConverters
import io.github.leofuso.argo.plugin.getStringType
import io.github.leofuso.argo.plugin.getTemplateDirectory
import io.github.leofuso.argo.plugin.tasks.SpecificRecordCompilerTask
import org.apache.avro.LogicalTypes
import org.apache.avro.LogicalTypes.LogicalTypeFactory
import org.apache.avro.Schema
import org.apache.avro.compiler.specific.SpecificCompiler
import java.io.Closeable
import java.io.File

class SpecificCompilerTaskDelegate(private val task: SpecificRecordCompilerTask) : Closeable {

    private val stubSchema = Schema.createEnum("STUB", "A Stub Schema", "io.github.leofuso.argo.stub", listOf("STUB"))
    private val logger = task.logger

    private val classLoader = urlClassLoader(task.classpath.files)

    init {
        task.getAdditionalLogicalTypeFactories().orNull?.forEach {
            val unknownClass = classLoader.loadClass(it.value)
            val requiredDefaultConstructor = unknownClass.getDeclaredConstructor()
            val instance = requiredDefaultConstructor.newInstance()
            if (instance is LogicalTypeFactory) {
                LogicalTypes.register(it.key, instance)
            } else {
                logger.warn("Class [${unknownClass.name}] cannot be used as a LogicalTypeFactory.")
            }
        }
    }

    fun run(resolution: Resolution, output: File) {
        configurationReport()
        resolution.schemas
            .forEach { (_, schema) ->
                val compiler = SpecificCompiler(schema)
                doConfigure(compiler)
                compiler.compileToDestination(null, output)
                logger.lifecycle("Schema [{}] successfully compiled to destination.", schema.fullName)
            }
        resolution.protocol
            .forEach { (_, protocol) ->
                val compiler = SpecificCompiler(protocol)
                doConfigure(compiler)
                compiler.compileToDestination(null, output)
                logger.lifecycle("Schema [{}] successfully compiled to destination.", protocol.namespace + "." + protocol.name)
            }
    }

    private fun doConfigure(compiler: SpecificCompiler) {

        task.getAdditionalVelocityTools().orNull
            ?.map {
                val velocityToolClass = classLoader.loadClass(it)
                val requiredDefaultConstructor = velocityToolClass.getDeclaredConstructor()
                requiredDefaultConstructor.newInstance()
            }
            .let(compiler::setAdditionalVelocityTools)

        task.getAdditionalConverters().orNull?.map { classLoader.loadClass(it) }?.forEach(compiler::addCustomConversion)

        task.getVelocityTemplateDirectory().orNull?.let {
            val path = it.asFile.absolutePath
            val correctPath = path.endsWith(File.separator)
            if (correctPath) {
                compiler.setTemplateDir(path)
            } else {
                compiler.setTemplateDir(path + File.separator)
            }
        }

        task.getStringType().orNull?.let { compiler.setStringType(it) }
        task.noSetters.orNull?.let { compiler.isCreateSetters = it.not() }
        task.useOptionalGetters.orNull?.let { compiler.isGettersReturnOptional = it }

        val optionalGettersStrategy = task.optionalGettersForNullableFieldsOnly
        if (compiler.isGettersReturnOptional && optionalGettersStrategy.isPresent) {
            compiler.isOptionalGettersForNullableFieldsOnly = optionalGettersStrategy.get()
        }

        task.addExtraOptionalGetters.orNull?.let { compiler.isCreateOptionalGetters = it }
        task.useDecimalType.orNull?.let { compiler.setEnableDecimalLogicalType(it) }
        task.getEncoding().orNull?.let { compiler.setOutputCharacterEncoding(it) }
        task.getFieldVisibility().orNull?.let { compiler.setFieldVisibility(it) }
    }

    @SuppressWarnings("kotlin:S1192")
    private fun configurationReport() {

        if (logger.isInfoEnabled.not()) {
            logger.lifecycle("Run in info mode (-i or --info) to view configuration report.")
            return
        }

        /*  Doing this way as it would not be the true values passed to the compiler otherwise. */
        val stubCompiler = SpecificCompiler(stubSchema)
        doConfigure(stubCompiler)

        logger.info("SpecificCompiler configuration report: ")
        logger.info("\tOutput encoding [{}]", stubCompiler.getCharacterEncoding())
        logger.info("\tString class [{}]", stubCompiler.getStringType())
        logger.info("\tUse private field visibility [{}]", stubCompiler.privateFields())
        logger.info("\tUse decimal type [{}]", stubCompiler.enableDecimalLogicalType())
        logger.info("\tNo setters [{}]", stubCompiler.isCreateSetters.not())
        logger.info("\tAdd Extra Optional getters [{}]", stubCompiler.isCreateOptionalGetters)
        logger.info(
            "\tGetters return Optional [{}], nullable fields only? [{}]",
            stubCompiler.isGettersReturnOptional,
            stubCompiler.isOptionalGettersForNullableFieldsOnly
        )

        logger.info("\tVelocity template directory ['{}']", stubCompiler.getTemplateDirectory())
        stubCompiler.getAdditionalVelocityTools()
            .also { tools ->
                if (tools.isNotEmpty()) {
                    logger.info(
                        "\tAdditional velocity tools {}",
                        tools.map { it?.javaClass?.simpleName }.joinToString(
                            ",\n",
                            "[\n",
                            "\n\t ]",
                            transform = { "\t\t$it" }
                        )
                    )
                } else {
                    logger.info("\tNo additional velocity tools.")
                }
            }

        stubCompiler.getConverters()
            .also { converters ->
                if (converters.isNotEmpty()) {
                    logger.info(
                        "\tAdditional converters {}",
                        converters.joinToString(",\n", "[\n", "\n\t ]", transform = { "\t\t${it.javaClass.name}" })
                    )
                } else {
                    logger.info("\tNo additional converters.")
                }
            }

        LogicalTypes.getCustomRegisteredTypes().entries
            .also { types ->
                if (types.isNotEmpty()) {
                    logger.info(
                        "\tAdditional Logical Type factories {}",
                        types.joinToString(",\n", "[\n", "\n\t ]", transform = { "\t\t${it.key}:${it.value.javaClass.name}" })
                    )
                } else {
                    logger.info("\tNo Additional Logical Type factories.")
                }
            }
    }

    override fun close() {
        classLoader.close()
    }
}
