package com.findmychild.app.data.remote

import com.findmychild.app.data.local.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 中继服务器配置。
 *
 * 服务器地址通过 DataStore 持久化，支持运行时动态修改。
 * 默认值 [SettingsDataStore.DEFAULT_SERVER_URL] 为模拟器地址。
 *
 * - 模拟器: ws://10.0.2.2:8080/ws
 * - 真机(同 WiFi): ws://192.168.x.x:8080/ws
 * - 生产环境: wss://your-server.com/ws
 */
@Singleton
class RelayConfig @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    /** 当前服务器地址（Flow，可观察变化） */
    val wsUrlFlow: Flow<String> = settingsDataStore.serverUrl

    /** 同步获取当前缓存的服务器地址 */
    private var cachedUrl: String = SettingsDataStore.DEFAULT_SERVER_URL

    /** 更新缓存（由 WebSocket 连接前调用） */
    fun updateCachedUrl(url: String) {
        cachedUrl = url
    }

    /** 获取当前服务器地址 */
    val wsUrl: String
        get() = cachedUrl
}
