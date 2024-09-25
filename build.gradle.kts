// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Apply the Android application plugin to the app module
    alias(libs.plugins.android.application) apply false
    // Apply the Kotlin Android plugin to the app module
    alias(libs.plugins.kotlin.android) apply false
}

// If you're using Kotlin DSL, you might want to declare the Kotlin version globally
// However, it's not necessary if you're using the Gradle Plugin for Kotlin
// You can specify the version of Kotlin here if required

// Add additional build configurations here if needed
