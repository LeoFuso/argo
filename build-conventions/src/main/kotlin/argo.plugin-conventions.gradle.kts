@file:Suppress("UnstableApiUsage")


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
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
}


