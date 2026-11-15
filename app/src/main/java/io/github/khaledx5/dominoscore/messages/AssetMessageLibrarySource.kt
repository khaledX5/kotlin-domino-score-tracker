package io.github.khaledx5.dominoscore.messages

import android.content.Context
import android.util.Log
import io.github.khaledx5.dominoscore.domain.messages.MessageLibrary
import io.github.khaledx5.dominoscore.domain.messages.MessageLibrarySource
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Loads the message library shipped in assets, once, off the main thread. A missing or broken file
 * is logged and gives an empty library: the game keeps working, it just has no jokes.
 */
class AssetMessageLibrarySource(
    private val context: Context,
    private val fileName: String = FILE_NAME,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MessageLibrarySource {

    private val mutex = Mutex()
    private var cached: MessageLibrary? = null

    override suspend fun load(): MessageLibrary = mutex.withLock {
        cached ?: read().also { cached = it }
    }

    private suspend fun read(): MessageLibrary = withContext(ioDispatcher) {
        val content = try {
            context.assets.open(fileName).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (error: IOException) {
            Log.w(TAG, "Message library $fileName could not be read", error)
            return@withContext MessageLibrary.EMPTY
        }
        val parsed = MessageLibraryParser.parse(content)
        parsed.problems.forEach { Log.w(TAG, it) }
        parsed.library
    }

    companion object {
        const val FILE_NAME = "ranking_messages.json"
        private const val TAG = "MessageLibrary"
    }
}
