package com.findmychild.app.data.remote

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 中继服务器配置。
 * 根据运行环境选择对应地址：
 * - 模拟器: ws://10.0.2.2:8080/ws
 * - 真机(同 WiFi): ws://192.168.x.x:8080/ws
 * - 生产环境: wss://your-server.com/ws
 */
@Singleton
class RelayConfig @Inject constructor() {
    /**
     * WebSocket 中继服务器地址。
     *
     * 使用方法：
     * 1. 先启动 relay-server: ./gradlew :relay-server:run
     * 2. 查看电脑 IP: ifconfig | grep "inet "
     * 3. 把下面地址改成: ws://你的电脑IP:8080/ws
     */
    val wsUrl: String
        get() = "ws://10.0.2.2:8080/ws"  // 👈 改这里
}
