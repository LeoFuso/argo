package io.github.leofuso.columba.compiler.cli.runner

import io.github.leofuso.columba.compiler.cli.CompileCommandRunner
import io.github.leofuso.columba.compiler.cli.configurer.SpecificCompilerConfigurer
import org.apache.avro.compiler.specific.SpecificCompiler

class SpecificCompilerRunner(runner: CompileCommandRunner) : Runnable {

    private val logger = runner.logger

    private val configurer = SpecificCompilerConfigurer(runner)
    private val parser = SchemaParserSupplier(runner).get()
    private val output = runner.dest

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
