package io.github.leofuso.argo.plugin.properties

import io.github.leofuso.argo.plugin.ArgoPlugin
import java.io.InputStream
import java.net.URL
import java.util.Properties

object GlobalProperties : Properties() {

    init {
        Properties()
        val classLoader = ArgoPlugin::class.java.classLoader
        val resource = classLoader.getResource("versions.properties") ?: error("Could not resolve 'versions.properties'.")
        load(resource.openSafeStream())
    }

    /**
     * Copy-paste from [Detekt](https://github.com/detekt/detekt).
     *
     * Due to [JDK-6947916](https://bugs.openjdk.java.net/browse/JDK-6947916) and
     * [JDK-8155607](https://bugs.openjdk.java.net/browse/JDK-8155607),
     * it is necessary to disallow caches to maintain stability on JDK 8 and 11 (and possibly more).
     * Otherwise, simultaneous invocations of Detekt in the same VM can fail spuriously. A similar bug is referenced in
     * [issue-3396](https://github.com/detekt/detekt/issues/3396). The performance regression is likely unnoticeable.
     * Due to [issue-4332](https://github.com/detekt/detekt/issues/4332) it is included for all JDKs.
     *
     **/
    private fun URL.openSafeStream(): InputStream {
        return openConnection()
            .apply { useCaches = false }
            .getInputStream()
    }
}
