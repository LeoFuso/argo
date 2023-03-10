# CHANGELOG

`build.gradle`
```groovy
plugins {
    id 'io.github.leofuso.argo' version '0.1.1'   
}

```

`build.gradle.kts`
```kotlin
plugins {
    id("io.github.leofuso.argo") version "0.1.1"   
}
```

## Unreleased

### 0.1.2 ― ???


## Releases

### 0.1.1 ― incremental release
* BugFixes.
* SonarQube and JaCoCo setup.
* Publication setup.

### 0.1.0 ― Columba release
* Implementation of most of the current functionality present in [David's plugin](https://github.com/davidmc24/gradle-avro-plugin).
* Support for external sources, and external class dependencies for Tooling during Code Generation.
