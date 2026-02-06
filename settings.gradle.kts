pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Убедитесь, что нет ссылок на старые репозитории с support библиотеками
    }
}

rootProject.name = "WeightTracker"
include(":app")