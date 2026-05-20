# WiliWili 应用简介

WiliWili 是一个专为个人开发和使用的轻量级 Bilibili 视频播放 Android 客户端，致力于为用户带来沉浸式、便捷流畅的 B站观影体验。全程采用 Kotlin 与 Jetpack Compose 等现代 Android 技术打造，提供安全易用的主流 B站功能。

## 核心功能

- **网页内安全登录**：通过内嵌官方 Bilibili 登录页，保障账号信息安全，无需在本地输入密码。
- **主页推荐与热门浏览**：获取并展示 Bilibili 官方热门视频内容。
- **视频搜索**：支持关键词检索，快速定位心仪视频。
- **视频详情页面**：展现弹幕、简介等详细内容，便于了解视频信息。
- **安全 Cookie 会话管理**：采用 EncryptedSharedPreferences 本地加密存储登录 Cookie，实现后续无痕免登录。
- **WebView 视频播放**：内置 Bilibili 手机版视频播放页，保证兼容性与流畅体验。
- **用户基础资料展示**：简单便捷地访问并查看登录用户的基础信息页面。

## 技术架构

- **语言**：Kotlin 全面支持
- **UI 框架**：Jetpack Compose，配合 Navigation Compose 保障页面流转
- **网络通信**：OkHttp
- **数据解析**：kotlinx.serialization
- **本地安全存储**：EncryptedSharedPreferences
- **网页能力**：WebView 深度嵌入页面

## 登录方案详解

WiliWili 不会采集或存储用户明文密码。所有登录流程均在官方登录页面（passport.bilibili.com）内通过 WebView 完成，成功后自动提取 Cookie 并加密存储，用于后续所有请求和页面，无需反复登录，保障用户隐私与安全。

## 数据获取与风险提示

- 所有数据均通过 Bilibili 官方网页端接口或页面提取，未使用任何未公开或侵权 API。
- 视频播放全程通过 WebView 加载官方页面，不涉及直接解码 B站媒体流。
- 由于 Bilibili 暂未对个人开发者开放稳定的第三方 API，相关功能和接口可能会有变动导致部分功能不可用。
- 当前仅提供未签名调试版 APK，仅用于测试与学习，禁止商用。

## 已实现功能

- 首页推荐/热门、视频搜索、详情页访问、官方网页登录及持久免密登录、视频 WebView 播放、账户基础资料获取

## 未来规划

- 评论查看与弹幕本地渲染
- 收藏、历史记录、本地缓存与下载功能
- 更丰富的个人中心和消息提醒
- 在线直播、视频投稿与高级互动
- 正式版商店签名 APK

## 构建与环境

本项目推荐在以下环境编译运行：

- Android SDK: 36.1
- Build Tools: 36.1.0
- Java 环境: Android Studio JBR 21

命令行打包方式：

```powershell
.\gradlew.bat assembleDebug
```
APK 输出路径：

```
app/build/outputs/apk/debug/app-debug.apk
```

---

本项目仅供个人学习与交流，禁止用于商业用途。API 接口如有变动，欢迎通过 Issue 反馈和讨论。