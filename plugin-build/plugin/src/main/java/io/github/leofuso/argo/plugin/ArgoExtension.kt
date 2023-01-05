package io.github.leofuso.argo.plugin

import org.apache.avro.compiler.specific.SpecificCompiler.*
import org.apache.avro.generic.GenericData.StringType
import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

abstract class ArgoExtension {

    @Nested
    abstract fun getColumbae(): ColumbaeOptions

    @Nested
    abstract fun getNavis(): NavisOptions

    fun columbae(action: Action<in ColumbaeOptions>) {
        action.execute(getColumbae())
    }

    fun navis(action: Action<in NavisOptions>) {
        action.execute(getNavis())
    }
}

interface NavisOptions

abstract class ColumbaeOptions {

    abstract fun getCompilerVersion(): Property<String>

    fun compilerVersion(action: Action<Property<String>>) {
        action.execute(getCompilerVersion())
    }

    abstract fun getOutputEncoding(): Property<String>

    @Nested
    abstract fun getFields(): ColumbaeFieldOptions

    fun fields(action: Action<in ColumbaeFieldOptions>) {
        action.execute(getFields())
    }

    @Nested
    abstract fun getAccessors(): ColumbaeAccessorOptions

    fun accessors(action: Action<in ColumbaeAccessorOptions>) {
        action.execute(getAccessors())
    }

    abstract fun getVelocityTemplatesDir(): DirectoryProperty
}

abstract class ColumbaeFieldOptions {

    abstract fun getStringType(): Property<StringType>

    abstract fun getVisibility(): Property<FieldVisibility>

    //abstract fun useBigDecimal(): Property<Boolean>

}

abstract class ColumbaeAccessorOptions {

    abstract fun isNoSetters(): Property<Boolean>

    abstract fun isAddExtraOptionalGetters(): Property<Boolean>

    abstract fun getOptionalGetters(): Property<String>
}
