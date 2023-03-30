import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider

object Versions {

    inline fun Configuration.replace(replacement: Provider<MinimalExternalModuleDependency>, crossinline cause: () -> String? = { null }) {
        val dependency = replacement.get()
        resolutionStrategy.eachDependency {
            if(requested.group == dependency.group && requested.name == dependency.name) {
                useVersion(dependency.version!!)
                val because = cause.invoke()
                if (because != null) {
                    because(because)
                }
            }
        }
    }
}
