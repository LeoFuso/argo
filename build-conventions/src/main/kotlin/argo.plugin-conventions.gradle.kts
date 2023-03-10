@file:Suppress("UnstableApiUsage")

import java.util.Base64


plugins {
    id("argo.kotlin-conventions")
    `java-gradle-plugin`
    id("com.gradle.plugin-publish")
    signing
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    testImplementation(gradleTestKit())
}

tasks.withType<AbstractArchiveTask> {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

if (System.getenv("CI") != null) {
    signing {
        val signingKeyId: String? by project
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKeyId, base64Decode(signingKey), signingPassword)
    }
}

fun base64Decode(secret: String?) =
    secret?.let {
        String(Base64.getDecoder().decode(secret))
    }

