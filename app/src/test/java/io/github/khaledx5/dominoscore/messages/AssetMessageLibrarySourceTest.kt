package io.github.khaledx5.dominoscore.messages

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AssetMessageLibrarySourceTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun theLibraryIsPackagedInTheAppAssets() = runTest {
        val source = AssetMessageLibrarySource(context, ioDispatcher = StandardTestDispatcher(testScheduler))

        assertEquals(111, source.load().size)
    }

    @Test
    fun aMissingFileMeansAnEmptyLibraryNotACrash() = runTest {
        val source = AssetMessageLibrarySource(
            context,
            fileName = "missing.json",
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        assertEquals(0, source.load().size)
    }

    @Test
    fun theFileIsReadOnce() = runTest {
        val source = AssetMessageLibrarySource(context, ioDispatcher = StandardTestDispatcher(testScheduler))

        assertSame(source.load(), source.load())
    }
}
