@file:Suppress("UnstableApiUsage")


plugins {
    id("io.github.gradle-nexus.publish-plugin")
}

nexusPublishing {

    packageGroup.set("io.github.leofuso")

    val isSnapshot = System.getProperty("global.version").endsWith("SNAPSHOT")
    useStaging.set(isSnapshot)

    repositories {
        create("Sonatype") {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}
