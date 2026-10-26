package com.creativeblisslabs.wearproductivity.wear.media

data class LrcLyricLine(
    val timestampMs: Long,
    val timeLabel: String,
    val text: String
)

object LrcLyricsParser {

    private val lrcRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")

    fun parseLrc(rawLyrics: String?): List<LrcLyricLine> {
        if (rawLyrics.isNullOrBlank()) return emptyList()

        val lines = mutableListOf<LrcLyricLine>()
        rawLyrics.lines().forEach { line ->
            val match = lrcRegex.find(line.trim())
            if (match != null) {
                val mins = match.groupValues[1].toLongOrNull() ?: 0L
                val secs = match.groupValues[2].toLongOrNull() ?: 0L
                val msPart = match.groupValues[3].toLongOrNull() ?: 0L
                val ms = (mins * 60 * 1000) + (secs * 1000) + (if (msPart < 100) msPart * 10 else msPart)
                val text = match.groupValues[4].trim()

                if (text.isNotBlank()) {
                    val timeLabel = String.format(java.util.Locale.ROOT, "%02d:%02d", mins, secs)
                    lines.add(LrcLyricLine(timestampMs = ms, timeLabel = timeLabel, text = text))
                }
            } else if (line.isNotBlank() && !line.startsWith("[")) {
                // Non-timestamped plain text fallback
                lines.add(LrcLyricLine(timestampMs = 0L, timeLabel = "", text = line.trim()))
            }
        }
        return lines.sortedBy { it.timestampMs }
    }
}
