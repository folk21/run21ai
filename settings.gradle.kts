pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "run21ai"
include(":core")
include(":test-support")
include(":dispatchers:llm")
include(":dispatchers:llm-ollama")
include(":tools:formula")
include(":runtime:local")
include(":runtime:rest-api")
