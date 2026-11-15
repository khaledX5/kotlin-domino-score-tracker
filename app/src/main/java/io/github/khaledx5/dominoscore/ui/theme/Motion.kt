package io.github.khaledx5.dominoscore.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Animation timings. Compose already scales every animation with the system animator setting;
 * decorative effects (shimmer, confetti, floating labels) are skipped when that setting is off.
 */
object Motion {
    /** Everyday feedback: score count-up, scale pop, sheets. */
    const val QUICK_MS = 250

    /** Ranking changes: KING / KOOZ badges, card highlights. */
    const val RANKING_MS = 350

    /** How long a floating +N / -N label stays on screen. */
    const val FLOATING_LABEL_MS = 900

    /** The shine that sweeps across a new KING's badge. */
    const val SHIMMER_MS = 600

    /** The winner celebration. */
    const val CELEBRATION_MS = 1_500

    /** Gap between cards appearing one after another. */
    const val STAGGER_MS = 120
}

/** True when the user switched animations off in the system settings (accessibility or developer options). */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
