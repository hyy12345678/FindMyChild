package com.findmychild.app.domain.repository

import com.findmychild.app.domain.model.PairingInfo

interface PairingRepository {

    /** 家长端：创建配对会话，生成 sessionId */
    suspend fun createPairingSession(deviceName: String): PairingInfo

    /** 孩子端：通过扫描到的 sessionId 加入配对 */
    suspend fun joinPairingSession(sessionId: String, deviceName: String): Result<PairingInfo>
}
