# 构建说明

1.1 Release 使用原版 `v1.1.0` APK 为基线，保留新版资源、Compose/Miuix 界面及 Kotlin 功能，在 `classes4.dex` 加入当前仓库中的 Java 兼容入口，并修改配置同步、作用域及版本号。兼容入口保留已验证的亮度/HDR/色温修复，并调用上游新增的 AOD、144Hz、晕动和护眼界面入口；护眼系统端使用当前原生管线。Gradle 工程同步记录对应源码改动。

构建脚本已完整实跑，通过签名和 zipalign 校验。Gradle Release 配置明确关闭 debuggable，未绑定 debug 签名；独立 Gradle 构建需自行配置签名。本次采用下述构建流程，不以独立 Gradle 输出替换验收包。

## 工具依赖

- Python 3.9+、JDK 21。
- Android SDK：Platform 36、Build Tools 36.0.0，以及带 `com.android.tools.smali` 库的 Command-line Tools；本次使用 smali 3.0.3。
- apktool 3.0.3。
- Xposed API 82 JAR，仅用于编译。
- 自己的签名密钥。仓库和 Release 不提供本次签名私钥。

将原作者 [v1.1.0 Release](https://github.com/benbaobaoshigemi/FkOScreen/releases/tag/v1.1.0) 中的 APK 下载到本地。脚本要求其 SHA-256 为：

```text
d1821d95f2f1adcc666b6fb96587a57fb8f4c3f6629bfbb429a86ef62cd57a4b
```

## 构建适配包

以下路径均替换为本机工具实际位置；密码由环境变量传给 apksigner，不写入源码或命令参数。

```sh
read -s FKO_KEYSTORE_PASSWORD
export FKO_KEYSTORE_PASSWORD

python3 tools/build_compat.py \
  --source-apk local/io.github.benbaobaoshigemi.fkoscreen-v1.1.0.apk \
  --sdk "$ANDROID_SDK_ROOT" \
  --java-home "$JAVA_HOME" \
  --apktool local/apktool.jar \
  --xposed-api local/xposed-api-82.jar \
  --keystore local/fkoscreen-release.jks \
  --alias fkoscreen

unset FKO_KEYSTORE_PASSWORD
```

默认输出为 `build/compat/FkOScreen-1.1.0-coloros17.1.apk`，versionCode 3，同时输出 SHA256SUMS.txt。若私钥密码与 keystore 密码不同，可另设置 `FKO_KEY_PASSWORD`。

脚本依次执行：核对原包哈希 → 编译兼容类 → D8 → 解包原包 → 添加兼容 dex / 同步调用 → apktool → zipalign → 签名 → 签名和包内容校验。

工具路径、下载目录、生成 APK 和私钥均不提交 Git。自己重建时可能因工具版本、签名或打包时间产生不同 APK 哈希；不能用独立签名的包覆盖官方作者版本或本次 Release。
