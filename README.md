# FkOScreen · ColorOS 17

本仓库是 [benbaobaoshigemi/FkOScreen](https://github.com/benbaobaoshigemi/FkOScreen) 的 ColorOS 17 适配 Fork，基于上游 `da7499dbce5b21cb2209f689c97c76b156b87327`。保留原版 Compose/Miuix 界面，适配当前系统的亮度、HDR 和色温接口。

**[下载 1.0 Release](https://github.com/yishisanren/FkOScreen/releases/tag/v1.0)**

Release 名称为 **1.0**；APK 内部版本为 `1.0.0-coloros17.1`，versionCode 为 `2`。这是未启用 `debuggable` 的适配包，使用本地测试签名，与原作者签名不同。

## 已验证环境

OPPO PME110，`PME110_17.0.0.102(CN01)`，Android 17 / API 37，LSPosed 2.2.1 (7912)。其他机型或系统版本尚未验证。

## 适配内容

- 手动 HBM：接入新版亮度模型、BrightnessInfo 和 OPlus 控制中心滑杆范围。关闭自动亮度后，滑杆可调到本机 HBM 档，关闭 HBM 后恢复原生上限。
- HDR：返回独立 DisplayInfo 副本；开关变化时使客户端显示缓存失效，关闭后恢复原生 HDR/SDR 比例。
- 色彩双滑杆：接入新版 `color.config` / `color.control` / `color.model` 管线，叠加色温球 RGB 白点并触发原生 CCT 重绘。
- 系统设置：开放深色模式增强、适中、柔和三个选项，以及色温球入口。
- 跨进程配置：使用 `Settings.System` 和 `ContentObserver` 同步。写入放行仅针对模块自身 UID 与明确的配置键。
- FOSS：接入本机仍存在的 `getReduceInfo` / `getFossInfo` 接口；未测量特定应用实际降亮幅度。

峰值亮度与温控绕过属于实验功能，默认关闭；没有做高温或长时峰值测试。

## 安装

1. 设备需 Root，并安装支持 legacy 模块的 LSPosed。
2. 安装 Release 中的 APK；在 LSPosed 中启用并勾选**系统框架（`system`）**、**系统界面（`com.android.systemui`）**、**设置（`com.android.settings`）**。
3. 授予模块修改设置权限。可在 Root shell 中执行：

   ```sh
   pm grant io.github.benbaobaoshigemi.fkoscreen android.permission.WRITE_SECURE_SETTINGS
   appops set io.github.benbaobaoshigemi.fkoscreen WRITE_SETTINGS allow
   ```

4. 完整重启，然后打开模块设置。

本机 SettingsProvider 与系统框架共享进程，不需要额外勾选“设置存储”。手动 HBM 只在关闭系统“自动调节”后扩展范围。

若已安装原作者版本，因签名不同，可能需先卸载原版；卸载只会移除该模块自身配置。回退显示效果时，在 LSPosed 关闭本模块并重启。

## 验收与构建

- [真机验收说明](docs/ACCEPTANCE.md)：工具实测结果与未验证范围。
- [构建说明](docs/BUILD.md)：Release 的构建方法、工具依赖及签名要求。
- [上游 README](docs/UPSTREAM_README.md)：保留原版说明，不作为本适配版兼容性承诺。

没有用光度计测量真实亮度或色准，也没有确认 Ultra HDR 高光观感。亮度曲线中的 nits 参数不等于光学实测值。

## 许可

沿用上游 [Apache License 2.0](LICENSE)，作者及改动说明见 [NOTICE](NOTICE)。签名私钥不在仓库中。
