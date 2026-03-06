plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.compose")
}

android {
    namespace = "com.example.j2me.android"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.j2me.android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    
    // Configure C++ calculation (JNI) via CMake
    externalNativeBuild {
        cmake {
            path = file("../nativeMain/CMakeLists.txt")
            version = "3.22.1" // Match current NDK
        }
    }
    
    // Configure NDK ABI filters (Build only for common architectures to reduce time)
    defaultConfig {
        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86_64") // Needed for Emulator (AVD)
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.ui)
    implementation(compose.material)
    implementation(compose.runtime)
    implementation(compose.foundation)
    // activity-compose 1.7.2 is highly stable with Compose 1.5.x
    implementation("androidx.activity:activity-compose:1.7.2")
}
