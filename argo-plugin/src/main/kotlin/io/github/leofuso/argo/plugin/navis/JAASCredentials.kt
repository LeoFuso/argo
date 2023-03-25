package io.github.leofuso.argo.plugin.navis

import org.gradle.api.credentials.Credentials
import org.gradle.api.tasks.Internal
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag
import javax.security.auth.spi.LoginModule

/**
 * Used to allow for user customization of SASL over JAAS.
 *
 * A user can populate this credential by any of these strategies:
 *
 *  1. Populating a `gradle.config` file, either in the project or in the Gradle directory, with the needed properties;
 *  2. Passing the needed properties as variables to Gradle;
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
interface JAASCredentials : Credentials {

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
    @Internal
    fun getSaslJaasConfig(): String

    /**
     * A way of manually setting the [sasl.jaas.config][org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG]
     * without needing to populate the other properties.
     * @param config the complete [sasl.jaas.config][org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG].
     */
    fun setSaslJaasConfig(config: String)

    fun loginModule(module: Class<out LoginModule>)

    fun controlFlag(flag: LoginModuleControlFlag)

    fun options(options: Map<String, String>)

    fun option(key: String, value: String)

}
