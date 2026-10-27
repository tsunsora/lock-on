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

    // The update feed installed apps read (lockon.updateFeed). Publish it with the two files.
    val dist = layout.projectDirectory.dir("dist").asFile
    val changelog = layout.projectDirectory.file("CHANGELOG.md").asFile
    doLast {
        fun entry(name: String): String {
            val file = File(dist, name)
            val sha = java.security.MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
            return """{ "file": "$name", "sha256": "$sha", "size": ${file.length()} }"""
        }
        // The first paragraph under this version's heading in the changelog, shown before updating.
        val notes = changelog.takeIf { it.exists() }?.readLines()
            ?.dropWhile { !it.startsWith("## $lockOnVersion") }?.drop(1)
            ?.dropWhile { it.isBlank() }?.takeWhile { it.isNotBlank() && !it.startsWith("#") }
            ?.joinToString(" ")?.trim()?.takeIf { it.isNotEmpty() }
        fun quoted(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        File(dist, "lock-on-update.json").writeText(
            listOfNotNull(
                "{",
                "  \"version\": ${quoted(lockOnVersion)},",
                notes?.let { "  \"notes\": ${quoted(it)}," },
                "  \"windows\": ${entry("LockOn-$lockOnVersion-windows.msi")},",
                "  \"android\": ${entry("LockOn-$lockOnVersion-android.apk")}",
                "}",
                "",
            ).joinToString("\n"),
        )
    }
}
