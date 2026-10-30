package pl.stapik.media.data.backup

import android.content.ContentResolver
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ContentFiles {
    const val MAX_BYTES = 64 * 1024 * 1024
    private const val BUFFER_BYTES = 8 * 1024

    suspend fun read(resolver: ContentResolver, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val stream = resolver.openInputStream(uri) ?: throw IOException("Cannot open the file")
        stream.use { input ->
            val collected = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (collected.size() + count > MAX_BYTES) throw BackupException(BackupFailure.NOT_A_BACKUP)
                collected.write(buffer, 0, count)
            }
            collected.toByteArray()
        }
    }

    suspend fun write(resolver: ContentResolver, uri: Uri, bytes: ByteArray) = withContext(Dispatchers.IO) {
        val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open the file")
        stream.use { it.write(bytes) }
    }
}
