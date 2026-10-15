# 蹭课网 Android 课表导出工具

这是一个面向南京大学教务系统的 Android 导出工具，当前版本 v0.3.0。它在手机内打开教务页面，由用户手动登录并导出课程 JSON，供蹭课网网页端或微信小程序导入。

原型会在应用内打开南大 HTTPS 页面。用户手动登录并进入课表列表后，可以点击“读取当前页”或“读取全部页”。全部读取会检查分页连续性、总记录数和重复页；识别成功后可以点击“保存 JSON”写入应用私有目录，或点击“分享 JSON”写入公共 `Download/蹭课网/` 并打开系统分享面板。文件不会上传到云端。

访问范围限制为 `*.nju.edu.cn`，应用不接入云数据库、账号系统或服务器，也不读取、保存或提交密码。

## 使用方式

1. 用 Android Studio 打开本仓库目录；
2. 等待 Gradle 同步完成，连接 Android 8.0 以上手机或启动模拟器；
3. 点击工具栏的运行按钮安装调试版；
4. 点击“打开全校课表”，在页面内手动登录，进入本科“全校课程表查询”并选择学期；
5. 确认课程表格已经显示后，点击“读取当前页”；
6. 看到“读取成功”后点击“保存 JSON”；如果要传给微信小程序，点击“分享 JSON”，选择微信文件传输助手。

读取完整课表时，先点击“读取全部页”，等待状态显示全部读取成功，再分享 JSON。小程序收到文件后点击“选择 JSON”导入。

如果页面提示未找到标准课表列表，先确认当前页面是课程列表而不是登录页，并确保表头和底部分页栏可见。开始全部读取前请回到第 1 页，并保持筛选条件不变。

## 构建

使用 Android Studio 自带的 Gradle 集成，项目要求 JDK 17、Android SDK 35、Build Tools 34.0.0。命令行构建需要在本机安装 Gradle 8.11.1 后运行 `gradle assembleDebug`。本项目未提交真实账号、Cookie 或课表数据。

## 下载 APK

稳定 APK 应通过 GitHub Releases 发布。不要把真实课程 JSON、账号信息或本地缓存放入仓库；APK 未签名时只适合个人测试。

## 适配范围

当前只适配南京大学“本科课表查询”页面的标准课程列表，并支持当前页读取和分页读取。页面结构变化、登录策略变化或非南大系统可能导致读取失败。

## 隐私

详见 [PRIVACY.md](PRIVACY.md)。

### Gradle 下载失败

如果 Android Studio 提示下载 `gradle-9.3.0-bin.zip` 连接被拒绝，打开 `File > Settings > Build, Execution, Deployment > Build Tools > Gradle`，把 `Gradle distribution` 改为 `Local installation`，选择本机的 Gradle 8.11.1 目录。当前电脑的缓存目录通常是：

`C:\Users\lenovo\.gradle\wrapper\dists\gradle-8.11.1-bin\eac4u065zwes5phgltp5f9b9e\gradle-8.11.1`

`Gradle JDK` 选择 Android Studio 自带的 `Embedded JDK`，然后点击 `Apply`、`OK`，再点右上角的 Gradle 同步按钮。如果本机没有这个目录，就先在 Android Studio 的 SDK/Gradle 设置里配置可用的 JDK，并在网络可用时下载 Gradle 8.11.1。
