import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing comes from keystore.properties (git-ignored) or LOCKON_* environment variables.
// Without either, release builds fall back to the debug key so they still install for testing.
val signingProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun signingValue(key: String, env: String): String? = signingProps.getProperty(key) ?: System.getenv(env)
val releaseStore = signingValue("storeFile", "LOCKON_KEYSTORE_PATH")

android {
    namespace = "app.lockon.android"
    compileSdk = 36
    defaultConfig {
        applicationId = "app.lockon"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("lockon.versionCode").get().toInt()
        versionName = providers.gradleProperty("lockon.version").get()
    }
    signingConfigs {
        if (releaseStore != null) create("release") {
            storeFile = rootProject.file(releaseStore)
            storePassword = signingValue("storePassword", "LOCKON_KEYSTORE_PASSWORD")
            keyAlias = signingValue("keyAlias", "LOCKON_KEY_ALIAS")
            keyPassword = signingValue("keyPassword", "LOCKON_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            // R8 stays off until release builds have been checked on a device; Room, Ktor and
            // serialization all need keep rules that can only be confirmed by running the app.
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug").also {
                logger.warn("Lock On: no release key configured, signing the release APK with the debug key. See RELEASING.md.")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    packaging { resources.excludes += setOf("META-INF/INDEX.LIST", "META-INF/io.netty.versions.properties") }
}

// Releases up to 26.11.15 were signed with the debug key. Android 9 and later accept the release key
// over it because the lineage file proves the debug key handed over; Android 8 has no such thing, so
// it keeps seeing the debug key. apksigner writes both signatures; the Android plugin can't.
val previousStore = signingValue("previousStoreFile", "LOCKON_PREVIOUS_KEYSTORE_PATH")
val lineageFile = signingValue("lineageFile", "LOCKON_LINEAGE_PATH")
val publishableApk = layout.buildDirectory.file("outputs/apk/publish/androidApp-release.apk")
tasks.register<Exec>("publishableApk") {
    group = "distribution"
    description = "Signs the release APK so that phones with an earlier release can update to it."
    dependsOn("assembleRelease")
    val builtApk = layout.buildDirectory.file("outputs/apk/release/androidApp-release.apk")
    inputs.file(builtApk)
    outputs.file(publishableApk)
    doFirst {
        if (releaseStore == null || previousStore == null || lineageFile == null) throw GradleException(
            "Publishing needs the release key, the previous key and the lineage file. See Android signing in RELEASING.md.",
        )
        publishableApk.get().asFile.parentFile.mkdirs()
    }
    // Passwords go through the environment so they never appear in a command line.
    environment("LOCKON_SIGN_PASS", signingValue("storePassword", "LOCKON_KEYSTORE_PASSWORD").orEmpty())
    environment("LOCKON_SIGN_KEY_PASS", signingValue("keyPassword", "LOCKON_KEY_PASSWORD").orEmpty())
    environment("LOCKON_PREVIOUS_PASS", signingValue("previousStorePassword", "LOCKON_PREVIOUS_KEYSTORE_PASSWORD").orEmpty())
    environment("LOCKON_PREVIOUS_KEY_PASS", signingValue("previousKeyPassword", "LOCKON_PREVIOUS_KEY_PASSWORD").orEmpty())
    commandLine(
        File(System.getProperty("java.home"), "bin/java").path,
        "-jar", android.sdkDirectory.resolve("build-tools/${android.buildToolsVersion}/lib/apksigner.jar").path,
        "sign",
        "--ks", previousStore?.let { rootProject.file(it).path }.orEmpty(),
        "--ks-key-alias", signingValue("previousKeyAlias", "LOCKON_PREVIOUS_KEY_ALIAS").orEmpty(),
        "--ks-pass", "env:LOCKON_PREVIOUS_PASS", "--key-pass", "env:LOCKON_PREVIOUS_KEY_PASS",
        "--next-signer",
        "--ks", releaseStore?.let { rootProject.file(it).path }.orEmpty(),
        "--ks-key-alias", signingValue("keyAlias", "LOCKON_KEY_ALIAS").orEmpty(),
        "--ks-pass", "env:LOCKON_SIGN_PASS", "--key-pass", "env:LOCKON_SIGN_KEY_PASS",
        "--lineage", lineageFile?.let { rootProject.file(it).path }.orEmpty(),
        "--rotation-min-sdk-version", "28",
        "--out", publishableApk.get().asFile.path,
        builtApk.get().asFile.path,
    )
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        optIn.addAll("kotlin.uuid.ExperimentalUuidApi", "kotlin.time.ExperimentalTime")
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    implementation("io.ktor:ktor-client-okhttp:3.3.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3")
}
