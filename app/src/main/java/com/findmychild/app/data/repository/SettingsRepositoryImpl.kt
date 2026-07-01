package com.findmychild.app.data.repository

import com.findmychild.app.data.local.SettingsDataStore
import com.findmychild.app.domain.model.PairingInfo
import com.findmychild.app.domain.model.UserRole
import com.findmychild.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore
) : SettingsRepository {

    override fun getUserRole(): Flow<UserRole?> = dataStore.userRole

    override suspend fun setUserRole(role: UserRole) = dataStore.setUserRole(role)

    override fun getPairingInfo(): Flow<PairingInfo?> = dataStore.pairingInfo

    override suspend fun savePairingInfo(info: PairingInfo) = dataStore.savePairingInfo(info)

    override suspend fun clearAll() = dataStore.clearAll()
}
