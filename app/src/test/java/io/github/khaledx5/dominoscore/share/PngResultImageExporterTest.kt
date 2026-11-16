package io.github.khaledx5.dominoscore.share

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PngResultImageExporterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val exporter = PngResultImageExporter(context)

    private fun card(rtl: Boolean, vararg names: String) = ResultCard(
        brand = "DOMINO SCORE TRACKER",
        trophy = "🏆",
        winnerLine = "${names.first()} wins!",
        scoreLine = "160 pts",
        rows = names.mapIndexed { index, name ->
            ResultCardRow(
                mark = if (index == 0) "👑" else "#${index + 1}",
                name = name,
                score = (160 - index * 40).toString(),
                title = if (index == 0) {
                    CardTitle.KING
                } else if (index == names.lastIndex) {
                    CardTitle.KOOZ
                } else {
                    null
                },
                titleLabel = if (index == 0) {
                    "👑 KING"
                } else if (index == names.lastIndex) {
                    "🪣 KOOZ"
                } else {
                    null
                },
            )
        },
        footer = "Nov 16, 2026 · Target 150",
        rtl = rtl,
    )

    @Test
    fun writesA4By5PngIntoTheSharedCacheFolder() = runTest {
        val file = exporter.export(card(rtl = false, "Omar", "Mona", "Ali", "A very long name for a seat"))

        assertTrue(file.exists())
        assertEquals(PngResultImageExporter.SHARE_FOLDER, file.parentFile?.name)
        assertEquals(context.cacheDir, file.parentFile?.parentFile)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        assertEquals(ResultCardRenderer.WIDTH, bounds.outWidth)
        assertEquals(ResultCardRenderer.HEIGHT, bounds.outHeight)
    }

    @Test
    fun anArabicCardReusesTheSameFile() = runTest {
        val first = exporter.export(card(rtl = false, "Omar", "Mona"))
        val second = exporter.export(card(rtl = true, "عمر", "منى", "علي"))

        assertEquals(first, second)
        assertTrue(second.length() > 0)
    }
}
