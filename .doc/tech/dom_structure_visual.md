## HiFiTi 页面 DOM 结构可视化

```
<!DOCTYPE html>
└── <html lang="zh-cn">
    ├── <head>
    │   ├── <meta charset="utf-8">
    │   ├── <title>张学友《一千个伤心的理由》[FLAC/MP3-320K]</title>
    │   ├── <link> Bootstrap CSS
    │   └── <link> APlayer CSS (plugin/fox_splayer/oddfox/static/css/APlayer.min.css)
    │
    └── <body>
        ├── <header class="navbar"> 
        │   └── 导航栏 (HiFiNi logo, 华语/日韩/欧美等分类)
        │
        ├── <main class="container">
        │   ├── <div class="breadcrumb"> 
        │   │   └── 面包屑导航
        │   │
        │   ├── <article class="thread">
        │   │   ├── <header>
        │   │   │   └── 标题: "张学友《一千个伤心的理由》[FLAC/MP3-320K]"
        │   │   │
        │   │   ├── <div class="message" id="thread-content">
        │   │   │   │
        │   │   │   ├── 🎵 【音乐播放器区域】
        │   │   │   │   ├── <link href="...APlayer.min.css">
        │   │   │   │   └── <div id="FoxSplayer" class="aplayer">
        │   │   │   │       └── "音乐获取中..." (初始化前的占位文本)
        │   │   │   │       
        │   │   │   │       [JavaScript 初始化后变成:]
        │   │   │   │       └── <div class="aplayer-body">
        │   │   │   │           ├── <div class="aplayer-pic">
        │   │   │   │           │   └── <div class="aplayer-button aplayer-play">
        │   │   │   │           │       └── 播放按钮 ▶️
        │   │   │   │           └── <div class="aplayer-info">
        │   │   │   │               ├── <div class="aplayer-music">
        │   │   │   │               │   ├── <span class="aplayer-title">
        │   │   │   │               │   │   └── "一千个伤心的理由" ⭐ 歌曲名在这里
        │   │   │   │               │   └── <span class="aplayer-author">
        │   │   │   │               │       └── "张学友" ⭐ 艺术家在这里
        │   │   │   │               └── <div class="aplayer-controller">
        │   │   │   │                   ├── 进度条
        │   │   │   │                   ├── 时间显示
        │   │   │   │                   └── 音量控制
        │   │   │   │
        │   │   │   ├── 📝 【歌词区域】
        │   │   │   │   ├── <h5>歌词</h5>
        │   │   │   │   └── <p>爱过的人我已不再拥有<br/>...</p>
        │   │   │   │
        │   │   │   ├── 📥 【下载区域】
        │   │   │   │   ├── <h5>下载</h5>
        │   │   │   │   ├── <p><a href="百度网盘链接">...</a></p>
        │   │   │   │   ├── <h5>提取码</h5>
        │   │   │   │   ├── <div class="alert">需要回复后查看</div>
        │   │   │   │   ├── <h5>备份</h5>
        │   │   │   │   └── <div class="alert">需要回复后查看</div>
        │   │   │   │
        │   │   │   └── 🏷️【标签区域】
        │   │   │       └── <div class="thread-tags">
        │   │   │           └── <a href="tag-97.htm">张学友</a>
        │   │   │
        │   │   └── <footer>
        │   │       └── 操作按钮 (反馈、收藏等)
        │   │
        │   └── <aside class="sidebar">
        │       └── 相关推荐 (其他张学友的歌曲)
        │
        └── <footer>
            └── 页面底部信息

```

---

## JavaScript 执行流程

```
页面加载
  ↓
HTML 解析到 <div id="FoxSplayer">
  ↓
显示: "音乐获取中..."
  ↓
加载 APlayer.min.js
  ↓
执行初始化脚本:
const ap = new APlayer({
  element: document.getElementById("FoxSplayer"),
  audio: [{
    name: '一千个伤心的理由',      ← 从这里读取
    artist: '张学友',               ← 从这里读取
    url: 'https://...getmusic.htm?key=...',
    cover: 'https://img1.kuwo.cn/...'
  }]
})
  ↓
APlayer 渲染 DOM 结构
  ↓
创建播放器 UI:
  - .aplayer-title (显示歌曲名)
  - .aplayer-author (显示艺术家)
  - .aplayer-play (播放按钮)
  - <audio> 元素 (HTML5 音频)
  ↓
播放器就绪 ✅
```

---

## 数据提取点位图

```
【方法 1: 从 JavaScript 代码提取】
<script>
const ap = new APlayer({
  audio: [{
    name: '一千个伤心的理由',    ← 提取点 1: 歌曲名
    artist: '张学友',             ← 提取点 2: 艺术家
    url: 'https://...',           ← 提取点 3: 音频 URL (加密)
    cover: 'https://...'          ← 提取点 4: 封面图
  }]
})
</script>

【方法 2: 从渲染后的 DOM 提取】
<div id="FoxSplayer">
  <div class="aplayer-info">
    <span class="aplayer-title">   ← 提取点 1: 歌曲名
      一千个伤心的理由
    </span>
    <span class="aplayer-author">  ← 提取点 2: 艺术家
      张学友
    </span>
  </div>
</div>

【方法 3: 从页面元数据提取】
<meta name="keywords" content="一千个伤心的理由mp3下载,..."/>
                                ↑ 提取点: 关键词
<title>张学友《一千个伤心的理由》[FLAC/MP3-320K]</title>
       ↑                    ↑
  提取点: 艺术家        提取点: 歌曲名
```

---

## 音频 URL 加密机制

```
原始 URL (在 JS 中):
https://www.hifiti.com/getmusic.htm?key=p2CnnZlBoizr...
                                        ↑
                                    加密的 key

Key 的编码规则:
  + (加号) → _2B
  / (斜杠) → _2F

解码过程:
encoded_key = "p2Cnn...VG_2Bw7ql_2FF3W8..."
              ↓
decoded_key = "p2Cnn...VG+w7ql/F3W8..."
              ↓
完整 URL = "https://www.hifiti.com/getmusic.htm?key=" + decoded_key
              ↓
发送 GET 请求 → 可能重定向到真实音频文件 URL
```

---

## 爬虫数据流向图

```
1. 发送请求
   GET https://www.hifiti.com/thread-1394.htm
   ↓
2. 接收 HTML
   ↓
3. 解析 HTML
   ├─→ 提取 <title> → 获取歌曲名+艺术家
   ├─→ 查找 <script> 中的 APlayer 初始化代码
   │   ↓
   │   正则匹配: name:'([^']+)',artist:'([^']+)',url:'([^']+)'
   │   ↓
   │   提取: {name, artist, url, cover}
   │   ↓
   ├─→ 解码 URL key (_2B→+, _2F→/)
   │   ↓
   └─→ 构造完整音频 URL
       ↓
4. (可选) 访问音频 URL 获取真实文件地址
   GET https://www.hifiti.com/getmusic.htm?key=decoded_key
   ↓
5. 下载音频文件
   ↓
6. 保存: {artist}-{song_name}.mp3
```

---

## CSS 选择器速查表

| 目标 | 选择器 | 说明 |
|------|--------|------|
| 播放器容器 | `#FoxSplayer` | ID 选择器 (最快) |
| 歌曲标题 | `.aplayer-title` | 需等 JS 渲染 |
| 艺术家 | `.aplayer-author` | 需等 JS 渲染 |
| 播放按钮 | `.aplayer-play` | 需等 JS 渲染 |
| 封面图片 | `.aplayer-pic img` | 需等 JS 渲染 |
| 歌词内容 | `h5:contains('歌词') + p` | 静态内容 |
| 下载链接 | `h5:contains('下载') + p a` | 静态内容 |
| 标签 | `.thread-tags a[href^="tag-"]` | 静态内容 |
| 页面标题 | `title` | 静态内容 |

注: "需等 JS 渲染" 表示需要使用浏览器自动化工具 (Selenium/Playwright)
