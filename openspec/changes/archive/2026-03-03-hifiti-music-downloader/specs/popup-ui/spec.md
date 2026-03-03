## ADDED Requirements

### Requirement: Popup 展示当前页面的音乐列表
插件 SHALL 在 Popup 界面展示当前标签页检测到的所有可下载音乐。

#### Scenario: 在 hifiti.com 音乐页面打开 Popup
- **WHEN** 用户在 hifiti.com 音乐详情页点击插件图标打开 Popup
- **THEN** Popup SHALL 展示当前页面检测到的所有音乐，每条记录包含歌曲名称和下载按钮

#### Scenario: 在非 hifiti.com 页面打开 Popup
- **WHEN** 用户在非 hifiti.com 页面点击插件图标
- **THEN** Popup SHALL 展示提示信息"请在 hifiti.com 音乐页面使用本插件"

#### Scenario: 尚未检测到音乐
- **WHEN** 用户在 hifiti.com 页面打开 Popup，但尚未播放任何音乐
- **THEN** Popup SHALL 展示提示信息"请点击页面上的播放按钮以检测音乐"

### Requirement: 下载按钮与状态展示
Popup SHALL 为每首检测到的歌曲提供下载按钮，并实时展示下载状态。

#### Scenario: 歌曲待下载状态
- **WHEN** 歌曲已被检测到但尚未下载
- **THEN** SHALL 展示可点击的"下载"按钮

#### Scenario: 歌曲下载中状态
- **WHEN** 歌曲正在下载
- **THEN** SHALL 展示"下载中..."状态文字，按钮变为不可点击

#### Scenario: 歌曲下载完成状态
- **WHEN** 歌曲下载已完成
- **THEN** SHALL 展示"已下载 ✓"状态文字

#### Scenario: 歌曲下载失败状态
- **WHEN** 歌曲下载失败
- **THEN** SHALL 展示"下载失败"状态文字和"重试"按钮

### Requirement: 一键全部下载
Popup SHALL 提供一个"全部下载"按钮，支持批量下载当前页面所有检测到的音乐。

#### Scenario: 点击全部下载
- **WHEN** 用户点击"全部下载"按钮
- **THEN** SHALL 依次对所有尚未下载的歌曲发起下载

#### Scenario: 无可下载歌曲时
- **WHEN** 没有检测到任何歌曲或所有歌曲已下载
- **THEN** "全部下载"按钮 SHALL 变为不可点击状态
