package com.findmychild.app.data.repository

import com.findmychild.app.domain.model.PairingInfo
import com.findmychild.app.domain.repository.PairingRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 配对仓库实现。
 * 家长端：生成 sessionId (=UUID)，作为配对码写入 QR；
 * 孩子端：扫描 QR 获得 sessionId，调用 joinPairingSession。
 *
 * 当前不涉及 HTTP 验证（中继服务器 WebSocket 层做角色校验），
 * 这个 Repository 主要负责生成/解析配对码。
 */
@Singleton
class PairingRepositoryImpl @Inject constructor() : PairingRepository {

    override suspend fun createPairingSession(deviceName: String): PairingInfo {
        val sessionId = UUID.randomUUID().toString()
        return PairingInfo(
            sessionId = sessionId,
            deviceName = deviceName,
            pairedAt = System.currentTimeMillis()
        )
    }

    override suspend fun joinPairingSession(
        sessionId: String,
        deviceName: String
    ): Result<PairingInfo> {
        return try {
            if (sessionId.isBlank()) {
                return Result.failure(IllegalArgumentException("无效的配对码"))
            }
            Result.success(
                PairingInfo(
                    sessionId = sessionId,
                    deviceName = deviceName,
                    pairedAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
