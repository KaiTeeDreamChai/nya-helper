<div align="center">

# 🐾 QQ高级喵喵助手 (NyaHelper)

**极简、轻量、纯本地的 Android 聊天文本实时萌化辅助工具**
<br />
*100% Open Source • 0 Network Permissions • No Backdoors • Pure Local Processing*

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Permissions: 0-Network](https://img.shields.io/badge/Permissions-0--Network-success?style=flat-square)](app/src/main/AndroidManifest.xml)
[![LSPosed](https://img.shields.io/badge/LSPosed-Supported-FFA000?style=flat-square)](https://github.com/LSPosed/LSPosed)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg?style=flat-square)](LICENSE)
[![Vibe Coding](https://img.shields.io/badge/Built%20with-Vibe%20Coding-blueviolet?style=flat-square)](https://github.com/KaiTeeDreamChai)

</div>

---

## 🔒 安全与透明承诺 (Security & Privacy)

本应用为 **100% 完全开源项目**，所有源码在 GitHub 完全透明公开，接受社区公开审计与监督：

- 🚫 **0 联网权限（Zero Network Permission）**：App 在 `AndroidManifest.xml` 中**完全未声明 `INTERNET` 权限**，物理层面上无法建立任何网络连接或外发数据。
- 🛡️ **无暗箱 / 无后门 / 0 数据收集**：所有萌化转换、规则解析、自定义词库均在**手机本地内存**毫秒级运行，不记录、不落盘、不上传任何聊天隐私。
- 🥷 **防风控隐身加固**：去除一切外置存储探测文件，LSPosed 采用纯内存级 Hook，无障碍模式内置 **15~30ms 拟人化随机微抖动**，杜绝机械化外挂特征。

---

## ✨ 核心特性

- ⚡ **双驱动引擎**：
  - **LSPosed 模块模式**：零延迟进程内 UI 拦截，点击发送瞬间无感替换；
  - **无障碍辅助模式**：基于 Android 13+ `AccessibilityInputConnection` 合法输入通道，深度突破沙盒屏蔽，免 Root 可用。
- 🎛️ **首页一键总开关**：首页顶部设有一键总开关，随时暂停或恢复所有萌化功能（双模式全局生效）。
- 🎭 **智能情景与单字识别**：
  - 自动识别 8 大情感情景（疑问、愤怒、问候、喜悦、难过、疲惫、鼓励、感谢）；
  - 精准识别中文高频单字回复（如 `哦`、`额`、`啊`、`草`、`操`、`好`、`对`、`嗯` 等）并匹配专属一对一颜文字。
- 👥 **双开 / 分身一键兼容**：配置页内置 Root 多用户空间探测，一键为系统双开应用激活模块。
- 🎨 **Material 3 原生美学**：完整适配 Material Design 3 规范与 Android 12+ Monet 动态壁纸取色，支持深色模式。
- 🪶 **极致轻量**：安装包仅 **1.7 MB**，无冗余第三方依赖。

---

## 🔧 工作流程

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

## 🚀 快速使用

### 1. 安装与启动
从 [Releases 页面](../../releases) 下载最新版的 `QQ高级喵喵助手_Release.apk` 并安装。

### 2. 选择适合你的模式

| 模式 | 运行环境 | 配置方法 |
| :--- | :--- | :--- |
| **LSPosed 模式 (推荐)** | 已 Root + LSPosed | 1. 在 LSPosed 中勾选本模块，作用域勾选 `QQ` 和 `微信`<br>2. 强行停止目标应用重新打开即可 |
| **无障碍模式 (免 Root)** | 任意 Android 8.0+ | 1. 在系统设置中开启「QQ高级喵喵助手」无障碍服务<br>2. 首页选择「标点触发」或「实时修改」即可 |

---

## 👨‍💻 开发者署名

本项目由 **`92ch41`**、**`KTDC`** 与 **`USCP`** 共同构建，旨在打造最干净、纯粹、安全的 Android 效率玩具。

---

## ☕ 关于本项目 (About)

本项目为 **Vibe Coding** 产物，专注于开箱即用、无冗余依赖的极简体验。
- **开源精神**：代码结构干净透明，欢迎自由 Fork、定制与二次开发！
- **维护说明**：个人业余开源项目，按需维护与迭代。

---

## 📄 License

本项目基于 [MIT License](LICENSE) 开源。
