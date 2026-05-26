package com.nandohypesoft.vibecast.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
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
        val AD_FREE_EXPIRATION = longPreferencesKey("ad_free_expiration_time")
        val LAST_INTERSTITIAL_TIME = longPreferencesKey("last_interstitial_time")
        val IS_ADMIN = booleanPreferencesKey("is_admin_access")
        val DEVICE_ID = stringPreferencesKey("device_unique_id")
    }

    val deviceIdFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.DEVICE_ID] ?: run {
            // Usa o ANDROID_ID do sistema, que é persistente ao aparelho
            val androidId = android.provider.Settings.Secure.getString(
                context.contentResolver, 
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: java.util.UUID.randomUUID().toString().substring(0, 8)
            
            val finalId = androidId.take(8).uppercase()
            finalId
        }
    }

    suspend fun saveDeviceId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DEVICE_ID] = id
        }
    }

    val adFreeExpirationFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[Keys.AD_FREE_EXPIRATION] ?: 0L
    }

    val lastInterstitialTimeFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[Keys.LAST_INTERSTITIAL_TIME] ?: 0L
    }

    val isAdminFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.IS_ADMIN] ?: false
    }

    suspend fun setAdFreeExpiration(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AD_FREE_EXPIRATION] = timestamp
        }
    }

    suspend fun setLastInterstitialTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LAST_INTERSTITIAL_TIME] = timestamp
        }
    }

    suspend fun setAdminStatus(isAdmin: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_ADMIN] = isAdmin
        }
    }

    // ... (restante do código da playlist e perfis mantido internamente)
    val playlistFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val json = preferences[Keys.PLAYLIST] ?: "[]"
        try { Json.decodeFromString<List<String>>(json) } catch (e: Exception) { emptyList() }
    }
    val vignettesFlow: Flow<List<String?>> = context.dataStore.data.map { preferences ->
        val json = preferences[Keys.VIGNETTES] ?: "[null, null, null, null, null]"
        try { Json.decodeFromString<List<String?>>(json) } catch (e: Exception) { List(5) { null } }
    }
    suspend fun savePlaylist(uris: List<String>) {
        context.dataStore.edit { preferences -> preferences[Keys.PLAYLIST] = Json.encodeToString(uris) }
    }
    suspend fun saveVignettes(uris: List<String?>) {
        context.dataStore.edit { preferences -> preferences[Keys.VIGNETTES] = Json.encodeToString(uris) }
    }
    val profilesFlow: Flow<List<StreamConfig>> = context.dataStore.data.map { preferences ->
        val json = preferences[Keys.PROFILES] ?: "[]"
        try { Json.decodeFromString<List<StreamConfig>>(json) } catch (e: Exception) { emptyList() }
    }
    val selectedProfileIdFlow: Flow<String?> = context.dataStore.data.map { preferences -> preferences[Keys.SELECTED_ID] }
    suspend fun saveProfiles(profiles: List<StreamConfig>) {
        context.dataStore.edit { preferences -> preferences[Keys.PROFILES] = Json.encodeToString(profiles) }
    }
    suspend fun selectProfile(id: String) {
        context.dataStore.edit { preferences -> preferences[Keys.SELECTED_ID] = id }
    }
}
