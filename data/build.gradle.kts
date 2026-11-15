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
    lint {
        warningsAsErrors = true
        abortOnError = true
        // Version-nag checks fail the build whenever a new release ships; updates are handled on purpose.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

dependencies {
    implementation(project(":domain"))
}
