plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
}

android {
    namespace = "com.entertainment.kurtineck.deignss"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.entertainment.kurtineck.deignss"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "1.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources=true
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.androidx.multidex)
    implementation(libs.kotlin.stdlib.jdk7)
    implementation(libs.androidx.appcompat.v161)
    implementation(libs.material.v190)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.espresso.core.v351)
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.gson)

    implementation(libs.androidx.cardview)
    // Admob Ads
    implementation(libs.play.services.ads)
    // Recycleview
    implementation(libs.androidx.recyclerview)
    // kapt("androidx.lifecycle:lifecycle-compiler:2.3.1")
    implementation(libs.kotlinx.coroutines.android) // Couroutines
    // Kotlin
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.material.v1110alpha01)

    implementation(libs.fresco)
    // animation library
    implementation(libs.animated.webp)
    implementation(libs.webpsupport)
    // PHOTO DRAWEE VIEW FOR ZOOM
    implementation(libs.photodraweeview)
    // HTML
    implementation(libs.html.dsl)

}