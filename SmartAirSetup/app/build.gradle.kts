plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.smartairsetup"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.smartairsetup"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
}

kotlin {
    jvmToolchain(11)

}

dependencies {

    // --- AndroidX & UI stuff ---
    implementation(libs.core.ktx)
    implementation(libs.appcompat.v170)
    implementation(libs.material.v1120)
    implementation(libs.constraintlayout.v221)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("com.itextpdf:itextpdf:5.5.13.3")

    // --- Firebase ---
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)

    // --- Jetpack Compose ---
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.activity)
    implementation(libs.recyclerview)
    implementation(libs.firebase.messaging)

    // --- Tests (from origin/main + compose tests) ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    //Video with Rawad uses older version of mockito
    testImplementation("org.mockito:mockito-core:5.3.1")

    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}