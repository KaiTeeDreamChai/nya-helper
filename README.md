<div align="center">

# 🐾 QQ高级喵喵助手 (NyaHelper)

**为 Android 打造的极简、轻量、多模式聊天文本实时萌化修饰工具**
<br />
*A lightweight, multi-engine text stylization and cute-affix assistant for QQ & WeChat on Android.*

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Material 3](https://img.shields.io/badge/Material%203-Monet%20Color-007AFF?style=flat-square&logo=materialdesign&logoColor=white)](https://m3.material.io)
[![LSPosed](https://img.shields.io/badge/LSPosed-Xposed%20API-FFA000?style=flat-square)](https://github.com/LSPosed/LSPosed)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg?style=flat-square)](LICENSE)
[![Vibe Coding](https://img.shields.io/badge/Built%20with-Vibe%20Coding-blueviolet?style=flat-square)](https://github.com/KaiTeeDreamChai)

</div>

---

## 📌 项目简介

日常在 QQ 或微信聊天时，频繁手动敲击萌系语气词（如「喵~」、「捏」、「呢~」）或颜文字不仅繁琐耗时，而且**在跨应用、双开分身与不同系统权限限制下，传统辅助工具往往难以稳定生效或容易造成界面卡顿**。

**QQ高级喵喵助手** 采用 **双引擎协同架构（LSPosed Hook 进程注入 + 无障碍 InputConnection 辅助通道）**，以 **1.7 MB** 的极致轻量体积，提供**无感发送拦截、标点触发与打字实时修改**三种触发模式。无需联网、纯本地极速转换，原生适配 Material 3 与 Monet 动态壁纸取色。

---

## ✨ 核心特性

- ⚡ **双驱动引擎（Dual Engine）**：
  - **LSPosed 模块模式**：零延迟进程内 UI 拦截，点击发送瞬间完成替换，极致无感。
  - **无障碍辅助模式**：基于 Android 13+ `AccessibilityInputConnection` 合法通道，深度突破沙盒屏蔽，免 Root 也能稳定使用。
- 🎯 **三档触发策略**：支持「发送时转换（Hook 独占）」、「标点触发（输入句号/问号/感叹号自动转换）」与「实时修改」三种模式。
- 🎭 **多重语气与颜文字库**：内置「标准喵喵」、「傲娇语气」、「软萌语气」、「可爱颜文字」等预设模板，支持自定义后缀词库与随机表情轮播。
- 👥 **多开/双开分身深度兼容**：内置一键 Root 多用户空间挂载（支持 ColorOS / realme UI / MIUI 等应用分身空间）。
- 🎨 **Material 3 原生美学**：完整适配 Material Design 3 规范与 Android 12+ Monet 系统动态壁纸取色，支持深色模式。
- 🔍 **内置实时排错控制台**：关于页面自带环形缓冲日志终端，支持实时事件追踪与一键复制诊断信息。
- 🪶 **极简安全架构**：安装包仅 **1.7 MB**，纯本地离线运行，0 联网权限，安全无风险。

---

## 🔧 工作原理

```
                           [ 用户输入文本 / 点击发送 ]
                                      │
           ┌──────────────────────────┴──────────────────────────┐
           ▼ (Root / LSPosed 环境)                               ▼ (免 Root / 无障碍环境)
   [ LSPosed Hook 引擎 ]                                  [ 无障碍服务引擎 ]
           │                                                      │
           ├─► 捕获目标应用 (QQ / 微信 / 分身)                    ├─► 监听 TYPE_VIEW_TEXT_CHANGED 事件
           ├─► WeakReference 追踪活跃 EditText                    ├─► 绕过虚拟节点屏蔽
           └─► View.performClick 瞬间原子注入                     └─► 走 InputConnection.commitText 通道
           │                                                      │
           └──────────────────────────┬──────────────────────────┘
                                      │
                                      ▼
                           [ Nya 规则引擎 (本地处理) ]
                                      │
                                      ├──► 敏感词 / 特殊符号过滤
                                      ├──► 语气词与预设规则拼装
                                      └──► 随机颜文字池抽取与补全
                                      │
                                      ▼
                           [ 输出转换后的萌化文本并发送 ]
```

---

## 🚀 快速开始

### 方式一：直接安装 Release 安装包（推荐）

1. 从 [Releases 页面](../../releases) 下载最新的 `QQ高级喵喵助手_v1.0.0_Release.apk`；
2. 安装至 Android 设备并打开应用；
3. 根据设备环境选择 **「LSPosed 模式」** 或 **「无障碍辅助模式」** 即可。

### 方式二：源码构建与打包

1. **克隆代码仓库**：
   ```bash
   git clone https://github.com/KaiTeeDreamChai/nya-helper.git
   cd nya-helper
   ```
2. **使用 Gradle 编译 Release APK**：
   ```bash
   ./gradlew assembleRelease
   ```
   > 编译产物位于 `app/build/outputs/apk/release/app-release.apk`。

> **系统要求**：Android 8.0 (API 26) 及以上，支持 Android 12 ~ 15 Monet 动态取色。

---

## 💡 使用指南

### 1. LSPosed 模块模式配置（推荐玩机用户）
1. 在 LSPosed 管理器中启用 **「QQ高级喵喵助手」** 模块；
2. 勾选推荐作用域：`QQ` (`com.tencent.mobileqq`) 与 `微信` (`com.tencent.mm`)；
3. （可选）若使用了系统应用双开，点击助手首页的 **「激活双开分身支持」** 一键挂载镜像；
4. 强行停止一次 QQ / 微信并重新打开即可生效。

### 2. 无障碍模式配置（免 Root 用户）
1. 打开系统「设置」 $\to$ 「无障碍」 $\to$ 找到并开启 **「QQ高级喵喵助手」**；
2. 在助手首页选择触发模式为 **「标点识别」** 或 **「实时修改」**；
3. 打开微信/QQ，输入一句话并在句尾输入标点（如 `。`），文字将自动完成萌化修饰。

### 触发模式速查

| 触发模式 | 适用引擎 | 特点与使用方式 |
| :--- | :--- | :--- |
| **发送拦截** | LSPosed 专用 | 输入普通文字，点击发送的瞬间自动注入萌化并发出，界面最整洁 |
| **标点触发** | LSPosed / 无障碍 | 输入内容后输入 `。`、`！`、`？` 等标点时立即原地替换 |
| **实时修改** | LSPosed / 无障碍 | 每键入一个汉字或字符时立即同步转换 |

---

## ⚠️ 注意事项与说明

- **无障碍模式权限重载**：若在更新版本后无障碍失效，请在系统设置中**关闭一次服务并重新开启**，以便系统刷新 `AccessibilityInputConnection` 配置。
- **系统电池优化白名单**：无障碍模式依赖后台常驻，建议在系统设置中将本助手加入 **「后台无限制 / 忽略电池优化」** 列表，防止被系统深度清理。
- **安全性与封号风险**：本工具纯本地运行，不拦截网络数据包、不修改通讯协议。无障碍模式使用系统原生合法输入通道，封号风险趋近于 0。

---

## 🏗️ 项目结构

```
nya-helper/
├── app/
│   ├── src/main/java/com/nya/helper/
│   │   ├── engine/             # 核心转换引擎 (RuleEngine, ConfigManager)
│   │   ├── model/              # 数据实体与配置模型 (NyaConfig, Rule)
│   │   ├── service/            # 无障碍辅助服务 (NyaAccessibilityService - InputConnection)
│   │   ├── ui/                 # Material 3 UI 界面 (Home, Settings, AboutFragment)
│   │   ├── util/               # 工具类与实时排错控制台 (DebugLogger, RootHelper)
│   │   └── xposed/             # LSPosed Hook 注入逻辑 (NyaHook)
│   ├── src/main/res/           # Material 3 布局、矢量图标与 Monet 主题
│   └── proguard-rules.pro      # R8 混淆与反射保护规则
├── gradle/                     # Gradle Wrapper 与依赖配置
├── build.gradle.kts            # 项目级构建脚本
├── settings.gradle.kts         # 模块设置
└── README.md                   # 项目说明文档
```

---

## ☕ 关于本项目 (About)

本项目为 **Vibe Coding** 产物，旨在以最精简、开箱即用的方式解决日常实际痛点。
- **定位**：开箱即用、轻量无冗余依赖、专注于单一场景。
- **维护说明**：个人业余项目，后期大概率按需维护，不承诺长期高频更新与功能迭代。
- **二次开发**：代码结构干净透明，欢迎自由 Fork、定制与魔改！

---

## 📄 License

本项目基于 [MIT License](LICENSE) 开源。
