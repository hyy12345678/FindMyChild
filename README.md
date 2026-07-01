<div align="center">

# 🔍 找小朋友 FindMyChild

**让每一次走散，都有方向可寻**

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Changelog](https://img.shields.io/badge/Changelog-0.1.0--dev-blue.svg)](CHANGELOG.md)
[![Status](https://img.shields.io/badge/状态-开发中-orange.svg)]()

</div>

---

## 为什么需要这个项目

> 据公安部统计，中国每年约有 **20 万名儿童走失**。在人潮涌动的商场、车站、景区，一个转身的瞬间，孩子就可能消失在视线之外。而黄金寻找时间往往只有最初的 **几分钟**——越快确定方向，越快找到孩子。

**找小朋友** 正是为这一刻而生的。

我们的愿景是：不依赖地图路线规划，不依赖网络预约等待，只做一件事——**让家长在最短时间内知道孩子在哪个方向、有多远**。一块指南针，一个方向，一段距离——这就是全部。简单，但足以改变结局。

想象这样一个场景：当焦虑的家长在人群中四处张望时，打开手机，看到箭头指向正北、距离 47 米——那一刻，方向感取代了慌乱，行动替代了无助。

**这不仅仅是一款应用，更是一份安全感。**

> ⚠️ **本项目仍在积极开发中**，尚未正式发布。部分功能已搭建框架但未经过完整测试，请勿用于真实走失场景下的紧急寻人。如遇儿童走失，请立即拨打 **110** 报警求助。

---

## 功能规划

> 以下为项目设计的完整功能蓝图。标注 ✅ 的功能已搭建代码框架，标注 🔧 的功能仍在开发或调试中，尚未达到生产可用状态。

### 🧭 实时指南针导航 ✅🔧

- 基于真北方位角的 **方向箭头**，始终指向孩子的方向
- **实时距离显示**，颜色随距离变化（绿→黄→橙→红），直觉化提示
- 中文方向描述（正北、东北、正东……），无需解读角度
- 数据刷新配合平滑动画，追踪体验流畅

### 📱 极简配对 ✅🔧

- 家长端一键生成 **二维码**，孩子端扫码即连
- 无需注册账号、无需输入配对码、无需任何复杂操作
- 设计目标：30 秒内完成配对，专注寻找，而非操作手机

### 🔄 后台持续定位 ✅🔧

- 孩子端通过 **前台服务** 持续上报 GPS 位置
- 支持应用切至后台、锁屏状态下持续运行
- `START_STICKY` 机制确保服务被系统回收后自动重启

### 📡 自动重连 ✅🔧

- WebSocket 断线后采用 **指数退避策略** 自动重连（1s → 2s → 4s → … → 30s）
- 网络波动无感知，恢复后自动同步最新位置

### 🌗 深色模式 ✅

- 完整支持 Light / Dark 双主题
- 指南针在深色环境下依然清晰可读

---

## 技术架构

项目采用 **Clean Architecture** 分层架构 + **MVVM** 模式，确保代码可测试、可维护、可扩展。

```
┌──────────────────────────────────────────────────────────┐
│                       UI Layer                            │
│         Compose Screen ← HiltViewModel                    │
├──────────────────────────────────────────────────────────┤
│                     Domain Layer                          │
│      Model / Repository Interface / UseCase              │
├──────────────────────────────────────────────────────────┤
│                      Data Layer                           │
│   Repository Impl / Sensor / WebSocket / DataStore       │
├──────────────────────────────────────────────────────────┤
│                    Relay Server                            │
│       Ktor + Netty + WebSocket Session Manager            │
└──────────────────────────────────────────────────────────┘
```

### 模块结构

```
FindMyChild/
├── app/                              # Android 客户端
│   └── src/main/java/.../app/
│       ├── data/                     # 数据层
│       │   ├── local/                #   本地持久化 (DataStore)
│       │   ├── remote/               #   网络通信 (WebSocket + Moshi)
│       │   ├── repository/           #   Repository 实现
│       │   └── sensor/               #   传感器 (GPS + 磁力计)
│       ├── di/                       # 依赖注入 (Hilt Modules)
│       ├── domain/                   # 领域层
│       │   ├── model/                #   核心数据模型
│       │   ├── repository/           #   Repository 接口
│       │   └── usecase/              #   业务用例
│       └── ui/                       # 表现层
│           ├── compass/              #   指南针页面
│           ├── component/            #   自定义组件 (CompassView)
│           ├── navigation/           #   导航图与路由
│           ├── pairing/              #   配对流程 (QR 生成/扫描)
│           ├── role/                 #   角色选择
│           └── theme/                #   主题与样式
│
└── relay-server/                     # WebSocket 中继服务器
    └── src/main/kotlin/.../relay/
        ├── Application.kt            #   Ktor 启动入口
        ├── WebSocketHandler.kt       #   消息路由与处理
        ├── SessionManager.kt         #   配对会话管理
        └── Messages.kt              #   通信协议定义
```

### 通信协议

客户端与中继服务器之间通过 WebSocket 交换 JSON 消息：

| 方向 | 消息类型 | 说明 |
|:----:|:--------:|:-----|
| 客户端 → 服务器 | `REGISTER` | 注册设备角色（PARENT / CHILD）到会话 |
| 客户端 → 服务器 | `LOCATION_UPDATE` | 上报 GPS 坐标 |
| 服务器 → 客户端 | `PAIRING_STATUS` | 配对状态通知（WAITING / PAIRED / DISCONNECTED） |
| 服务器 → 客户端 | `PEER_LOCATION` | 对端位置转发 |
| 服务器 → 客户端 | `ERROR` | 错误消息 |

### 核心流程

```
家长选择角色 → 生成 UUID 作为会话 ID → 生成二维码
                                          ↓
孩子扫码获取会话 ID → 双方 WebSocket 连接中继服务器
                                          ↓
双方 REGISTER → 服务器检测配对完成 → 通知 PAIRED
                                          ↓
孩子端启动前台定位服务 → 持续上报 GPS 坐标
                                          ↓
中继服务器将位置转发至家长端
                                          ↓
家长端合并 (我的位置 + 孩子位置 + 设备朝向)
    → Haversine 公式计算方位角与距离
    → 更新指南针 UI：方向箭头 + 距离显示
```

---

## 技术栈

| 分类 | 技术 | 版本 |
|:----:|:-----|:----:|
| 语言 | Kotlin | 2.0.21 |
| UI 框架 | Jetpack Compose + Material 3 | BOM 2024.09 |
| 依赖注入 | Hilt | 2.51.1 |
| 导航 | Navigation Compose | 2.8.0 |
| 定位 | Google Play Services FusedLocation | 21.3.0 |
| 传感器 | Android SensorManager (磁力计 + 加速度计) | — |
| 网络客户端 | OkHttp WebSocket + Moshi | 4.12.0 / 1.15.1 |
| 中继服务器 | Ktor + Netty + kotlinx.serialization | 3.0.3 |
| 相机 | CameraX | 1.4.1 |
| 二维码 | ZXing (生成 + 解码) | 3.5.3 |
| 本地存储 | DataStore Preferences | 1.1.1 |
| 协程 | kotlinx-coroutines-android | 1.9.0 |
| 构建 | Gradle (Kotlin DSL) + Version Catalog | 8.7 |

---

## 开发调试

> ⚠️ 本项目尚处于早期开发阶段，以下步骤仅供开发者本地构建和调试使用。

### 环境要求

- Android Studio Hedgehog 及以上
- JDK 17
- Android 设备或模拟器 (API 26+ / Android 8.0+)
- 调试时两台设备需连接同一网络（或中继服务器可公开访问）

### 1. 启动中继服务器

```bash
# 在项目根目录执行
./gradlew :relay-server:run
```

服务器默认监听 `0.0.0.0:8080`。

### 2. 配置服务器地址

编辑 `app/src/main/java/com/findmychild/app/data/remote/RelayConfig.kt`：

```kotlin
val wsUrl: String
    get() = "ws://<你的电脑IP>:8080/ws"  // 👈 改这里
```

- **模拟器**：使用 `ws://10.0.2.2:8080/ws`
- **真机（同 WiFi）**：使用 `ws://192.168.x.x:8080/ws`

### 3. 构建并安装

```bash
# 安装到连接的 Android 设备或模拟器
./gradlew :app:installDebug
```

### 4. 调试流程

1. **家长设备**：打开 App → 选择「我是家长」→ 展示二维码
2. **孩子设备**：打开 App → 选择「我是小朋友」→ 扫描二维码
3. 配对成功后：
   - 家长端跳转至**指南针页面**
   - 孩子端显示**位置共享状态页**

> 💡 调试时可能遇到连接不稳定、定位精度不足等问题，这与项目当前的开发状态有关，欢迎提交 Issue 反馈。

---

## 设计理念

### 为什么是指南针，不是地图？

在紧急寻人场景下，家长需要的不是路线规划，而是**即时方向感**：

- **地图**需要加载、需要辨认自身朝向、需要在缩放中判断远近——每一步都在消耗黄金时间
- **指南针**只需抬手一看：箭头指向哪里，人就往哪里跑。距离越近颜色越绿，直觉即行动

我们刻意做减法。少一个功能，多一份专注。

### 配对为何如此简单？

配对发生在日常——出门前、到商场后、孩子刚跑开之前。这是防患于未然的时刻，不是慌乱中的补救。但即便如此，任何多余的注册、验证、输入步骤都是阻碍家长迈出这一步的门槛。一键生成、一扫即连，让配对像系鞋带一样自然，才能确保走散那一刻，设备之间早已心意相通。

---

## 路线图

### 已完成框架搭建

- [x] Clean Architecture 分层架构 + MVVM
- [x] 角色选择与本地持久化
- [x] QR 码生成与扫描配对流程
- [x] 指南针自定义 Canvas 组件
- [x] WebSocket 中继服务器（Ktor + Netty）
- [x] 后台前台定位服务
- [x] 深色模式支持

### 开发中

- [ ] 完善指南针数据刷新与动画稳定性
- [ ] WebSocket 重连机制优化（改为协程实现）
- [ ] 服务器地址动态配置
- [ ] 完整的权限请求流程与异常处理
- [ ] 单元测试与集成测试

### 计划中

- [ ] 地图视图模式（指南针 + 地图双模式切换）
- [ ] 位置历史轨迹回放
- [ ] 多子女同时追踪
- [ ] 电子围栏（离开设定范围自动告警）
- [ ] 中继服务器认证与加密
- [ ] Firebase / MQTT 替代 WebSocket 通信层
- [ ] iOS 版本（Kotlin Multiplatform）
- [ ] 无障碍适配与国际化
- [ ] 正式发布上架

---

## 许可证

本项目基于 [MIT License](LICENSE) 开源。

```
MIT License

Copyright (c) 2026 FindMyChild Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<div align="center">

**每一个孩子都值得被找到。**

<br/>

感谢 [CatPawAI](https://www.catpawai.com) 提供免费的模型算力 🐾

</div>
