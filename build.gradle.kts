plugins {
    id("com.android.application") apply false
    id("com.android.library")     apply false
    id("org.jetbrains.kotlin.android") apply false
    id("org.jetbrains.kotlin.multiplatform") apply false
    id("org.jetbrains.kotlin.plugin.compose") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    id("com.google.devtools.ksp") apply false
    id("app.cash.sqldelight") apply false
    // ❗ parcelize не оголошуємо тут
}
