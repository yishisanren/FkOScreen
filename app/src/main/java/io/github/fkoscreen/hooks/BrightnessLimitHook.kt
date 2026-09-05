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

    // 硬件极限峰值 1600 nits 对应硬件亮度档位 4674.0f (mTotalBrightness)
    const val PEAK_1600_BRIGHTNESS = 4674.0f
    const val PEAK_1600_NIT = 1600.0f

    fun getTargetBrightness(): Float? {
        return when {
            ConfigManager.isManual1600Enabled() -> PEAK_1600_BRIGHTNESS
            ConfigManager.isManual1250Enabled() -> DOLBY_MAX_BRIGHTNESS
            else -> null
        }
    }

    fun getTargetNit(): Float? {
        return when {
            ConfigManager.isManual1600Enabled() -> PEAK_1600_NIT
            ConfigManager.isManual1250Enabled() -> DOLBY_MAX_NIT
            else -> null
        }
    }

    fun init(classLoader: ClassLoader) {
        initSystemServer(classLoader)
    }

    fun initSystemServer(classLoader: ClassLoader) {
        // 1. 拦截 OplusFeatureIncreaseBrightnessRange
        try {
            val increaseRangeClass = XposedHelpers.findClass(
                "com.android.server.display.feature.postprocess.OplusFeatureIncreaseBrightnessRange",
                classLoader
            )
            XposedBridge.hookAllMethods(increaseRangeClass, "getScreenNormalMaxBrightness", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val target = getTargetBrightness() ?: return
                    param.result = target
                }
            })
            XposedBridge.log("$TAG: Hooked OplusFeatureIncreaseBrightnessRange.getScreenNormalMaxBrightness")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook OplusFeatureIncreaseBrightnessRange: ${t.message}")
        }

        // 2. 拦截 OplusBrightnessPostProcessingManager
        try {
            val postProcessingMgrClass = XposedHelpers.findClass(
                "com.android.server.display.feature.postprocess.OplusBrightnessPostProcessingManager",
                classLoader
            )
            XposedBridge.hookAllMethods(postProcessingMgrClass, "getScreenNormalMaxBrightness", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val target = getTargetBrightness() ?: return
                    param.result = target
                }
            })
            XposedBridge.log("$TAG: Hooked OplusBrightnessPostProcessingManager.getScreenNormalMaxBrightness")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook OplusBrightnessPostProcessingManager: ${t.message}")
        }

        // 3. 拦截 OplusFeatureWindowBrightness：loadWindowMaxBrightnessLimitInfo 与 postProcessing
        try {
            val windowBrightnessClass = XposedHelpers.findClass(
                "com.android.server.display.feature.postprocess.OplusFeatureWindowBrightness",
                classLoader
            )
            XposedBridge.hookAllMethods(windowBrightnessClass, "loadWindowMaxBrightnessLimitInfo", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val targetNit = getTargetNit() ?: return
                    try {
                        XposedHelpers.setFloatField(param.thisObject, "mLimitNit", targetNit)
                    } catch (_: Throwable) {}
                }
            })
            XposedBridge.hookAllMethods(windowBrightnessClass, "postProcessing", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val targetNit = getTargetNit() ?: return
                    try {
                        XposedHelpers.setFloatField(param.thisObject, "mLimitNit", targetNit)
                    } catch (_: Throwable) {}
                }
            })
            XposedBridge.log("$TAG: Hooked OplusFeatureWindowBrightness limits")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook OplusFeatureWindowBrightness: ${t.message}")
        }

        // 4. 拦截 OplusDisplayBrightnessModel 关键上限方法
        try {
            val brightnessModelClass = XposedHelpers.findClass(
                "com.android.server.display.model.OplusDisplayBrightnessModel",
                classLoader
            )
            XposedBridge.hookAllMethods(brightnessModelClass, "getMaxBrightness", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val target = getTargetBrightness() ?: return
                    param.result = target
                }
            })
            XposedBridge.hookAllMethods(brightnessModelClass, "getMaxPanelNit", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val targetNit = getTargetNit() ?: return
                    val current = (param.result as? Number)?.toFloat() ?: 0f
                    if (current < targetNit) {
                        param.result = targetNit
                    }
                }
            })
            XposedBridge.hookAllMethods(brightnessModelClass, "getTotalBrightness", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val target = getTargetBrightness() ?: return
                    val current = (param.result as? Number)?.toFloat() ?: 0f
                    if (current < target) {
                        param.result = target
                    }
                }
            })
            XposedBridge.log("$TAG: Hooked OplusDisplayBrightnessModel limit methods")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook OplusDisplayBrightnessModel: ${t.message}")
        }

        // 5. 拦截 DisplayPowerController，扩展 AOSP 内部普通最大亮度限制
        try {
            val dpcClass = XposedHelpers.findClass(
                "com.android.server.display.DisplayPowerController",
                classLoader
            )
            XposedBridge.hookAllMethods(dpcClass, "setScreenBrightnessNormalMaximum", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val target = getTargetBrightness() ?: return
                    param.args[0] = target
                }
            })
            XposedBridge.hookAllMethods(dpcClass, "getScreenBrightnessNormalMaximum", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val target = getTargetBrightness() ?: return
                    param.result = target
                }
            })
            XposedBridge.log("$TAG: Hooked DisplayPowerController normal maximum brightness")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook DisplayPowerController: ${t.message}")
        }

        // 6. 拦截 DisplayManagerService.getBrightnessInfo，修改返回给外部的 brightnessMaximum
        try {
            val dmsClass = XposedHelpers.findClass(
                "com.android.server.display.DisplayManagerService",
                classLoader
            )
            XposedBridge.hookAllMethods(dmsClass, "getBrightnessInfo", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val info = param.result ?: return
                    val target = getTargetBrightness() ?: return
                    try {
                        XposedHelpers.setFloatField(info, "brightnessMaximum", target)
                    } catch (_: Throwable) {}
                }
            })
            XposedBridge.log("$TAG: Hooked DisplayManagerService.getBrightnessInfo")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook DisplayManagerService.getBrightnessInfo: ${t.message}")
        }
    }

    fun initSystemUI(classLoader: ClassLoader) {
        // 7. 拦截 SystemUI BrightnessController，拓宽滑块组件上限
        try {
            val bcClass = XposedHelpers.findClass(
                "com.android.systemui.settings.brightness.BrightnessController",
                classLoader
            )
            XposedBridge.hookAllConstructors(bcClass, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val target = getTargetBrightness() ?: return
                    try {
                        XposedHelpers.setFloatField(param.thisObject, "mBrightnessMax", target)
                    } catch (_: Throwable) {}
                }
            })
            XposedBridge.log("$TAG: Hooked SystemUI BrightnessController")
        } catch (_: Throwable) {}
    }
}
