package io.github.khaledx5.dominoscore.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.audio.MusicSettings
import io.github.khaledx5.dominoscore.domain.sound.GameSoundEvent
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import io.github.khaledx5.dominoscore.ui.theme.Spacing

/**
 * The game event sounds: a master switch, one switch per event (greyed out while the master is
 * off, but remembered), and the visual reactions, which work on their own.
 */
@Composable
fun GameSoundOptions(
    settings: MusicSettings,
    onEventSoundsChange: (Boolean) -> Unit,
    onEventSoundChange: (GameSoundEvent, Boolean) -> Unit,
    onVisualReactionsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        Column(modifier = Modifier.padding(start = Spacing.l)) {
            GameSoundEvent.entries.forEach { event ->
                SwitchRow(
                    title = stringResource(event.label),
                    checked = event !in settings.mutedEvents,
                    enabled = settings.eventSoundsEnabled,
                    onCheckedChange = { onEventSoundChange(event, it) },
                )
            }
        }
        SwitchRow(
            title = stringResource(R.string.settings_visual_reactions),
            hint = stringResource(R.string.settings_visual_reactions_hint),
            checked = settings.visualReactionsEnabled,
            onCheckedChange = onVisualReactionsChange,
        )
    }
}

@get:StringRes
private val GameSoundEvent.label: Int
    get() = when (this) {
        GameSoundEvent.SECOND_QUALIFIED -> R.string.settings_event_second_qualified
        GameSoundEvent.FINAL_STRETCH -> R.string.settings_event_final_stretch
        GameSoundEvent.LAST_PLAYER_SCORES -> R.string.settings_event_last_player_scores
        GameSoundEvent.EQUAL_SCORES -> R.string.settings_event_equal_scores
        GameSoundEvent.LARGE_PENALTY -> R.string.settings_event_large_penalty
    }

@Preview(showBackground = true)
@Composable
private fun GameSoundOptionsPreview() {
    DominoTheme {
        GameSoundOptions(
            settings = MusicSettings(mutedEvents = setOf(GameSoundEvent.LARGE_PENALTY)),
            onEventSoundsChange = {},
            onEventSoundChange = { _, _ -> },
            onVisualReactionsChange = {},
        )
    }
}
