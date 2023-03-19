package io.github.leofuso.argo.plugin

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

object ArgoExtensionSupplier {
    fun get(project: Project): ArgoExtension {
        val extension = project.extensions.create<ArgoExtension>("argo")
        extension.getColumba().applyConventions()
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
abstract class ColumbaOptions(@Inject val project: Project, @Inject val javaToolchainService: JavaToolchainService) {

    @get:Nested
    abstract val launcher: Property<JavaLauncher>

    abstract fun getCompiler(): Property<String>

    abstract fun getVersion(): Property<String>

    abstract fun getExcluded(): ListProperty<String>

    abstract fun getOutputEncoding(): Property<String>

    @Nested
    abstract fun getFields(): ColumbaFieldOptions

    fun fields(action: Action<in ColumbaFieldOptions>) = action.invoke(getFields())

    @Nested
    abstract fun getAccessors(): ColumbaAccessorOptions

    fun accessors(action: Action<in ColumbaAccessorOptions>) = action.invoke(getAccessors())

    abstract fun getAdditionalVelocityTools(): ListProperty<String>

    /**
     * Add a Velocity Tool to the additional velocity tools collection.
     * @param tool the fully qualified name of the velocity tool to be added.
     */
    @Suppress("unused")
    fun velocityTool(tool: String) = getAdditionalVelocityTools().add(tool)

    abstract fun getVelocityTemplateDirectory(): DirectoryProperty

    abstract fun getAdditionalLogicalTypeFactories(): MapProperty<String, String>

    /**
     * Add a Logical Type Factory to the additional logical type factory collection.
     * @param name the name of the factory to be added.
     * @param reference the fully qualified name of the logical type factory to be added.
     */
    @Suppress("unused")
    fun logicalTypeFactory(name: String, reference: String) = getAdditionalLogicalTypeFactories().put(name, reference)

    abstract fun getAdditionalConverters(): ListProperty<String>

    /**
     * Add a Converter to the additional converter collection.
     * @param converter the fully qualified name of the converter to be added.
     */
    @Suppress("unused")
    fun converter(converter: String) = getAdditionalConverters().add(converter)

    @Internal
    fun applyConventions(): ColumbaOptions {

        val toolchain = project.extensions.getByType<JavaPluginExtension>().toolchain
        val defaultLauncher = javaToolchainService.launcherFor(toolchain)
        launcher.convention(defaultLauncher)

        getCompiler().convention(DEFAULT_APACHE_AVRO_COMPILER_DEPENDENCY)
        getVersion().convention(DEFAULT_COLUMBA_CLI_DEPENDENCY)
        getExcluded().convention(listOf())
        getOutputEncoding().convention("UTF-8")
        getAdditionalVelocityTools().convention(listOf())
        getVelocityTemplateDirectory().convention(project.objects.directoryProperty())
        getAdditionalConverters().convention(listOf())
        getAdditionalLogicalTypeFactories().convention(mapOf())

        fields {
            it.getStringType().convention("CharSequence")
            it.getVisibility().convention("PRIVATE")
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

    abstract fun getStringType(): Property<String>

    abstract fun getVisibility(): Property<String>
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
