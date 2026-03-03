## Context

hifiti.com 是一个音乐资源网站，用户在音乐详情页（如 `/thread-*.htm`）点击播放按钮后，前端 JS 会发起 HTTP 请求获取 MP3 源文件。目前用户想下载音乐需要手动打开 DevTools 抓取请求 URL，流程繁琐。

当前项目为全新项目，没有已有代码或架构约束。需要基于 Chrome Extension Manifest V3 构建。

## Goals / Non-Goals

**Goals:**

- 自动捕获 hifiti.com 音乐页面中的 MP3 资源请求 URL
- 从页面 DOM 提取歌曲名称用于文件命名
- 提供简洁的 Popup UI 展示音乐列表和一键下载
- 支持用户在音乐页面无需额外操作即可发现可下载的音乐

**Non-Goals:**

- 不支持批量爬取整个网站的音乐
- 不支持 hifiti.com 以外的网站
- 不做音乐播放器功能（网站自身已有）
- 不做音乐格式转换
- 不支持需要登录才能访问的付费内容破解

## Decisions

### 1. 使用 Manifest V3 而非 V2

**选择**: Manifest V3
**理由**: Chrome 已逐步淘汰 Manifest V2，新扩展必须使用 V3。V3 使用 Service Worker 替代 Background Page，更安全、更省资源。
**替代方案**: Manifest V2 仍可用但即将被禁用，不适合新项目。

### 2. MP3 URL 捕获方案：Content Script 监听 + 页面脚本注入

**选择**: 通过 Content Script 注入页面脚本，拦截 XMLHttpRequest / fetch 调用来捕获 MP3 URL
**理由**:
- hifiti.com 的播放器是前端 JS 驱动的，点击播放后通过 XHR/fetch 请求获取音频 URL
- Content Script 可以注入页面上下文的脚本来 hook 网络请求
- 捕获到的 URL 通过 `window.postMessage` 传给 Content Script，再通过 `chrome.runtime.sendMessage` 传给 Background Service Worker
**替代方案**:
- `chrome.webRequest` API：Manifest V3 中 webRequest 的拦截能力受限（仅 `onBeforeRequest` 在 declarativeNetRequest 下有限支持），且对 media 请求的捕获不够灵活
- `chrome.declarativeNetRequest`：主要用于请求修改/拦截，不适合纯粹的 URL 捕获场景

### 3. 歌曲信息提取方式：DOM 解析

**选择**: Content Script 直接解析页面 DOM 获取歌曲名
**理由**: hifiti.com 的播放器 UI 中包含歌曲名称信息，通过 CSS 选择器可以直接获取，无需额外 API 调用。
**替代方案**: 从 MP3 文件的 ID3 标签读取——增加复杂度且需要额外库。

### 4. UI 技术栈：原生 HTML/CSS/JS

**选择**: 使用原生 HTML + CSS + JavaScript 构建 Popup
**理由**: 插件功能简单，UI 仅需展示列表和下载按钮，引入框架（React/Vue）会增加不必要的打包复杂度和体积。
**替代方案**: 使用 React/Vue —— 对这个简单 UI 来说 overkill。

### 5. 文件下载方案：chrome.downloads API

**选择**: 使用 `chrome.downloads.download()` API
**理由**: Chrome Extension 原生提供的下载 API，支持自定义文件名，可靠且无需额外权限。

## Risks / Trade-offs

- **[网站结构变更]** → hifiti.com 页面改版后 DOM 选择器可能失效。缓解：将选择器配置化，便于快速更新。
- **[MP3 URL 格式变化]** → 如果网站改用加密或 token 化的 URL，拦截方案可能失效。缓解：当前先做最简方案，后续按需调整。
- **[Manifest V3 Service Worker 生命周期]** → Service Worker 会在空闲时被销毁，需确保消息传递机制不受影响。缓解：使用 `chrome.runtime.onMessage` 响应式通信，不依赖长驻后台。
- **[跨域下载限制]** → MP3 文件可能在 CDN 域名下，`chrome.downloads` API 不受同源策略限制，无此问题。
