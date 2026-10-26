package com.creativeblisslabs.wearproductivity.wear.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class WearReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "LifeOS Alarm"
        val message = intent.getStringExtra("message") ?: "Time for your scheduled item!"
        val alarmId = intent.getIntExtra("alarm_id", 0)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "lifeos_alarms_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "LifeOS Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(alarmId, notification)
    }
}
