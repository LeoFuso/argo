package io.github.leofuso.argo.plugin.navis.credentials

import org.gradle.api.credentials.Credentials
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.security.auth.spi.LoginModule

/**
 * Used to allow for user customization of SASL over JAAS.
 *
 * A user can populate this credential by any of these strategies:
 *
 *  1. Populating a `gradle.config` file, either in the project or in the Gradle directory, with the needed properties;
 *  2. Passing the needed properties as project variables to Gradle, e.g. `--project-prop schema.registry.sasl.jaas.config=config`;
 *  2. Using the DSL to manually populate this credential;
 *
 *  Expected configuration:
 *
 *  ```
 *  schema.registry.sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username='username' password='password';
 *  schema.registry.sasl.jaas.config.login.module=org.apache.kafka.common.security.scram.ScramLoginModule
 *  schema.registry.sasl.jaas.config.control.flag=required
 *  schema.registry.sasl.jaas.config.option.username=username
 *  schema.registry.sasl.jaas.config.option.password=password
 *  ```
 */
abstract class JAASCredentials : Credentials {

    enum class LoginModuleControlFlag(val flag: String) {
        REQUIRED("required"),
        REQUISITE("requisite"),
        SUFFICIENT("sufficient"),
        OPTIONAL("optional")
    }

    /**
     * Used by the [JAAS Configuration Parser][org.apache.kafka.common.security.JaasConfig] to construct a JAAS configuration
     * object with a single login context from the Kafka configuration option
     * [sasl.jaas.config][org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG].
     *
     * JAAS configuration file format is described
     * [here](http://docs.oracle.com/javase/8/docs/technotes/guides/security/jgss/tutorials/LoginConfigFile.html).
     *
     * The format of the property must be the following:
     *
     * ```
     *   <loginModuleClass> <controlFlag> (<optionName>=<optionValue>)*;
     * ```
     *
     */
    fun toProperty(): String {

        if (getSaslJaasConfig().isPresent) {
            return getSaslJaasConfig().get()
        }

        val loginModule = getLoginModule()
            .orNull
            ?: error("Missing property value for 'LoginModule'.")

        val loginModuleControlFlag = getLoginModuleControlFlag()
            .map(LoginModuleControlFlag::flag)
            .orNull
            ?: error("Missing property value for 'LoginModuleControlFlag'.")

        val options = getOptions()
            .map(Map<String, String>::toList)
            .map { it.joinToString(" ") { (key: String, value: String) -> "$key='$value'" } }
            .get()

        return "${loginModule.name} $loginModuleControlFlag $options;"
    }

    abstract fun getSaslJaasConfig(): Property<String>

    /**
     * A way of manually setting the [sasl.jaas.config][org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG]
     * without needing to populate the other properties.
     * @param config the complete [sasl.jaas.config][org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG].
     */
    fun saslJaasConfig(config: String) = getSaslJaasConfig().set(config)

    abstract fun getLoginModule(): Property<Class<out LoginModule>>

    fun loginModule(module: Class<out LoginModule>) = getLoginModule().set(module)

    abstract fun getLoginModuleControlFlag(): Property<LoginModuleControlFlag>

    fun controlFlag(flag: LoginModuleControlFlag) = getLoginModuleControlFlag().set(flag)

    abstract fun getOptions(): MapProperty<String, String>

    fun options(options: Map<String, String>) = options.forEach(::option)

    fun option(key: String, value: String) = getOptions().put(key, value)

}
