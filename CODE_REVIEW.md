# FindMyChild 代码走读报告

> 📌 **本文是对 0.1.0-dev 阶段代码的快照式走读**，写于后续修复之前。文中引用的行号、
> 代码片段和"当前行为"描述多数已过期——其中的问题基本都已在 v0.1.1-dev 及之后修复。
> 请把它当作"当时发现了什么"的历史记录，**不要**用它推断现在的代码状态；
> 现行状态以源码、[CHANGELOG.md](CHANGELOG.md) 和
> [doc/tracking/TASK_TRACKING.md](doc/tracking/TASK_TRACKING.md) 为准。
>
> 已知与现状不符的段落（节选）：
>
> - §配对流程走读：`LaunchedEffect(connectionState)` + `delay(500)` 的跳转方式、
>   `QrScanUiState(isConnected = true)`——两者都已在 v0.2.1-dev 改为由
>   `PAIRING_STATUS == PAIRED` 触发，`isConnected` 字段已删除
> - §RelayWebSocketManager：`peerLocationChannel` / `pairingStatusChannel` /
>   `connectionStateChannel` 已改为 `SharedFlow` / `StateFlow`
>   （`Channel(CONFLATED).receiveAsFlow()` 是单消费者，见 DESIGN §3.5）

## 一、整体评价

项目结构清晰，Clean Architecture + MVVM 分层到位，Domain 层零 Android 依赖可测试性好。作为 0.1.0-dev 阶段，功能骨架完整，但从"可用于紧急寻人"的标准来看，存在若干**必须修复的 Bug**和**安全隐患**。

---

## 二、严重问题 (Bug / 安全)

### 1. `LocationProvider.getLastLocation()` 永远返回 null — 致命 Bug

`LocationProvider.kt:57-65`：

```kotlin
suspend fun getLastLocation(): LocationData? {
    return try {
        val task = fusedClient.lastLocation
        val loc: Location? = null  // <-- 硬编码 null，task 结果被丢弃
        loc?.toLocationData()
    } catch (e: Exception) { null }
}
```

`task` 的结果从未被使用。需要用 `kotlinx-coroutines-play-services` 的 `await()` 或 `Tasks.await()` 获取实际位置。

### 2. WebSocket 重连使用 `Thread` + `runBlocking` — 协程泄漏 + 线程安全

`RelayWebSocketManager.kt:159-172`：

```kotlin
private fun attemptReconnect(sessionId: String, role: UserRole) {
    Thread {
        Thread.sleep(delay)
        runBlocking { connect(sessionId, role) }  // 在新线程阻塞式重连
    }.start()
}
```

- 在 OkHttp 回调线程中启动裸 Thread，无法取消、无法跟踪
- `runBlocking` 在非协程上下文中使用，违背项目协程风格
- 多次重连可能并发执行 `connect()`，`webSocket` 字段无同步保护
- **应改为协程 `delay` + `viewModelScope`/`CoroutineScope` 实现**

### 3. `LocationService` 依赖具体实现类型而非接口 — 违反依赖倒置

`LocationService.kt:27`：

```kotlin
@Inject lateinit var syncRepository: SyncRepositoryImpl  // 应为 SyncRepository
```

直接注入 `Impl` 类，绕过了 Hilt 的接口绑定，破坏 Clean Architecture 的分层隔离。

### 4. 服务器地址硬编码在代码中

`RelayConfig.kt:24`：`wsUrl` 硬编码为模拟器地址。生产环境用户无法修改。应改为 DataStore 持久化配置或 BuildConfig 注入。

### 5. 中继服务器无任何认证/加密

- WebSocket 连接无认证，任何人可猜测 sessionId 窃听/伪造位置
- 全部使用 `ws://` 明文传输，GPS 坐标可被中间人截获
- 孩子位置数据极度敏感，**生产环境必须用 `wss://` + token 认证**

### 6. QR 码仅包含裸 sessionId

`QrCodeUtil.kt` 生成的 QR 码内容仅为 UUID 字符串，无签名、无有效期。被拍照/截屏后可被第三方冒充孩子端加入会话。

---

## 三、架构设计问题

### 7. DI 模块重复注册 — `SettingsDataStore`

- `AppModule.kt:33-35` 用 `@Provides` 手动创建 `SettingsDataStore`
- 但 `SettingsDataStore` 本身有 `@Inject constructor` + `@Singleton`
- 两套注册会冲突或造成困惑，应删除 `AppModule.provideSettingsDataStore()`，让 Hilt 自动注入
- 同理 `SensorModule` 中的 `LocationProvider` / `HeadingProvider` 也有 `@Inject constructor`，`@Provides` 多余

### 8. `CompassViewModel` 未处理连接恢复逻辑

- 配对成功进入指南针页后，如果 `StartupViewModel` 判断已有配对信息直接跳转 `Compass`，但 `CompassViewModel.init` 只调用 `observeData()`，**没有主动 `connect()` WebSocket**
- 结果：已配对重开 App 时，指南针页面无法收到对端位置

### 9. `QrGenerationViewModel.onPaired()` 方法存在但从未被调用

`QrGenerationViewModel.kt:73-82`：`onPaired()` 方法在内部又 `collect` 了连接状态，但 Screen 层已经用 `LaunchedEffect` 监听了 `connectionState`，这个方法是死代码，且会导致重复 collect。

### 10. `QrScanViewModel` 缺少 `disconnect()` 调用

当用户从扫码页按"返回"时（`onBack`），只导航回角色选择页，但没有调用 `syncRepository.disconnect()` 和 `settingsRepository.clearAll()`，WebSocket 连接可能仍存活。

---

## 四、健壮性问题

### 11. `CompassView` 动画角度跨越 0°/360° 会反向旋转

`animateFloatAsState` 从 350° → 10° 时会逆时针转 340° 而非顺时针转 20°。需要对角度变化做最短路径插值（检查差值 > 180° 则取补角）。

### 12. `CompassView` 文字叠加在 Canvas 之上但居中对齐

距离文字居中于整个 Box，但 Canvas 的指南针中心不一定等于 Box 中心（`canvasSize = min(w,h) * 0.85f`），宽高不等时文字与指南针中心偏移。

### 13. `HeadingProvider` 未处理设备无磁力计的情况

`sensorManager.getDefaultSensor()` 可能返回 `null`，此时 `registerListener` 会崩溃。需要增加 null 检查并降级处理。

### 14. `SessionManager` 无会话过期清理

`ConcurrentHashMap` 中的 session 永不过期。如果家长生成 QR 后不扫码，session 永远残留。应加定时清理机制（如 30 分钟过期）。

### 15. `relayLocation()` 中 `==` 比较 WebSocketSession 引用

`SessionManager.kt:92-96`：用 `==` 比较 `fromSession` 和 `parentSession/childSession`，依赖引用相等。Ktor 的 WebSocketSession 实现是否保证同一连接同一引用需确认，否则应用 sessionId+role 标识发送者。

---

## 五、代码风格 / 小问题

| # | 位置 | 问题 |
|---|------|------|
| 16 | `CompassView.kt:329-334` | `distanceColor()` 中的颜色值与 `Color.kt` 中定义的 `CompassGreen` 等重复，应统一引用 |
| 17 | `QrGenerationScreen.kt:161` | 使用全限定名 `androidx.compose.material3.ButtonDefaults` 而顶部已有 import |
| 18 | `ChildActiveScreen.kt` | 放在 `ui/navigation/` 包下，应放在 `ui/child/` 或 `ui/pairing/child/` |
| 19 | `MainActivity.kt:37` | `onRoleSelected = { role -> }` 空回调，回调逻辑实际在 NavGraph 中处理，应清理 |
| 20 | `app/build.gradle.kts:89-90` | 引入了 `retrofit` + `retrofit-moshi` 但项目未使用 HTTP REST，仅用 OkHttp WebSocket，多余依赖 |
| 21 | `QrScanScreen.kt:39` | `LocalLifecycleOwner.current` 已 deprecated，应使用 `LifecycleOwnerProvider` 或 `LocalLifecycleOwner` 的新 API |

---

## 六、亮点

- `CalculateBearingUseCase` 纯函数设计，Haversine/方位角计算可单元测试
- `ConnectionState` sealed class 设计清晰
- 指南针 Canvas 绘制层次分明，深色模式下可读性好
- 距离颜色直觉化（绿→红）是 UX 好决策
- QR 扫码配对流程简洁，30 秒可达

---

## 七、优先修复建议

| 优先级 | 事项 |
|--------|------|
| **P0** | 修复 `getLastLocation()` 硬编码 null |
| **P0** | WebSocket 重连改为协程实现 |
| **P0** | `LocationService` 注入 `SyncRepository` 接口而非 Impl |
| **P1** | 已配对重开 App 时 CompassViewModel 主动 connect |
| **P1** | 角度动画跨越 0°/360° 的最短路径处理 |
| **P1** | 消除重复 DI 注册（SettingsDataStore/SensorModule） |
| **P2** | 服务器地址动态配置 |
| **P2** | 中继服务器加 wss + 认证 |
| **P2** | SessionManager 会话过期清理 |

---

## 八、运行时代码走读（入口→指南针）

### 第1步：应用启动入口

**`FindMyChildApp.kt`** — Application 类

```kotlin
@HiltAndroidApp
class FindMyChildApp : Application()
```

这是整个 App 的起点。`@HiltAndroidApp` 触发 Hilt 依赖注入框架的初始化，生成依赖图。此时 Hilt 会扫描所有 `@Module` 并准备好依赖：

- `AppModule`：提供 Moshi、OkHttpClient、SettingsDataStore
- `RepositoryModule`：绑定 4 个 Repository 接口到实现类
- `SensorModule`：提供 LocationProvider、HeadingProvider

App 启动后，Android 框架创建 `MainActivity`。

### 第2步：MainActivity — 第一个 Activity

**`MainActivity.kt:21-43`**

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FindMyChildTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val startupViewModel: StartupViewModel = hiltViewModel()
                    val startupRoute by startupViewModel.startupRoute.collectAsState()

                    AppNavGraph(
                        navController = navController,
                        startDestination = startupRoute,
                        onRoleSelected = { role -> }
                    )
                }
            }
        }
    }
}
```

关键流程：

1. `@AndroidEntryPoint` — Hilt 向此 Activity 注入依赖
2. `enableEdgeToEdge()` — 全屏沉浸式
3. `FindMyChildTheme` — 根据系统深色模式选择 Light/Dark 配色
4. **`StartupViewModel`** — 决定首页路由
5. `AppNavGraph` — 导航图，`startDestination` 由 StartupViewModel 动态决定

### 第3步：StartupViewModel — "用户该去哪个页面？"

**`StartupViewModel.kt:17-65`**

```kotlin
@HiltViewModel
class StartupViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _startupRoute = MutableStateFlow(NavRoute.RoleSelection.route)
    val startupRoute: StateFlow<String> = _startupRoute.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.getUserRole(),      // Flow<UserRole?>
                settingsRepository.getPairingInfo()     // Flow<PairingInfo?>
            ) { role, pairingInfo ->
                determineStartDestination(role, pairingInfo)
            }.collect { route ->
                _startupRoute.value = route
            }
        }
    }
```

决策逻辑：

| 角色 | 配对信息 | 跳转 |
|------|---------|------|
| null | — | `RoleSelection`（首次使用） |
| PARENT | 有 | `Compass`（直接进指南针） |
| PARENT | 无 | `ParentQrGeneration`（去生成二维码） |
| CHILD | 有 | `ChildActive`（直接进共享状态页） |
| CHILD | 无 | `ChildQrScan`（去扫码） |

这些数据从 `SettingsDataStore` 来，它用 DataStore Preferences 持久化在本地：

```
HAS_SELECTED_ROLE: Boolean  →  是否选过角色
IS_PARENT: Boolean          →  是不是家长
SESSION_ID: String          →  配对会话 ID
DEVICE_NAME: String         →  设备名
PAIRED_AT: Long             →  配对时间
PEER_DEVICE_NAME: String    →  对端设备名
```

### 第4步：AppNavGraph — 导航骨架

**`AppNavGraph.kt:22-112`**

5 个路由页面：

```
RoleSelection  →  ParentQrGeneration  →  Compass
               →  ChildQrScan         →  ChildActive
```

导航策略：每次角色切换都 `popUpTo` 清空栈，用户不能"返回"到前一个状态（因为角色流程是单向的）。

### 第5步：RoleSelectionScreen — "你是家长还是小朋友？"

**`RoleSelectionScreen.kt:29-120`**

两个大按钮：

- "我是家长" → `viewModel.selectRole(PARENT)` → 保存角色 → 导航到 `ParentQrGeneration`
- "我是小朋友" → `viewModel.selectRole(CHILD)` → 保存角色 → 导航到 `ChildQrScan`

**`RoleSelectionViewModel.kt`**：

```kotlin
fun selectRole(role: UserRole, onComplete: () -> Unit) {
    viewModelScope.launch {
        settingsRepository.setUserRole(role)  // 写入 DataStore
        onComplete()                          // 通知 UI 跳转
    }
}
```

### 第6步（家长端）：QrGenerationViewModel — 生成二维码 + 建立 WebSocket

**`QrGenerationViewModel.kt:33-63`**

```kotlin
private fun generateSession() {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)

        // 1. 生成 UUID 作为会话 ID
        val pairingInfo = pairingRepository.createPairingSession("家长设备")

        // 2. 保存配对信息到本地
        settingsRepository.savePairingInfo(pairingInfo)

        // 3. 用 sessionId 生成二维码图片
        val bitmap = qrCodeUtil.generateQrCode(pairingInfo.sessionId)
        _uiState.value = QrGenerationUiState(
            sessionId = pairingInfo.sessionId,
            qrBitmap = bitmap,
            isLoading = false
        )

        // 4. 连接到中继服务器，注册为 PARENT 角色
        syncRepository.connect(pairingInfo.sessionId, UserRole.PARENT)

        // 5. 监听连接状态
        launch {
            syncRepository.observeConnectionState().collect { state ->
                _connectionState.value = state
            }
        }
    }
}
```

### 第7步（家长端）：QrGenerationScreen — 展示二维码

**`QrGenerationScreen.kt:41-170`**

- 展示 QR 码卡片
- 显示连接状态（未连接/正在连接/已连接/出错）
- `LaunchedEffect(connectionState)` — 一旦 `Connected`，延迟 500ms 后调用 `onPaired()` 跳转指南针

### 第8步（孩子端）：QrScanScreen — 扫码

**`QrScanScreen.kt:48-194`**

1. `LaunchedEffect(Unit)` — 检查/请求 CAMERA 权限
2. 权限通过 → `viewModel.startScanning()`
3. CameraX 绑定到 `PreviewView` + `ImageAnalysis`
4. 每帧图像传给 `viewModel.analyzeQrImage(imageProxy)`

### 第9步（孩子端）：QrScanViewModel — 解码 + 连接

**`QrScanViewModel.kt:39-105`**

```kotlin
fun analyzeQrImage(imageProxy: ImageProxy) {
    if (hasScanned) { imageProxy.close(); return }  // 防止重复扫描

    // 1. 从 YUV 数据构建 ZXing BinaryBitmap
    val source = PlanarYUVLuminanceSource(data, w, h, ...)
    val result = qrReader.decode(BinaryBitmap(HybridBinarizer(source)))

    // 2. 解码成功 → hasScanned = true → onQrScanned(sessionId)
}

private fun onQrScanned(sessionId: String) {
    viewModelScope.launch {
        // 3. 加入配对（本地验证 sessionId 非空）
        val result = pairingRepository.joinPairingSession(sessionId, "孩子设备")

        // 4. 保存配对信息
        settingsRepository.savePairingInfo(pairingInfo)

        // 5. 连接到中继服务器，注册为 CHILD 角色
        syncRepository.connect(sessionId, UserRole.CHILD)

        // 6. 标记已连接
        _uiState.value = QrScanUiState(isConnected = true)
    }
}
```

`QrScanScreen` 的 `LaunchedEffect(uiState.isConnected)` 检测到 `isConnected = true` 后：

- 启动 `LocationService`（前台定位服务）
- 跳转到 `ChildActive` 页面

### 中继服务器端：配对过程

**SessionManager.register()**：

```
家长先到：session.childSession == null → 返回 Waiting，发 PAIRING_STATUS=WAITING
孩子后到：session.parentSession != null → 返回 Paired，发 PAIRING_STATUS=PAIRED 给双方
```

WebSocketHandler 在 `Paired` 时：

- 给新注册者发 PAIRED
- 给已在等待的对端发 PAIRED

此时家长端 `connectionState` 变为 `Connected` → 触发跳转指南针。

### 第10步（家长端）：CompassViewModel 指南针核心

**`CompassViewModel.kt:22-79`**

```kotlin
private fun observeData() {
    viewModelScope.launch {
        // 监听连接状态
        launch {
            syncRepository.observeConnectionState().collect { ... }
        }

        // 合并三路 Flow
        launch {
            combine(
                locationRepository.observeMyLocation(),    // 我的 GPS
                syncRepository.observePeerLocation(),      // 孩子的 GPS（从中继来）
                locationRepository.observeDeviceHeading()  // 我的手机朝向
            ) { myLoc, peerLoc, heading ->
                Triple(myLoc, peerLoc, heading)
            }.collect { (myLoc, peerLoc, heading) ->
                val bearing = calculateBearing.bearing(myLoc, peerLoc)
                val distance = calculateBearing.distance(myLoc, peerLoc)
                val arrowRotation = calculateBearing.normalizeAngle(bearing - heading)

                _compassState.value = CompassState(
                    bearing = bearing,
                    heading = heading,
                    arrowRotation = arrowRotation,
                    distance = distance,
                    lastUpdateTime = peerLoc.timestamp,
                    isTargetFound = true,
                    locationAccuracy = peerLoc.accuracy
                )
            }
        }
    }
}
```

三路数据来源：

| Flow | 来源 |
|------|------|
| `observeMyLocation()` | `LocationProvider` → FusedLocation 5秒一次回调 → `callbackFlow` |
| `observePeerLocation()` | `RelayWebSocketManager.peerLocationChannel` ← 中继服务器推送 `PEER_LOCATION` |
| `observeDeviceHeading()` | `HeadingProvider` → 磁力计+加速度计 → `SensorManager.getOrientation()` → 方位角 |

### 第11步（家长端）：CompassView — Canvas 绘制

**`CompassView.kt:60-207`**

视觉层次（从底到顶）：

```
1. 深色背景圆 (CompassDialBg)
2. 刻度盘 — 随设备朝向旋转 (animatedDialRotation = -heading)
   - 每 5° 一个刻度，每 30° 加粗，每 90° 红色 + N/S/E/W 文字
3. 方向箭头 — 旋转角度 = -arrowRotation (animatedArrowRotation)
   - 颜色随距离变化 (绿<5m → 红>200m)
4. 中心白点 + 彩色内点
5. 文字叠加：距离数字 + "米" + 方向描述 + 更新时间
```

关键动画：

- `animatedArrowRotation`：`animateFloatAsState(500ms, FastOutSlowInEasing)` — 箭头平滑转向
- `animatedDialRotation`：`animateFloatAsState(300ms)` — 刻度盘随手机转动

### 第12步（孩子端）：LocationService 后台定位

**`LocationService.kt:24-84`**

```kotlin
@AndroidEntryPoint
class LocationService : LifecycleService() {
    @Inject lateinit var locationProvider: LocationProvider
    @Inject lateinit var syncRepository: SyncRepositoryImpl

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())  // 前台通知
    }

    private fun startLocationReporting() {
        lifecycleScope.launch {
            locationProvider.observeLocation().collectLatest { location ->
                syncRepository.sendLocation(location)  // 每次位置更新 → 发送到中继
            }
        }
    }
}
```

- `START_STICKY`：系统杀掉后自动重启
- 前台通知："位置共享中"，用户知道服务在跑
- 位置更新 → `sendLocation()` → OkHttp WebSocket → 中继服务器 → 转发给家长端

### 第13步（孩子端）：ChildActiveScreen

**`ChildActiveScreen.kt:34-99`**

简单的状态页：

- 绿色勾 + "位置共享中"
- "停止共享并退出"按钮 → `context.stopService()` → `onDisconnected()` → 导航回角色选择

### 完整数据流总结

```
孩子端 GPS (5s)
    ↓
LocationService.collectLatest
    ↓
SyncRepository.sendLocation()
    ↓
RelayWebSocketManager.sendLocation() → OkHttp WebSocket → JSON
    ↓
中继服务器 WebSocketHandler
    ↓ decode LOCATION_UPDATE
SessionManager.relayLocation() → 找到对端 session
    ↓ encode PEER_LOCATION
send() → 家长端 WebSocket
    ↓
RelayWebSocketManager.onMessage → peerLocationChannel
    ↓
SyncRepository.observePeerLocation() Flow
    ↓
CompassViewModel.combine(我的位置, 孩子位置, 设备朝向)
    ↓
CalculateBearingUseCase → bearing + distance + arrowRotation
    ↓
CompassState → CompassView → Canvas 绘制
```
