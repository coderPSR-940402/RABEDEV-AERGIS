import java.net.URI
import java.io.File
import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val generatedFontResDir = layout.buildDirectory.dir("generated/aergis-font-res")

android {
    namespace = "com.airgesture.control"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.airgesture.control"
        minSdk = 26
        targetSdk = 36
        versionCode = 63
        versionName = "0.18.38-preview"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets.getByName("main").res.srcDir(generatedFontResDir)

    // Debug builds use Android's ephemeral debug keystore. A preview/release
    // keystore must never be required from, or embedded in, source control.
    signingConfigs {
        create("release") {
            val keystorePath = providers.environmentVariable("RELEASE_KEYSTORE_PATH").orNull
            if (!keystorePath.isNullOrBlank()) {
                storeFile = rootProject.file(keystorePath)
                storePassword = providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        getByName("debug") {
            // Deliberately use the Android debug key instead of a repository key.
            isDebuggable = true
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Kotlin 2.4 removed the legacy kotlinOptions DSL. Keep the Kotlin compiler
// target aligned with Android's Java 17 compile target using the typed API.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

android.packaging {
    resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-service:2.9.1")
    implementation("androidx.core:core-ktx:1.19.0")

    val cameraX = "1.4.2"
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")

    implementation("com.google.mediapipe:tasks-vision:1.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

val gestureModel = layout.projectDirectory.file("src/main/assets/gesture_recognizer.task").asFile
val expectedGestureModelSha256 =
    "97952348cf6a6a4915c2ea1496b4b37ebabc50cbbf80571435643c455f2b0482"

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

val verifyGestureModel by tasks.registering {
    inputs.file(gestureModel)
    doLast {
        check(gestureModel.isFile) { "Missing gesture model: ${gestureModel.path}" }
        val actual = sha256(gestureModel)
        check(actual == expectedGestureModelSha256) {
            "gesture_recognizer.task SHA-256 mismatch: expected $expectedGestureModelSha256, got $actual"
        }
    }
}

tasks.named("preBuild") {
    dependsOn(verifyGestureModel)
}

val generatedFontZip = layout.buildDirectory.file("downloads/aergis-font.zip")
val generatedFontDir = layout.buildDirectory.dir("generated/aergis-font-res")

val downloadFont by tasks.registering {
    outputs.dir(generatedFontDir)
    doLast {
        val zipFile = generatedFontZip.get().asFile
        zipFile.parentFile.mkdirs()
        val uri = URI("https://github.com/googlefonts/noto-emoji/raw/main/fonts/NotoColorEmoji.ttf")
        uri.toURL().openStream().use { input ->
            zipFile.outputStream().use { output -> input.copyTo(output) }
        }
        generatedFontDir.get().asFile.mkdirs()
    }
}

// Keep the custom asset generation task isolated from configuration-cache state.
tasks.named("preBuild") {
    dependsOn(downloadFont)
}
