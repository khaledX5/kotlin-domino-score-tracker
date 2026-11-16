package io.github.khaledx5.dominoscore

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.khaledx5.dominoscore.ui.startup.StartupUiState
import io.github.khaledx5.dominoscore.ui.startup.StartupViewModel
import io.github.khaledx5.dominoscore.ui.theme.DominoTheme
import io.github.khaledx5.dominoscore.ui.theme.Motion

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val startupViewModel: StartupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { startupViewModel.uiState.value == StartupUiState.SystemSplash }
        splashScreen.setOnExitAnimationListener { splash ->
            splash.view.animate()
                .alpha(0f)
                .setDuration(Motion.QUICK_MS.toLong())
                .withEndAction(splash::remove)
                .start()
        }
        enableEdgeToEdge()
        setContent {
            DominoTheme {
                val startupState by startupViewModel.uiState.collectAsStateWithLifecycle()
                DominoApp(startupState = startupState)
            }
        }
    }
}
