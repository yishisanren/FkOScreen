# FkOScreen · ColorOS 17

本仓库是 [benbaobaoshigemi/FkOScreen](https://github.com/benbaobaoshigemi/FkOScreen) 的 ColorOS 17 适配 Fork，当前合并上游 `v1.1.0`（`959a29594352b71af978d516438955aa0396bc1d`），保留新版界面和功能，以及此前验证的亮度、HDR 缓存和色温修复。

**[下载 1.1 Release](https://github.com/yishisanren/FkOScreen/releases/tag/v1.1)** · [旧版 1.0](https://github.com/yishisanren/FkOScreen/releases/tag/v1.0)

Release 名称为 **1.1**；APK 内部版本为 `1.1.0-coloros17.1`，versionCode 为 `3`。这是未启用 `debuggable` 的适配包，沿用本仓库 1.0 Release 的本地签名，可覆盖升级；与原作者签名不同。

## 已验证环境

当前适配目标：OPPO PME110，`PME110_17.0.0.103(CN01)`，Android 17 / API 37，LSPosed 2.2.1 (7925)。此前 1.0 Release 验证的是 `.102`。其他机型或系统版本尚未验证。

## 适配内容

- 手动 HBM：接入新版亮度模型、BrightnessInfo 和 OPlus 控制中心滑杆范围。关闭自动亮度后，滑杆可调到本机 HBM 档，关闭 HBM 后恢复原生上限。
- HDR：返回独立 DisplayInfo 副本；开关变化时使客户端显示缓存失效，关闭后恢复原生 HDR/SDR 比例。
- 色彩双滑杆：接入新版 `color.config` / `color.control` / `color.model` 管线，叠加色温球 RGB 白点并触发原生 CCT 重绘。
- 系统设置：开放深色模式增强、适中、柔和三个选项，以及色温球入口。
- 跨进程配置：使用 `Settings.System` 和 `ContentObserver` 同步。写入放行仅针对模块自身 UID 与明确的配置键。
- FOSS：接入本机仍存在的 `getReduceInfo` / `getFossInfo` 接口；未测量特定应用实际降亮幅度。
- 全景 AOD 亮度：保留上游 ×0.50～×1.00 滑杆，默认原生 ×0.625；保留暗处第 0 档及息屏前亮度上限。
- 144Hz：保留上游从设置选择到系统刷新率策略的实现，支持面板具有 144Hz 模式的本机。
- 护眼与晕动舒缓：保留上游阅读模式、纸质效果和车辆自动开启选项；护眼系统端改接当前 MatrixConverter / OplusFeatureReadMode 管线。
- 温控实验项：补全上游新增的拖动限制查询和独立 EDR 温度入口，仅在既有温控绕过开关开启时生效。

峰值亮度与温控绕过属于实验功能，首次安装默认关闭；覆盖升级会保留已有配置，包括已开启的实验项。没有做高温或长时峰值测试。

## 安装

1. 设备需 Root，并安装支持 legacy 模块的 LSPosed。
2. 安装 Release 中的 APK；在 LSPosed 中启用并勾选**系统框架（`system`）**、**系统界面（`com.android.systemui`）**、**设置（`com.android.settings`）**、**护眼（`com.oplus.eyeprotect`）**。
3. 授予模块修改设置权限。可在 Root shell 中执行：

   ```sh
   pm grant io.github.benbaobaoshigemi.fkoscreen android.permission.WRITE_SECURE_SETTINGS
   appops set io.github.benbaobaoshigemi.fkoscreen WRITE_SETTINGS allow
   ```

4. 完整重启，然后打开模块设置。

本机 SettingsProvider 与系统框架共享进程，不需要额外勾选“设置存储”。手动 HBM 只在关闭系统“自动调节”后扩展范围。

升级系统或 LSPosed 后，应检查系统框架作用域仍为 `system`。本次更新发现作用域为 `android` 时，模块只加载了界面、没有加载系统框架；已按实际进程日志修正。

若已安装原作者版本，因签名不同，可能需先卸载原版；卸载只会移除该模块自身配置。回退显示效果时，在 LSPosed 关闭本模块并重启。

## 验收与构建

- [真机验收说明](docs/ACCEPTANCE.md)：工具实测结果与未验证范围。
- [构建说明](docs/BUILD.md)：Release 的构建方法、工具依赖及签名要求。
- [上游 README](docs/UPSTREAM_README.md)：保留原版说明，不作为本适配版兼容性承诺。

没有用光度计测量真实亮度或色准，也没有确认 Ultra HDR 高光观感。亮度曲线中的 nits 参数不等于光学实测值。

## 许可

沿用上游 [Apache License 2.0](LICENSE)，作者及改动说明见 [NOTICE](NOTICE)。签名私钥不在仓库中。
