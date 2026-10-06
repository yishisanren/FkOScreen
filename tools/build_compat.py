"""Build the ColorOS 17 compatibility package from the unchanged upstream APK."""
from pathlib import Path
import argparse
import hashlib
import os
import re
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
ORIGINAL_SHA256 = "d7bd4b5af81d3310e67abac0766d8ba45d599b3d7d3e7deee01a1f52141abe88"
VERSION = "1.0.0-coloros17.1"


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--source-apk", type=Path, required=True)
    p.add_argument("--sdk", type=Path, required=True)
    p.add_argument("--java-home", type=Path, required=True)
    p.add_argument("--apktool", type=Path, required=True)
    p.add_argument("--xposed-api", type=Path, required=True)
    p.add_argument("--keystore", type=Path, required=True)
    p.add_argument("--alias", default="fkoscreen")
    p.add_argument("--output", type=Path, default=ROOT / "build/compat" / f"FkOScreen-{VERSION}.apk")
    a = p.parse_args()
    if hashlib.sha256(a.source_apk.read_bytes()).hexdigest() != ORIGINAL_SHA256:
        p.error("Source APK does not match upstream v1.0.0 SHA-256")
    if not os.environ.get("FKO_KEYSTORE_PASSWORD"):
        p.error("Set FKO_KEYSTORE_PASSWORD before signing")
    a.output = a.output.resolve()
    a.output.parent.mkdir(parents=True, exist_ok=True)
    bt = a.sdk / "build-tools/36.0.0"
    android = a.sdk / "platforms/android-36/android.jar"
    jars = sorted((a.sdk / "cmdline-tools/latest/lib").rglob("*.jar"))
    if not any("smali-baksmali" in j.name for j in jars):
        p.error("SDK Command-line Tools must include com.android.tools.smali baksmali")
    env = dict(os.environ, JAVA_HOME=str(a.java_home))
    if "FKO_KEY_PASSWORD" not in env:
        env["FKO_KEY_PASSWORD"] = env["FKO_KEYSTORE_PASSWORD"]

    def run(*args):
        subprocess.run([str(x) for x in args], check=True, env=env)

    with tempfile.TemporaryDirectory(prefix="fko-", dir=a.output.parent) as temporary:
        w = Path(temporary)
        classes = w / "classes"
        classes.mkdir()
        sources = sorted((ROOT / "app/src/main/java/io/github/fkoscreen/compat").glob("*.java"))
        if not sources:
            p.error("Compatibility sources are missing")
        run(a.java_home / "bin/javac", "-source", "8", "-target", "8", "-proc:none",
            "-cp", os.pathsep.join([str(android), str(a.xposed_api)]), "-d", classes, *sources)
        run(bt / "d8", "--min-api", "26", "--lib", android, "--classpath", a.xposed_api,
            "--output", w, *sorted(classes.rglob("*.class")))
        sdk_cp = os.pathsep.join(str(j) for j in jars)
        run(a.java_home / "bin/javac", "-cp", sdk_cp, "-d", classes, ROOT / "tools/DexInspect.java")
        decoded = w / "decoded"
        run(a.java_home / "bin/java", "-jar", a.apktool, "d", "-f", a.source_apk, "-o", decoded)
        run(a.java_home / "bin/java", "-cp", os.pathsep.join([str(classes), sdk_cp]),
            "DexInspect", w / "classes.dex", ".*;", decoded / "smali_classes4")
        (decoded / "assets/xposed_init").write_text("io.github.fkoscreen.compat.ColorOS17Entry\n")
        arrays = decoded / "res/values/arrays.xml"
        content = arrays.read_text()
        if content.count("<item>android</item>") != 1:
            raise RuntimeError("Expected one original system scope")
        arrays.write_text(content.replace("<item>android</item>", "<item>system</item>"))
        config = decoded / "smali_classes2/io/github/fkoscreen/ConfigManager.smali"
        body = """.method public final fixPermissions(Landroid/content/Context;)V
    .locals 0
    invoke-static {p1}, Lio/github/fkoscreen/compat/RuntimeConfig;->syncAppPrefs(Landroid/content/Context;)V
    return-void
.end method"""
        content, count = re.subn(
            r"\.method public final fixPermissions\(Landroid/content/Context;\)V.*?\.end method",
            body, config.read_text(), flags=re.S)
        if count != 1:
            raise RuntimeError("UI preference synchronization seam missing")
        config.write_text(content)
        activity = decoded / "smali_classes2/io/github/fkoscreen/ui/MainActivity.smali"
        needle = "invoke-super {p0, p1}, Landroidx/activity/ComponentActivity;->onCreate(Landroid/os/Bundle;)V"
        content = activity.read_text()
        if content.count(needle) != 1:
            raise RuntimeError("Expected one Activity.onCreate synchronization seam")
        activity.write_text(content.replace(needle, needle + "\n\n    invoke-static {p0}, "
                            "Lio/github/fkoscreen/compat/RuntimeConfig;->syncAppPrefs(Landroid/content/Context;)V"))
        metadata = decoded / "apktool.yml"
        content = re.sub(r"versionCode: .*", "versionCode: 2", metadata.read_text())
        content = re.sub(r"versionName: .*", f"versionName: {VERSION}", content)
        metadata.write_text(content)
        run(a.java_home / "bin/java", "-jar", a.apktool, "b", decoded, "-o", w / "unsigned.apk")
        run(bt / "zipalign", "-f", "4", w / "unsigned.apk", w / "aligned.apk")
        run(a.java_home / "bin/java", "-jar", bt / "lib/apksigner.jar", "sign",
            "--ks", a.keystore, "--ks-key-alias", a.alias,
            "--ks-pass", "env:FKO_KEYSTORE_PASSWORD", "--key-pass", "env:FKO_KEY_PASSWORD",
            "--out", a.output, w / "aligned.apk")
        run(a.java_home / "bin/java", "-jar", bt / "lib/apksigner.jar", "verify", "--verbose", a.output)
        run(bt / "zipalign", "-c", "4", a.output)
    with zipfile.ZipFile(a.output) as z:
        if z.read("assets/xposed_init") != b"io.github.fkoscreen.compat.ColorOS17Entry\n":
            raise RuntimeError("Unexpected Xposed entry")
        if "classes4.dex" not in z.namelist():
            raise RuntimeError("Compatibility dex missing")
    digest = hashlib.sha256(a.output.read_bytes()).hexdigest()
    (a.output.parent / "SHA256SUMS.txt").write_text(f"{digest}  {a.output.name}\n")
    print(f"Built {a.output.name}: {digest}")


if __name__ == "__main__":
    main()
