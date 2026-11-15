plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.khaledx5.dominoscore.data"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

dependencies {
    implementation(project(":domain"))
}
