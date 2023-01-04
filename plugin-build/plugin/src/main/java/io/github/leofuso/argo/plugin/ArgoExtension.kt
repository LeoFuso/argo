package io.github.leofuso.argo.plugin

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.gradle.util.internal.VersionNumber
import java.nio.charset.Charset


interface ArgoExtension {
    @Nested
    fun getColumbae(): ColumbaeOptions

    @Nested
    fun getNavis(): NavisOptions

}

interface NavisOptions

interface ColumbaeOptions {

    val version: Property<String>

    val outputEncoding: Property<in Charset>

    @Nested
    fun getFields(): ColumbaeFieldOptions

    @Nested
    fun getAccessors(): ColumbaeAccessorOptions

    val velocityTemplatesDir: DirectoryProperty

}

interface ColumbaeFieldOptions {

    val stringType: Property<String>

    val visibility: Property<String>

    val useBigDecimal: Property<Boolean>

}

interface ColumbaeAccessorOptions {

    val noSetters: Property<Boolean>

    val addExtraOptionalGetters: Property<Boolean>

    val optionalGetters: Property<String>

}
