// Root-level build.gradle.kts
plugins {
    // Kotlin
    kotlin("jvm") version "1.9.23" apply false
    kotlin("android") version "1.9.23" apply false
    kotlin("plugin.serialization") version "1.9.23" apply false

    // Compose
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0" apply false

    // Android Gradle Plugin
    id("com.android.application") version "8.11.1" apply false
    id("com.android.library") version "8.11.1" apply false

    // ✅ Use working Hilt version
    id("com.google.dagger.hilt.android") version "2.56.2" apply false
}
