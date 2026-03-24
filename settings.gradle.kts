pluginManagement {
    repositories {
        // Standard plugin sources
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Ensures only these repos are used (no random project repos)
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // These two contain all dependencies we need, including Gemini 0.7.0
        google()
        mavenCentral()
    }
}

// The name of your Gradle project and app module
rootProject.name = "PantryChef"
include(":app")
