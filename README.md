# WiliWili

轻量级 Bilibili 视频播放 Android 客户端，个人学习与日常使用。Kotlin + Jetpack Compose + Material3 构建。

## 功能一览

| 页面 | 功能 |
|---|---|
| **首页** | 推荐/热门视频流，无限滚动，自动加载更多 |
| **动态** | 用户关注动态时间线，无限滚动（需登录） |
| **搜索** | 关键词搜索，无限滚动加载更多结果 |
| **详情** | 视频封面/标题/描述/统计，ExoPlayer 内嵌播放，一键全屏 |
| **全屏播放** | ExoPlayer 横屏沉浸式播放，手势控制系统栏 |
| **个人** | 登录后可查看个人资料 |
| **用户空间** | 查看指定 UP 主的视频列表 |
| **登录** | WebView 加载官方登录页，自动提取 Cookie 加密存储 |

## 技术栈

| 层 | 选型 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material3 |
| 导航 | Navigation Compose（淡入淡出/滑入滑出过渡） |
| 网络 | OkHttp 4.12（内置 CookieJar） |
| 序列化 | kotlinx.serialization |
| 视频播放 | Media3 ExoPlayer（音视频分离流合并播放） |
| 图片加载 | Coil |
| 安全存储 | EncryptedSharedPreferences |
| WebView | 官方登录页 & Cookie 提取 |

## 项目结构

```
com.example.bilimini/
├── MainActivity.kt              # 入口 Activity
├── SplashActivity.kt            # 开屏（splashscreen API）
├── WiliWiliApplication.kt       # Application 初始化
├── AppContainer.kt              # 依赖容器
├── data/
│   ├── api/
│   │   └── BiliApiClient.kt     # OkHttp 封装，CookieJar，WBI 签名
│   ├── model/                   # 数据模型（VideoSummary, VideoDetail, PlayableSource, DynamicItem 等）
│   ├── recommendation/          # 首页推荐排序算法 & 用户画像
│   └── repository/
│       └── BiliRepository.kt    # 数据仓库（首页/动态/搜索/详情/播放等）
├── session/
│   ├── SessionManager.kt        # Cookie 加密存储 & 管理
│   └── SessionState.kt
└── ui/
    ├── components/              # 通用组件（VideoCard, DynamicCard, PageBanner, RemoteImage, BiliPlayerView）
    ├── navigation/
    │   ├── AppDestination.kt    # 路由定义
    │   └── WiliWiliRoot.kt      # 根导航 + 底部导航栏
    ├── screen/
    │   ├── feed/                # 首页推荐（无限流）
    │   ├── dynamic/             # 用户动态（无限流）
    │   ├── search/              # 视频搜索（无限流）
    │   ├── detail/              # 视频详情 + 内嵌播放
    │   ├── player/              # 全屏 ExoPlayer
    │   ├── profile/             # 个人中心
    │   ├── auth/                # WebView 登录
    │   └── space/               # 用户空间
    └── theme/                   # Material3 主题、颜色、字体
```

## 数据源

- 所有内容来自 Bilibili 官方网页端接口
- 首页：推荐 feed + 热门 popular 双接口兜底
- 播放：先尝试 API 获取 DASH 流，失败则从 HTML 页面解析
- 用户空间：WBI 签名接口
- 接口变更可能导致功能异常

## 构建

```powershell
.\gradlew.bat assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

发布签名版需在项目根目录创建 `keystore.properties`：

```properties
storeFile=../your-keystore.jks
keyAlias=your-alias
storePassword=xxx
keyPassword=xxx
```

## 许可

仅供个人学习与交流，禁止商业用途。
