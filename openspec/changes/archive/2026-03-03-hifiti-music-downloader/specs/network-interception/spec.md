## ADDED Requirements

### Requirement: 自动捕获 MP3 资源 URL
插件 SHALL 在用户访问 hifiti.com 音乐详情页（URL 匹配 `*://www.hifiti.com/thread-*.htm`）时，自动监听页面中的网络请求，捕获所有音频资源（MP3/M4A 等）的 URL。

#### Scenario: 用户点击播放按钮后捕获 MP3 URL
- **WHEN** 用户在 hifiti.com 音乐详情页点击播放按钮
- **THEN** 插件 SHALL 捕获到播放器发起的音频文件请求 URL，并将其存储供后续下载使用

#### Scenario: 页面加载时自动注入监听脚本
- **WHEN** 用户导航到匹配 `*://www.hifiti.com/thread-*.htm` 的页面
- **THEN** Content Script SHALL 自动注入并开始监听网络请求，无需用户手动操作

### Requirement: 音频 URL 识别与过滤
插件 SHALL 仅捕获音频类型的资源 URL，过滤掉非音频请求（如图片、CSS、JS 等）。

#### Scenario: 正确识别 MP3 资源
- **WHEN** 页面发起的网络请求 URL 包含 `.mp3`、`.m4a`、`.flac` 等音频文件扩展名，或响应的 Content-Type 为 `audio/*`
- **THEN** 该 URL SHALL 被捕获并记录

#### Scenario: 过滤非音频请求
- **WHEN** 页面发起的网络请求为图片、样式表、脚本等非音频资源
- **THEN** 该请求 SHALL 被忽略，不记录

### Requirement: 捕获结果传递给 Background Service Worker
插件 SHALL 将 Content Script 捕获到的音频 URL 通过消息机制传递给 Background Service Worker 进行集中管理。

#### Scenario: URL 成功传递到后台
- **WHEN** Content Script 捕获到一个新的音频 URL
- **THEN** SHALL 通过 `chrome.runtime.sendMessage` 将 URL 和相关页面信息发送给 Background Service Worker
