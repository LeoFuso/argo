@file:Suppress("UnstableApiUsage", "DSL_SCOPE_VIOLATION") //Fixe: https://github.com/gradle/gradle/issues/22797


plugins {
    base
    `jacoco-report-aggregation`
    alias(libs.plugins.sonarqube)
}

repositories {
    mavenCentral()
}

dependencies {
    jacocoAggregation(projects.argoPlugin)
    jacocoAggregation(projects.columbaCli)
}

reporting {
    reports {
        val testCodeCoverageReport by creating(JacocoCoverageReport::class) {
            testType.set(TestSuiteType.UNIT_TEST)
        }
    }
}


tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
    finalizedBy(tasks.sonar)
}

sonar {
    properties {
        property("sonar.projectKey", "io.github.leofuso.argo")
        property("sonar.organization", "leofuso")
        property("sonar.host.url", "https://sonarcloud.io")
    }
    if (System.getenv("SONAR_TOKEN") == null) {
        isSkipProject = true
    }
}
