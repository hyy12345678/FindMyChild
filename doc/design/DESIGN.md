# 设计文档 — FindMyChild

> 版本: 0.2.0-dev | 更新日期: 2026-10-23

---

## 1. 项目概述

### 1.1 背景

据公安部统计，中国每年约有 20 万名儿童走失。在人潮涌动的商场、车站、景区，黄金寻找时间往往只有最初的几分钟。FindMyChild 致力于让家长在最短时间内知道孩子在哪个方向、有多远。

### 1.2 核心理念

- **指南针优于地图**：紧急寻人场景下，即时方向感 > 路线规划
- **极简配对**：一码生成、一扫即连，30 秒内完成配对
- **做减法**：少一个功能，多一份专注

### 1.3 目标用户

| 角色 | 设备 | 职责 |
|:----:|:----:|:-----|
| 家长 | Android 手机 | 生成二维码、查看指南针、追踪孩子方向与距离 |
| 孩子 | Android 手机 | 扫码配对、后台持续上报 GPS 位置 |

---

## 2. 系统架构

### 2.1 整体架构

项目采用 **Clean Architecture** 分层架构 + **MVVM** 模式：

```
┌──────────────────────────────────────────────────────────┐
│                       UI Layer                            │
│         Compose Screen ← HiltViewModel                    │
│     (Compass / QR Pairing / Role / Navigation)            │
├──────────────────────────────────────────────────────────┤
│                     Domain Layer                          │
│      Model / Repository Interface / UseCase              │
│     (UserRole / PairingInfo / SyncRepository)             │
├──────────────────────────────────────────────────────────┤
│                      Data Layer                           │
│   Repository Impl / Sensor / WebSocket / DataStore       │
│  (LocationProvider / HeadingProvider / RelayWebSocket)   │
├──────────────────────────────────────────────────────────┤
│                    Relay Server                            │
│       Ktor + Netty + WebSocket Session Manager            │
│     (SessionManager / WebSocketHandler / Messages)       │
└──────────────────────────────────────────────────────────┘
```

### 2.2 模块结构

```
FindMyChild/
├── app/                              # Android 客户端
│   └── src/main/java/com/findmychild/app/
│       ├── data/                     # 数据层
│       │   ├── local/                #   本地持久化 (DataStore)
│       │   │   └── SettingsDataStore.kt
│       │   ├── remote/               #   网络通信 (WebSocket + Moshi)
│       │   │   ├── RelayConfig.kt
│       │   │   ├── RelayWebSocketManager.kt
│       │   │   └── dto/
│       │   ├── repository/           #   Repository 实现
│       │   │   ├── SettingsRepositoryImpl.kt
│       │   │   ├── PairingRepositoryImpl.kt
│       │   │   └── SyncRepositoryImpl.kt
│       │   └── sensor/               #   传感器 (GPS + 磁力计)
│       │       ├── LocationProvider.kt
│       │       ├── HeadingProvider.kt
│       │       └── LocationService.kt
│       ├── di/                       # 依赖注入 (Hilt Modules)
│       │   ├── AppModule.kt
│       │   └── SensorModule.kt
│       ├── domain/                   # 领域层
│       │   ├── model/                #   核心数据模型
│       │   │   ├── UserRole.kt
│       │   │   ├── PairingInfo.kt
│       │   │   └── LocationData.kt
│       │   ├── repository/           #   Repository 接口
│       │   │   ├── SettingsRepository.kt
│       │   │   ├── PairingRepository.kt
│       │   │   └── SyncRepository.kt
│       │   └── usecase/              #   业务用例
│       └── ui/                       # 表现层
│           ├── compass/              #   指南针页面
│           │   ├── CompassScreen.kt
│           │   └── CompassViewModel.kt
│           ├── component/            #   自定义组件
│           │   └── CompassView.kt
│           ├── navigation/           #   导航图与路由
│           │   ├── AppNavGraph.kt
│           │   ├── NavRoute.kt
│           │   └── ChildActiveScreen.kt
│           ├── pairing/              #   配对流程
│           │   ├── child/
│           │   │   ├── QrScanScreen.kt
│           │   │   └── QrScanViewModel.kt
│           │   └── parent/
│           │       ├── QrGenerationScreen.kt
│           │       └── QrGenerationViewModel.kt
│           ├── role/                 #   角色选择
│           │   └── RoleSelectionScreen.kt
│           └── theme/                #   主题与样式
│               ├── Theme.kt
│               └── Color.kt
│
└── relay-server/                     # WebSocket 中继服务器
    └── src/main/kotlin/com/findmychild/relay/
        ├── Application.kt            #   Ktor 启动入口
        ├── WebSocketHandler.kt       #   消息路由与处理
        ├── SessionManager.kt         #   配对会话管理（含过期清理）
        └── Messages.kt              #   通信协议定义
```

### 2.3 技术栈

| 分类 | 技术 | 版本 | 选型理由 |
|:----:|:-----|:----:|:-------|
| 语言 | Kotlin | 2.0.21 | 现代 JVM 语言，协程原生支持 |
| UI 框架 | Jetpack Compose + Material 3 | BOM 2024.09 | 声明式 UI，减少模板代码 |
| 依赖注入 | Hilt | 2.51.1 | Android 官方推荐的 DI 方案 |
| 导航 | Navigation Compose | 2.8.0 | 与 Compose 深度集成 |
| 定位 | Google Play Services FusedLocation | 21.3.0 | 低功耗 GPS 融合定位 |
| 传感器 | Android SensorManager | — | 磁力计 + 加速度计融合朝向 |
| 网络客户端 | OkHttp WebSocket + Moshi | 4.12.0 / 1.15.1 | 轻量可靠，WebSocket 支持完善 |
| 中继服务器 | Ktor + Netty + kotlinx.serialization | 3.0.3 | 纯 Kotlin 后端，部署简单 |
| 相机 | CameraX | 1.4.1 | 生命周期感知，简化相机开发 |
| 二维码 | ZXing (生成 + 解码) | 3.5.3 | 成熟稳定的 QR 码库 |
| 本地存储 | DataStore Preferences | 1.1.1 | 协程友好，替代 SharedPreferences |
| 协程 | kotlinx-coroutines-android | 1.9.0 | 异步编程基础设施 |
| 构建 | Gradle (Kotlin DSL) + Version Catalog | 8.7 | 类型安全构建脚本 |

---

## 3. 通信设计

### 3.1 通信协议

客户端与中继服务器之间通过 WebSocket 交换 JSON 消息：

| 方向 | 消息类型 | 说明 |
|:----:|:--------:|:-----|
| 客户端 → 服务器 | `REGISTER` | 注册设备角色（PARENT / CHILD）到会话 |
| 客户端 → 服务器 | `LOCATION_UPDATE` | 上报 GPS 坐标 |
| 服务器 → 客户端 | `PAIRING_STATUS` | 配对状态通知（WAITING / PAIRED / DISCONNECTED） |
| 服务器 → 客户端 | `PEER_LOCATION` | 对端位置转发 |
| 服务器 → 客户端 | `ERROR` | 错误消息 |

### 3.2 消息格式

```json
// REGISTER
{ "type": "REGISTER", "sessionId": "uuid-xxx", "role": "PARENT" }

// LOCATION_UPDATE
{ "type": "LOCATION_UPDATE", "latitude": 39.9042, "longitude": 116.4074, "accuracy": 5.0, "timestamp": 1697894400000 }

// PAIRING_STATUS
{ "type": "PAIRING_STATUS", "status": "PAIRED", "peerDeviceName": "孩子设备" }

// PEER_LOCATION
{ "type": "PEER_LOCATION", "latitude": 39.9042, "longitude": 116.4074, "accuracy": 5.0, "timestamp": 1697894400000 }

// ERROR
{ "type": "ERROR", "message": "该会话已有家长设备" }
```

> ⚠️ **`type` 必须实际出现在报文里。** 服务端用 kotlinx.serialization，
> 而 `Json { ignoreUnknownKeys = true; isLenient = true }` 这个配置下
> **带默认值的字段不会被编码**——`type` 正好都是有默认值的，结果报文只剩
> `{"status":"WAITING"}`。客户端 `RelayWebSocketManager.handleMessage()` 是纯按
> `type` 路由的，缺字段就整条丢弃，表现为"收不到任何服务端消息"。
> 因此 `WebSocketHandler` 的 `json` 必须显式 `encodeDefaults = true`。
> 反方向（客户端 → 服务端）用 Moshi，Kotlin 默认值已实体化成对象字段，不受影响。

### 3.3 连接生命周期

```
                          ┌─────────────┐
                          │  App 启动   │
                          └──────┬──────┘
                                 │
                    ┌────────────▼────────────┐
                    │  读取 DataStore 配对信息  │
                    └────────────┬────────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
     ┌────────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐
     │  无配对信息     │  │  家长已配对   │  │  孩子已配对   │
     └────────┬───────┘  └──────┬───────┘  └──────┬───────┘
              │                  │                  │
     ┌────────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐
     │  角色选择页     │  │  指南针页面   │  │  孩子状态页   │
     └────────┬───────┘  └──────┬───────┘  └──────┬───────┘
              │                  │                  │
     ┌────────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐
     │  QR 生成/扫描   │  │ ensureConnect │  │ 申请定位权限  │
     │                 │  │               │  │ →启动定位服务 │
     └────────┬───────┘  └──────┬───────┘  └──────┬───────┘
              │                  │                  │
              └──────────────────┼──────────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │  WebSocket 连接 Relay     │
                    └────────────┬────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │  REGISTER → 服务器匹配    │
                    └────────────┬────────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
     ┌────────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐
     │   配对成功      │  │   等待中      │  │   断线重连    │
     │   PAIRED       │  │   WAITING     │  │ 指数退避策略  │
     └────────────────┘  └──────────────┘  └──────────────┘
```

### 3.4 WebSocket 重连策略

采用指数退避策略，避免短时间内大量重连请求：

```
重连间隔序列: 1s → 2s → 4s → 8s → 16s → 30s → 30s → 30s ...
最大重连间隔: 30s
重连次数无上限，直至用户主动断开或连接成功
```

重连实现使用协程（`SupervisorJob` + `Dispatchers.IO`），可通过 `reconnectJob.cancel()` 取消。

---

## 4. 核心算法设计

### 4.1 方位角计算（Haversine 公式）

```kotlin
fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val dLon = Math.toRadians(lon2 - lon1)
    val lat1Rad = Math.toRadians(lat1)
    val lat2Rad = Math.toRadians(lat2)

    val y = sin(dLon) * cos(lat2Rad)
    val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
    val bearing = Math.toDegrees(atan2(y, x))
    return ((bearing + 360) % 360).toFloat()
}
```

### 4.2 距离计算

```kotlin
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0]
}
```

### 4.3 指南针角度计算

最终指南针箭头角度 = 方位角（孩子方向） - 设备朝向（磁力计/加速度计融合）

```
显示角度 = (bearing - heading + 360) % 360
```

角度动画使用 `shortestAngle()` 计算最短旋转路径，避免跨越 0°/360° 时反向旋转 340°。

### 4.4 距离颜色映射

| 距离范围 | 颜色 | 含义 |
|:-------:|:----:|:-----|
| < 5m | 绿色 (#4CAF50) | 非常近 |
| < 20m | 浅绿 (#8BC34A) | 较近 |
| < 50m | 黄色 (#FFC107) | 适中 |
| < 200m | 橙色 (#FF9800) | 较远 |
| ≥ 200m | 红色 (#F44336) | 远 |

> 色值以 `CompassView.distanceColor()` 为准（该函数有单元测试锁定边界）。

---

## 5. 服务器设计

### 5.1 SessionManager

管理配对会话的生命周期：

- **会话模型**：`sessionId → Session(parentSession?, childSession?, createdAt, lastActiveAt)`
- **注册**：家长或孩子通过 `REGISTER` 消息加入会话，双方都注册后触发 `PAIRED`
- **位置转发**：识别发送者身份，将位置消息转发至对端
- **断开处理**：任一端断开时通知对端 `DISCONNECTED`，双方都断开时移除会话
- **过期清理**：每 5 分钟扫描，超过 30 分钟未活跃的空闲会话自动清除

### 5.2 WebSocketHandler

消息路由与处理：

```
接收消息 → JSON 解析 → 判断 type 字段 → 路由到对应处理器
  REGISTER → sessionManager.register()
  LOCATION_UPDATE → sessionManager.relayLocation()
  其他 → 返回 ERROR
```

---

## 6. 数据持久化设计

### 6.1 DataStore 存储

| Key | 类型 | 说明 |
|:---:|:----:|:-----|
| `is_parent` | Boolean | 是否为家长角色 |
| `has_selected_role` | Boolean | 是否已选择角色 |
| `session_id` | String | 配对会话 ID |
| `device_name` | String | 本机设备名称 |
| `paired_at` | Long | 配对时间戳 |
| `peer_device_name` | String | 对端设备名称 |
| `server_url` | String | 中继服务器地址（默认 `ws://10.0.2.2:8080/ws`）|

### 6.2 清除策略

- 用户主动"停止共享并退出"时调用 `clearAll()` 清除全部数据
- `clearAll()` 会清除角色、配对信息和服务器地址配置

---

## 7. 安全设计

### 7.1 当前状态

| 项目 | 状态 | 说明 |
|:----:|:----:|:-----|
| 服务器地址动态配置 | ✅ 已完成 | DataStore 持久化，运行时可修改（尚无设置界面） |
| 会话过期清理 | ✅ 已完成 | 30 分钟超时自动清除 |
| 相机权限请求流程 | ✅ 已完成 | 拒绝后 UI 反馈 + 重试 |
| 定位权限请求流程 | ✅ 已完成 | 家长端与孩子端均接入 `ui/permission/LocationPermissionGate` |
| WebSocket 断开清理 | ✅ 已完成 | 退出时正确断开连接和清除状态 |
| 冷启动连接恢复 | ✅ 已完成 | 两端 `ensureConnected()`，且 `connect()` 对同会话同角色幂等 |
| 消息协议 `type` 字段 | ✅ 已修复 | 服务端 `encodeDefaults = true`，见 3.2 的说明 |
| 未授权时定位降级 | ✅ 已完成 | `LocationProvider` 无权限时关闭 Flow 而非抛 `SecurityException` |

### 7.2 待实现

| 项目 | 优先级 | 说明 |
|:----:|:----:|:-----|
| WSS 加密传输 | P3 | 生产环境必须使用 `wss://` 替代 `ws://` |
| WebSocket 认证 | P3 | 连接时携带 token，服务器校验合法性 |
| QR 码签名与有效期 | P3 | QR 码内含签名和过期时间，防冒充 |
| 位置数据加密 | P3 | 位置消息端到端加密，中继服务器不可读 |

---

## 8. UI 设计

### 8.1 页面导航图

```
App 启动
  │
  ├─ 无角色 → RoleSelectionScreen
  │              ├─ 家长 → QrGenerationScreen ←─┐
  │              └─ 孩子 → QrScanScreen          │
  │                                                │
  ├─ 家长已配对 → CompassScreen                   │
  │                  ├─ 断线 → 显示连接状态       │
  │                  └─ 收到位置 → 更新指南针     │
  │                                                │
  └─ 孩子已配对 → ChildActiveScreen               │
                   └─ 停止共享 → 清除数据 → ──────┘
```

### 8.2 指南针组件

自定义 `Canvas` 绘制：

- 外圈刻度盘（每 30° 一个主刻度，每 15° 一个次刻度）
- N/S/E/W 方位标记
- 中心方向箭头（指向孩子方向）
- 中心距离文字（颜色随距离变化）
- 底部信息栏（方向描述、GPS 精度、最后更新时间）

### 8.3 主题

- 支持 Light / Dark 双主题
- Material 3 色彩体系
- 指南针配色在深色模式下自动适配

---

## 9. 性能设计

### 9.1 位置上报频率

- 孩子端定位服务：`PRIORITY_HIGH_ACCURACY`，间隔 5s，最快 2s（见 `LocationProvider`）
- `setWaitForAccurateLocation(true)`：等待高精度定位结果后再下发
- **尚未实现位移过滤**：每个回调都会上报。设计初衷是"变化超过 1 米才上报"以减少流量，
  当前版本没有这层过滤，属于待优化项

### 9.2 UI 刷新策略

- 指南针箭头旋转使用 `animateFloatAsState` + `FastOutSlowInEasing`
- 距离文字直接更新，无需动画
- 传感器采样间隔：`SENSOR_DELAY_GAME`（约 20ms），磁力计 + 加速度计融合后得到朝向

### 9.3 后台保活

- 前台服务 + 常驻通知栏
- `START_STICKY` 确保被系统回收后自动重启
- WebSocket 在 App 后台时保持连接，恢复前台后自动同步

---

## 10. 后续演进方向

| 方向 | 说明 | 优先级 |
|:----:|:-----|:-----:|
| 地图视图 | 指南针 + 地图双模式切换 | P3 |
| 位置历史轨迹 | 回放孩子运动轨迹 | P3 |
| 多子女追踪 | 支持同时追踪多个孩子 | P3 |
| 电子围栏 | 离开设定范围自动告警 | P3 |
| 通信层替换 | Firebase / MQTT 替代自建 WebSocket | P4 |
| 跨平台 | iOS 版本 (Kotlin Multiplatform) | P4 |
| 无障碍 | 适配 TalkBack，多语言国际化 | P4 |
