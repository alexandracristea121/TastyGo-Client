pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven(url = "https://jitpack.io") // Adaugă JitPack și aici, pentru pluginuri
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // Schimbă politica repository-urilor
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")  // Adaugă JitPack și aici
    }
}

rootProject.name = "Licenta_Food_Ordering"
include(":app")
