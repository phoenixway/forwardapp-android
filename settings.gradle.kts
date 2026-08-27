// Файл: /settings.gradle.kts (у корені проєкту)

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()

        ivy {
            name = "Node.js distributions"
            url = uri("https://nodejs.org/dist/")
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("org.nodejs", "node")
            }
        }

        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ForwardAppMobile"
include(":app")
include(":desktop-data")
include(":shared-application")
include(":shared-contracts")
include(":shared-domain")
include(":shared-core-data-models")
include(":shared-core-domain")
include(":core-data-models")
include(":core-data-interfaces")
include(":sync")
