package io.github.khaledx5.dominoscore.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** The FileProvider authority declared in the manifest. */
fun fileProviderAuthority(context: Context): String = "${context.packageName}.files"

/**
 * A share sheet for the image in [file]. The receiving app gets read access to that one file only;
 * the clip data lets the sheet show the image preview too.
 */
fun shareImageIntent(context: Context, file: File, chooserTitle: String): Intent {
    val uri = FileProvider.getUriForFile(context, fileProviderAuthority(context), file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(send, chooserTitle)
}
