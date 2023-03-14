package io.github.leofuso.columba.cli.configurer

import io.github.leofuso.columba.cli.command.CompileCommand
import io.github.leofuso.columba.cli.enableDecimalLogicalType
import io.github.leofuso.columba.cli.getAdditionalVelocityTools
import io.github.leofuso.columba.cli.getCharacterEncoding
import io.github.leofuso.columba.cli.getConverters
import io.github.leofuso.columba.cli.getStringType
import io.github.leofuso.columba.cli.getTemplateDirectory
import org.apache.avro.LogicalTypes
import org.apache.avro.Schema
import org.apache.avro.compiler.specific.SpecificCompiler
import java.io.File

class SpecificCompilerConfigurer(private val command: CompileCommand) {

    private val logger = command.logger

    init {
        command.converter.additionalLogicalTypeFactories.forEach {
            val unknownClass = Class.forName(it.value)
            val requiredDefaultConstructor = unknownClass.getDeclaredConstructor()
            val instance = requiredDefaultConstructor.newInstance()
            if (instance is LogicalTypes.LogicalTypeFactory) {
                LogicalTypes.register(it.key, instance)
            } else {
                logger.error("Class [${unknownClass.name}] cannot be used as a LogicalTypeFactory.")
            }
        }
    }

    fun configure(compiler: SpecificCompiler) {

        command.velocity.additionalVelocityTools
            .map {
                val velocityToolClass = Class.forName(it)
                val requiredDefaultConstructor = velocityToolClass.getDeclaredConstructor()
                requiredDefaultConstructor.newInstance()
            }
            .let(compiler::setAdditionalVelocityTools)

        command.converter.additionalConverters.map { Class.forName(it) }.forEach(compiler::addCustomConversion)

        command.velocity.velocityTemplate?.let {
            val path = it.toString()
            val correctPath = path.endsWith(File.separator)
            if (correctPath) {
                compiler.setTemplateDir(path)
            } else {
                compiler.setTemplateDir(path + File.separator)
            }
        }

        command.outputEncoding.let { compiler.setOutputCharacterEncoding(it) }
        command.fields.stringType.let { compiler.setStringType(it) }
        command.fields.visibility.let { compiler.setFieldVisibility(it) }
        command.fields.useDecimalType.let { compiler.setEnableDecimalLogicalType(it) }
        command.accessors.allowSetters.let { compiler.isCreateSetters = it }
        command.accessors.useOptionalGetters.let { compiler.isGettersReturnOptional = it }
        command.accessors.useOptionalGettersForNullableFieldsOnly.let { compiler.isOptionalGettersForNullableFieldsOnly = it }
        command.accessors.addExtraOptionalGetters.let { compiler.isCreateOptionalGetters = it }
    }

    @SuppressWarnings("kotlin:S1192")
    fun report() {

        if (!logger.isInfoEnabled()) {
            logger.lifecycle("Run in info mode (-i or --info) to view configuration report.")
            return
        }

        /*  Doing this way as it would not be the true values passed to the compiler otherwise. */
        val stubSchema = Schema.createEnum("STUB", "A Stub Schema", "io.github.leofuso.columba.stub", listOf("STUB"))
        val stubCompiler = SpecificCompiler(stubSchema)
        configure(stubCompiler)

        logger.info("SpecificCompiler configuration report: ")
        logger.info("\tOutput encoding [${stubCompiler.getCharacterEncoding()}]")
        logger.info("\tString class [${stubCompiler.getStringType()}]")
        logger.info("\tUse private field visibility [${stubCompiler.privateFields()}]")
        logger.info("\tUse decimal type [${stubCompiler.enableDecimalLogicalType()}]")
        logger.info("\tNo setters [${stubCompiler.isCreateSetters.not()}]")
        logger.info("\tAdd Extra Optional getters [${stubCompiler.isCreateOptionalGetters}]")
        logger.info(
            "\tGetters return Optional [${stubCompiler.isGettersReturnOptional}], " +
                "nullable fields only? [${stubCompiler.isOptionalGettersForNullableFieldsOnly}]"
        )

        logger.info("\tVelocity template directory ['${stubCompiler.getTemplateDirectory()}']")
        stubCompiler.getAdditionalVelocityTools()
            .also { tools ->
                if (tools.isNotEmpty()) {
                    logger.info(
                        "\tAdditional velocity tools ${
                        tools.map { it?.javaClass?.simpleName }.joinToString(
                            ",\n",
                            "[\n",
                            "\n\t ]",
                            transform = { "\t\t$it" }
                        )
                        }"
                    )
                } else {
                    logger.info("\tNo additional velocity tools.")
                }
            }

        stubCompiler.getConverters()
            .also { converters ->
                if (converters.isNotEmpty()) {
                    logger.info(
                        "\tAdditional converters ${
                        converters.joinToString(
                            ",\n",
                            "[\n",
                            "\n\t ]",
                            transform = { "\t\t${it.javaClass.name}" }
                        )
                        }"
                    )
                } else {
                    logger.info("\tNo additional converters.")
                }
            }

        LogicalTypes.getCustomRegisteredTypes().entries
            .also { types ->
                if (types.isNotEmpty()) {
                    logger.info(
                        "\tAdditional Logical Type factories ${
                        types.joinToString(
                            ",\n",
                            "[\n",
                            "\n\t ]",
                            transform = { "\t\t${it.key}:${it.value.javaClass.name}" }
                        )
                        }"
                    )
                } else {
                    logger.info("\tNo Additional Logical Type factories.")
                }
            }
    }

}
