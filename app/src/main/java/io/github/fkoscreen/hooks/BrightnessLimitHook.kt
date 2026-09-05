package io.github.fkoscreen.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.fkoscreen.ConfigManager

object BrightnessLimitHook {
    private const val TAG = "FkOScreen:BrightnessLimit"
    // 杜比视界上限 1250 nits 对应硬件亮度档位 4543.0f (SDR 原厂封顶 4095.0f = 800 nits)
    const val DOLBY_MAX_BRIGHTNESS = 4543.0f
    const val DOLBY_MAX_NIT = 1250.0f

    fun init(classLoader: ClassLoader) {
        // 1. 拦截 OplusFeatureIncreaseBrightnessRange，拓展正常手动最高亮度至 4543 (1250 nits)
        try {
            val increaseRangeClass = XposedHelpers.findClass(
                "com.android.server.display.feature.postprocess.OplusFeatureIncreaseBrightnessRange",
                classLoader
            )
            XposedBridge.hookAllMethods(increaseRangeClass, "getScreenNormalMaxBrightness", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!ConfigManager.isManual1250Enabled()) return
                    param.result = DOLBY_MAX_BRIGHTNESS
                }
            })
            XposedBridge.log("$TAG: Hooked OplusFeatureIncreaseBrightnessRange.getScreenNormalMaxBrightness")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook OplusFeatureIncreaseBrightnessRange: ${t.message}")
        }

        // 2. 拦截 OplusBrightnessPostProcessingManager，同步最大正常亮度
        try {
            val postProcessingMgrClass = XposedHelpers.findClass(
                "com.android.server.display.feature.postprocess.OplusBrightnessPostProcessingManager",
                classLoader
            )
            XposedBridge.hookAllMethods(postProcessingMgrClass, "getScreenNormalMaxBrightness", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!ConfigManager.isManual1250Enabled()) return
                    param.result = DOLBY_MAX_BRIGHTNESS
                }
            })
            XposedBridge.log("$TAG: Hooked OplusBrightnessPostProcessingManager.getScreenNormalMaxBrightness")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook OplusBrightnessPostProcessingManager: ${t.message}")
        }

        // 3. 拦截 OplusFeatureWindowBrightness，将 SDR 窗口亮度硬上限提升至 1250 nits
        try {
            val windowBrightnessClass = XposedHelpers.findClass(
                "com.android.server.display.feature.postprocess.OplusFeatureWindowBrightness",
                classLoader
            )
            XposedBridge.hookAllMethods(windowBrightnessClass, "loadWindowMaxBrightnessLimitInfo", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!ConfigManager.isManual1250Enabled()) return
                    try {
                        XposedHelpers.setFloatField(param.thisObject, "mLimitNit", DOLBY_MAX_NIT)
                    } catch (_: Throwable) {}
                }
            })
            XposedBridge.log("$TAG: Hooked OplusFeatureWindowBrightness.loadWindowMaxBrightnessLimitInfo")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook OplusFeatureWindowBrightness: ${t.message}")
        }
    }
}
