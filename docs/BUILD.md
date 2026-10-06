# 构建说明

1.0 Release 使用原版 `v1.0.0` APK 为基线，保留其资源、Compose/Miuix 界面及依赖，在 `classes4.dex` 加入当前仓库中的 Java 兼容入口，并修改配置同步、作用域及版本号。Gradle 工程中的源码同步记录了对应改动；本次发布包由下述流程构建。

构建脚本已完整实跑，通过签名和 zipalign 校验；生成包中的四份 dex、AndroidManifest.xml、resources.arsc 与真机验收版一致。Release 仍发布原先已安装验证的 APK。Gradle Release 配置明确关闭 debuggable，未绑定 debug 签名，独立 Gradle 构建需自行配置签名；本次未以 Gradle 输出替换验收包。

## 工具依赖

- Python 3.9+、JDK 21。
- Android SDK：Platform 36、Build Tools 36.0.0，以及带 `com.android.tools.smali` 库的 Command-line Tools；本次使用 smali 3.0.3。
- apktool 3.0.3。
- Xposed API 82 JAR，仅用于编译。
- 自己的签名密钥。仓库和 Release 不提供本次签名私钥。

将原作者 [v1.0.0 Release](https://github.com/benbaobaoshigemi/FkOScreen/releases/tag/v1.0.0) 中的 APK 下载到本地。脚本要求其 SHA-256 为：

```text
d7bd4b5af81d3310e67abac0766d8ba45d599b3d7d3e7deee01a1f52141abe88
```

## 构建适配包

以下路径均替换为本机工具实际位置；密码由环境变量传给 apksigner，不写入源码或命令参数。

```sh
read -s FKO_KEYSTORE_PASSWORD
export FKO_KEYSTORE_PASSWORD

python3 tools/build_compat.py \
  --source-apk local/io.github.benbaobaoshigemi.fkoscreen-v1.0.0.apk \
  --sdk "$ANDROID_SDK_ROOT" \
  --java-home "$JAVA_HOME" \
  --apktool local/apktool.jar \
  --xposed-api local/xposed-api-82.jar \
  --keystore local/fkoscreen-release.jks \
  --alias fkoscreen

unset FKO_KEYSTORE_PASSWORD
```

默认输出为 `build/compat/FkOScreen-1.0.0-coloros17.1.apk`，同时输出 SHA256SUMS.txt。若私钥密码与 keystore 密码不同，可另设置 `FKO_KEY_PASSWORD`。

脚本依次执行：核对原包哈希 → 编译兼容类 → D8 → 解包原包 → 添加兼容 dex / 同步调用 → apktool → zipalign → 签名 → 签名和包内容校验。

工具路径、下载目录、生成 APK 和私钥均不提交 Git。自己重建时可能因工具版本、签名或打包时间产生不同 APK 哈希；不能用独立签名的包覆盖官方作者版本或本次 Release。
