# 说明书管理

一个用来管理家里各种设备说明书的 Android 应用。

买回来的电器、路由器、开发板，说明书要么丢了要么压在箱底，真要用的时候翻半天。这个应用让你把说明书拍照存进手机，按设备归类，需要时一搜就到。

**所有数据都保存在手机本地，不联网、不上传、不需要账号。**

## 功能

- **设备管理** —— 记录名称、品牌、型号、分类、序列号、购买日期、备注
- **拍照上传** —— 直接调用相机拍说明书，自动摆正方向并压缩
- **相册导入** —— 从相册选择已有图片，也可以拍完再导入
- **图片管理** —— 每台设备可存多张，能给每张加说明文字（比如「第 3 页 接线图」）
- **分类筛选** —— 内置网络设备、家电、机顶盒、开发板等分类，也可自定义
- **搜索排序** —— 按名称/品牌/型号/备注模糊搜索，支持多种排序
- **图片查看器** —— 全屏查看，左右翻页，双指缩放
- **备份与恢复** —— 导出为 zip 文件（含所有图片），换机或重装后导入即可

## 截图

<!-- 有空补上 -->

## 技术栈

| | |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 图片加载 | Coil |
| 数据持久化 | Gson（JSON 文件）+ 应用私有目录 |
| 最低版本 | Android 8.1 (API 27) |

架构很简单，分两层：`ui` 单向依赖 `data`。

- `ManualRepository` 是唯一的数据源和唯一写入者，所有改动经它串行化后落盘
- 设备信息存在 `devices.json`，图片存在 `images/` 目录（只记文件名，不记路径）
- 图片入库前统一走 `ImageStore`：读 EXIF 摆正方向、限制最长边、重新编码为 JPEG
- 备份用 Storage Access Framework 读写 zip，不碰文件路径

没用数据库，是因为数据量小（几百台设备量级），单个 JSON 文件足够且更简单。

## 构建

需要 JDK 21、Android SDK（含 `platforms;android-37.1`、`build-tools;37.0.0`）。

```bash
# 指向你的 SDK
export ANDROID_HOME=/path/to/android-sdk

./gradlew assembleDebug
```

产物在 `app/build/outputs/apk/debug/app-debug.apk`。

> 若用 Android Studio 打开，直接 Sync 即可，SDK 路径会写进 `local.properties`（该文件已在 `.gitignore` 中，不要提交）。

## 权限说明

应用**没有申请任何权限**：

- 拍照走 `ACTION_IMAGE_CAPTURE` + FileProvider，不需要 `CAMERA`
- 图片存在应用私有目录，不需要存储权限
- 备份通过 SAF 让用户自己选位置，不需要存储权限

## 数据位置与备份

数据在应用私有目录：

```
/data/data/com.example.instructionmanual/files/
├── devices.json      # 设备信息与图片索引
└── images/           # 说明书图片（JPEG）
```

这个目录随应用卸载一起删除。**重要资料请用应用内的「导出备份」存一份到别处。**

## 许可证

[MIT](LICENSE)
