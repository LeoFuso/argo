# CHANGELOG

`build.gradle`
```groovy
plugins {
    id 'io.github.leofuso.argo' version '0.1.4'   
}

```

`build.gradle.kts`
```kotlin
plugins {
    id("io.github.leofuso.argo") version "0.1.4"   
}
```

## Unreleased

### 0.1.5 ― ???


## Releases

### 0.1.4 ― incremental release
* Signing fix.
* Deprecated behavior on '8.0.2' fix.

### 0.1.3 ― incremental release
* Publication fix.
* Security update.

### 0.1.2 ― Process isolation
* BugFixes.
* All task actions delegate to a separate runtime now, called Columba-CLI.
* SonarQube and JaCoCo setup for Columba-CLI
* Columba-CLI publication setup.

### 0.1.1 ― incremental release
* BugFixes.
* SonarQube and JaCoCo setup.
* Publication setup.

### 0.1.0 ― Columba release
* Implementation of most of the current functionality present in [David's plugin](https://github.com/davidmc24/gradle-avro-plugin).
* Support for external sources, and external class dependencies for Tooling during Code Generation.
