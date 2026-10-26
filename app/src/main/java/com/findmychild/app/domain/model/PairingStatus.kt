package com.findmychild.app.domain.model

/**
 * 中继服务器下发的配对状态。
 *
 * WAITING：自己已注册，对端还没连上来。家长生成二维码后一直停在这个状态，
 * 它**不是**配对成功——早期版本把"WebSocket 已打开"当成配对成功，
 * 家长连自己都还没被扫码就跳去了指南针页，二维码一闪而过。
 * PAIRED：双方都在线，可以开始共享位置。
 * DISCONNECTED：对端掉线。
 */
enum class PairingStatus {
    WAITING,
    PAIRED,
    DISCONNECTED;

    companion object {
        /** 服务端字符串 → 枚举。未知值按 WAITING 处理：宁可多等，也不要误跳。 */
        fun fromWire(value: String): PairingStatus = when (value) {
            "PAIRED" -> PAIRED
            "DISCONNECTED" -> DISCONNECTED
            else -> WAITING
        }
    }
}
