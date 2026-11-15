plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.khaledx5.dominoscore"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.github.khaledx5.dominoscore"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
}
