package io.github.fkoscreen.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.fkoscreen.ConfigManager

object HdrRatioHook {
    private const val TAG = "FkOScreen:HdrRatio"
    // 1250 nits / 800 nits = 1.5625
    const val TARGET_HDR_SDR_RATIO = 1.5625f

    fun init(classLoader: ClassLoader) {
        try {
            val displayInfoClass = XposedHelpers.findClass("android.view.DisplayInfo", classLoader)

            XposedBridge.hookAllMethods(displayInfoClass, "getHdrSdrRatio", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!ConfigManager.isHdrRatioEnabled()) return
                    param.result = TARGET_HDR_SDR_RATIO
                }
            })

            XposedBridge.hookAllMethods(displayInfoClass, "copyFrom", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!ConfigManager.isHdrRatioEnabled()) return
                    try {
                        XposedHelpers.setFloatField(param.thisObject, "hdrSdrRatio", TARGET_HDR_SDR_RATIO)
                    } catch (_: Throwable) {}
                }
            })

            XposedBridge.log("$TAG: Hooked DisplayInfo hdrSdrRatio")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook DisplayInfo: ${t.message}")
        }

        try {
            val displayClass = XposedHelpers.findClass("android.view.Display", classLoader)
            XposedBridge.hookAllMethods(displayClass, "getHdrSdrRatio", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!ConfigManager.isHdrRatioEnabled()) return
                    param.result = TARGET_HDR_SDR_RATIO
                }
            })
            XposedBridge.log("$TAG: Hooked Display.getHdrSdrRatio")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook Display: ${t.message}")
        }
    }
}
