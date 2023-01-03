package io.github.leofuso.argo.plugin

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import java.nio.charset.Charset


interface ArgoExtension {
    @Nested
    fun getColumbae(): ColumbaeOptions

}

interface ColumbaeOptions {

    val outputEncoding: Property<in Charset>

    @Nested
    fun getFields(): FieldOptions

    @Nested
    fun getAccessors(): AccessorOptions

    val velocityTemplatesDir: DirectoryProperty

}

interface FieldOptions {

    val stringType: Property<String>

    val visibility: Property<String>

    val useBigDecimal: Property<Boolean>

}

interface AccessorOptions {

    val noSetters: Property<Boolean>

    val addExtraOptionalGetters: Property<Boolean>

    val optionalGetters: Property<String>

}
