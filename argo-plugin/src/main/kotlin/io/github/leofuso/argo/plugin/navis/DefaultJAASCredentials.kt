package io.github.leofuso.argo.plugin.navis

import org.apache.kafka.common.config.SaslConfigs
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag
import javax.security.auth.spi.LoginModule

class DefaultJAASCredentials() : JAASCredentials {

    private var completeConfig: String? = null
    private var loginModule: Class<out LoginModule>? = null
    private var controlFlag: LoginModuleControlFlag? = null
    private val options: MutableMap<String, String> = mutableMapOf()

    constructor(loginModule: String, controlFlag: String) : this() {

        if (loginModule.isBlank() || controlFlag.isBlank()) {
            return
        }

        this.loginModule = Class.forName(loginModule)
            .let { clazz ->
                if (clazz.isAssignableFrom(LoginModule::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    clazz as Class<out LoginModule>
                } else {
                    error("Unsupported 'LoginModule' property. Class [$loginModule] does not implement LoginModule.")
                }
            }

        this.controlFlag = when (controlFlag.uppercase()) {
            "REQUIRED" -> LoginModuleControlFlag.REQUIRED
            "REQUISITE" -> LoginModuleControlFlag.REQUISITE
            "SUFFICIENT" -> LoginModuleControlFlag.SUFFICIENT
            "OPTIONAL" -> LoginModuleControlFlag.OPTIONAL
            else -> error("Invalid login module control flag '$controlFlag' in JAAS config.")
        }
    }

    override fun getSaslJaasConfig(): String {

        if (completeConfig != null) {
            return completeConfig!!
        }

        val module = loginModule!!
        val flag = when (controlFlag) {
            LoginModuleControlFlag.REQUIRED -> "required"
            LoginModuleControlFlag.REQUISITE -> "requisite"
            LoginModuleControlFlag.SUFFICIENT -> "sufficient"
            LoginModuleControlFlag.OPTIONAL -> "optional"
            else -> error("Invalid login module control flag '$controlFlag' in JAAS config.")
        }
        val options = options.toList()
            .joinToString(" ") {
                    (key: String, value: String) ->
                "$key='$value'"
            }

        return "$module $flag $options;"
    }

    override fun setSaslJaasConfig(config: String) {
        if (config.isBlank()) {
            error("Property [${SaslConfigs.SASL_JAAS_CONFIG}] must not be blank.")
        }
        completeConfig = config
    }

    override fun loginModule(module: Class<out LoginModule>) {
        loginModule = module
    }

    override fun controlFlag(flag: LoginModuleControlFlag) {
        controlFlag = flag
    }

    override fun options(options: Map<String, String>) = options.forEach(::option)

    override fun option(key: String, value: String) {
        options[key] = value
    }
}
