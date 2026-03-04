# HiFiTi 音乐下载工具

从 [hifiti.com](https://www.hifiti.com) 搜索并下载高品质音乐（FLAC/MP3-320K），提供三种使用方式。

## 使用方式

### 1. Android App

Kotlin + Jetpack Compose 开发的原生 Android 应用，内置音乐播放器。

```
cd android-app
# 用 Android Studio 打开，连接手机运行
```

**功能：** 搜索歌曲 → 查看详情（封面、歌词） → 点击即播（流式播放） → 可选下载到 Music/HiFiTi/

**播放器特性：**
- 在线流式播放，点击即听，无需等待下载
- 后台播放，切出 App / 锁屏后继续播放
- 通知栏 + 锁屏界面播放控制
- 播放列表管理，支持上一首 / 下一首
- 迷你播放栏，搜索页底部实时显示播放状态

### 2. Chrome 插件

在 hifiti.com 歌曲页面上一键下载 MP3。

```
# Chrome 浏览器 → 扩展程序 → 开发者模式 → 加载已解压的扩展程序 → 选择 chrome-extension 目录
```

### 3. Python 脚本

命令行工具，传入歌曲页面 URL 直接下载。

```bash
cd python-script
pip install -r requirements.txt
python hifiti_extractor.py https://www.hifiti.com/thread-1394.htm
```

支持批量下载：

```bash
python hifiti_extractor.py URL1 URL2 URL3
```

## 项目结构

```
├── android-app/          # Android App（Kotlin + Compose）
├── chrome-extension/     # Chrome 浏览器插件
├── python-script/        # Python 命令行脚本
└── .doc/                 # 项目文档
```

## 技术栈

| 模块 | 技术 |
|------|------|
| Android App | Kotlin, Jetpack Compose, Material 3, Media3 (ExoPlayer), OkHttp, Jsoup, Coil |
| Chrome 插件 | Manifest V3, Content Script, Chrome Downloads API |
| Python 脚本 | requests, BeautifulSoup4 |
