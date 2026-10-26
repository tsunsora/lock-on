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

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
}

compose.desktop {
    application {
        mainClass = "app.lockon.desktop.MainKt"
        // `./gradlew :desktopApp:run -Plockon.dataDir=...` runs a separate profile, e.g. for testing.
        providers.gradleProperty("lockon.dataDir").orNull?.let { jvmArgs("-Dlockon.dataDir=$it") }
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "Lock On"
            packageVersion = "1.0.0" // DMG and MSI need a major version above zero.
            vendor = "Lock On"
            description = "Gundam Card Game deck builder"
            modules("java.naming", "jdk.crypto.ec", "java.sql")
            windows {
                menuGroup = "Lock On"
                upgradeUuid = "6b1f3f0e-7f0b-4c55-9d38-3d8f3c2a9e41"
            }
        }
    }
}
