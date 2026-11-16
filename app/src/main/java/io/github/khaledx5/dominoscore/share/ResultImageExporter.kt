package io.github.khaledx5.dominoscore.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.khaledx5.dominoscore.R
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Turns a [ResultCard] into an image file that can be handed to the share sheet. */
fun interface ResultImageExporter {
    suspend fun export(card: ResultCard): File
}

/**
 * Renders the card and writes it as PNG into the app cache, under the folder the FileProvider
 * exposes. One file is reused, so shared images never pile up; nothing goes to shared storage,
 * so no storage permission is needed.
 */
class PngResultImageExporter @Inject constructor(@ApplicationContext private val context: Context) :
    ResultImageExporter {

    override suspend fun export(card: ResultCard): File = withContext(Dispatchers.IO) {
        val bitmap = renderer(card).render(card)
        val folder = File(context.cacheDir, SHARE_FOLDER).apply { mkdirs() }
        val file = File(folder, FILE_NAME)
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
        } finally {
            bitmap.recycle()
        }
        file
    }

    /** Cairo for Arabic, Inter otherwise, the same fonts as the screens. */
    private fun renderer(card: ResultCard): ResultCardRenderer {
        val base = ResourcesCompat.getFont(context, if (card.rtl) R.font.cairo else R.font.inter) ?: Typeface.DEFAULT
        return ResultCardRenderer(regular = base, bold = ResultCardRenderer.boldOf(base))
    }

    companion object {
        /** Must match the cache-path in res/xml/shared_files.xml. */
        const val SHARE_FOLDER = "shared"
        private const val FILE_NAME = "domino-result.png"
        private const val PNG_QUALITY = 100
    }
}
