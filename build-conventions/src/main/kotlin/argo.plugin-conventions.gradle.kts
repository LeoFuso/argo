@file:Suppress("UnstableApiUsage")



plugins {
    id("argo.kotlin-conventions")
    id("argo.signing-conventions")
    `java-gradle-plugin`
    id("com.gradle.plugin-publish")
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
