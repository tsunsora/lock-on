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
