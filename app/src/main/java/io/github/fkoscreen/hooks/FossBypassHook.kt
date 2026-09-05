package io.github.fkoscreen.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.fkoscreen.ConfigManager

object FossBypassHook {
    private const val TAG = "FkOScreen:FossBypass"

    fun init(classLoader: ClassLoader) {
        // 1. 阻断 OplusFeatureReduceBrightness 对主流 App 的 15% 亮度偷扣
        try {
            val reduceBrightnessClass = XposedHelpers.findClass(
                "com.android.server.display.feature.postprocess.OplusFeatureReduceBrightness",
                classLoader
            )
            XposedBridge.hookAllMethods(reduceBrightnessClass, "getReduceInfo", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!ConfigManager.isFossBypassEnabled()) return
                    param.result = false
                }
            })
            XposedBridge.log("$TAG: Hooked OplusFeatureReduceBrightness.getReduceInfo")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook OplusFeatureReduceBrightness: ${t.message}")
        }

        // 2. 阻断 OplusFeatureFoss 面板层降耗
        try {
            val fossClass = XposedHelpers.findClass(
                "com.android.server.display.feature.panel.OplusFeatureFoss",
                classLoader
            )
            XposedBridge.hookAllMethods(fossClass, "getFossInfo", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!ConfigManager.isFossBypassEnabled()) return
                    param.result = false
                }
            })
            XposedBridge.log("$TAG: Hooked OplusFeatureFoss.getFossInfo")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook OplusFeatureFoss: ${t.message}")
        }
    }
}
