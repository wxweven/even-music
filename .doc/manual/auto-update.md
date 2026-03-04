# HiFiTi 应用自动升级手册

## 一、整体流程

每次发版的流程：

```
修改版本号 → 构建 Release APK → 更新 version.json → 创建 GitHub Release 上传 APK → 用户打开 App 自动检测更新
```

## 二、前置准备（仅首次）

### 1. 生成签名密钥

Android 升级要求新旧 APK 使用**相同签名**，所以你需要一个固定的 keystore 文件：

```bash
keytool -genkey -v -keystore hifiti-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias hifiti
```

按提示填写信息，记住密码。将 `hifiti-release.jks` 保存在安全的地方（**不要提交到 Git**）。

### 2. 配置签名

在 `android-app/app/build.gradle.kts` 中添加签名配置：

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("/path/to/hifiti-release.jks")
            storePassword = "你的密码"
            keyAlias = "hifiti"
            keyPassword = "你的密码"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
}
```

> 提示：密码建议通过 `local.properties` 或环境变量管理，不要硬编码。

### 3. 版本检查 URL

版本信息文件托管在仓库根目录，Raw URL 为：

```
https://raw.githubusercontent.com/wxweven/even-music/master/version.json
```

## 三、每次发版操作步骤

### 步骤 1：修改版本号

编辑 `android-app/app/build.gradle.kts`：

```kotlin
defaultConfig {
    versionCode = 2          // 每次 +1，系统靠这个判断是否需要升级
    versionName = "1.1.0"    // 给用户看的版本号
}
```

### 步骤 2：构建 Release APK

```bash
cd android-app
./gradlew assembleRelease
```

构建产物在：`app/build/outputs/apk/release/app-release.apk`

### 步骤 3：更新仓库根目录的 version.json

```json
{
  "versionCode": 2,
  "versionName": "1.1.0",
  "apkUrl": "https://github.com/wxweven/even-music/releases/download/v1.1.0/even-music-release.apk",
  "changelog": "- 修复了搜索闪退问题\n- 优化了播放器性能",
  "forceUpdate": false
}
```

字段说明：

| 字段 | 说明 |
|------|------|
| `versionCode` | 必须和 `build.gradle.kts` 中一致，整数，每次递增 |
| `versionName` | 显示给用户的版本号 |
| `apkUrl` | APK 的下载地址（GitHub Release 附件地址） |
| `changelog` | 更新日志，显示在更新弹窗中 |
| `forceUpdate` | `true` 时用户无法跳过更新 |

### 步骤 4：提交并推送 version.json

```bash
git add version.json
git commit -m "release: v1.1.0"
git push origin master
```

### 步骤 5：创建 GitHub Release 并上传 APK

**方式一：使用 gh 命令行（推荐）**

```bash
gh release create v1.1.0 \
  app/build/outputs/apk/release/app-release.apk#even-music-release.apk \
  --title "v1.1.0" \
  --notes "- 修复了搜索闪退问题
- 优化了播放器性能"
```

**方式二：GitHub 网页操作**

1. 打开 https://github.com/wxweven/even-music/releases/new
2. Tag 填写 `v1.1.0`
3. Title 填写 `v1.1.0`
4. 在 "Attach binaries" 区域上传 APK（建议重命名为 `even-music-release.apk`）
5. 填写更新说明
6. 点击 "Publish release"

### 步骤 6：验证

- 确认 `version.json` 可访问：`https://raw.githubusercontent.com/wxweven/even-music/master/version.json`
- 确认 APK 可下载：点击 Release 页面的 APK 链接
- 在旧版本 App 上验证弹出更新提示

## 四、关于数据保留

**无需担心数据丢失**。Android 的升级机制保证以下数据全部保留：

- SharedPreferences（收藏、搜索历史等）
- 内部存储文件（下载的音乐等）
- 数据库

前提是**包名不变**（`com.getmusic.hifiti`）且**签名密钥不变**。

## 五、快速检查清单

每次发版前确认：

- [ ] `versionCode` 已递增
- [ ] `versionName` 已更新
- [ ] 使用的是同一个 keystore 签名
- [ ] `version.json` 中的 `versionCode` 和 `apkUrl` 正确
- [ ] GitHub Release 已创建且 APK 已上传
- [ ] `version.json` 已推送到 master 分支
