# HiFiTi 音乐页面结构分析报告

## 页面基本信息
- **URL**: https://www.hifiti.com/thread-1394.htm
- **歌曲**: 张学友《一千个伤心的理由》[FLAC/MP3-320K]
- **网站**: HiFiNi - 音乐磁场

---

## 1. 音乐播放器 UI 结构

### 播放器类型
该网站使用 **APlayer** 作为音乐播放器（一个流行的 HTML5 音频播放器）

### DOM 结构
```html
<link href="plugin/fox_splayer/oddfox/static/css/APlayer.min.css" rel="stylesheet">
<div id="FoxSplayer" class="aplayer">音乐获取中...</div>
```

### 关键元素
- **容器 ID**: `FoxSplayer`
- **容器 Class**: `aplayer`
- **初始化文本**: "音乐获取中..."

---

## 2. CSS 选择器方案

### 推荐的 CSS 选择器

#### 查找播放器容器
- `#FoxSplayer` - 通过 ID 选择（最精确）
- `.aplayer` - 通过 class 选择

#### 查找歌曲名称
根据 APlayer 的标准 DOM 结构，歌曲名称通常在以下位置：
- `.aplayer-title` - 歌曲标题
- `.aplayer-author` - 艺术家名称
- `.aplayer .aplayer-info .aplayer-music .aplayer-title` - 完整路径

#### 查找播放按钮
- `.aplayer-icon-play` - 播放按钮
- `.aplayer-button` - 播放/暂停按钮

---

## 3. JavaScript 初始化代码

### 播放器配置
```javascript
const ap = new APlayer({
    element: document.getElementById("FoxSplayer"),
    autoplay: false,
    preload: "none",
    mutex: true,
    theme: "#090",
    audio: [{
        name: '一千个伤心的理由',
        artist: '张学友',
        url: 'https://www.hifiti.com/getmusic.htm?key=p2CnnZlBoizrIUxOLCkSrBfSxLyN8ySQKKXBMN_2Bt1Ts5rSv7Fv6kogt20CceNryc3ZZZutkKt8x5o3VG_2Bw7ql_2FF3W8Yc6erwrUIQaN7pZRYGc_2FbV5Y3KwROou0G_2FqdM_2F4UMCPpRp5H8ayTXx_2BL_2BVPeyIUu_2FxErsye4qFjOw8cDHRfNHr0jzx_2B7_2BSb6wcZoK9afxsx44hsHI5mVKSPBlZLLB8w_2FIkl534PcCLh9iOfPmsr76Vd5Xr0dsxIzo81WuF84x2Zps8jXyknGqq',
        cover: 'https://img1.kuwo.cn/star/albumcover/500/s4s8/43/2675816828.jpg'
    }]
});
```

### 关键配置参数
- **name**: 歌曲名称（DOM 中可提取）
- **artist**: 艺术家名称（DOM 中可提取）
- **url**: 音频文件 URL（通过加密 key 获取）
- **cover**: 封面图片 URL
- **autoplay**: false（不自动播放）
- **preload**: "none"（不预加载）
- **mutex**: true（互斥播放）

---

## 4. 音频播放机制

### 播放器类型
- **HTML5 Audio Player**: APlayer 基于 HTML5 `<audio>` 元素
- **插件路径**: `plugin/fox_splayer/oddfox/static/js/APlayer.min.js`

### 音频 URL 获取方式
音频文件通过动态 API 获取：
```
https://www.hifiti.com/getmusic.htm?key=[加密的 key]
```

**注意**: 
- URL 中的 key 是经过编码的（`_2B` 代表 `+`，`_2F` 代表 `/`）
- 这是一个防盗链机制，需要解码才能获取真实音频链接

### 解码 key 示例
原始 key (URL编码):
```
p2CnnZlBoizrIUxOLCkSrBfSxLyN8ySQKKXBMN_2Bt1Ts5rSv7Fv6kogt20CceNryc3ZZZutkKt8x5o3VG_2Bw7ql_2FF3W8Yc6erwrUIQaN7pZRYGc_2FbV5Y3KwROou0G_2FqdM_2F4UMCPpRp5H8ayTXx_2BL_2BVPeyIUu_2FxErsye4qFjOw8cDHRfNHr0jzx_2B7_2BSb6wcZoK9afxsx44hsHI5mVKSPBlZLLB8w_2FIkl534PcCLh9iOfPmsr76Vd5Xr0dsxIzo81WuF84x2Zps8jXyknGqq
```

解码规则:
- `_2B` → `+`
- `_2F` → `/`

---

## 5. 页面其他元素

### 歌词区域
```html
<h5>歌词</h5>
<p>
    爱过的人我已不再拥有<br />
    许多故事有伤心的理由<br />
    ...
</p>
```

### 下载区域
```html
<h5>下载</h5>
<p>
    <a href="https://pan.baidu.com/s/1BHVpm779v15rSV2BiIlBHA" target="_blank">
        https://pan.baidu.com/s/1BHVpm779v15rSV2BiIlBHA
    </a>
</p>

<h5>提取码</h5>
<p>
    <div class="alert alert-warning">
        本帖含有隐藏内容，请您回复后查看
    </div>
</p>
```

**注意**: 提取码和备份链接需要登录后回复才能查看

---

## 6. 爬虫建议

### 提取歌曲信息的方法

#### 方法 1: 直接从 JavaScript 代码提取
```python
import re
import requests

# 获取页面内容
response = requests.get('https://www.hifiti.com/thread-1394.htm')
html = response.text

# 正则匹配 APlayer 配置
pattern = r"name:'([^']+)',artist:'([^']+)',url:'([^']+)'"
matches = re.search(pattern, html)

if matches:
    song_name = matches.group(1)  # 一千个伤心的理由
    artist = matches.group(2)     # 张学友
    audio_url = matches.group(3)  # 加密的 URL
```

#### 方法 2: 等待 DOM 渲染后提取
使用 Selenium 或 Playwright:
```python
from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page()
    page.goto('https://www.hifiti.com/thread-1394.htm')
    
    # 等待播放器加载
    page.wait_for_selector('#FoxSplayer')
    
    # 提取歌曲名称
    song_title = page.query_selector('.aplayer-title').inner_text()
    artist = page.query_selector('.aplayer-author').inner_text()
```

### 解码音频 URL
```python
def decode_url_key(encoded_key):
    """解码 HiFiTi 的 URL key"""
    decoded = encoded_key.replace('_2B', '+').replace('_2F', '/')
    return decoded

# 使用
audio_url = "https://www.hifiti.com/getmusic.htm?key=" + decode_url_key(encoded_key)
```

### 获取真实音频文件
```python
# 访问 getmusic.htm 可能会重定向到真实的音频文件
import requests

response = requests.get(audio_url, allow_redirects=True)
real_audio_url = response.url  # 获取最终 URL
```

---

## 7. 技术栈总结

- **前端框架**: Bootstrap (响应式布局)
- **音频播放器**: APlayer (HTML5)
- **JavaScript 库**: jQuery
- **后端**: 自定义 PHP/ASP (基于 .htm 扩展名推测)
- **防盗链**: 基于加密 key 的动态 URL 生成

---

## 8. 注意事项

1. **反爬虫机制**:
   - 音频 URL 使用加密 key
   - 下载链接需要登录+回复
   - 可能有 User-Agent 检测

2. **合规建议**:
   - 尊重网站的 robots.txt
   - 控制爬取频率
   - 仅用于个人学习和研究

3. **数据时效性**:
   - 音频 URL 的 key 可能有时效性
   - 百度网盘链接可能会失效

---

## 9. APlayer 标准 DOM 结构参考

当 APlayer 完全加载后，DOM 结构如下：

```html
<div id="FoxSplayer" class="aplayer">
    <div class="aplayer-body">
        <div class="aplayer-pic">
            <div class="aplayer-button aplayer-play">
                <svg class="aplayer-icon aplayer-icon-play">...</svg>
            </div>
        </div>
        <div class="aplayer-info">
            <div class="aplayer-music">
                <span class="aplayer-title">一千个伤心的理由</span>
                <span class="aplayer-author">张学友</span>
            </div>
            <div class="aplayer-controller">
                <div class="aplayer-bar-wrap">
                    <div class="aplayer-bar">...</div>
                </div>
                <div class="aplayer-time">...</div>
                <div class="aplayer-volume-wrap">...</div>
            </div>
        </div>
    </div>
</div>
```

### 关键选择器汇总

| 元素 | CSS 选择器 |
|------|-----------|
| 播放器容器 | `#FoxSplayer` |
| 歌曲标题 | `#FoxSplayer .aplayer-title` |
| 艺术家 | `#FoxSplayer .aplayer-author` |
| 播放按钮 | `#FoxSplayer .aplayer-play` |
| 封面图 | `#FoxSplayer .aplayer-pic` |
| 进度条 | `#FoxSplayer .aplayer-bar` |
| 音量控制 | `#FoxSplayer .aplayer-volume-wrap` |

---

## 完成日期
2026-03-03
