plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "io.github.khaledx5.dominoscore.data"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    // MigrationTestHelper reads the exported schemas as test assets.
    sourceSets.getByName("test").assets.srcDir("$projectDir/schemas")
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

room {
    // Exported schemas are committed, so every future migration can be tested against the real v1.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":domain"))

    // The app module builds the database for dependency injection, so RoomDatabase is part of the API.
    api(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
