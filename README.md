<div align="center">

# 🐾 QQ高级喵喵助手 (NyaHelper)

**为 Android 打造的极简、轻量、纯本地聊天文本实时萌化修饰工具**
<br />
*A lightweight, multi-engine, 100% local text stylization assistant for QQ & WeChat on Android.*

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Material 3](https://img.shields.io/badge/Material%203-Monet%20Color-007AFF?style=flat-square&logo=materialdesign&logoColor=white)](https://m3.material.io)
[![LSPosed](https://img.shields.io/badge/LSPosed-Supported-FFA000?style=flat-square)](https://github.com/LSPosed/LSPosed)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg?style=flat-square)](LICENSE)
[![Vibe Coding](https://img.shields.io/badge/Built%20with-Vibe%20Coding-blueviolet?style=flat-square)](https://github.com/KaiTeeDreamChai)

</div>

---

## 📌 项目简介

日常在 QQ 或微信聊天时，手动敲击萌系语气词与颜文字繁琐耗时，而在**多模式切换、系统分身与不同权限环境下，传统工具常面临无法稳定生效、卡顿或过度申请高危权限的问题**。

**QQ高级喵喵助手** 采用 **双引擎架构（LSPosed Hook 内存级拦截 + 无障碍 InputConnection 合法输入通道）**，以 **1.7 MB** 的极简体积提供无感发送拦截、标点触发与打字实时修改三种模式。**代码 100% 开源透明、0 联网权限、无暗箱后门，所有转换纯本地毫秒级运算完成**。

---

## ✨ 核心特性

- ⚡ **双驱动引擎（Dual Engine）**：支持 **LSPosed 内存级 Hook（零延迟瞬间拦截）** 与 **Android 13+ InputConnection 无障碍服务（免 Root 合法通道）**。
- 🎛️ **首页一键总开关**：首页顶部设有一键全局总开关，随时暂停或恢复所有萌化功能（双模式统一受控）。
- 🎭 **智能情景与单字识别**：
  - 自动识别 8 大情感情景（疑问、愤怒、问候、喜悦、难过、疲惫、鼓励、感谢）；
  - 精准识别中文高频单字回复（如 `哦`、`额`、`啊`、`草`、`操`、`好`、`对`、`嗯` 等）并匹配专属一对一颜文字。
- 🛡️ **纯本地安全架构（100% Privacy）**：**0 联网权限**，代码全公开无后门，去除一切外置存储探测痕迹，内置 15~30ms 拟人化微抖动防风控。
- 👥 **双开 / 分身深度兼容**：配置页内置 Root 多用户空间探测，一键为系统双开空间激活模块。
- 🎨 **Material 3 原生美学**：完整适配 Material Design 3 规范与 Android 12+ Monet 动态壁纸取色，支持深色模式。

---

## 🔧 工作原理

```
                           [ 用户输入文本 / 点击发送 ]
                                      │
           ┌──────────────────────────┴──────────────────────────┐
           ▼ (Root / LSPosed 模式)                               ▼ (免 Root / 无障碍模式)
   [ LSPosed 内存级 Hook ]                                [ InputConnection 合法通道 ]
           │                                                      │
           ├─► 捕获目标应用 (QQ / 微信 / 分身)                    ├─► 监听文本变更 (800ms 防抖)
           └─► 点击发送瞬间 0ms 原子替换                          └─► 15~30ms 拟人抖动 commitText
           │                                                      │
           └──────────────────────────┬──────────────────────────┘
                                      │
                                      ▼
                      [ 纯本地 Nya 规则引擎 (0 联网) ]
                                      │
                                      ├──► 中文高频单字精准匹配 (哦/额/草/操/好/对...)
                                      ├──► 8 大情景情绪识别与专属颜文字池
                                      └──► 人称替换 (我->本喵, 你->主人) 与断句加喵
                                      │
                                      ▼
                           [ 最终输出并完成发送 ]
```

---

## 🚀 快速开始

### 方式一：直接安装 Release 安装包（推荐）

1. 从 [Releases 页面](../../releases) 下载最新的 `QQ高级喵喵助手_Release.apk`；
2. 安装至 Android 设备并打开应用；
3. 根据设备环境选择 **「LSPosed 模式」** 或 **「无障碍辅助模式」** 即可。

### 方式二：源码构建运行

1. **克隆代码仓库**：
   ```bash
   git clone https://github.com/KaiTeeDreamChai/nya-helper.git
   cd nya-helper
   ```
2. **编译生成 Release APK**：
   ```bash
   ./gradlew assembleRelease
   ```
   > 产物位于 `app/build/outputs/apk/release/app-release.apk`。

> **系统要求**：Android 8.0 (API 26) 及以上，支持 Android 12 ~ 15 Monet 动态取色。

---

## 💡 使用指南

1. **LSPosed 模式配置**：
   - 在 LSPosed 管理器中启用本模块，作用域勾选 `QQ` (`com.tencent.mobileqq`) 与 `微信` (`com.tencent.mm`)；
   - 强行停止一次 QQ / 微信重新打开即可生效（若使用应用双开，可在【配置】页点击一键激活分身）。
2. **无障碍模式配置**：
   - 打开系统「设置」 $\to$ 「无障碍」 $\to$ 开启「QQ高级喵喵助手」；
   - 在助手首页选择触发模式为「标点触发」或「实时修改」即可。

### 模式速查表

| 模式 | 运行环境 | 特点与使用方式 |
| :--- | :--- | :--- |
| **发送拦截** | LSPosed 专用 | 输入普通文字，点击发送的瞬间自动注入萌化并发出，界面最整洁 |
| **标点触发** | LSPosed / 无障碍 | 输入内容后输入 `。`、`！`、`？` 等标点时即刻原地替换 |
| **实时修改** | LSPosed / 无障碍 | 每键入一个汉字或字符时立即同步转换 |

---

## ⚠️ 注意事项与说明

- **0 联网权限与隐私透明**：本项目在 `AndroidManifest.xml` 中**完全未声明 `INTERNET` 权限**，物理上无法发起网络请求；所有源码完全公开透明，不存在任何暗箱与后门。
- **无障碍配置刷新**：若更新版本后无障碍失效，请在系统设置中**关闭一次服务并重新开启**，以便系统重载 `AccessibilityInputConnection`。
- **后台常驻优化**：无障碍模式依赖后台常驻，建议在系统设置中加入 **「忽略电池优化 / 后台无限制」** 白名单。

---

## 🏗️ 项目结构

```
nya-helper/
├── app/
│   ├── src/main/java/com/nya/helper/
│   │   ├── engine/             # 核心转换与单字识别引擎 (RuleEngine, ConfigManager)
│   │   ├── model/              # 数据实体与配置模型 (NyaConfig, Rule)
│   │   ├── service/            # 无障碍辅助服务 (NyaAccessibilityService - InputConnection)
│   │   ├── ui/                 # Material 3 界面 (Home, Config, AboutFragment)
│   │   ├── util/               # 工具类与实时排错控制台 (DebugLogger, RootHelper)
│   │   └── xposed/             # LSPosed 内存级 Hook 注入逻辑 (NyaHook)
│   ├── src/main/res/           # Material 3 布局、矢量图标与 Monet 主题
│   └── proguard-rules.pro      # R8 混淆规则
├── gradle/                     # Gradle Wrapper 与依赖清单
├── build.gradle.kts            # 项目级构建脚本
├── settings.gradle.kts         # 模块设置
└── README.md                   # 项目说明文档
```

---

## ☕ 关于本项目 (About)

本项目为 **Vibe Coding** 产物，由 **`92ch41`**、**`KTDC`** 与 **`USCP`** 共同构建，旨在以最精简、开箱即用的方式解决日常实际痛点。
- **定位**：开箱即用、轻量无冗余依赖、专注于单一场景。
- **维护说明**：个人业余项目，后期大概率按需维护，不承诺长期高频更新与功能迭代。
- **二次开发**：代码结构干净透明，欢迎自由 Fork、定制与魔改！

---

## 📄 License

本项目基于 [MIT License](LICENSE) 开源。
