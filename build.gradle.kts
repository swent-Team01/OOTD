// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.10" apply false
    alias(libs.plugins.compose.compiler) apply false
    // Add the dependency for the Google services Gradle plugin
    id("com.google.gms.google-services") version "4.4.3" apply false
    id("org.sonarqube") version "5.1.0.4882" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
}

// Apply to all projects
subprojects {
    configurations.all {
        resolutionStrategy {
            force("org.apache.commons:commons-compress:1.26.0")
        }
    }
}