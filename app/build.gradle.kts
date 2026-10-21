@file:Suppress("UnstableApiUsage")
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    kotlin("plugin.serialization") version "2.3.20"
}




android {
    namespace = "com.ljyh.mei"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.ljyh.mei"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.54"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    buildFeatures {
        buildConfig = true
        compose = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-Xopt-in=androidx.media3.common.util.UnstableApi",
                "-Xopt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-Xopt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                // 添加其他你需要的 opt-in
            )
        )
    }
}

dependencies {
    implementation(libs.quickjs)

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
    implementation(libs.media3.hls)
    implementation(libs.media3.transformer)
    implementation(libs.play.services.location)
    implementation(libs.annotations)
    implementation(libs.androidx.core.animation)
    implementation(libs.androidx.compose.material3.window.size.class1)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.ktor.client.mock)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    ksp(libs.hilt.compiler)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.core)
    implementation(libs.coil.gif)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kotlinx.coroutines.guava)
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
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(project(":UnblockNeteaseMusic"))

    implementation("com.google.crypto.tink:tink:1.7.0") {
        // Only the standalone X25519 primitive is used, not Tink's JSON keyset APIs.
        exclude(group = "com.google.code.gson", module = "gson")
    }
    implementation(libs.logging.interceptor)
    ksp("org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.20")


    // 列表拖拽
    implementation(libs.reorderable)

    // 歌词组件
    implementation(libs.lyrics.core)
    implementation(libs.lyrics.ui)
    implementation(libs.zoomable)
    implementation(libs.timber)
    implementation(libs.compose.cloudy)

    implementation(libs.haze)
    implementation(libs.haze.glass)
    implementation(libs.zxing.core)


}

// Local tests load the desktop native library; devices keep the Android AAR.
configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("io.github.dokar3:quickjs-kt-android"))
            .using(module("io.github.dokar3:quickjs-kt-jvm:${libs.versions.quickjs.get()}"))
    }
}

// Cargo sources are outside AGP's C/C++ source discovery. Changes must invalidate each ABI build.
val usbRustInputs = fileTree("src/main/rust/mei-usb-core") {
    include("src/**/*.rs", "include/**/*.h", "cmake/**/*.cmake", "Cargo.toml", "Cargo.lock")
}
tasks.configureEach {
    if (name.startsWith("buildCMake")) inputs.files(usbRustInputs).withPropertyName("usbRustCore")
}
