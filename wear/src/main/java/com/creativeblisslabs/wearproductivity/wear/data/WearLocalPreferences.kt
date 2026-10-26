package com.creativeblisslabs.wearproductivity.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.wearDataStore: DataStore<Preferences> by preferencesDataStore(name = "lifeos_wear_preferences")

class WearLocalPreferences(private val context: Context) {

    fun getBoolean(key: String, defaultValue: Boolean): Flow<Boolean> {
        val prefKey = booleanPreferencesKey(key)
        return context.wearDataStore.data.map { prefs -> prefs[prefKey] ?: defaultValue }
    }

    suspend fun setBoolean(key: String, value: Boolean) {
        val prefKey = booleanPreferencesKey(key)
        context.wearDataStore.edit { prefs -> prefs[prefKey] = value }
    }

    fun getInt(key: String, defaultValue: Int): Flow<Int> {
        val prefKey = intPreferencesKey(key)
        return context.wearDataStore.data.map { prefs -> prefs[prefKey] ?: defaultValue }
    }

    suspend fun setInt(key: String, value: Int) {
        val prefKey = intPreferencesKey(key)
        context.wearDataStore.edit { prefs -> prefs[prefKey] = value }
    }

    fun getString(key: String, defaultValue: String): Flow<String> {
        val prefKey = stringPreferencesKey(key)
        return context.wearDataStore.data.map { prefs -> prefs[prefKey] ?: defaultValue }
    }

    suspend fun setString(key: String, value: String) {
        val prefKey = stringPreferencesKey(key)
        context.wearDataStore.edit { prefs -> prefs[prefKey] = value }
    }
}
