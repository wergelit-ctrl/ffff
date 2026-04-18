import org.gradle.api.initialization.resolve.RepositoriesMode
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.7.3"
        id("org.jetbrains.kotlin.android") version "2.1.10"
        id("org.jetbrains.kotlin.plugin.compose") version "2.1.10"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
        id("com.google.devtools.ksp") version "2.1.10-1.0.31"
        id("com.google.dagger.hilt.android") version "2.57.1"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "fintik"
