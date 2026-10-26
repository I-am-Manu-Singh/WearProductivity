package com.creativeblisslabs.wearproductivity.mobile.sync

import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import com.creativeblisslabs.wearproductivity.shared.CalendarEvent
import com.creativeblisslabs.wearproductivity.shared.LifeRepository
import com.creativeblisslabs.wearproductivity.shared.SyncStamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarSyncManager(
    private val context: Context,
    private val repository: LifeRepository,
    private val scope: CoroutineScope
) {
    fun syncEventsFromDevice() {
        scope.launch(Dispatchers.IO) {
            try {
                val projection = arrayOf(
                    CalendarContract.Events._ID,
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND,
                    CalendarContract.Events.EVENT_LOCATION
                )

                val now = System.currentTimeMillis()
                val threeDaysFromNow = now + (3 * 24 * 60 * 60 * 1000L)
                val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
                val selectionArgs = arrayOf(now.toString(), threeDaysFromNow.toString())

                val cursor: Cursor? = context.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${CalendarContract.Events.DTSTART} ASC"
                )

                cursor?.use {
                    val eventsList = mutableListOf<CalendarEvent>()
                    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

                    while (it.moveToNext()) {
                        val title = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)) ?: "Event"
                        val startMs = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
                        val endMs = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
                        val loc = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)) ?: ""

                        val timeRangeStr = "${timeFormatter.format(Date(startMs))} - ${timeFormatter.format(Date(endMs))}"

                        eventsList.add(
                            CalendarEvent(
                                stamp = SyncStamp(id = "sys-cal-${it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events._ID))}"),
                                title = title,
                                project = "System Calendar",
                                timeRange = timeRangeStr,
                                location = loc,
                                syncStatus = "Synced"
                            )
                        )
                    }

                    if (eventsList.isNotEmpty()) {
                        repository.syncSystemCalendarEvents(eventsList)
                    }
                }
            } catch (e: SecurityException) {
                // Requires Manifest.permission.READ_CALENDAR
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
