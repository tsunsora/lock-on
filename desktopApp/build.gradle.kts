import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val ktorVersion = "3.3.3"

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        optIn.addAll("kotlin.uuid.ExperimentalUuidApi", "kotlin.time.ExperimentalTime")
    }
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-network-tls-certificates:$ktorVersion")
    implementation("com.google.zxing:core:3.5.3")
    implementation("org.jmdns:jmdns:3.6.3")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    // Windows calls for the app's own title bar (WindowFrame.kt).
    implementation("net.java.dev.jna:jna-platform:5.9.0")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("io.coil-kt.coil3:coil-core:3.4.0")
}

tasks.test {
    // Tests run with assertions on, which also turns on coroutine debugging: every dispatch renames
    // the thread and every cancellation records a stack trace. The installed app runs without it,
    // so the scroll benchmark turns it off to time what people actually get.
    if (System.getenv("LOCKON_BENCH") == "1") systemProperty("kotlinx.coroutines.debug", "off")
}

compose.desktop {
    application {
        mainClass = "app.lockon.desktop.MainKt"
        // `./gradlew :desktopApp:run -Plockon.dataDir=...` runs a separate profile, e.g. for testing.
        providers.gradleProperty("lockon.dataDir").orNull?.let { jvmArgs("-Dlockon.dataDir=$it") }
        // `-Plockon.scrollProbe=<file>` times scrolling the card grid and closes the app (ScrollProbe.kt).
        providers.gradleProperty("lockon.scrollProbe").orNull?.let { jvmArgs("-Dlockon.scrollProbe=$it") }
        // `-Plockon.deckOpenProbe=<file>` times opening decks from the library and closes the app (DeckOpenProbe.kt).
        providers.gradleProperty("lockon.deckOpenProbe").orNull?.let { jvmArgs("-Dlockon.deckOpenProbe=$it") }
        providers.gradleProperty("lockon.deckOpenProbe.wait").orNull?.let { jvmArgs("-Dlockon.deckOpenProbe.wait=$it") }
        // `-Plockon.tabProbe=<file>` times the first tab switches and deck opens after boot (TabSwitchProbe.kt);
        // `-Plockon.skipBoot=true` starts without the boot screen, to compare.
        listOf("lockon.tabProbe", "lockon.tabProbe.label", "lockon.tabProbe.settle", "lockon.skipBoot").forEach { key ->
            providers.gradleProperty(key).orNull?.let { jvmArgs("-D$key=$it") }
        }
        // `-Plockon.dividerProbe=<file>` times dragging the deck editor's divider and closes the app (DividerProbe.kt).
        providers.gradleProperty("lockon.dividerProbe").orNull?.let { jvmArgs("-Dlockon.dividerProbe=$it") }
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "Lock On"
            packageVersion = providers.gradleProperty("lockon.version").get()
            vendor = "Lock On"
            description = "Gundam Card Game deck builder"
            // From `suggestRuntimeModules`, plus TLS, preferences and logging used at runtime.
            modules(
                "java.instrument", "java.management", "jdk.jfr", "jdk.unsupported",
                "java.naming", "jdk.crypto.ec", "java.sql", "java.prefs", "java.logging",
            )
            copyright = "Copyright 2026 tsunsora"
            windows {
                menuGroup = "Lock On"
                // Keep this UUID forever: it lets new installers upgrade old installs in place.
                upgradeUuid = "6b1f3f0e-7f0b-4c55-9d38-3d8f3c2a9e41"
                // Installs for the current user, so no administrator prompt is needed.
                perUserInstall = true
                dirChooser = true
                shortcut = true
                menu = true
                iconFile = project.file("icons/lock-on.ico")
            }
            macOS {
                bundleID = "app.lockon"
                iconFile = project.file("icons/lock-on.icns")
            }
            linux {
                packageName = "lock-on"
                iconFile = project.file("icons/lock-on.png")
            }
        }
    }
}
