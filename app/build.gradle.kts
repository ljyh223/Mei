@file:Suppress("UnstableApiUsage")
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.kapt)

    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)

    kotlin("plugin.serialization") version "2.0.0"

}




android {
    namespace = "com.ljyh.mei"
    compileSdk = 35


    defaultConfig {
        applicationId = "com.ljyh.mei"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.39"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
        jvmTarget = "21"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

configurations.all {
    exclude("com.soywiz.korlibs.krypto","krypto-android")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.okhttp)
    implementation(libs.annotations)
    implementation(libs.androidx.core.animation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    kapt(libs.hilt.compiler)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.core)
    implementation(libs.coil.network.okhttp)
    implementation(libs.converter.gson)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.retrofit)
    implementation(libs.retrofit.scalars)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.palette)

    implementation(libs.material.icons.extended)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.jaudiotagger)

    implementation(libs.androidx.foundation)

    implementation(libs.compose.shimmer)

    implementation(libs.material.kolor)
    implementation(libs.kmpalette.core)
    implementation(libs.kmpalette.extensions.network)
    implementation (libs.compose.colorful.sliders)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.serialization.android)
    implementation(libs.composeSettings.ui)
    implementation(libs.composeSettings.ui.extended)
    implementation(libs.korlibs.crypto){
        exclude("com.soywiz.korlibs.krypto", "krypto-android")
    }
    implementation(libs.logging.interceptor)
    implementation(kotlin("reflect"))

    implementation(libs.android.gpuimage)
    implementation(libs.reorderable)

    implementation(libs.lyrics.core)
    implementation(libs.lyrics.ui)
//    implementation(libs.composeIcons.evaIcons)


}
kotlin {
    sourceSets {
        getByName("main") {
            dependencies {
                implementation(kotlin("reflect"))
            }
        }
    }
}