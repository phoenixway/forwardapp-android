plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services.plugin) apply false
    alias(libs.plugins.firebase.crashlytics.plugin) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}

// Kotlin 2.4 emits metadata that older transitive kotlin-metadata-jvm
// readers used by Hilt cannot read. Hilt 2.57+ keeps this dependency
// overridable, so keep the metadata reader aligned with the compiler.
val kotlinMetadataVersion =
    extensions
        .getByType<org.gradle.api.artifacts.VersionCatalogsExtension>()
        .named("libs")
        .findVersion("kotlin")
        .get()
        .requiredVersion

subprojects {
    configurations.configureEach {
        resolutionStrategy.force(
            "org.jetbrains.kotlin:kotlin-metadata-jvm:$kotlinMetadataVersion",
        )
    }
}

// AI CLI verification capability authority.
//
// Task identities are explicit authored facts. The Bridge treats task names
// as opaque identities and reads semantics only from aicliVerification.
fun Project.aicliVerificationTask(
    taskName: String,
    provides: List<String>,
    target: String,
) {
    tasks
        .matching { it.name == taskName }
        .configureEach {
            extensions.extraProperties.set(
                "aicliVerification",
                mapOf(
                    "provides" to provides,
                    "target" to target,
                    "cost" to "normal",
                    "confidence" to 1.0,
                ),
            )
        }
}

project(":app").aicliVerificationTask(
    taskName = "compileProdDebugKotlin",
    provides = listOf("compile"),
    target = "app",
)

project(":app").aicliVerificationTask(
    taskName = "testProdDebugUnitTest",
    provides = listOf("compile", "unit-test"),
    target = "app",
)

project(":sync").aicliVerificationTask(
    taskName = "compileDebugKotlin",
    provides = listOf("compile"),
    target = "sync",
)

project(":shared-core-data-models").aicliVerificationTask(
    taskName = "jvmTest",
    provides = listOf("compile", "unit-test"),
    target = "shared-core-data-models",
)

project(":shared-core-domain").aicliVerificationTask(
    taskName = "jvmTest",
    provides = listOf("compile", "unit-test"),
    target = "shared-core-domain",
)
