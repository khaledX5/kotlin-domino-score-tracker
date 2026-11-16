package io.github.khaledx5.dominoscore.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection

/**
 * Scores read left to right in every language. In an Arabic (right-to-left) line a lone "-25"
 * would otherwise be laid out as "25-", and "+5" as "5+".
 */
fun TextStyle.forNumbers(): TextStyle = copy(textDirection = TextDirection.Ltr)
