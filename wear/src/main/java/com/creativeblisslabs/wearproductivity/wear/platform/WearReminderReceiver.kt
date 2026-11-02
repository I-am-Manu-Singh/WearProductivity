package com.creativeblisslabs.wearproductivity.wear.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class WearReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ALARMS = "lifeos_alarms_channel"
        const val CHANNEL_CALENDAR = "lifeos_calendar_channel"
        const val CHANNEL_HABITS = "lifeos_habits_channel"
        const val CHANNEL_FOCUS = "lifeos_focus_channel"
        const val CHANNEL_WORKOUT = "lifeos_workout_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title")
            ?: intent.getStringExtra("reminder_title")
            ?: "LifeOS Reminder"
        val message = intent.getStringExtra("message")
            ?: intent.getStringExtra("reminder_body")
            ?: "Time for your scheduled event!"
        val alarmId = intent.getIntExtra("alarm_id", 0).takeIf { it != 0 }
            ?: intent.getStringExtra("reminder_id")?.hashCode()
            ?: (System.currentTimeMillis() % 10000).toInt()

        val itemType = intent.getStringExtra("type")?.lowercase() ?: when {
            title.contains("habit", ignoreCase = true) || intent.hasExtra("reminder_id") -> "habit"
            title.contains("focus", ignoreCase = true) || title.contains("break", ignoreCase = true) -> "focus"
            title.contains("workout", ignoreCase = true) || title.contains("exercise", ignoreCase = true) -> "workout"
            title.contains("calendar", ignoreCase = true) || title.contains("event", ignoreCase = true) -> "calendar"
            else -> "alarm"
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val (channelId, channelName, iconRes) = when (itemType) {
            "habit" -> Triple(CHANNEL_HABITS, "LifeOS Habits & Routines", android.R.drawable.ic_popup_reminder)
            "focus" -> Triple(CHANNEL_FOCUS, "LifeOS Focus & Pomodoro", android.R.drawable.ic_dialog_info)
            "workout" -> Triple(CHANNEL_WORKOUT, "LifeOS Workout Milestones", android.R.drawable.ic_menu_compass)
            "calendar" -> Triple(CHANNEL_CALENDAR, "LifeOS Calendar & Events", android.R.drawable.ic_menu_agenda)
            else -> Triple(CHANNEL_ALARMS, "LifeOS Alarms & Timers", android.R.drawable.ic_lock_idle_alarm)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val contentPending = launchIntent?.let {
            PendingIntent.getActivity(context, alarmId, it, flags)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(iconRes)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .apply {
                if (contentPending != null) {
                    setContentIntent(contentPending)
                }
            }
            .build()

        notificationManager.notify(alarmId, notification)
    }
}
