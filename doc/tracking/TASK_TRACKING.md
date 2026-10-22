# 任务执行跟踪 — FindMyChild

> 版本: 0.2.0-dev | 最后更新: 2026-10-22

---

## 1. 总览

| 里程碑 | 任务总数 | 已完成 | 进行中 | 待开始 | 完成率 |
|:------:|:--------:|:------:|:------:|:------:|:------:|
| v0.1.0-dev 框架搭建 | 15 | 15 | 0 | 0 | 100% |
| v0.1.1-dev Bug 修复与安全加固 | 16 | 16 | 0 | 0 | 100% |
| v0.2.0-dev 测试与质量保障 | 9 | 9 | 0 | 0 | 100% |
| v0.3.0-dev 安全加固与生产化 | 8 | 0 | 0 | 8 | 0% |
| v0.4.0-dev 功能扩展 | 7 | 0 | 0 | 7 | 0% |
| v0.5.0-dev 跨平台与通信层升级 | 4 | 0 | 0 | 4 | 0% |
| **合计** | **59** | **40** | **0** | **19** | **67.8%** |

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
| `CalculateBearingUseCaseTest.kt` | 14 | Domain (方位角 + 距离 + normalizeAngle) |
| `CompassViewUtilTest.kt` | 28 | UI 工具函数 (shortestAngle + distanceColor + formatDistance + bearingToText) |
| `SessionManagerTest.kt` | 15 | relay-server (注册/配对/转发/断开) |
| `SettingsDataStoreTest.kt` | 13 | Data (角色/配对/服务器地址/clearAll) |
| `RelayWebSocketManagerTest.kt` | 4 | Data (连接状态/disconnect) |
| `RelayServerE2ETest.kt` | 6 | relay-server (端到端通信) |
| `CompassViewTest.kt` | 7 | UI (CompassView 渲染) |
| **合计** | **87** | — |

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

---

## 9. 当前状态仪表盘

```
项目整体进度
┌────────────────────────────────────────────────┐
│ ███████████████████████████████░░░░░░░░░░░░░░░░ │  67.8%
│ 40/59 tasks completed                          │
└────────────────────────────────────────────────┘

v0.1.0-dev  ████████████████████  100%  ✅ 已完成
v0.1.1-dev  ████████████████████  100%  ✅ 已完成
v0.2.0-dev  ████████████████████  100%  ✅ 已完成
v0.3.0-dev  ░░░░░░░░░░░░░░░░░░░░    0%  🔲 待开始
v0.4.0-dev  ░░░░░░░░░░░░░░░░░░░░    0%  🔲 待开始
v0.5.0-dev  ░░░░░░░░░░░░░░░░░░░░    0%  🔲 待开始
```

**下一步行动**: 启动 v0.3.0-dev 安全加固与生产化里程碑，优先完成 P1 级安全任务（WSS 加密传输、WebSocket 认证、QR 码签名）。
