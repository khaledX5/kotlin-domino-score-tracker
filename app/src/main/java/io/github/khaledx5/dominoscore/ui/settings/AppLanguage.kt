package io.github.khaledx5.dominoscore.ui.settings

/** Languages the app ships. [SYSTEM] follows the phone language. */
enum class AppLanguage(val languageTag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    ARABIC("ar"),
    ;

    companion object {
        /** Maps stored locale tags such as "ar" or "en-GB" back to an option; anything else means the system language. */
        fun fromLanguageTags(tags: String): AppLanguage {
            val primary = tags.substringBefore(',').substringBefore('-')
            return entries.firstOrNull { it.languageTag == primary } ?: SYSTEM
        }
    }
}
