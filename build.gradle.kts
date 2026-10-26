plugins {
    kotlin("multiplatform") version "2.3.21" apply false
    kotlin("android") version "2.3.21" apply false
    kotlin("jvm") version "2.3.21" apply false
    kotlin("plugin.serialization") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("org.jetbrains.compose") version "1.10.3" apply false
    id("com.android.application") version "8.13.2" apply false
    id("com.android.kotlin.multiplatform.library") version "8.13.2" apply false
    id("com.google.devtools.ksp") version "2.3.7" apply false
    id("androidx.room") version "2.8.4" apply false
}

val lockOnVersion = providers.gradleProperty("lockon.version").get()

// `./gradlew releaseArtifacts` builds the Windows installer and the release APK into dist/.
tasks.register<Copy>("releaseArtifacts") {
    group = "distribution"
    description = "Builds the Windows MSI and the release APK and copies them to dist/."
    // packageMsi skips ProGuard, which needs keep rules for Netty, Ktor and Room that are not written yet.
    dependsOn(":desktopApp:packageMsi", ":androidApp:assembleRelease")
    from(project(":desktopApp").layout.buildDirectory.dir("compose/binaries/main/msi")) {
        include("*.msi")
        rename { "LockOn-$lockOnVersion-windows.msi" }
    }
    from(project(":androidApp").layout.buildDirectory.dir("outputs/apk/release")) {
        include("*.apk")
        rename { "LockOn-$lockOnVersion-android.apk" }
    }
    into(layout.projectDirectory.dir("dist"))
}
