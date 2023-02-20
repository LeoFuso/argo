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
