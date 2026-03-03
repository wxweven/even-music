## ADDED Requirements

### Requirement: 从页面 DOM 提取歌曲名称
插件 SHALL 从 hifiti.com 音乐详情页的播放器 UI 中提取歌曲名称信息。

#### Scenario: 成功提取歌曲名称
- **WHEN** 用户访问 hifiti.com 音乐详情页且页面包含播放器组件
- **THEN** Content Script SHALL 从播放器 DOM 元素中提取歌曲名称文本

#### Scenario: 页面结构异常时的降级处理
- **WHEN** 页面 DOM 结构不符合预期（选择器未匹配到元素）
- **THEN** SHALL 使用页面标题（`document.title`）作为降级歌曲名称，并在名称前加上 "unknown-" 前缀标记

### Requirement: 歌曲信息与音频 URL 关联
插件 SHALL 将提取到的歌曲名称与对应的音频 URL 进行关联，形成完整的歌曲记录。

#### Scenario: 歌曲名称与 URL 成功关联
- **WHEN** Content Script 同时获取到歌曲名称和音频 URL
- **THEN** SHALL 将歌曲名称和 URL 打包为一个歌曲记录对象，发送给 Background Service Worker

### Requirement: 歌曲名称清洗
插件 SHALL 对提取到的歌曲名称进行清洗，移除不适合作为文件名的特殊字符。

#### Scenario: 移除文件名非法字符
- **WHEN** 歌曲名称包含 `/`、`\`、`:`、`*`、`?`、`"`、`<`、`>`、`|` 等字符
- **THEN** SHALL 将这些字符替换为 `-` 或移除，确保名称可作为合法文件名使用
