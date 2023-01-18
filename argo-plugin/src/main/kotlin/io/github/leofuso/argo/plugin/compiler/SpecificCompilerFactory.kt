package io.github.leofuso.argo.plugin.compiler

import io.github.leofuso.argo.plugin.OptionalGettersStrategy
import io.github.leofuso.argo.plugin.tasks.SpecificRecordCompilerTask
import org.apache.avro.LogicalTypes
import org.apache.avro.Protocol
import org.apache.avro.Schema
import org.apache.avro.compiler.specific.SpecificCompiler
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.execution.commandline.TaskConfigurationException
import java.io.File

fun SpecificRecordCompilerTask.configure(): (SpecificCompiler) -> Unit = { compiler: SpecificCompiler ->

    /* Introduces side effect */
    getAdditionalLogicalTypeFactories().orNull?.forEach {
        val factoryClass = it
        val requiredDefaultConstructor = factoryClass.getDeclaredConstructor()
        val factory = requiredDefaultConstructor.newInstance()
        LogicalTypes.register(factory)
    }

    getAdditionalVelocityTools().orNull
        ?.map {
            val velocityToolClass = it
            val requiredDefaultConstructor = velocityToolClass.getDeclaredConstructor()
            requiredDefaultConstructor.newInstance()
        }
        .let(compiler::setAdditionalVelocityTools)

    getVelocityTemplateDirectory().orNull?.let {
        compiler.setTemplateDir(it.asFile.absolutePath)
    }

    getAdditionalConverters().orNull?.forEach(compiler::addCustomConversion)

    getStringType().orNull?.let { compiler.setStringType(it) }
    noSetters.orNull?.let { compiler.isCreateSetters = it.not() }
    useOptionalGetters.orNull?.let { compiler.isGettersReturnOptional = it }

    val optionalGettersStrategy = getOptionalGettersStrategy()
    if (compiler.isGettersReturnOptional && optionalGettersStrategy.isPresent) {
        compiler.isOptionalGettersForNullableFieldsOnly =
            optionalGettersStrategy.get() == OptionalGettersStrategy.ONLY_NULLABLE_FIELDS
    }

    addExtraOptionalGetters.orNull?.let { compiler.isCreateOptionalGetters = it }
    useDecimalType.orNull?.let { compiler.setEnableDecimalLogicalType(it) }
    getEncoding().orNull?.let { compiler.setOutputCharacterEncoding(it) }
    getFieldVisibility().orNull?.let { compiler.setFieldVisibility(it) }
}

fun fromSchema(
    parser: Schema.Parser,
    config: (SpecificCompiler) -> SpecificCompiler,
    errSupplier: (File, Throwable) -> (TaskConfigurationException)
) = { source: File ->

    runCatching { parser.parse(source) }
        .mapCatching { SpecificCompiler(it) }
        .mapCatching { config.invoke(it) }
        .fold(onSuccess = {
            it
        }, onFailure = {
                throw errSupplier.invoke(source, it)
            })
}

inline fun fromSchema(schema: Schema, config: (SpecificCompiler) -> Unit, errSupplier: (Throwable) -> (TaskExecutionException)) =
    runCatching { SpecificCompiler(schema) }
        .mapCatching { config.invoke(it) }
        .fold(onSuccess = {}, onFailure = { cause -> throw errSupplier.invoke(cause) })

inline fun fromProtocol(protocol: Protocol, config: (SpecificCompiler) -> Unit, errSupplier: (Throwable) -> (TaskExecutionException)) =
    runCatching { SpecificCompiler(protocol) }
        .mapCatching { config.invoke(it) }
        .fold(onSuccess = {}, onFailure = { cause -> throw errSupplier.invoke(cause) })

fun fromProtocol(config: (SpecificCompiler) -> SpecificCompiler, errSupplier: (File, Throwable) -> (TaskConfigurationException)) =
    { source: File ->
        runCatching { Protocol.parse(source) }
            .mapCatching { SpecificCompiler(it) }
            .mapCatching { config.invoke(it) }
            .fold(onSuccess = {
                it
            }, onFailure = {
                    throw errSupplier.invoke(source, it)
                })
    }
