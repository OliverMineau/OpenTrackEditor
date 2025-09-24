import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.dokka") version "2.0.0"
    id("com.google.gms.google-services")
}
android {
    namespace = "com.minapps.trackeditor"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.minapps.trackeditor"
        minSdk = 24
        targetSdk = 36
        versionCode = 212
        versionName = "2.1.2"

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
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.location)
    implementation(libs.androidx.junit.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2")
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    kapt("androidx.room:room-compiler:2.7.2")
    implementation("com.google.dagger:hilt-android:2.57")
    kapt("com.google.dagger:hilt-compiler:2.57")
    implementation("androidx.hilt:hilt-navigation-fragment:1.0.0")
    kapt("androidx.hilt:hilt-compiler:1.0.0")

    implementation("com.google.firebase:firebase-database:22.0.0")
    implementation("com.google.firebase:firebase-analytics:23.0.0")

    // Unit test dependencies
    testImplementation(libs.junit)                // JUnit for unit tests
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)                // For mocking in unit tests

    // Instrumented (Android) tests
    androidTestImplementation(libs.androidx.junit.v115)        // androidx.test.ext:junit:1.1.5
    androidTestImplementation(libs.androidx.espresso.core.v351) // Espresso
    androidTestImplementation(libs.androidx.room.testing)       // Room in-memory DB
    androidTestImplementation(libs.kotlinx.coroutines.test)     // coroutines-test for suspend tests
    androidTestImplementation(libs.androidx.core)       // Needed for ApplicationProvider
}

tasks.withType<DokkaTask>().configureEach {
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
}