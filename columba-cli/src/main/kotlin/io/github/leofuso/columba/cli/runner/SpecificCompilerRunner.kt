package io.github.leofuso.columba.cli.runner

import io.github.leofuso.columba.cli.command.CompileCommand
import io.github.leofuso.columba.cli.configurer.SpecificCompilerConfigurer
import org.apache.avro.compiler.specific.SpecificCompiler

class SpecificCompilerRunner(command: CompileCommand) : Runnable {

    private val logger = command.logger

    private val configurer = SpecificCompilerConfigurer(command)
    private val parser = SchemaParserSupplier(command).get()
    private val output = command.dest

    override fun run() {

        val resolution = parser.parse()
        configurer.report()

        resolution.schemas
            .forEach { (_, schema) ->
                val compiler = SpecificCompiler(schema)
                configurer.configure(compiler)
                compiler.compileToDestination(null, output)
                logger.lifecycle("Schema [{}] successfully compiled to destination.", schema.fullName)
            }

        resolution.protocol
            .forEach { (_, protocol) ->
                val compiler = SpecificCompiler(protocol)
                configurer.configure(compiler)
                compiler.compileToDestination(null, output)
                logger.lifecycle("Schema [{}] successfully compiled to destination.", protocol.namespace + "." + protocol.name)
            }
    }
}
