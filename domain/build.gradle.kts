plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}
