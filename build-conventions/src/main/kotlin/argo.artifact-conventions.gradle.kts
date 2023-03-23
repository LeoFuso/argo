@file:Suppress("UnstableApiUsage")

import java.util.Base64


plugins {
    id("argo.signing-conventions")
    `maven-publish`
}

publishing {
    repositories {

        val isSnapshot = project.version.toString().endsWith("SNAPSHOT")
        if (isSnapshot) {
            maven {
                name = "Sonatype"
                url = uri("https://oss.sonatype.org/content/repositories/snapshots")
                credentials(PasswordCredentials::class.java)
            }
        } else {
            maven {
                name = "Sonatype"
                url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
                credentials(PasswordCredentials::class.java)
            }
        }
    }

    publications.register<MavenPublication>("Sonatype") {
        artifactId = project.name
        groupId = project.group.toString()
        version = project.version.toString()
        from(components["java"])
        versionMapping {
            usage("java-api") {
                fromResolutionOf("runtimeClasspath")
            }
            usage("java-runtime") {
                fromResolutionResult()
            }
        }
        pom {
            description.set(extra["local.description"] as String)
            name.set("$groupId:${project.name}")
            url.set("https://github.com/leofuso/argo")
            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            issueManagement {
                system.set("GitHub Issue Tracking")
                url.set("https://github.com/leofuso/argo/issues")
            }
            developers {
                developer {
                    id.set("leofuso")
                    name.set("Leonardo Fuso Nuzzo")
                    email.set("leonardofusonuzzo@gmail.com")
                }
            }
            scm {
                url.set("https://github.com/LeoFuso/argo")
                connection.set("scm:git://github.com/leofuso/argo.git")
                developerConnection.set("scm:git:git@github.com:LeoFuso/argo.git")
            }
        }
    }
}

signing {
    sign(publishing.publications["Sonatype"])
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

