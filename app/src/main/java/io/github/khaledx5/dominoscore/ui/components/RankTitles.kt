package io.github.khaledx5.dominoscore.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import io.github.khaledx5.dominoscore.ui.theme.DominoThemeExtras
import io.github.khaledx5.dominoscore.ui.theme.Spacing

/** 👑 KING: the player (or players) on the highest score. */
@Composable
fun KingBadge(modifier: Modifier = Modifier) {
    val gameColors = DominoThemeExtras.gameColors
    TitlePill(
        text = stringResource(R.string.rank_king_badge),
        container = gameColors.king,
        modifier = modifier,
    )
}

/**
 * 🪣 KOOZ: the player (or players) on the lowest score. The jokes that go with it come from the
 * message library and are shown by PlayerMessageLine, not here.
 */
@Composable
fun KoozBadge(modifier: Modifier = Modifier) {
    val gameColors = DominoThemeExtras.gameColors
    TitlePill(text = stringResource(R.string.rank_kooz_badge), container = gameColors.kooz, modifier = modifier)
}

@Composable
private fun TitlePill(text: String, container: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = container,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Text(
            text = text,
            // The title reads "👑 KING" in both languages: an RTL layout would otherwise move the emoji after it.
            style = MaterialTheme.typography.labelMedium.copy(textDirection = TextDirection.Ltr),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs / 2),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RankTitlesPreview() {
    DominoTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            KingBadge()
            KoozBadge()
        }
    }
}
