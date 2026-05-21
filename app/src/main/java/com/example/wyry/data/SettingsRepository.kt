package com.example.wyry.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val HOST = stringPreferencesKey("host")
        val PORT = intPreferencesKey("port")
        val USER = stringPreferencesKey("user")
        val PASS = stringPreferencesKey("pass")
        val MOUNTPOINT = stringPreferencesKey("mountpoint")
        val BITRATE = intPreferencesKey("bitrate")
    }

    val streamConfigFlow: Flow<StreamConfig> = context.dataStore.data.map { preferences ->
        StreamConfig(
            host = preferences[Keys.HOST] ?: "",
            port = preferences[Keys.PORT] ?: 8000,
            user = preferences[Keys.USER] ?: "source",
            pass = preferences[Keys.PASS] ?: "",
            mountpoint = preferences[Keys.MOUNTPOINT] ?: "/stream",
            bitrate = preferences[Keys.BITRATE] ?: 128
        )
    }

    suspend fun saveConfig(config: StreamConfig) {
        context.dataStore.edit { preferences ->
            preferences[Keys.HOST] = config.host
            preferences[Keys.PORT] = config.port
            preferences[Keys.USER] = config.user
            preferences[Keys.PASS] = config.pass
            preferences[Keys.MOUNTPOINT] = config.mountpoint
            preferences[Keys.BITRATE] = config.bitrate
        }
    }
}
