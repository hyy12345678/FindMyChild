# 任务执行跟踪 — FindMyChild

> 版本: 0.2.1-dev | 最后更新: 2026-10-26

---

## 1. 总览

| 里程碑 | 任务总数 | 已完成 | 进行中 | 待开始 | 完成率 |
|:------:|:--------:|:------:|:------:|:------:|:------:|
| v0.1.0-dev 框架搭建 | 15 | 15 | 0 | 0 | 100% |
| v0.1.1-dev Bug 修复与安全加固 | 16 | 16 | 0 | 0 | 100% |
| v0.2.0-dev 测试与质量保障 | 9 | 9 | 0 | 0 | 100% |
| v0.2.1-dev 配对流程缺陷修复 | 8 | 8 | 0 | 0 | 100% |
| v0.3.0-dev 安全加固与生产化 | 8 | 0 | 0 | 8 | 0% |
| v0.4.0-dev 功能扩展 | 7 | 0 | 0 | 7 | 0% |
| v0.5.0-dev 跨平台与通信层升级 | 4 | 0 | 0 | 4 | 0% |
| **合计** | **67** | **48** | **0** | **19** | **71.6%** |

---

## 2. v0.1.0-dev 框架搭建 — 执行记录

> 时间范围: 2026-06 ~ 2026-07-01 | 状态: ✅ 全部完成

| 编号 | 任务名称 | 优先级 | 状态 | 完成日期 | 备注 |
|:----:|:---------|:------:|:----:|:--------:|:-----|
| M0-01 | Clean Architecture 分层架构搭建 | P0 | ✅ | 2026-06-15 | Domain / Data / UI 三层分离 |
| M0-02 | Hilt 依赖注入配置 | P0 | ✅ | 2026-06-15 | AppModule + SensorModule |
| M0-03 | 角色选择页面 + 持久化 | P0 | ✅ | 2026-06-18 | DataStore 存储角色选择 |
| M0-04 | QR 码生成（家长端） | P0 | ✅ | 2026-06-20 | UUID + ZXing 生成二维码 |
| M0-05 | QR 码扫描（孩子端） | P0 | ✅ | 2026-06-25 | CameraX + ZXing 解码 |
| M0-06 | WebSocket 中继服务器 | P0 | ✅ | 2026-06-22 | Ktor + Netty，端口 8080 |
| M0-07 | WebSocket 客户端管理器 | P0 | ✅ | 2026-06-23 | OkHttp WebSocket + Moshi |
| M0-08 | 配对流程与 SessionManager | P0 | ✅ | 2026-06-25 | 双端注册 → PAIRED 通知 |
| M0-09 | GPS 定位服务 | P0 | ✅ | 2026-06-27 | 前台服务 + FusedLocation |
| M0-10 | 磁力计 + 加速度计传感器融合 | P0 | ✅ | 2026-06-28 | SensorManager 朝向计算 |
| M0-11 | 指南针自定义 Canvas 组件 | P0 | ✅ | 2026-06-30 | 刻度盘 + 箭头 + 距离 |
| M0-12 | Haversine 方位角与距离计算 | P0 | ✅ | 2026-06-29 | 标准公式实现 |
| M0-13 | 智能启动路由 | P1 | ✅ | 2026-06-30 | StartupViewModel 路由决策 |
| M0-14 | 深色模式支持 | P1 | ✅ | 2026-07-01 | Material 3 动态主题 |
| M0-15 | Version Catalog 依赖管理 | P1 | ✅ | 2026-06-15 | libs.versions.toml |

### 遗留问题（v0.1.0-dev）

| 问题 | 严重性 | 在 v0.1.1-dev 中的处理 |
|:-----|:------:|:----------------------|
| `getLastLocation()` 硬编码返回 null | P0 致命 | ✅ P0-01 已修复 |
| WebSocket 重连用 Thread.sleep + runBlocking | P0 安全 | ✅ P0-02 已修复 |
| `LocationService` 注入实现类而非接口 | P0 架构 | ✅ P0-03 已修复 |
| 重开 App 不主动恢复连接 | P1 功能 | ✅ P1-01 已修复 |
| 指南针动画跨越 0°/360° 反向旋转 | P1 体验 | ✅ P1-02 已修复 |
| 无磁力计设备崩溃 | P1 稳定性 | ✅ P1-03 已修复 |
| DI 重复注册 | P1 架构 | ✅ P1-04 已修复 |
| 服务器地址硬编码 | P2 灵活性 | ✅ P2-02 已修复 |
| SessionManager 无过期清理 | P2 安全 | ✅ P2-01 已修复 |
| 孩子退出不清理状态 | P2 安全 | ✅ P2-03 已修复 |
| 权限拒绝无 UI 反馈 | P2 体验 | ✅ P2-04 已修复 |
| 多余 Retrofit 依赖 | P3 清理 | ✅ P3-01 已修复 |
| 死代码 `onPaired()` | P3 清理 | ✅ P3-02 已修复 |
| 编译错误（多个） | P3 编译 | ✅ P3-05 已修复 |

---

## 3. v0.1.1-dev Bug 修复与安全加固 — 执行记录

> 时间范围: 2026-10-22 | 状态: ✅ 全部完成

### 3.1 P0 致命 Bug 修复

| 编号 | 任务名称 | 优先级 | 状态 | 完成日期 | 修改文件 | 备注 |
|:----:|:---------|:------:|:----:|:--------:|:--------|:-----|
| P0-01 | 修复 `getLastLocation()` 硬编码 null | P0 | ✅ | 2026-10-22 | `LocationProvider.kt` | 改用 `Tasks.await(fusedClient.lastLocation)` |
| P0-02 | WebSocket 重连改为协程实现 | P0 | ✅ | 2026-10-22 | `RelayWebSocketManager.kt` | `SupervisorJob` + `delay()` 替代 `Thread.sleep` + `runBlocking` |
| P0-03 | `LocationService` 注入接口 | P0 | ✅ | 2026-10-22 | `LocationService.kt` | `SyncRepositoryImpl` → `SyncRepository` |

### 3.2 P1 功能缺陷修复

| 编号 | 任务名称 | 优先级 | 状态 | 完成日期 | 修改文件 | 备注 |
|:----:|:---------|:------:|:----:|:--------:|:--------|:-----|
| P1-01 | `CompassViewModel` 主动连接 | P1 | ✅ | 2026-10-22 | `CompassViewModel.kt` | 新增 `ensureConnected()` |
| P1-02 | 指南针角度最短路径 | P1 | ✅ | 2026-10-22 | `CompassView.kt` | 新增 `shortestAngle()` |
| P1-03 | `HeadingProvider` 空传感器保护 | P1 | ✅ | 2026-10-22 | `HeadingProvider.kt` | 磁力计/加速度计 null 检查 |
| P1-04 | 消除重复 DI 注册 | P1 | ✅ | 2026-10-22 | `AppModule.kt` / `SensorModule.kt` | Hilt 自动注入 |

### 3.3 P2 安全与健壮性

| 编号 | 任务名称 | 优先级 | 状态 | 完成日期 | 修改文件 | 备注 |
|:----:|:---------|:------:|:----:|:--------:|:--------|:-----|
| P2-01 | SessionManager 会话过期清理 | P2 | ✅ | 2026-10-22 | `SessionManager.kt` | 5 分钟间隔扫描，30 分钟超时 |
| P2-02 | 服务器地址动态配置 | P2 | ✅ | 2026-10-22 | `RelayConfig.kt` / `SettingsDataStore.kt` / `SettingsRepository.kt` | DataStore 持久化 |
| P2-03 | `ChildActiveScreen` 退出清理 | P2 | ✅ | 2026-10-22 | `ChildActiveScreen.kt` | 新增 `ChildActiveViewModel` |
| P2-04 | 完善相机权限流程 | P2 | ✅ | 2026-10-22 | `QrScanScreen.kt` | 拒绝后显示提示 + 重试按钮 |

### 3.4 P3 代码清理

| 编号 | 任务名称 | 优先级 | 状态 | 完成日期 | 修改文件 | 备注 |
|:----:|:---------|:------:|:----:|:--------:|:--------|:-----|
| P3-01 | 移除多余 Retrofit 依赖 | P3 | ✅ | 2026-10-22 | `app/build.gradle.kts` | 已用 OkHttp 替代 |
| P3-02 | 删除死代码 | P3 | ✅ | 2026-10-22 | `QrGenerationViewModel.kt` | 删除 `onPaired()` |
| P3-03 | `QrScanViewModel` disconnect | P3 | ✅ | 2026-10-22 | `QrScanViewModel.kt` | 返回时调用 `cancelPairing()` |
| P3-04 | 清理全限定名 | P3 | ✅ | 2026-10-22 | `QrGenerationScreen.kt` | `ButtonDefaults` 改为 import |
| P3-05 | 编译修复 | P3 | ✅ | 2026-10-22 | 多文件 | 见下方详细记录 |

### 3.5 编译修复详细记录

| 编号 | 错误描述 | 修复方案 | 修改文件 |
|:----:|:---------|:--------|:--------|
| B-01 | `WebSocketHandler.kt` `decodeFromJsonElement` 签名不匹配 | 显式传入 `serializer()` | `WebSocketHandler.kt` |
| B-02 | `SettingsDataStore.kt` `!prefs[Boolean?]` 空安全错误 | 改为 `prefs[...] != true` | `SettingsDataStore.kt` |
| B-03 | `LocationProvider.kt` `await()` 未解析 | 改为 `Tasks.await()` | `LocationProvider.kt` |
| B-04 | `AndroidManifest.xml` 找不到 `ic_launcher` | 补充 adaptive icon 资源 | `res/mipmap-anydpi-v26/ic_launcher.xml` |
| B-05 | `SettingsDataStore` 双 companion object 编译错误 | 合并到 `companion object Keys` | `SettingsDataStore.kt` |

### 3.6 编译验证

| 验证时间 | 命令 | 结果 |
|:--------:|:-----|:----:|
| 2026-10-22 | `./gradlew :app:assembleDebug :relay-server:installDist` | ✅ BUILD SUCCESSFUL |

---

## 4. v0.2.0-dev 测试与质量保障 — 执行记录

> 状态: ✅ 全部完成 | 完成日期: 2026-10-22

### 4.1 测试框架搭建

| 项目 | 说明 |
|:-----|:-----|
| 测试框架 | JUnit 4 + MockK + Turbine + Robolectric |
| Android UI 测试 | Compose UI Test (createComposeRule) |
| Ktor 测试 | ktor-server-test-host |
| CI/CD | GitHub Actions (relay-server-test + android-unit-test + build) |
| 新增依赖 | junit, mockk, coroutines-test, robolectric, turbine, ktor-server-test-host, compose-ui-test-junit4 |
| 构建配置 | `testOptions.unitTests.isIncludeAndroidResources = true` (Robolectric 支持) |

### 4.2 任务执行记录

| 编号 | 任务名称 | 优先级 | 状态 | 完成日期 | 测试文件 | 测试用例数 | 备注 |
|:----:|:---------|:------:|:----:|:--------:|:--------|:---------:|:-----|
| M2-01 | Haversine 方位角计算单元测试 | P1 | ✅ | 2026-10-22 | `CalculateBearingUseCaseTest.kt` | 8 | 正北/正东/正南/正西/对角线/范围验证/同位置 |
| M2-02 | Haversine 距离计算单元测试 | P1 | ✅ | 2026-10-22 | `CalculateBearingUseCaseTest.kt` | 6 | 零距离/1°纬度/1°经度/对称性/短距离/长距离 |
| M2-03 | `shortestAngle()` 单元测试 | P1 | ✅ | 2026-10-22 | `CompassViewUtilTest.kt` | 8+7+5+8 | shortestAngle + distanceColor + formatDistance + bearingToText |
| M2-04 | `SessionManager` 单元测试 | P1 | ✅ | 2026-10-22 | `SessionManagerTest.kt` | 15 | 注册/配对/重复注册/无效角色/位置转发/断开/多会话隔离 |
| M2-05 | `SettingsDataStore` 单元测试 | P2 | ✅ | 2026-10-22 | `SettingsDataStoreTest.kt` | 13 | 角色读写/配对信息读写/服务器地址/clearAll (Robolectric) |
| M2-06 | `RelayWebSocketManager` 集成测试 | P2 | ✅ | 2026-10-22 | `RelayWebSocketManagerTest.kt` | 4 | disconnect状态/多次disconnect/connect后disconnect/Flow测试 |
| M2-07 | relay-server 端到端测试 | P2 | ✅ | 2026-10-22 | `RelayServerE2ETest.kt` | 6 | WAITING/PAIRED/位置转发/重复注册/无效角色 (Ktor testApplication) |
| M2-08 | UI 测试 | P2 | ✅ | 2026-10-22 | `CompassViewTest.kt` | 7 | 等待状态/距离显示/方向描述/GPS精度/公里格式/更新时间/提示文案 |
| M2-09 | CI/CD 流水线配置 | P2 | ✅ | 2026-10-22 | `.github/workflows/ci.yml` | — | 3 个 Job: relay-server-test / android-unit-test / build |

### 4.3 测试统计

| 测试文件 | 测试用例数 | 覆盖模块 |
|:--------|:---------:|:--------|
| `CalculateBearingUseCaseTest.kt` | 20 | Domain (方位角 + 距离 + normalizeAngle) |
| `CompassViewUtilTest.kt` | 39 | UI 几何与工具 (screenAngleFromNorth + shortestAngle + distanceColor + formatDistance + distanceUnit + bearingToText) |
| `SessionManagerTest.kt` | 17 | relay-server (注册/配对/转发/断开) |
| `SettingsDataStoreTest.kt` | 14 | Data (角色/配对/服务器地址/clearAll) |
| `RelayWebSocketManagerTest.kt` | 15 | Data (连接状态 / disconnect / connect 幂等 / Flow 迟到订阅) |
| `RelayServerE2ETest.kt` | 5 | relay-server (端到端通信) |
| `CompassViewTest.kt` | 7 | UI (CompassView 渲染) |
| `LocationPermissionTest.kt` | 7 | UI (定位权限判据 + 权限门三态) |
| `ChildActiveViewModelTest.kt` | 5 | UI (冷启动重连 / 连接状态) |
| `CompassViewModelTest.kt` | 5 | UI (ensureConnected / 配对状态订阅) |
| `StartupViewModelTest.kt` | 8 | UI (起始路由决策 + 会话中途不改变路由) |
| `QrGenerationScreenTest.kt` | 4 | UI (配对状态驱动跳转) |
| **合计** | **146** | — |

> ⚠️ 0.2.0-dev 首次记录时写的是 87 个用例，那个数字没有跑通过——其中 15 个从未成功执行过
> （见第 10 节）。上表是 2026-10-23 修复后重新统计的真实值。

### 4.4 代码变更

| 变更类型 | 文件 | 说明 |
|:--------|:-----|:-----|
| 新增测试 | `CalculateBearingUseCaseTest.kt` | 方位角/距离/normalizeAngle 单元测试 |
| 新增测试 | `CompassViewUtilTest.kt` | shortestAngle/distanceColor/formatDistance/bearingToText 单元测试 |
| 新增测试 | `SessionManagerTest.kt` | SessionManager 注册/配对/转发/断开单元测试 |
| 新增测试 | `SettingsDataStoreTest.kt` | SettingsDataStore 读写/清除单元测试 (Robolectric) |
| 新增测试 | `RelayWebSocketManagerTest.kt` | RelayWebSocketManager 集成测试 |
| 新增测试 | `RelayServerE2ETest.kt` | relay-server 端到端测试 (Ktor testApplication) |
| 新增测试 | `CompassViewTest.kt` | CompassView UI 渲染测试 |
| 新增配置 | `.github/workflows/ci.yml` | GitHub Actions CI 流水线 |
| 修改 | `libs.versions.toml` | 新增 junit/mockk/turbine/robolectric 等测试依赖版本 |
| 修改 | `app/build.gradle.kts` | 添加 testImplementation/androidTestImplementation 依赖 + testOptions |
| 修改 | `relay-server/build.gradle.kts` | 添加 testImplementation 依赖 |
| 修改 | `CompassView.kt` | `shortestAngle()` 从 private 改为 internal (可测试) |

---

## 5. v0.3.0-dev 安全加固与生产化 — 待执行

> 状态: 🔲 待开始 | 预计工时: 14.5h

| 编号 | 任务名称 | 优先级 | 状态 | 预计工时 | 分配人 | 开始日期 | 完成日期 | 备注 |
|:----:|:---------|:------:|:----:|:--------:|:------:|:--------:|:--------:|:-----|
| M3-01 | WSS 加密传输 | P1 | 🔲 待开始 | 2h | — | — | — | TLS/SSL 证书配置 |
| M3-02 | WebSocket 连接认证 | P1 | 🔲 待开始 | 2h | — | — | — | token 校验 |
| M3-03 | QR 码签名与有效期 | P1 | 🔲 待开始 | 2h | — | — | — | HMAC 签名 + 过期时间 |
| M3-04 | 位置消息端到端加密 | P2 | 🔲 待开始 | 3h | — | — | — | ECDH 密钥交换 |
| M3-05 | Docker 容器化部署 | P2 | 🔲 待开始 | 1.5h | — | — | — | Dockerfile + docker-compose |
| M3-06 | 服务器监控与日志 | P2 | 🔲 待开始 | 2h | — | — | — | 请求量/连接数/错误率 |
| M3-07 | ProGuard 混淆配置 | P2 | 🔲 待开始 | 1h | — | — | — | Release 构建 |
| M3-08 | 崩溃监控 | P3 | 🔲 待开始 | 1h | — | — | — | Crashlytics / Bugly |

---

## 6. v0.4.0-dev 功能扩展 — 待执行

> 状态: 🔲 待开始 | 预计工时: 18.5h

| 编号 | 任务名称 | 优先级 | 状态 | 预计工时 | 分配人 | 开始日期 | 完成日期 | 备注 |
|:----:|:---------|:------:|:----:|:--------:|:------:|:--------:|:--------:|:-----|
| M4-01 | 地图视图模式 | P2 | 🔲 待开始 | 4h | — | — | — | Google Maps SDK |
| M4-02 | 位置历史轨迹回放 | P3 | 🔲 待开始 | 3h | — | — | — | 需位置存储 |
| M4-03 | 多子女同时追踪 | P3 | 🔲 待开始 | 3h | — | — | — | 多 session 管理 |
| M4-04 | 电子围栏 | P3 | 🔲 待开始 | 3h | — | — | — | 地理围栏 + 告警通知 |
| M4-05 | 服务器地址设置页面 | P2 | 🔲 待开始 | 1.5h | — | — | — | UI 配置界面 |
| M4-06 | 无障碍适配 | P3 | 🔲 待开始 | 2h | — | — | — | TalkBack |
| M4-07 | 多语言国际化 | P3 | 🔲 待开始 | 2h | — | — | — | 中/英 |

---

## 7. v0.5.0-dev 跨平台与通信层升级 — 待执行

> 状态: 🔲 待开始 | 预计工时: 17h+

| 编号 | 任务名称 | 优先级 | 状态 | 预计工时 | 分配人 | 开始日期 | 完成日期 | 备注 |
|:----:|:---------|:------:|:----:|:--------:|:------:|:--------:|:--------:|:-----|
| M5-01 | Firebase FCM 替代 WebSocket | P3 | 🔲 待开始 | 4h | — | — | — | 推送位置更新 |
| M5-02 | MQTT 协议支持 | P3 | 🔲 待开始 | 3h | — | — | — | Paho MQTT |
| M5-03 | iOS 版本 (KMP) | P4 | 🔲 待开始 | 8h+ | — | — | — | Kotlin Multiplatform |
| M5-04 | 正式发布上架 | P3 | 🔲 待开始 | 2h | — | — | — | Google Play |

---

## 8. 变更日志

| 日期 | 变更内容 | 变更人 |
|:----:|:---------|:------:|
| 2026-07-01 | 创建 v0.1.0-dev 任务记录，15 个任务全部标记完成 | — |
| 2026-10-22 | 创建 v0.1.1-dev 任务记录，16 个任务全部标记完成 | CatPaw AI |
| 2026-10-22 | 完成 v0.2.0-dev 测试与质量保障，9 个任务全部完成，新增 87 个测试用例 | CatPaw AI |
| 2026-10-22 | 配置 CI/CD 流水线（GitHub Actions），3 个 Job | CatPaw AI |
| 2026-10-23 | 修复 15 个失败测试：补 Robolectric runner、E2E 改用 createClient、修正 2 条错误断言、修 mock 写法；测试数 87 → 96 | Claude |
| 2026-10-23 | 修复服务端 `encodeDefaults` 导致 `type` 字段缺失的协议 bug——服务端消息此前全被客户端丢弃，指南针收不到位置 | Claude |
| 2026-10-23 | 修复指南针箭头东西方向镜像 + km 距离单位错配；旋转几何收敛到 `screenAngleFromNorth()` 单一点 | Claude |
| 2026-10-23 | 接入定位权限申请（此前从未申请，未授权时 `requestLocationUpdates` 抛 SecurityException 会崩）；`LocationService` 上报移至 onCreate 避免重复回调 | Claude |
| 2026-10-23 | 孩子端冷启动重连 + `connect()` 同会话同角色幂等；测试数 96 → 125 | Claude |
| 2026-10-23 | 文档对齐：README 服务器配置说明、模块结构、DESIGN 颜色表/性能参数、TASK_PLAN v0.2.0 状态 | Claude |
| 2026-10-26 | 修复"二维码一闪而过"：跳转判据从 `ConnectionState.Connected` 改为 `PAIRING_STATUS == PAIRED`，新增 `PairingStatus` 领域模型 | Claude |
| 2026-10-26 | 修复 `Channel(CONFLATED).receiveAsFlow()` 单消费者问题：三条 Flow 改 `StateFlow` / `SharedFlow(replay = 1)`，指南针页不再误显"连接已断开" | Claude |
| 2026-10-26 | 修复 `startDestination` 会话中途被改写：`StartupViewModel` 改为 `.first()` 一次性决定，`MainActivity` 加加载态；测试数 125 → 144 | Claude |
| 2026-10-26 | 双端真机联调（模拟器当家长 + 真机当孩子）跑通完整配对与位置链路；顺带发现并修复指南针页不观察配对状态、孩子离开后仍显示"已连接" | Claude |

---

## 9. 当前状态仪表盘

```
项目整体进度
┌────────────────────────────────────────────────┐
│ ████████████████████████████████░░░░░░░░░░░░░░ │  71.6%
│ 48/67 tasks completed                          │
└────────────────────────────────────────────────┘

v0.1.0-dev  ████████████████████  100%  ✅ 已完成
v0.1.1-dev  ████████████████████  100%  ✅ 已完成
v0.2.0-dev  ████████████████████  100%  ✅ 已完成
v0.2.1-dev  ████████████████████  100%  ✅ 已完成
v0.3.0-dev  ░░░░░░░░░░░░░░░░░░░░    0%  🔲 待开始
v0.4.0-dev  ░░░░░░░░░░░░░░░░░░░░    0%  🔲 待开始
v0.5.0-dev  ░░░░░░░░░░░░░░░░░░░░    0%  🔲 待开始
```

**下一步行动**: 启动 v0.3.0-dev 安全加固与生产化里程碑，优先完成 P1 级安全任务
（WSS 加密传输、WebSocket 认证、QR 码签名）。

---

## 10. 2026-10-23 缺陷修复记录（未纳入编号任务）

这些是通读代码 + 首次真正跑测试时发现的缺陷，不属于任何编号任务，单独记录。

### 10.1 测试体系从未通过（15/96 失败）

`ci.yml` 一直接跑 `:relay-server:test` 和 `:app:testDebugUnitTest`，但 0.2.0-dev 记录的
"编译验证"用的是 `assembleDebug` / `installDist`——这两个任务**不跑测试**，所以当时看到 ✅，
而唯一会跑测试的 CI 从未绿过。GitHub 上第一次运行（`4c3c647`）即为 failure，且 `build` job
因 `needs` 依赖被跳过，从未执行。

| 类别 | 数量 | 根因 |
|------|----|------|
| `CompassViewTest` 全挂 | 7 | 缺 `@RunWith(RobolectricTestRunner::class)`，跑在裸 JVM 上 `Build.FINGERPRINT` 为 null |
| `RelayServerE2ETest` 全挂 | 5 | test classpath 无 ktor client 引擎，回退 `DefaultEngine`；且硬编码 `localhost:8080` 而 testApplication 用随机端口 |
| 断言与实现矛盾 | 2 | `normalizeAngle(180)` 与 `shortestAngle(180, 0)` 的期望值写错 |
| mock 写法不成立 | 1 | `RelayConfig` 构造时即读取 `serverUrl` Flow，relaxed mock 不发射 |

### 10.2 服务端消息协议 bug（P0）

`WebSocketHandler` 的 `Json { ignoreUnknownKeys = true; isLenient = true }` 不编码默认值字段，
而所有消息的 `type` 都有默认值 → 实际发出 `{"status":"WAITING"}`。客户端 `handleMessage()`
纯按 `type` 路由，缺字段整条丢弃。**家长端因此永远收不到 `PEER_LOCATION`，指南针永远显示
"等待位置..."**；也解释了 `QrGenerationScreen` 为何靠 `delay(500)` + `Connected` 跳转
（因为 `PAIRED` 根本收不到）。修复：显式 `encodeDefaults = true`。

### 10.3 指南针箭头方向镜像 + 距离单位错配

- 箭头旋转传了负号，与 DESIGN §4.3 的公式矛盾，东西方向反向；正北/正南因 180° 对称看不出
- ≥1000m 时单位仍硬编码"米"，显示成"1.2 米"；原测试只断言数字没断言单位，所以漏过
- 重构：刻度盘与箭头统一从 `screenAngleFromNorth(compassAngle, heading)` 推导，
  删除 `CompassState.arrowRotation`，几何只有一处决定符号

### 10.4 定位权限从未申请（P0）

全 App 只申请了相机权限。`LocationProvider` 在 `@SuppressLint("MissingPermission")` 下调
`requestLocationUpdates()`，未授权抛 `SecurityException`，而两个收集点都在
`viewModelScope` / `lifecycleScope` → 家长端进指南针即崩。已接入 `LocationPermissionGate`，
并在 `LocationProvider` 内做无权限降级（关闭 Flow 而非抛异常）。

### 10.5 孩子端冷启动不重连 + 重复建连

- 进程重启后 `ChildActiveViewModel` 没有 `ensureConnected()`，页面显示"位置共享中"但
  WebSocket 没连、`sessionId` 为空，消息被服务端当未知会话丢弃
- 修这个问题时发现 `connect()` 不幂等：扫码页连过一次 + `ensureConnected()` 会开第二个 socket，
  旧的被覆盖而不关闭，服务端第二次 REGISTER 直接报"该会话已有孩子设备"——**会把正常配对流程搞坏**。
  已在持有 mutex 的前提下对同会话同角色做幂等

### 10.6 其他

| 事项 | 说明 |
|------|------|
| `LocationService` 重复上报 | 原来在 `onStartCommand` 里 collect，每次 `startService` 都新增一个 `LocationCallback`；已移至 `onCreate` |
| `.kotlin/` 被误提交 | `.gitignore` 只有 `*.kotlin_module`，已补 `.kotlin/` 并从未推送的提交中移除 |
| 死代码 | `RelayWebSocketManager.pairingStatus` 无消费者、`LocationRepository.getLastKnownLocation()` 无调用点、`LocationData.userId` 从未赋值、`Color.kt` 中 `CompassGreen/CompassTeal/CompassOrange` 与 `CompassView` 硬编码色值重复 |

### 10.7 验证

```
125 tests, 0 failed   （app 108 + relay-server 17）
CI: 3 jobs 全绿（7dd9b93、c423c77 两次运行均 success）
```

---

## 11. 2026-10-26 配对流程缺陷修复记录（未纳入编号任务）

> 真机联调暴露。三个问题单独看都不像同一个根因，串起来才是一次完整的事故。

### 11.1 拿"已连接"当"已配对"，二维码一闪而过（P0）

**现象**：真机点"我是家长"后，二维码停留约 500ms 就被带去指南针页，孩子根本来不及扫。

**定位过程**：

1. 设备 DataStore 里已有 `session_id = 4df2df2b-...`，relay 日志里能看到 netty channel
   活动，但**没有应用层的 join 日志**——说明家长端连上过，孩子端从未加入
2. 读 `QrGenerationScreen` 发现跳转判据是 `ConnectionState.Connected` + `delay(500)`
3. `Connected` 在 `onOpen` 里就置位，只代表**家长自己的 socket 打开了**；
   孩子还没扫码时服务端回的是 `PAIRING_STATUS(status = "WAITING")`
4. 那 500ms `delay` 原本是在赌 `PAIRED` 会在这段时间内到达——但它不会来，因为孩子还没扫

**修复**：

- 新增 `domain/model/PairingStatus.kt`，`fromWire()` 对未知值一律按 `WAITING` 处理
  （宁可多等，也不要误跳）
- `SyncRepository.observePairingStatus()` 贯通到两端 UI
- 家长端 `QrGenerationScreen`、孩子端 `QrScanScreen` 的自动跳转都只在 `PAIRED` 时发生；
  状态文字改由配对状态优先描述
- 孩子端原来还有一份乐观跳转：`uiState.isConnected` 在 `connect()` 返回后直接置 true。
  `connect()` 是 `suspend` 函数，返回时 socket 可能还没 `onOpen`，更不能说明家长在不在线。
  顺手删掉这个已死的字段

### 11.2 `Channel(CONFLATED).receiveAsFlow()` 是单消费者（P0）

**现象**：配对成功后进入指南针页，界面显示"连接已断开"，而 socket 其实活着。

**根因**：`RelayWebSocketManager` 原来用三个 `Channel(CONFLATED).receiveAsFlow()`。
`receiveAsFlow()` 的元素**被取走就消失**——二维码页收掉 `Connected` 之后，
指南针页再订阅只会停在初始值 `Disconnected`。

这类 bug 只在"同一进程内两个页面先后订阅同一个 `@Singleton`"时出现，单看任何一个页面
都复现不了，所以它躲过了此前所有单页面测试。

**修复**：按"迟到的订阅者要不要拿到最近一条"重新选型，并把理由写进 DESIGN §3.5：

| Flow | 类型 | 理由 |
|:-----|:-----|:-----|
| `connectionState` | `StateFlow` | 连接状态是"当前状态"，天然有初始值 `Disconnected` |
| `peerLocation` | `SharedFlow(replay = 1)` | 位置是事件流，没有"空位置"这个初始值 |
| `pairingStatus` | `SharedFlow(replay = 1)` | 同上；且不能放占位值，否则会触发业务逻辑 |

### 11.3 `startDestination` 在会话中途被反复改写（P1）

**根因**：`StartupViewModel` 持续 `collect` `combine(getUserRole(), getPairingInfo())`，
于是任何一次 DataStore 写入都会重算 `startDestination`：选角色（`null → PARENT`）、
配对成功（`null → 有值`）、孩子断开（`clearAll`）都会触发。

而 `NavHost` 的 `startDestination` **只在建图时生效**：挂上去之后再改它，不会把用户带到
已配对的那一页（表现为冷启动回到角色选择页）；反复改则会让 NavHost 重建、
`QrGenerationViewModel` 重建并生成**新的 sessionId**——孩子扫到的码随即失效。
这大概也是"两个不同 sessionId 出现在同一个进程里"的来源。

**修复**：改为 `combine(...).first()` 一次性决定；`MainActivity` 在路由决定前显示加载态，
避免这几毫秒闪一下空白。

### 11.4 新增测试

| 文件 | 用例数 | 覆盖 |
|:-----|:------:|:-----|
| `StartupViewModelTest.kt` | 8 | 五种角色 × 配对状态的起始路由；外加两条回归护栏——会话中途保存/清除配对都不得改变已决定的路由 |
| `QrGenerationScreenTest.kt` | 4 | Compose + Robolectric：`WAITING`/`DISCONNECTED` 不跳转，`PAIRED` 跳转，先 WAITING 后 PAIRED 的真实时序 |
| `RelayWebSocketManagerTest.kt`（新增 5 例） | 15 | 后订阅者仍能拿到当前连接状态 / 最近一条配对状态 / 最近一条位置；未知配对状态按 `WAITING`；重新建连不继承上一次的配对状态 |
| `CompassViewModelTest.kt`（新增 2 例） | 5 | 配对状态在 init 里就订阅（不等定位权限）；disconnect 清空配对状态 |

### 11.5 验证

```
146 tests, 0 failed   （app 124 + relay-server 22）
真机: 家长端停在二维码页，状态"已连接，等待小朋友扫码..."，持续 15s 不跳走（此前约 500ms 即跳走）
```

### 11.6 双端真机联调（2026-10-26 晚，模拟器 + 真机）

用模拟器当家长、真机当孩子（真机摄像头对着 Mac 屏幕上模拟器的二维码）跑通了完整链路：

| 阶段 | 家长端（模拟器）显示 | 结论 |
|:-----|:-----|:-----|
| 孩子在扫码前 | "已连接，等待小朋友扫码..."，持续不跳走 | ✅ 家长端修复生效 |
| 孩子扫码后 | 双方各自进入指南针页 / 孩子状态页，DataStore 中 sessionId 一致 | ✅ 双端跳转均由 PAIRED 触发 |
| 指南针页连接指示 | "已连接" | ✅ `Channel`→`StateFlow` 修复生效（旧代码此处会显示"连接已断开"） |
| 收到位置后 | "1.0 公里" + "↑ 大约朝正东方向"，箭头指向右、N 在顶部 | ✅ 位置链路与指南针几何在真机上正确 |

联调中还发现并修掉了一个同源缺陷，见 §11.7。

### 11.7 指南针页也不观察配对状态（P1，联调暴露）

真机联调时把真机上的孩子端 `force-stop` 掉，家长端指南针页**仍然显示"已连接"**。

`CompassViewModel` 只 `observeConnectionState()` + `observePeerLocation()`，没有
`observePairingStatus()`。而孩子切后台、关机、点"停止共享"时，家长自己的 socket 毫发无损，
服务端发来的 `DISCONNECTED` 是唯一能看出"对方走了"的信号——它被
`RelayWebSocketManager` 收进了 SharedFlow，但指南针页没有订阅者。

这和第 11.1 节是**同一类错误的另一个化身**：把"自己这一侧的连接状态"当成"对方在不在"。

修复：

- `CompassViewModel` 新增 `pairingStatus`，在 `init` 里订阅（不放在 `startObserving()`——
  那个由定位权限就绪触发，而配对状态与定位权限无关）
- `ConnectionStatusBar` 改为配对状态优先，与 `QrGenerationScreen` 同一套措辞
- 三态均已真机验证：无孩子 → "等待小朋友连接..."；孩子在 → "已连接"；孩子离开 → "对方已断开"

> 联调时真机有锁屏密码，无法解锁操作界面，因此用 `force-stop` 释放孩子端 slot。
> 位置数据由临时的 WebSocket 客户端顶替孩子端上报（真机室内取不到 GPS fix，
> 但 App 确实已按 HIGH_ACCURACY / 5s 注册，服务也在跑）。

> ⚠️ 尚未验证的部分：孩子端扫码 → 双端进指南针的完整链路。当前只连了一台设备，
> 需要两台设备（或模拟器 + 真机）才能跑通。孩子端的改动与家长端同源、有单元测试覆盖，
> 但真机表现待确认。
