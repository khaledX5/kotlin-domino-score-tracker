package io.github.khaledx5.dominoscore.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.audio.MusicSettings
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import io.github.khaledx5.dominoscore.ui.theme.Spacing

/** One switch for every game event sound at once. The laugh, the winner tune and the volume stay in the music section. */
@Composable
fun GameSoundOptions(settings: MusicSettings, onEventSoundsChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(
            text = stringResource(R.string.settings_game_sounds),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SwitchRow(
            title = stringResource(R.string.settings_event_sounds),
            hint = stringResource(R.string.settings_event_sounds_hint),
            checked = settings.eventSoundsEnabled,
            onCheckedChange = onEventSoundsChange,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameSoundOptionsPreview() {
    DominoTheme {
        GameSoundOptions(settings = MusicSettings(), onEventSoundsChange = {})
    }
}
