package com.findmychild.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.findmychild.app.domain.model.PairingInfo
import com.findmychild.app.domain.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object Keys {
        val IS_PARENT = booleanPreferencesKey("is_parent")
        val HAS_SELECTED_ROLE = booleanPreferencesKey("has_selected_role")
        val SESSION_ID = stringPreferencesKey("session_id")
        val DEVICE_NAME = stringPreferencesKey("device_name")
        val PAIRED_AT = longPreferencesKey("paired_at")
        val PEER_DEVICE_NAME = stringPreferencesKey("peer_device_name")
    }

    /** 获取角色 Flow */
    val userRole: Flow<UserRole?> = context.dataStore.data.map { prefs ->
        if (!prefs[HAS_SELECTED_ROLE]) null
        else if (prefs[IS_PARENT] == true) UserRole.PARENT
        else UserRole.CHILD
    }

    /** 保存角色 */
    suspend fun setUserRole(role: UserRole) {
        context.dataStore.edit { prefs ->
            prefs[HAS_SELECTED_ROLE] = true
            prefs[IS_PARENT] = role == UserRole.PARENT
        }
    }

    /** 获取配对信息 Flow */
    val pairingInfo: Flow<PairingInfo?> = context.dataStore.data.map { prefs ->
        val sessionId = prefs[SESSION_ID] ?: return@map null
        val deviceName = prefs[DEVICE_NAME] ?: return@map null
        PairingInfo(
            sessionId = sessionId,
            deviceName = deviceName,
            pairedAt = prefs[PAIRED_AT] ?: 0L,
            peerDeviceName = prefs[PEER_DEVICE_NAME] ?: ""
        )
    }

    /** 保存配对信息 */
    suspend fun savePairingInfo(info: PairingInfo) {
        context.dataStore.edit { prefs ->
            prefs[SESSION_ID] = info.sessionId
            prefs[DEVICE_NAME] = info.deviceName
            prefs[PAIRED_AT] = info.pairedAt
            prefs[PEER_DEVICE_NAME] = info.peerDeviceName
        }
    }

    /** 清除所有数据 */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
