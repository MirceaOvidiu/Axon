import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    id("com.google.gms.google-services")
}

// Read signing config from env vars (CI) or local keystore.properties (dev)
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(keystorePropsFile.inputStream())
}

// Check if signing config is available
val hasSigningConfig = System.getenv("KEYSTORE_PATH")?.isNotEmpty() == true ||
                       (keystoreProps["storeFile"] as String?)?.isNotEmpty() == true

android {
    namespace = "com.axon"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.axon"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                storeFile = file(
                    System.getenv("KEYSTORE_PATH")
                        ?: keystoreProps["storeFile"] as String
                )
                storePassword = System.getenv("STORE_PASSWORD")
                    ?: keystoreProps["storePassword"] as String
                keyAlias = System.getenv("KEY_ALIAS")
                    ?: keystoreProps["keyAlias"] as String
                keyPassword = System.getenv("KEY_PASSWORD")
                    ?: keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        freeCompilerArgs.add("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
}

@Suppress("ktlint:standard:final-newline")
dependencies {
    implementation(libs.material)
    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.play.services.wearable)
    implementation(libs.jetbrains.kotlinx.coroutines.play.services)
    implementation(libs.androidx.tiles.material)
    implementation(libs.androidx.protolayout.material)
    implementation(libs.androidx.room.common.jvm)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.material3)
    ksp(libs.room.compiler)

    implementation(libs.gson)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Firebase and GCP - using explicit versions from version catalog
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.play.services.auth)

    // Google Sign-In via Credential Manager
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Coroutines support for Firebase
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
