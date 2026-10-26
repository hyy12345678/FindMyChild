# 更新日志

本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/) 规范，版本号格式为 `主版本号.次版本号.修订号`。

> ⚠️ 项目尚处于早期开发阶段，版本号带 `-dev` 后缀表示尚未正式发布。

---

## [0.2.1-dev] - 2026-10-26

### 🐛 配对流程三连 bug

> 这三个问题串起来才是完整的事故：真机上点"我是家长"后二维码一闪而过，
> 孩子根本来不及扫；即使扫上，指南针页又会显示"连接已断开"而 socket 其实活着。

#### P0 — 拿"已连接"当"已配对"，二维码一闪而过

- `QrGenerationScreen` 用 `ConnectionState.Connected` 触发跳转，外加一个
  `delay(500)` 赌 `PAIRING_STATUS` 会在这 500ms 内到达
- 但 `Connected` 只说明**家长自己的 socket 打开了**。孩子还没扫码时，服务端回的是
  `PAIRING_STATUS(status = "WAITING")`——于是家长被立刻带去指南针页，二维码停留约 500ms
- 修复：新增 `PairingStatus` 领域模型（含 `fromWire()`，未知值按 `WAITING` 处理：
  宁可多等，也不要误跳），`SyncRepository.observePairingStatus()` 贯通到两端 UI，
  跳转只在 `PAIRED` 时发生
- 孩子端 `QrScanScreen` 有同样的乐观跳转（`uiState.isConnected` 由 `connect()` 返回后
  直接置 true），同样改为由 `PAIRED` 触发；`connect()` 返回时 socket 可能还没打开，
  更不能说明家长在不在线

#### P0 — `Channel(CONFLATED).receiveAsFlow()` 是单消费者

- `RelayWebSocketManager` 原来用三个 `Channel(CONFLATED).receiveAsFlow()` 暴露
  连接状态、对端位置、配对状态
- `receiveAsFlow()` 的元素**被取走就消失**：二维码页收掉 `Connected` 之后，
  指南针页再订阅只会停在初始值 `Disconnected`——界面显示"连接已断开"而 socket 其实活着
- 修复：`connectionState` 改 `MutableStateFlow`（本来就是"当前状态"）；
  `peerLocation` / `pairingStatus` 没有"空"的初始值，改 `MutableSharedFlow(replay = 1)`
  ——迟到的订阅者能补上最近一条，又不会因为一个占位 `null` 触发业务逻辑

#### P1 — `startDestination` 在会话中途被反复改写

- `StartupViewModel` 持续 `collect` `combine(getUserRole(), getPairingInfo())`，
  于是任何一次 DataStore 写入都会重算 `startDestination`：选角色（`null → PARENT`）、
  配对成功（`null → 有值`）、孩子断开（`clearAll`）都会触发
- `startDestination` **只在建图时生效**，挂上去之后再改不会把用户带到已配对的那一页，
  而反复改则会让 NavHost 重建、`QrGenerationViewModel` 重新创建并生成新 sessionId
  ——孩子扫到的码随即失效
- 修复：改为 `.first()` 一次性决定，`MainActivity` 在路由决定前显示加载态而非空白

#### P2 — 重新扫码会叠订阅、并继承上一次的配对状态

- `QrScanViewModel.onQrScanned()` 每次扫码都新起一个 `observePairingStatus()` 订阅，
  断开后重新扫就会叠上去，`_uiState` 被多个订阅者重复写入；改为用一个 `Job` 接管
- `pairingStatus` 是 `SharedFlow(replay = 1)`，上一次的 `DISCONNECTED` 会留在 replay 缓存里，
  让新的一次尝试一上来就报"家长设备已断开"；`connect()` 真正发起连接前
  `resetReplayCache()`，让状态只属于当前这次连接

#### P1 — 指南针页同样不观察配对状态，孩子离开后仍显示"已连接"

双端真机联调时把孩子端 `force-stop` 掉，家长端指南针页纹丝不动，依旧"已连接"。
`CompassViewModel` 只 `observeConnectionState()` + `observePeerLocation()`，没有
`observePairingStatus()`——而孩子切后台 / 关机 / 点"停止共享"时，家长自己的 socket
毫发无损，服务端发来的 `DISCONNECTED` 是唯一能看出"对方走了"的信号，却无人订阅。
这与上面第 1 条是同一类错误的另一个化身：把"自己这一侧的连接状态"当成"对方在不在"。

修复：`CompassViewModel` 在 `init` 里订阅配对状态（不放 `startObserving()`——那个由定位
权限就绪触发，而配对状态与定位权限无关），`ConnectionStatusBar` 改为配对状态优先。

### ✅ 新增测试覆盖

- `StartupViewModelTest`（8 例）：五种角色 × 配对状态的起始路由，外加两条回归护栏——
  会话中途保存配对信息 / 清除配对都**不得**改变已决定的路由
- `QrGenerationScreenTest`（4 例，Compose + Robolectric）：`WAITING` / `DISCONNECTED`
  不跳转，`PAIRED` 跳转，以及"先 WAITING 后 PAIRED"的真实时序
- `RelayWebSocketManagerTest` 补 5 例：后订阅者仍能拿到当前连接状态、
  最近一条配对状态、最近一条位置；未知配对状态按 `WAITING` 处理；
  重新建连不继承上一次的配对状态
- `CompassViewModelTest` 补 2 例：配对状态在 `init` 里就订阅（不等定位权限）；
  `disconnect` 清空配对状态

### 📊 验证

```
测试: 146 passed, 0 failed（app 124 + relay-server 22）
真机: 家长端停在二维码页，状态"已连接，等待小朋友扫码..."，持续 15s 不跳走
双端: 模拟器当家长 + 真机当孩子，扫码后双方正确跳转；位置链路与指南针几何正确
       （孩子在正东 ~1.1km → 显示"1.0 公里 / 正东方向"，箭头指向右、N 在顶部）
       三种配对状态均已验证：无孩子"等待小朋友连接..." / 孩子在"已连接" / 孩子离开"对方已断开"
```

---

## [0.2.0-dev] - 2026-10-23

### 🐛 严重缺陷修复

> 本轮问题大多在"第一次真正跑测试"时暴露。此前 0.2.0-dev 记录的编译验证用的是
> `assembleDebug` / `installDist`，这两个任务不跑测试，所以测试套件从未被验证过，
> CI 自建立起也一直是红的（`build` job 因依赖失败被跳过，从未执行）。

#### P0 — 服务端消息协议：`type` 字段缺失

- `WebSocketHandler` 的 `Json { ignoreUnknownKeys = true; isLenient = true }` 不编码带默认值的字段，
  而所有消息类型的 `type` 都有默认值，实际报文只有 `{"status":"WAITING"}`
- 客户端 `RelayWebSocketManager.handleMessage()` 纯按 `type` 路由，缺字段即整条丢弃
  → **家长端永远收不到 `PEER_LOCATION`，指南针永远显示"等待位置..."**
- 这也解释了 `QrGenerationScreen` 为何用 `delay(500)` + `Connected` 状态跳转（因为 `PAIRED` 根本收不到）
- 修复：显式 `encodeDefaults = true`，并在 DESIGN §3.2 记录这个陷阱

#### P0 — 定位权限从未申请

- 全 App 只申请了相机权限；`LocationProvider` 在 `@SuppressLint("MissingPermission")` 下调
  `requestLocationUpdates()`，未授权时抛 `SecurityException`
- 两个收集点分别在 `viewModelScope` 和 `lifecycleScope`，异常无人捕获 → 家长端一进指南针页即崩溃
- 新增 `ui/permission/LocationPermission.kt`：可复用的权限门，家长端与孩子端共用；
  拒绝后提供重试，首次拒绝后额外提供系统设置入口
- `LocationProvider` 增加降级：无权限时关闭 Flow 而非抛异常

#### P1 — 指南针箭头方向镜像

- `CompassView` 箭头旋转传了负号，与 DESIGN §4.3 公式矛盾，导致**东西方向反向**
  （目标在正东时箭头指向西，而文字显示"正东"）；正北/正南因 180° 对称所以一直没被发现
- 重构：刻度盘与箭头统一由 `screenAngleFromNorth(compassAngle, heading)` 推导，
  删除 `CompassState.arrowRotation`，旋转几何收敛到单一真值来源

#### P1 — 距离单位错配

- ≥1000m 时单位仍硬编码"米"，界面显示"1.2 米"
- 新增 `distanceUnit()`，与 `formatDistance()` 在同一个阈值上切换

#### P1 — 孩子端冷启动不重连

- 进程被杀后 `StartupViewModel` 凭本地配对信息直接路由到 `ChildActive`，
  但 `ChildActiveViewModel` 没有 `ensureConnected()`，WebSocket 未连接、`sessionId` 为空，
  位置消息被服务端当未知会话丢弃——页面却显示"位置共享中"
- 修复时发现 `connect()` 不幂等：扫码页连过一次后 `ensureConnected()` 会开第二个 socket，
  旧的被覆盖而不关闭，服务端第二次 REGISTER 报"该会话已有孩子设备"。
  已在 mutex 保护下对同会话同角色做幂等

#### P2 — `LocationService` 重复上报

- 原来在 `onStartCommand` 中 collect，而它每次 `startService` 都会回调，
  导致重复注册 `LocationCallback`、同一位置上报多次；已移至 `onCreate`

### 🧪 测试体系修复

15 个测试从未成功执行过（此前统计的 87 个用例中）：

| 类别 | 数量 | 根因 |
|------|----|------|
| `CompassViewTest` 全挂 | 7 | 缺 `@RunWith(RobolectricTestRunner::class)`，裸 JVM 上 `Build.FINGERPRINT` 为 null |
| `RelayServerE2ETest` 全挂 | 5 | test classpath 无 ktor client 引擎；且硬编码 `localhost:8080` 而 testApplication 用随机端口 |
| 断言与实现矛盾 | 2 | `normalizeAngle(180)`、`shortestAngle(180, 0)` 期望值写错 |
| mock 写法不成立 | 1 | `RelayConfig` 构造时即读取 `serverUrl` Flow，relaxed mock 不发射 |

E2E 测试改用 `testApplication` 的 `createClient` + 相对路径 `/ws`，不再需要额外客户端引擎依赖。
`app/build.gradle.kts` 增加 `debugImplementation(compose-ui-test-manifest)`。

#### P1 — 指南针页同样不观察配对状态，孩子离开后仍显示"已连接"

双端真机联调时把孩子端 `force-stop` 掉，家长端指南针页纹丝不动，依旧"已连接"。
`CompassViewModel` 只 `observeConnectionState()` + `observePeerLocation()`，没有
`observePairingStatus()`——而孩子切后台 / 关机 / 点"停止共享"时，家长自己的 socket
毫发无损，服务端发来的 `DISCONNECTED` 是唯一能看出"对方走了"的信号，却无人订阅。
这与上面第 1 条是同一类错误的另一个化身：把"自己这一侧的连接状态"当成"对方在不在"。

修复：`CompassViewModel` 在 `init` 里订阅配对状态（不放 `startObserving()`——那个由定位
权限就绪触发，而配对状态与定位权限无关），`ConnectionStatusBar` 改为配对状态优先。

### ✅ 新增测试覆盖

- `screenAngleFromNorth()`：四个基本方位 × 三种设备朝向、360° 跨界最短路径、取值范围、
  以及"换基往返一致性"不变量（这条才是符号翻转的守门测试）
- `distanceUnit()`：与 `formatDistance()` 阈值一致性
- `LocationPermissionTest`：权限判据（fine / coarse / 无关权限）+ 权限门三种状态
- `ChildActiveViewModelTest` / `CompassViewModelTest`：两端 `ensureConnected()`
- `RelayWebSocketManagerTest`：`connect()` 幂等性（同会话同角色只开一个 socket；
  不同会话 / 不同角色 / 断开后重连仍开新 socket）

### 📊 验证

```
测试: 125 passed, 0 failed（app 108 + relay-server 17）
CI:   3 jobs 全绿（Relay Server Tests / Android Unit Tests / Build Debug APK & Relay Server）
```

---

## [0.1.1-dev] - 2026-10-22

### Bug 修复与安全加固

#### P0 致命 Bug 修复

- **修复 `LocationProvider.getLastLocation()` 硬编码 null**：使用 `Tasks.await()` 获取真实 GPS 位置，替代原来的 `null` 硬编码
- **WebSocket 重连改为协程实现**：`RelayWebSocketManager` 的 `attemptReconnect()` 从 `Thread.sleep + runBlocking` 重构为 `CoroutineScope(SupervisorJob + Dispatchers.IO)` + `delay()`，可取消、线程安全，`webSocket` 字段用 `Mutex` 保护
- **`LocationService` 注入接口**：从 `SyncRepositoryImpl` 改为 `SyncRepository` 接口，恢复 Clean Architecture 分层隔离

#### P1 功能缺陷修复

- **`CompassViewModel` 主动连接**：新增 `ensureConnected()` 方法，已配对重开 App 时从 DataStore 读取配对信息并主动建立 WebSocket 连接
- **指南针角度动画最短路径**：新增 `shortestAngle()` 函数，通过 `remember` + `LaunchedEffect` 跟踪显示角度，避免跨越 0°/360° 时反向旋转 340°
- **`HeadingProvider` 空传感器保护**：检查磁力计/加速度计是否为 null，无传感器时 Flow 安全关闭而非崩溃
- **消除重复 DI 注册**：删除 `AppModule.provideSettingsDataStore()` 和 `SensorModule` 中的手动 `@Provides`，让 Hilt 自动注入

#### P2 安全与健壮性

- **SessionManager 会话过期清理**：添加定时清理任务（5 分钟间隔），超过 30 分钟未活跃的空闲会话自动清除
- **服务器地址动态配置**：`RelayConfig` 改为从 DataStore 读取服务器地址，支持运行时修改，无需重新编译
- **`ChildActiveScreen` 退出清理**：新增 `ChildActiveViewModel`，退出时正确断开 WebSocket、停止服务、清除本地状态
- **相机权限流程完善**：`QrScanScreen` 添加权限拒绝后的 UI 反馈和重试按钮

#### P3 代码清理

- 移除未使用的 Retrofit + converter-moshi 依赖
- 删除死代码 `QrGenerationViewModel.onPaired()`
- 清理 `MainActivity` 和 `AppNavGraph` 中的空回调
- `QrScanViewModel` 返回时调用 `cancelPairing()` 清理连接
- `QrGenerationScreen` 全限定名 `ButtonDefaults` 改为 import

#### 编译修复

- 修复 `WebSocketHandler.kt` 的 `decodeFromJsonElement` API 兼容性
- 修复 `SettingsDataStore.kt` 的 `!prefs[Boolean?]` 空安全编译错误
- 添加缺失的 launcher 图标资源（adaptive icon）

---

## [0.1.0-dev] - 2026-07-01

### 开发预览版

FindMyChild 首个开发预览版本，搭建了从配对到指南针追踪的完整代码框架，但各模块仍需进一步开发和测试，尚未达到生产可用状态。

### ✨ 新增

#### 客户端 (app)

- **角色选择**：支持「家长」与「小朋友」两种角色，选择后自动持久化，下次启动直接进入对应页面
- **QR 码配对**：
  - 家长端一键生成 UUID 会话并渲染二维码
  - 孩子端通过 CameraX 实时相机预览 + ZXing 解码扫描配对
  - 扫码即连，无需手动输入任何信息
- **实时指南针**：
  - 自定义 Canvas 绘制指南针组件（刻度盘 + N/S/E/W 方位标记 + 方向箭头）
  - 基于 Haversine 公式计算真北方位角与实时距离
  - 磁力计 + 加速度计融合获取设备朝向
  - 三路数据合并（我的位置 + 对端位置 + 设备朝向）驱动 UI 更新
  - 箭头旋转与刻度盘旋转均带平滑动画（FastOutSlowInEasing）
  - 距离颜色渐变提示（< 5m 绿 → < 20m 浅绿 → < 50m 黄 → < 200m 橙 → 红色）
  - 中文方向描述（正北、东北、正东……）
  - GPS 精度提示（信号弱时显示 ±精度值）
  - 最后更新时间倒计时显示
- **后台持续定位**：
  - 前台 LocationService 持续上报孩子 GPS 位置
  - 通知栏常驻"位置共享中"提示
  - `START_STICKY` 保证服务被系统回收后自动重启
- **WebSocket 通信**：
  - OkHttp WebSocket 连接中继服务器
  - 断线后指数退避自动重连（1s → 2s → 4s → … → 30s 上限）
  - Moshi JSON 序列化 / 反序列化
- **智能启动路由**：根据已保存的角色和配对信息自动决定起始页面
- **深色模式**：完整支持 Light / Dark 双主题

#### 中继服务器 (relay-server)

- **Ktor + Netty** 轻量级 WebSocket 中继服务器，默认端口 8080
- **会话管理**：`SessionManager` 管理配对会话，支持家长 / 孩子双端注册
- **自动配对检测**：双方注册完成后自动触发 PAIRED 通知
- **位置转发**：识别发送者身份，将位置消息转发至对端
- **断线通知**：任一端断开时自动通知对端 DISCONNECTED
- **协议定义**：`REGISTER` / `LOCATION_UPDATE` / `PAIRING_STATUS` / `PEER_LOCATION` / `ERROR` 五种消息类型

#### 基础架构

- **Clean Architecture** 分层（Domain → Data → UI）
- **MVVM** 模式 + Hilt 依赖注入
- **Repository 模式**：接口定义在 Domain 层，实现在 Data 层
- **DataStore Preferences** 本地持久化
- **Version Catalog** 统一依赖版本管理

### ⚠️ 已知问题

- `RelayWebSocketManager.attemptReconnect()` 使用 `Thread.sleep` + `runBlocking` 实现重连，应改为协程方式
- `LocationProvider.getLastLocation()` 当前实现直接返回 `null`，缺少真实的最后位置获取逻辑
- 中继服务器无认证机制，任何客户端均可加入任意会话
- `PairingRepositoryImpl.joinPairingSession()` 仅验证 sessionId 非空，缺少服务端校验
- 服务器地址硬编码在 `RelayConfig` 中，需手动修改后重新编译
- 缺少完整的权限请求流程与异常处理
- 尚无单元测试与集成测试

---

## 历史版本

| 版本 | 日期 | 说明 |
|:----:|:----:|:-----|
| [0.2.0-dev] | 2026-10-23 | 测试体系修复 + 服务端协议/指南针/定位权限等严重缺陷 |
| [0.1.1-dev] | 2026-10-22 | Bug 修复 + 安全加固 + 代码清理 |
| [0.1.0-dev] | 2026-07-01 | 开发预览版，搭建完整代码框架，尚未正式发布 |

[0.2.0-dev]: https://github.com/your-org/FindMyChild/releases/tag/v0.2.0-dev
[0.1.1-dev]: https://github.com/your-org/FindMyChild/releases/tag/v0.1.1-dev
[0.1.0-dev]: https://github.com/your-org/FindMyChild/releases/tag/v0.1.0-dev
