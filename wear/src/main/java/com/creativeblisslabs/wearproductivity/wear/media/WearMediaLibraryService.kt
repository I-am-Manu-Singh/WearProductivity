package com.creativeblisslabs.wearproductivity.wear.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.creativeblisslabs.wearproductivity.shared.media.LocalMediaScanner

class WearMediaLibraryService : Service() {

    private val binder = LocalBinder()
    private var nativePlayer: MediaPlayer? = null
    private val playlistUris = mutableListOf<Uri>()
    private var currentIndex = 0

    var currentTrackId: String = ""
        private set
    var currentTrackUri: Uri? = null
        private set
    var currentTrackTitle: String = ""
        private set
    private var isPrepared: Boolean = false

    inner class LocalBinder : Binder() {
        fun getService(): WearMediaLibraryService = this@WearMediaLibraryService
    }

    companion object {
        const val CHANNEL_ID = "lifeos_wear_audio_bg_channel"
        const val NOTIFICATION_ID = 888
    }

    override fun onCreate() {
        super.onCreate()
        nativePlayer = MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setOnCompletionListener { playNext() }
        }

        runCatching {
            val localTracks = LocalMediaScanner.scanLocalAudio(this)
            playlistUris.clear()
            localTracks.forEach { track ->
                playlistUris.add(track.contentUri)
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification("LifeOS Audio Studio", "Ready for playback"))
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun buildNotification(title: String, subtitle: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    fun playTrack(
        uri: Uri,
        title: String,
        trackId: String = "",
        startPositionMs: Int = 0,
        autoStart: Boolean = true
    ) {
        runCatching {
            currentTrackUri = uri
            currentTrackTitle = title
            if (trackId.isNotBlank()) currentTrackId = trackId
            isPrepared = false

            val mp = nativePlayer ?: MediaPlayer().apply {
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                setOnCompletionListener { playNext() }
            }.also { nativePlayer = it }

            mp.reset()
            mp.setDataSource(applicationContext, uri)
            mp.prepare()
            isPrepared = true

            if (startPositionMs > 0) {
                mp.seekTo(startPositionMs.coerceAtLeast(0))
            }
            if (autoStart) {
                mp.start()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            }
            startForeground(
                NOTIFICATION_ID,
                buildNotification(if (autoStart) "Now Playing" else "Paused", title)
            )
        }
    }

    fun togglePlayPause(
        fallbackUri: Uri? = null,
        fallbackTitle: String = "Track",
        fallbackTrackId: String = "",
        fallbackPositionMs: Int = 0
    ): Boolean {
        val mp = nativePlayer ?: return false
        return runCatching {
            if (mp.isPlaying) {
                mp.pause()
                startForeground(NOTIFICATION_ID, buildNotification("Paused", currentTrackTitle.ifBlank { fallbackTitle }))
                false
            } else {
                if (!isPrepared && fallbackUri != null) {
                    playTrack(
                        uri = fallbackUri,
                        title = fallbackTitle,
                        trackId = fallbackTrackId,
                        startPositionMs = fallbackPositionMs,
                        autoStart = true
                    )
                    true
                } else {
                    if (fallbackPositionMs > 0 && mp.currentPosition < 500) {
                        mp.seekTo(fallbackPositionMs)
                    }
                    mp.start()
                    startForeground(NOTIFICATION_ID, buildNotification("Now Playing", currentTrackTitle.ifBlank { fallbackTitle }))
                    true
                }
            }
        }.getOrDefault(false)
    }

    fun seekTo(positionMs: Int) {
        runCatching {
            if (isPrepared) {
                nativePlayer?.seekTo(positionMs.coerceAtLeast(0))
            }
        }
    }

    fun isPlaying(): Boolean = runCatching { nativePlayer?.isPlaying == true }.getOrDefault(false)

    fun isTrackPrepared(): Boolean = isPrepared

    fun getCurrentPosition(): Int = runCatching {
        if (isPrepared) nativePlayer?.currentPosition ?: 0 else 0
    }.getOrDefault(0)

    fun getDuration(): Int = runCatching {
        if (isPrepared) nativePlayer?.duration ?: 0 else 0
    }.getOrDefault(0)

    fun getAudioSessionId(): Int = runCatching {
        nativePlayer?.audioSessionId ?: 0
    }.getOrDefault(0)

    fun pause() {
        runCatching {
            if (isPrepared && nativePlayer?.isPlaying == true) {
                nativePlayer?.pause()
            }
        }
    }

    fun resume() {
        runCatching {
            if (isPrepared && nativePlayer?.isPlaying == false) {
                nativePlayer?.start()
            }
        }
    }

    private fun playNext() {
        if (playlistUris.isNotEmpty()) {
            currentIndex = (currentIndex + 1) % playlistUris.size
            playTrack(playlistUris[currentIndex], "Track ${currentIndex + 1}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching {
            nativePlayer?.stop()
            nativePlayer?.release()
        }
        nativePlayer = null
        isPrepared = false
        stopForeground(true)
        super.onDestroy()
    }
}