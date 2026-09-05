#!/bin/bash
set -e

DEVICE="427cc046"
PACKAGE="io.github.fkoscreen"
APK="app/build/outputs/apk/debug/app-debug.apk"

export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
export ANDROID_HOME="$HOME/Library/Android/sdk"

echo "=== [1/3] Building FkOScreen APK ==="
./gradlew assembleDebug

echo "=== [2/3] Installing APK to device $DEVICE ==="
adb -s $DEVICE install -r $APK

echo "=== [3/3] Granting permissions & refreshing ==="
adb -s $DEVICE shell "su -c 'chmod -R 777 /data/data/$PACKAGE/shared_prefs 2>/dev/null || true'"

echo "=== Deployment Succeeded! ==="
