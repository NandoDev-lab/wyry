package com.nandohypesoft.wyry.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val PROFILES = stringPreferencesKey("radio_profiles")
        val SELECTED_ID = stringPreferencesKey("selected_profile_id")
        val PLAYLIST = stringPreferencesKey("playlist_uris")
        val VIGNETTES = stringPreferencesKey("vignette_uris")
    }

    val playlistFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val json = preferences[Keys.PLAYLIST] ?: "[]"
        try {
            Json.decodeFromString<List<String>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    val vignettesFlow: Flow<List<String?>> = context.dataStore.data.map { preferences ->
        val json = preferences[Keys.VIGNETTES] ?: "[null, null, null, null, null]"
        try {
            Json.decodeFromString<List<String?>>(json)
        } catch (e: Exception) {
            List(5) { null }
        }
    }

    suspend fun savePlaylist(uris: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[Keys.PLAYLIST] = Json.encodeToString(uris)
        }
    }

    suspend fun saveVignettes(uris: List<String?>) {
        context.dataStore.edit { preferences ->
            preferences[Keys.VIGNETTES] = Json.encodeToString(uris)
        }
    }

    val profilesFlow: Flow<List<StreamConfig>> = context.dataStore.data.map { preferences ->
        val json = preferences[Keys.PROFILES] ?: "[]"
        try {
            Json.decodeFromString<List<StreamConfig>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    val selectedProfileIdFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[Keys.SELECTED_ID]
    }

    suspend fun saveProfiles(profiles: List<StreamConfig>) {
        context.dataStore.edit { preferences ->
            preferences[Keys.PROFILES] = Json.encodeToString(profiles)
        }
    }

    suspend fun selectProfile(id: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SELECTED_ID] = id
        }
    }
}
