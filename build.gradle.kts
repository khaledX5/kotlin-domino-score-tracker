plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless)
}

spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    val composeRules = libs.compose.rules.ktlint.get().toString()
    val ktlintRules = mapOf(
        "ktlint_code_style" to "android_studio",
        "ij_kotlin_allow_trailing_comma" to "true",
        "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
        "max_line_length" to "120",
        "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
    )

    kotlin {
        // Source folders only: walking build/ races with tasks that write there in parallel builds.
        target("*/src/**/*.kt")
        ktlint(ktlintVersion)
            .editorConfigOverride(ktlintRules + mapOf("compose_allowed_composition_locals" to "LocalGameColors"))
            .customRuleSets(listOf(composeRules))
    }
    kotlinGradle {
        target("*.gradle.kts", "*/*.gradle.kts")
        ktlint(ktlintVersion).editorConfigOverride(ktlintRules)
    }
    format("xml") {
        target("*/src/**/*.xml")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
