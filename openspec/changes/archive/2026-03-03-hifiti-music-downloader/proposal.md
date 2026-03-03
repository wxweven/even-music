## Why

hifiti.com 网站上有丰富的音乐资源，但目前下载 MP3 需要手动打开开发者工具、找到网络请求、复制链接再下载，流程繁琐且重复。需要一个 Chrome 插件来自动化这个过程——拦截音乐播放请求、提取 MP3 源文件地址并自动下载，同时从页面播放器中提取歌曲名进行命名。

## What Changes

- 新建一个 Chrome Extension（Manifest V3），具备以下能力：
  - 监听 hifiti.com 音乐页面的网络请求，自动捕获 MP3 源文件 URL
  - 从页面 DOM 中提取歌曲名称信息
  - 提供一键下载功能，将 MP3 以歌曲名命名并保存到本地
  - 提供 Popup UI 展示当前页面检测到的音乐列表及下载状态

## Capabilities

### New Capabilities

- `network-interception`: 通过 Chrome webRequest/declarativeNetRequest API 或 content script 拦截和捕获音乐页面中的 MP3 资源请求 URL
- `song-info-extraction`: 从 hifiti.com 页面 DOM 中提取歌曲名称、艺术家等元信息
- `file-download`: 使用 Chrome downloads API 将 MP3 文件以正确的歌曲名下载到本地
- `popup-ui`: 提供 Chrome Extension Popup 界面，展示检测到的音乐列表、下载按钮和下载状态

### Modified Capabilities

（无已有 capability 需要修改）

## Impact

- **新增代码**：完整的 Chrome Extension 项目，包括 manifest.json、background service worker、content script、popup 页面
- **依赖**：Chrome Extension Manifest V3 API（webRequest / declarativeNetRequest、downloads、tabs、activeTab 权限）
- **目标网站**：hifiti.com（需根据其页面结构适配 DOM 解析逻辑）
- **浏览器兼容**：Chrome 88+（Manifest V3 最低版本）
