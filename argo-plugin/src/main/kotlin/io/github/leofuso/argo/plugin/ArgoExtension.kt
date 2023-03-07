package io.github.leofuso.argo.plugin

import org.apache.avro.compiler.specific.SpecificCompiler.FieldVisibility
import org.apache.avro.generic.GenericData.StringType
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

object ArgoExtensionSupplier {
    fun get(project: Project): ArgoExtension {
        val extension = project.extensions.create<ArgoExtension>("argo")
        extension.getColumba().withConventions(project)
        return extension
    }
}

@Suppress("unused")
abstract class ArgoExtension {

    @Nested
    abstract fun getColumba(): ColumbaOptions

    @Nested
    abstract fun getNavis(): NavisOptions

    fun columba(action: Action<in ColumbaOptions>) = action.invoke(getColumba())

    fun navis(action: Action<in NavisOptions>) = action.invoke(getNavis())
}

interface NavisOptions

@Suppress("TooManyFunctions", "MemberVisibilityCanBePrivate")
abstract class ColumbaOptions {

    abstract fun getCompiler(): Property<String>

    abstract fun getExcluded(): ListProperty<String>

    abstract fun getOutputEncoding(): Property<String>

    @Nested
    abstract fun getFields(): ColumbaFieldOptions

    fun fields(action: Action<in ColumbaFieldOptions>) = action.invoke(getFields())

    @Nested
    abstract fun getAccessors(): ColumbaAccessorOptions

    fun accessors(action: Action<in ColumbaAccessorOptions>) = action.invoke(getAccessors())

    abstract fun getAdditionalVelocityTools(): ListProperty<String>

    abstract fun getVelocityTemplateDirectory(): DirectoryProperty

    abstract fun getAdditionalLogicalTypeFactories(): MapProperty<String, String>

    abstract fun getAdditionalConverters(): ListProperty<String>

    @Internal
    fun withConventions(project: Project): ColumbaOptions {
        getCompiler().convention(DEFAULT_APACHE_AVRO_COMPILER_DEPENDENCY)
        getExcluded().convention(listOf())
        getOutputEncoding().convention("UTF-8")
        getAdditionalVelocityTools().convention(listOf())
        getVelocityTemplateDirectory().convention(project.objects.directoryProperty())
        getAdditionalConverters().convention(listOf())
        getAdditionalLogicalTypeFactories().convention(mapOf())

        fields {
            it.getStringType().convention(StringType.CharSequence)
            it.getVisibility().convention(FieldVisibility.PRIVATE)
            it.useDecimalTypeProperty.convention(true)
        }

        accessors {
            it.noSetterProperty.convention(true)
            it.addExtraOptionalGettersProperty.convention(false)
            it.useOptionalGettersProperty.convention(true)
            it.optionalGettersForNullableFieldsOnlyProperty.convention(true)
        }

        return this
    }
}

abstract class ColumbaFieldOptions(@Inject val project: Project) {

    private val _useDecimalTypeProperty = project.objects.property<Boolean>()

    val useDecimalTypeProperty: Property<Boolean>
        get() = _useDecimalTypeProperty

    @get:Internal
    var useDecimalType: Boolean
        get() = _useDecimalTypeProperty.get()
        set(value) = _useDecimalTypeProperty.set(value)

    abstract fun getStringType(): Property<StringType>

    abstract fun getVisibility(): Property<FieldVisibility>
}

abstract class ColumbaAccessorOptions(@Inject val project: Project) {

    private val _noSetterProperty = project.objects.property<Boolean>()
    private val _addExtraOptionalGettersProperty = project.objects.property<Boolean>()
    private val _useOptionalGettersProperty = project.objects.property<Boolean>()
    private val _optionalGettersForNullableFieldsOnlyProperty = project.objects.property<Boolean>()

    val noSetterProperty: Property<Boolean>
        get() = _noSetterProperty

    val addExtraOptionalGettersProperty: Property<Boolean>
        get() = _addExtraOptionalGettersProperty

    val useOptionalGettersProperty: Property<Boolean>
        get() = _useOptionalGettersProperty

    val optionalGettersForNullableFieldsOnlyProperty: Property<Boolean>
        get() = _optionalGettersForNullableFieldsOnlyProperty

    @get:Internal
    var noSetters: Boolean
        get() = _noSetterProperty.get()
        set(value) = _noSetterProperty.set(value)

    @get:Internal
    var addExtraOptionalGetters: Boolean
        get() = _addExtraOptionalGettersProperty.get()
        set(value) = _addExtraOptionalGettersProperty.set(value)

    @get:Internal
    var useOptionalGetters: Boolean
        get() = _useOptionalGettersProperty.get()
        set(value) = _useOptionalGettersProperty.set(value)

    @get:Internal
    var optionalGettersForNullableFieldsOnly: Boolean
        get() = _optionalGettersForNullableFieldsOnlyProperty.get()
        set(value) = _optionalGettersForNullableFieldsOnlyProperty.set(value)

}
