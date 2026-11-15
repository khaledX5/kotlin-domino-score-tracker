package io.github.khaledx5.dominoscore

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import io.github.khaledx5.dominoscore.audio.MusicController
import javax.inject.Inject

@HiltAndroidApp
class DominoApplication : Application() {

    /** Lazy: nothing about music is created until the app actually goes to the background. */
    @Inject
    lateinit var musicController: Lazy<MusicController>

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) musicController.get().onAppBackground()
            },
        )
    }
}
