# 更新日志

本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/) 规范，版本号格式为 `主版本号.次版本号.修订号`。

> ⚠️ 项目尚处于早期开发阶段，版本号带 `-dev` 后缀表示尚未正式发布。

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
| [0.1.1-dev] | 2026-10-22 | Bug 修复 + 安全加固 + 代码清理 |
| [0.1.0-dev] | 2026-07-01 | 开发预览版，搭建完整代码框架，尚未正式发布 |

[0.1.1-dev]: https://github.com/your-org/FindMyChild/releases/tag/v0.1.1-dev
[0.1.0-dev]: https://github.com/your-org/FindMyChild/releases/tag/v0.1.0-dev
