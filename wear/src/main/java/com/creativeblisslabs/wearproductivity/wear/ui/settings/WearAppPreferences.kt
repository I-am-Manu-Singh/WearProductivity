package com.creativeblisslabs.wearproductivity.wear.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Wear-only preferences stored locally on the watch.
 * Values are local to this watch and intentionally separate from synced LifeState data.
 */
object WearAppPreferences {
    const val FILE_NAME = "lifeos_wear_preferences"

    const val KEY_HAPTICS_ENABLED = "haptics_enabled"
    const val KEY_USE_24_HOUR_CLOCK = "use_24_hour_clock"
    const val KEY_COMPACT_DASHBOARD = "compact_dashboard_cards"
    const val KEY_WATER_GOAL_ML = "water_goal_ml"
    const val KEY_FOCUS_DEFAULT_MINUTES = "focus_default_minutes"
    const val KEY_TASK_DEFAULT_PRIORITY = "task_default_priority"
    const val KEY_QUICK_ALARM_LEAD_MINUTES = "quick_alarm_lead_minutes"
    const val KEY_EVENT_DEFAULT_REMINDER_MINUTES = "event_default_reminder_minutes"
    const val KEY_SMART_WAKE_ENABLED = "smart_wake_enabled"
    const val KEY_AUDIO_SHUFFLE_DEFAULT = "audio_shuffle_default"
    const val KEY_AUDIO_REPEAT_MODE = "audio_repeat_mode"
    const val KEY_AUDIO_EQ_PRESET = "audio_eq_preset"

    const val KEY_FACE_TOP_COMPLICATION = "face_top_complication"
    const val KEY_FACE_TOP_LEFT_COMPLICATION = "face_top_left_complication"
    const val KEY_FACE_TOP_RIGHT_COMPLICATION = "face_top_right_complication"
    const val KEY_FACE_BOTTOM_COMPLICATION = "face_bottom_complication"
    const val KEY_FACE_RING_METRIC = "face_ring_metric"

    fun getBoolean(context: Context, key: String, defaultValue: Boolean): Boolean =
        prefs(context).getBoolean(key, defaultValue)

    fun getInt(context: Context, key: String, defaultValue: Int): Int =
        prefs(context).getInt(key, defaultValue)

    fun getString(context: Context, key: String, defaultValue: String): String =
        prefs(context).getString(key, defaultValue) ?: defaultValue

    fun setBoolean(context: Context, key: String, value: Boolean) {
        prefs(context).edit().putBoolean(key, value).apply()
    }

    fun setInt(context: Context, key: String, value: Int) {
        prefs(context).edit().putInt(key, value).apply()
    }

    fun setString(context: Context, key: String, value: String) {
        prefs(context).edit().putString(key, value).apply()
    }

    fun reset(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun preferences(context: Context): SharedPreferences = prefs(context)

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
}

@Composable
fun rememberWearBooleanPreference(key: String, defaultValue: Boolean): Boolean {
    val context = LocalContext.current.applicationContext
    var value by remember(context, key, defaultValue) {
        mutableStateOf(WearAppPreferences.getBoolean(context, key, defaultValue))
    }

    DisposableEffect(context, key, defaultValue) {
        val preferences = WearAppPreferences.preferences(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { changedPrefs, changedKey ->
            if (changedKey == null || changedKey == key) {
                value = changedPrefs.getBoolean(key, defaultValue)
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return value
}

@Composable
fun rememberWearIntPreference(key: String, defaultValue: Int): Int {
    val context = LocalContext.current.applicationContext
    var value by remember(context, key, defaultValue) {
        mutableIntStateOf(WearAppPreferences.getInt(context, key, defaultValue))
    }

    DisposableEffect(context, key, defaultValue) {
        val preferences = WearAppPreferences.preferences(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { changedPrefs, changedKey ->
            if (changedKey == null || changedKey == key) {
                value = changedPrefs.getInt(key, defaultValue)
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return value
}

@Composable
fun rememberWearStringPreference(key: String, defaultValue: String): String {
    val context = LocalContext.current.applicationContext
    var value by remember(context, key, defaultValue) {
        mutableStateOf(WearAppPreferences.getString(context, key, defaultValue))
    }

    DisposableEffect(context, key, defaultValue) {
        val preferences = WearAppPreferences.preferences(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { changedPrefs, changedKey ->
            if (changedKey == null || changedKey == key) {
                value = changedPrefs.getString(key, defaultValue) ?: defaultValue
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return value
}
