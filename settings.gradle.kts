pluginManagement {
    repositories {
        google()
        mavenCentral() // Цей рядок дозволяє знаходити плагіни
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral() // А цей рядок дозволяє знаходити бібліотеки, як-от reorderable
    }
}

rootProject.name = "ForwardAppMobile2"
include(":app")