plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin) {
        exclude(group="org.jetbrains.kotlin", module="kotlin-gradle-plugin-api")
    }
    implementation(libs.publish)
    implementation(libs.detekt)
}
