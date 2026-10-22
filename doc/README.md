# FindMyChild 文档中心

> 本目录包含 FindMyChild 项目的所有技术文档。

---

## 目录结构

```
doc/
├── README.md                  ← 你在这里 — 文档索引
├── design/
│   └── DESIGN.md              ← 设计文档 — 架构、协议、算法、UI
├── plan/
│   └── TASK_PLAN.md           ← 任务计划 — 里程碑、任务分解、工时估算
└── tracking/
    └── TASK_TRACKING.md       ← 任务执行跟踪 — 进度、状态、变更记录
```

---

## 文档索引

| 文档 | 路径 | 内容概要 | 更新日期 |
|:----:|:-----|:--------|:--------:|
| 设计文档 | [design/DESIGN.md](design/DESIGN.md) | 系统架构、通信协议、核心算法、安全设计、UI 设计 | 2026-10-22 |
| 任务计划 | [plan/TASK_PLAN.md](plan/TASK_PLAN.md) | 版本里程碑（v0.1.0 ~ v0.5.0）、任务分解、工时估算 | 2026-10-22 |
| 任务执行跟踪 | [tracking/TASK_TRACKING.md](tracking/TASK_TRACKING.md) | 任务状态总览、执行记录、变更日志、进度仪表盘 | 2026-10-22 |

---

## 文档使用指南

### 快速了解项目

1. 阅读项目根目录的 [README.md](../README.md) — 了解项目背景和功能概览
2. 阅读 [设计文档](design/DESIGN.md) — 了解技术架构和核心设计
3. 阅读 [任务计划](plan/TASK_PLAN.md) — 了解版本规划和任务分解
4. 阅读 [任务执行跟踪](tracking/TASK_TRACKING.md) — 了解当前进度和历史执行记录

### 查看项目进度

- 查看任务跟踪文档的 **[第 1 节 总览](tracking/TASK_TRACKING.md#1-总览)** 获取各里程碑完成率
- 查看任务跟踪文档的 **[第 9 节 当前状态仪表盘](tracking/TASK_TRACKING.md#9-当前状态仪表盘)** 获取可视化进度条

### 查看历史变更

- 项目级别变更：查看根目录 [CHANGELOG.md](../CHANGELOG.md)
- 任务级别变更：查看 [任务跟踪文档第 8 节变更日志](tracking/TASK_TRACKING.md#8-变更日志)

---

## 文档维护规范

1. **更新时机**：每次完成任务或里程碑时，更新对应的跟踪文档
2. **命名规范**：文件名使用全大写 + `.md` 后缀（如 `DESIGN.md`）
3. **目录规范**：按文档类型分目录存放（`design/` / `plan/` / `tracking/`）
4. **链接规范**：文档间交叉引用使用相对路径
5. **日期格式**：统一使用 `YYYY-MM-DD` 格式
6. **状态标记**：统一使用 emoji 标记 — ✅ 已完成 / 🔧 进行中 / 🔲 待开始 / ⚠️ 有风险
