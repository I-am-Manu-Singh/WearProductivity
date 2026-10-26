package com.creativeblisslabs.wearproductivity.shared

object FileTypes {
    val AUDIO = listOf(".mp3", ".wav", ".m4a", ".aac", ".flac", ".ogg", ".wma", ".mid", ".midi", ".amr", ".opus")
    val IMAGE = listOf(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg")
    val VIDEO = listOf(".mp4", ".mkv", ".webm", ".avi", ".3gp", ".mov")

    fun isMedia(fileName: String): Boolean {
        val name = fileName.lowercase()
        return AUDIO.any { name.endsWith(it) } || 
               IMAGE.any { name.endsWith(it) } || 
               VIDEO.any { name.endsWith(it) }
    }
}
