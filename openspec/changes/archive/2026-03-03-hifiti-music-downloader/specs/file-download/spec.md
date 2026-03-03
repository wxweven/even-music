## ADDED Requirements

### Requirement: 使用 Chrome Downloads API 下载音频文件
插件 SHALL 使用 `chrome.downloads.download()` API 将捕获到的音频文件下载到用户本地。

#### Scenario: 成功下载 MP3 文件
- **WHEN** 用户在 Popup 界面点击某首歌曲的下载按钮
- **THEN** Background Service Worker SHALL 调用 `chrome.downloads.download()` 发起下载，文件以歌曲名命名（如 `歌曲名.mp3`）

#### Scenario: 自动确定文件扩展名
- **WHEN** 音频 URL 包含文件扩展名（如 `.mp3`、`.m4a`）
- **THEN** 下载的文件 SHALL 使用对应的扩展名
- **WHEN** URL 不包含明确的扩展名
- **THEN** SHALL 默认使用 `.mp3` 作为扩展名

### Requirement: 下载状态追踪
插件 SHALL 追踪每个下载任务的状态（等待中、下载中、已完成、失败）。

#### Scenario: 下载进度更新
- **WHEN** 下载任务开始后
- **THEN** SHALL 通过 `chrome.downloads.onChanged` 监听下载状态变化，并将状态同步到 Popup UI

#### Scenario: 下载失败处理
- **WHEN** 下载任务失败（网络错误、URL 失效等）
- **THEN** SHALL 将状态更新为"失败"，并在 Popup 中展示错误提示

### Requirement: 避免重复下载
插件 SHALL 避免对同一首歌曲发起重复下载。

#### Scenario: 用户重复点击下载
- **WHEN** 用户对已经在下载或已下载完成的歌曲再次点击下载按钮
- **THEN** SHALL 忽略此操作或给出已下载/正在下载的提示
