package io.github.khaledx5.dominoscore.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.khaledx5.dominoscore.R
import io.github.khaledx5.dominoscore.ui.components.ChoiceTile
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import io.github.khaledx5.dominoscore.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            LanguageOptions(selected = language, onSelect = viewModel::selectLanguage)
        }
    }
}

@Composable
fun LanguageOptions(selected: AppLanguage, onSelect: (AppLanguage) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(
            text = stringResource(R.string.settings_language),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Same height for all tiles: Arabic script is taller than Latin.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            AppLanguage.entries.forEach { language ->
                ChoiceTile(
                    selected = language == selected,
                    onClick = { onSelect(language) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    LanguageLabel(language)
                }
            }
        }
    }
}

@Composable
private fun LanguageLabel(language: AppLanguage, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        when (language) {
            AppLanguage.SYSTEM -> Icon(
                painter = painterResource(R.drawable.ic_language),
                contentDescription = null,
                tint = LocalContentColor.current,
                modifier = Modifier.size(24.dp),
            )

            AppLanguage.ENGLISH -> Text(
                stringResource(R.string.language_code_english),
                style = MaterialTheme.typography.titleMedium,
            )

            AppLanguage.ARABIC -> Text(
                stringResource(R.string.language_code_arabic),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text(text = languageName(language), style = MaterialTheme.typography.labelMedium)
    }
}

/** Language names are written in their own language, so people can find theirs whatever the current one is. */
@Composable
private fun languageName(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
    AppLanguage.ENGLISH -> stringResource(R.string.language_name_english)
    AppLanguage.ARABIC -> stringResource(R.string.language_name_arabic)
}

@Preview(showBackground = true)
@Composable
private fun LanguageOptionsPreview() {
    DominoTheme { LanguageOptions(selected = AppLanguage.SYSTEM, onSelect = {}) }
}
