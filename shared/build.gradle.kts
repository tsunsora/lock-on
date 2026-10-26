import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

val roomVersion = "2.8.4"
val appVersion = providers.gradleProperty("lockon.version").get()

// Writes the app version into shared code so every screen and the sync hello can show it.
val generateBuildInfo by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/buildinfo/commonMain/kotlin")
    val version = appVersion
    inputs.property("version", version)
    outputs.dir(outDir)
    doLast {
        val file = outDir.get().file("app/lockon/BuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            listOf(
                "package app.lockon",
                "",
                "/** Generated from gradle.properties. */",
                "object BuildInfo {",
                "    const val VERSION = \"$version\"",
                "}",
                "",
            ).joinToString(System.lineSeparator()),
        )
    }
}
val ktorVersion = "3.3.3"
val composeVersion = "1.10.3"

kotlin {
    androidLibrary {
        namespace = "app.lockon.shared"
        compileSdk = 36
        minSdk = 26
        androidResources { enable = true }
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }
    jvm { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }

    compilerOptions {
        optIn.addAll("kotlin.uuid.ExperimentalUuidApi", "kotlin.time.ExperimentalTime")
    }

    sourceSets {
        commonMain { kotlin.srcDir(generateBuildInfo) }
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
            implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")
            implementation("org.jetbrains.compose.ui:ui:$composeVersion")
            implementation("org.jetbrains.compose.components:components-resources:$composeVersion")
            implementation("org.jetbrains.compose.material3:material3:1.9.0")
            implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            api("androidx.room:room-runtime:$roomVersion")
            implementation("androidx.sqlite:sqlite-bundled:2.6.2")
            api("io.ktor:ktor-client-core:$ktorVersion")
            implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
    }
}

dependencies {
    add("kspAndroid", "androidx.room:room-compiler:$roomVersion")
    add("kspJvm", "androidx.room:room-compiler:$roomVersion")
}

room { schemaDirectory("$projectDir/schemas") }

compose.resources {
    packageOfResClass = "app.lockon.resources"
    publicResClass = true
}
