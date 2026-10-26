package com.creativeblisslabs.wearproductivity.shared.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object SystemAlarmScheduler {

    fun scheduleAlarm(
        context: Context,
        triggerAtMillis: Long,
        receiverClass: Class<*>,
        alarmId: Int,
        title: String,
        message: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, receiverClass).apply {
            putExtra("alarm_id", alarmId)
            putExtra("title", title)
            putExtra("message", message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelAlarm(
        context: Context,
        receiverClass: Class<*>,
        alarmId: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, receiverClass)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
