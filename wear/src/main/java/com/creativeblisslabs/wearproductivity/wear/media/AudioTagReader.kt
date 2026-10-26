package com.creativeblisslabs.wearproductivity.wear.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

data class AudioMetadataInfo(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val lyrics: String? = null,
    val artworkBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioMetadataInfo) return false
        if (title != other.title) return false
        if (artist != other.artist) return false
        if (album != other.album) return false
        if (durationMs != other.durationMs) return false
        if (lyrics != other.lyrics) return false
        if (artworkBytes != null) {
            if (other.artworkBytes == null) return false
            if (!artworkBytes.contentEquals(other.artworkBytes)) return false
        } else if (other.artworkBytes != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + (lyrics?.hashCode() ?: 0)
        result = 31 * result + (artworkBytes?.contentHashCode() ?: 0)
        return result
    }
}

object AudioTagReader {

    fun extractMetadata(context: Context, contentUri: Uri): AudioMetadataInfo {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, contentUri)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.ifBlank { null } ?: "Unknown Title"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.ifBlank { null } ?: "Unknown Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.ifBlank { null } ?: "Unknown Album"
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L

            val lyrics = retriever.extractMetadata(1000)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)

            val artworkBytes = retriever.embeddedPicture

            AudioMetadataInfo(
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                lyrics = lyrics,
                artworkBytes = artworkBytes
            )
        } catch (_: Exception) {
            AudioMetadataInfo(
                title = "Audio Track",
                artist = "Unknown Artist",
                album = "Local Album",
                durationMs = 0L,
                lyrics = null,
                artworkBytes = null
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }
}
