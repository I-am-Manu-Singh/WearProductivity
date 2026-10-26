package com.creativeblisslabs.wearproductivity.mobile.media

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.creativeblisslabs.wearproductivity.shared.media.LocalMediaScanner

class MobileMediaLibraryService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        val exoPlayer = ExoPlayer.Builder(this).build()
        player = exoPlayer

        mediaLibrarySession = MediaLibrarySession.Builder(this, exoPlayer, LibraryCallback()).build()

        // Load local scanned tracks into ExoPlayer
        val localTracks = LocalMediaScanner.scanLocalAudio(this)
        val mediaItems = localTracks.map { track ->
            MediaItem.fromUri(track.contentUri)
        }
        if (mediaItems.isNotEmpty()) {
            exoPlayer.setMediaItems(mediaItems)
            exoPlayer.prepare()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }

    private class LibraryCallback : MediaLibrarySession.Callback
}
