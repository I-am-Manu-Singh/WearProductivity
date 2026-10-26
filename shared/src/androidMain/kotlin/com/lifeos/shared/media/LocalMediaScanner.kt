package com.creativeblisslabs.wearproductivity.shared.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class DiscoveredTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val folderPath: String = "Music"
)

data class DiscoveredArtist(
    val name: String,
    val trackCount: Int
)

data class DiscoveredAlbum(
    val title: String,
    val artist: String,
    val trackCount: Int
)

data class DiscoveredFolder(
    val folderName: String,
    val fullPath: String,
    val trackCount: Int
)

object LocalMediaScanner {
    fun scanLocalAudio(context: Context): List<DiscoveredTrack> {
        val tracks = mutableListOf<DiscoveredTrack>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Title"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val durationMs = cursor.getLong(durationColumn)
                val fullPath = if (dataColumn >= 0) cursor.getString(dataColumn) ?: "" else ""
                val folderPath = if (fullPath.contains("/")) fullPath.substringBeforeLast("/") else "Music"

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )

                tracks.add(
                    DiscoveredTrack(
                        id = id.toString(),
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = durationMs,
                        contentUri = contentUri,
                        folderPath = folderPath
                    )
                )
            }
        }
        return tracks
    }

    fun scanArtists(context: Context): List<DiscoveredArtist> {
        val tracks = scanLocalAudio(context)
        return tracks.groupBy { it.artist }.map { (artist, list) ->
            DiscoveredArtist(name = artist, trackCount = list.size)
        }.sortedBy { it.name }
    }

    fun scanAlbums(context: Context): List<DiscoveredAlbum> {
        val tracks = scanLocalAudio(context)
        return tracks.groupBy { it.album to it.artist }.map { (_, list) ->
            DiscoveredAlbum(title = list.first().album, artist = list.first().artist, trackCount = list.size)
        }.sortedBy { it.title }
    }

    fun scanFolders(context: Context): List<DiscoveredFolder> {
        val tracks = scanLocalAudio(context)
        return tracks.groupBy { it.folderPath }.map { (path, list) ->
            val name = if (path.contains("/")) path.substringAfterLast("/") else path
            DiscoveredFolder(folderName = name.ifBlank { "Music" }, fullPath = path, trackCount = list.size)
        }.sortedBy { it.folderName }
    }
}
