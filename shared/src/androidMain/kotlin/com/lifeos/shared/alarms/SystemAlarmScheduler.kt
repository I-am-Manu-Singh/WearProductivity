
package com.creativeblisslabs.wearproductivity.shared.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object SystemAlarmScheduler {

    private const val ACTION_ALARM =
        "com.creativeblisslabs.wearproductivity.ALARM_TRIGGER"

    private const val EXTRA_ALARM_ID = "alarm_id"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_MESSAGE = "message"
    private const val EXTRA_IS_SNOOZE = "is_snooze"

    enum class ScheduleResult {
        EXACT,
        INEXACT_FALLBACK,
        FAILED
    }

    private fun createPendingIntent(
        context: Context,
        receiverClass: Class<*>,
        alarmId: Int,
        isSnooze: Boolean,
        title: String = "",
        message: String = "",
        flags: Int
    ): PendingIntent {
        val action = if (isSnooze) {
            "$ACTION_ALARM.SNOOZE"
        } else {
            "$ACTION_ALARM.NORMAL"
        }

        val intent = Intent(context, receiverClass).apply {
            this.action = action
            data = android.net.Uri.parse(
                "lifeos://alarm/$alarmId/${if (isSnooze) "snooze" else "normal"}"
            )

            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }

        val pendingFlags = flags or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE
                } else {
                    0
                }

        return PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            pendingFlags
        )
    }

    fun scheduleAlarm(
        context: Context,
        triggerAtMillis: Long,
        receiverClass: Class<*>,
        alarmId: Int,
        title: String,
        message: String,
        isSnooze: Boolean = false
    ): ScheduleResult {
        if (triggerAtMillis <= System.currentTimeMillis()) {
            return ScheduleResult.FAILED
        }

        val appContext = context.applicationContext

        val alarmManager = appContext.getSystemService(
            Context.ALARM_SERVICE
        ) as? AlarmManager ?: return ScheduleResult.FAILED

        val pendingIntent = try {
            createPendingIntent(
                context = appContext,
                receiverClass = receiverClass,
                alarmId = alarmId,
                isSnooze = isSnooze,
                title = title,
                message = message,
                flags = PendingIntent.FLAG_UPDATE_CURRENT
            )
        } catch (_: Exception) {
            return ScheduleResult.FAILED
        }

        return try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        !alarmManager.canScheduleExactAlarms() -> {

                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )

                    ScheduleResult.INEXACT_FALLBACK
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )

                    ScheduleResult.EXACT
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )

                    ScheduleResult.EXACT
                }

                else -> {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )

                    ScheduleResult.INEXACT_FALLBACK
                }
            }
        } catch (_: SecurityException) {
            scheduleInexactFallback(
                alarmManager,
                triggerAtMillis,
                pendingIntent
            )
        } catch (_: Exception) {
            ScheduleResult.FAILED
        }
    }

    private fun scheduleInexactFallback(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ): ScheduleResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }

            ScheduleResult.INEXACT_FALLBACK
        } catch (_: Exception) {
            ScheduleResult.FAILED
        }
    }

    fun cancelAlarm(
        context: Context,
        receiverClass: Class<*>,
        alarmId: Int,
        isSnooze: Boolean = false
    ) {
        val appContext = context.applicationContext

        val alarmManager = appContext.getSystemService(
            Context.ALARM_SERVICE
        ) as? AlarmManager ?: return

        val pendingIntent = try {
            createPendingIntent(
                context = appContext,
                receiverClass = receiverClass,
                alarmId = alarmId,
                isSnooze = isSnooze,
                flags = PendingIntent.FLAG_NO_CREATE
            )
        } catch (_: Exception) {
            null
        } ?: return

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun cancelAllAlarmTriggers(
        context: Context,
        receiverClass: Class<*>,
        alarmId: Int
    ) {
        cancelAlarm(context, receiverClass, alarmId, false)
        cancelAlarm(context, receiverClass, alarmId, true)
    }
}