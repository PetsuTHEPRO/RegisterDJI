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
        gradlePluginPortal()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox" // Este nome de usuário é fixo para Mapbox
                password = providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").orNull
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven { url = uri("https://dev.dji.com/repo/") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://api.mapbox.com/downloads/v2/releases/maven") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "RegisterApp"
include(":app")
