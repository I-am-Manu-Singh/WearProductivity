package com.creativeblisslabs.wearproductivity.wear.complications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.creativeblisslabs.wearproductivity.wear.LifeOSWearApplication
import com.creativeblisslabs.wearproductivity.wear.WearMainActivity
import kotlin.math.roundToInt

/**
 * Universal Complication Data Source for Pixel Watch & Galaxy Watch watch faces.
 * Supports SHORT_TEXT and RANGED_VALUE complication types.
 */
class TodayComplicationService : SuspendingComplicationDataSourceService() {

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val app = applicationContext as? LifeOSWearApplication
        val repo = app?.repository
        val state = repo?.current()

        val activeTasks = state?.tasks?.filter { it.stamp.deletedAt == null && !it.complete } ?: emptyList()
        val totalTasks = state?.tasks?.filter { it.stamp.deletedAt == null } ?: emptyList()
        val completedTasks = totalTasks.count { it.complete }

        val fastActive = state?.fast?.active == true
        val focusActive = state?.focus?.active == true

        val displayTitle: String
        val displayText: String
        val progressVal: Float
        val maxVal: Float

        when {
            focusActive -> {
                val focus = state?.focus
                val totalSec = (focus?.totalSeconds ?: 1500).coerceAtLeast(1)
                val remainingSec = (focus?.remainingSeconds ?: 0).coerceAtLeast(0)
                val elapsedSec = (totalSec - remainingSec).coerceAtLeast(0)
                val remainingMins = remainingSec / 60
                displayTitle = "FOCUS"
                displayText = "${remainingMins}m"
                progressVal = elapsedSec.toFloat()
                maxVal = totalSec.toFloat()
            }
            fastActive -> {
                val fast = state?.fast
                val now = System.currentTimeMillis()
                val elapsedSec = fast?.elapsedSeconds(now) ?: 0L
                val targetSec = fast?.targetSeconds ?: 57600L
                val elapsedHours = elapsedSec / 3600f
                val targetHours = (targetSec / 3600f).coerceAtLeast(1f)
                displayTitle = "FAST"
                displayText = "${elapsedHours.toInt()}h"
                progressVal = elapsedHours
                maxVal = targetHours
            }
            else -> {
                displayTitle = "TASKS"
                displayText = "${activeTasks.size} left"
                progressVal = completedTasks.toFloat()
                maxVal = totalTasks.size.coerceAtLeast(1).toFloat()
            }
        }

        val launchIntent = PendingIntent.getActivity(
            this as Context,
            0,
            Intent(this as Context, WearMainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(displayText).build(),
                    contentDescription = PlainComplicationText.Builder("LifeOS $displayTitle").build()
                )
                    .setTitle(PlainComplicationText.Builder(displayTitle).build())
                    .setTapAction(launchIntent)
                    .build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = progressVal.coerceIn(0f, maxVal),
                    min = 0f,
                    max = maxVal,
                    contentDescription = PlainComplicationText.Builder("LifeOS $displayTitle Progress").build()
                )
                    .setText(PlainComplicationText.Builder(displayText).build())
                    .setTitle(PlainComplicationText.Builder(displayTitle).build())
                    .setTapAction(launchIntent)
                    .build()
            }
            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("3 left").build(),
                    contentDescription = PlainComplicationText.Builder("LifeOS Tasks Preview").build()
                )
                    .setTitle(PlainComplicationText.Builder("TASKS").build())
                    .build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = 3f,
                    min = 0f,
                    max = 5f,
                    contentDescription = PlainComplicationText.Builder("LifeOS Tasks Preview").build()
                )
                    .setText(PlainComplicationText.Builder("3 left").build())
                    .setTitle(PlainComplicationText.Builder("TASKS").build())
                    .build()
            }
            else -> null
        }
    }
}
