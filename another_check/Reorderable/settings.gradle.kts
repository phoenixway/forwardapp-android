// Файл: /home/romankozak/studio/public/Reorderable/settings.gradle.kts

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // Встановлюємо стабільні, сумісні версії плагінів
    plugins {
        id("com.android.application") version "8.4.1" apply false
        id("com.android.library") version "8.4.1" apply false
        id("org.jetbrains.kotlin.multiplatform") version "2.0.0" apply false
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
        id("org.jetbrains.compose") version "1.6.10" apply false
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0" apply false
    }
}

// Застосовуємо плагін для авто-завантаження JDK
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}

rootProject.name = "reorderable-root"
include(":reorderable")
