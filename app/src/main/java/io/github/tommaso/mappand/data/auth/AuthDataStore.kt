package io.github.tommaso.mappand.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore("auth")

class AuthDataStore(private val context: Context) {

    companion object {
        val TOKEN = stringPreferencesKey("pcloud_token")
        val HOST = stringPreferencesKey("pcloud_host")
        val DEVICE_ID = stringPreferencesKey("pcloud_deviceid")
        val FOLDER_ID = longPreferencesKey("selected_folder_id")
        val FOLDER_NAME = stringPreferencesKey("selected_folder_name")
        const val EU_HOST = "https://eapi.pcloud.com"
        const val US_HOST = "https://api.pcloud.com"
    }

    data class SelectedFolder(val id: Long, val name: String)

    val selectedFolderFlow: Flow<SelectedFolder?> = context.dataStore.data.map { prefs ->
        val id = prefs[FOLDER_ID] ?: return@map null
        SelectedFolder(id, prefs[FOLDER_NAME] ?: "")
    }

    suspend fun setSelectedFolder(id: Long, name: String) {
        context.dataStore.edit { it[FOLDER_ID] = id; it[FOLDER_NAME] = name }
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[TOKEN] }
    val hostFlow: Flow<String> = context.dataStore.data.map { it[HOST] ?: EU_HOST }

    suspend fun getToken(): String? = tokenFlow.first()

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[TOKEN] = token }
    }

    suspend fun clearToken() {
        context.dataStore.edit { it.remove(TOKEN) }
    }

    suspend fun setHost(host: String) {
        context.dataStore.edit { it[HOST] = host }
    }

    suspend fun getHost(): String = hostFlow.first()

    suspend fun getDeviceId(): String {
        val prefs = context.dataStore.data.first()
        val existing = prefs[DEVICE_ID]
        if (existing != null) return existing
        val id = UUID.randomUUID().toString().replace("-", "")
        context.dataStore.edit { it[DEVICE_ID] = id }
        return id
    }
}
