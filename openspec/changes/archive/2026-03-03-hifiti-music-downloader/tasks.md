## 1. 项目初始化

- [ ] 1.1 创建 Chrome Extension 目录结构（manifest.json、background.js、content.js、popup 文件）
- [ ] 1.2 配置 manifest.json（Manifest V3，声明 permissions、host_permissions、content_scripts）
- [ ] 1.3 创建扩展图标（16/48/128px）

## 2. Content Script - 页面信息提取

- [ ] 2.1 实现 APlayer 配置正则提取（从 script 标签中匹配 name/artist/url/cover）
- [ ] 2.2 实现页面 title 降级提取（当 JS 提取失败时从标题解析艺术家和歌曲名）
- [ ] 2.3 实现歌曲信息主动上报（页面加载完成后通过 chrome.runtime.sendMessage 发送给 background）
- [ ] 2.4 实现按需查询响应（popup 请求时通过 onMessage 回复当前页面歌曲列表）

## 3. Background Service Worker - 下载管理

- [ ] 3.1 实现 tab 级歌曲缓存管理（tabSongs 存储、tab 关闭时清理）
- [ ] 3.2 实现音频 URL 解析（fetch getmusic.htm 跟随重定向获取真实 CDN 地址）
- [ ] 3.3 实现文件名清洗（移除非法字符）和扩展名检测
- [ ] 3.4 实现 chrome.downloads.download 调用（自动命名为「艺术家 - 歌曲名.mp3」）
- [ ] 3.5 实现下载状态追踪（通过 chrome.downloads.onChanged 监听状态变化）
- [ ] 3.6 实现防重复下载逻辑

## 4. Popup UI - 用户界面

- [ ] 4.1 实现 Popup 布局（歌曲列表含封面、名称、艺术家、下载按钮）
- [ ] 4.2 实现非 hifiti.com 页面的提示信息
- [ ] 4.3 实现下载状态实时展示（等待/下载中/已完成/失败+重试）
- [ ] 4.4 实现「全部下载」按钮
- [ ] 4.5 实现「刷新」按钮（重新从 content script 提取歌曲）

## 5. 测试与验证

- [ ] 5.1 在 chrome://extensions 加载插件并测试基本流程
- [ ] 5.2 测试单首下载（thread-1394.htm 张学友《一千个伤心的理由》）
- [ ] 5.3 测试批量下载和状态追踪
- [ ] 5.4 测试非音乐页面的 Popup 提示
- [ ] 5.5 测试 tab 切换和关闭时的状态清理
