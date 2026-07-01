package com.findmychild.app.domain.repository

import com.findmychild.app.domain.model.PairingInfo
import com.findmychild.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    /** 获取已保存的角色 */
    fun getUserRole(): Flow<UserRole?>

    /** 保存角色 */
    suspend fun setUserRole(role: UserRole)

    /** 获取配对信息 */
    fun getPairingInfo(): Flow<PairingInfo?>

    /** 保存配对信息 */
    suspend fun savePairingInfo(info: PairingInfo)

    /** 清除所有数据（断开配对后） */
    suspend fun clearAll()
}
