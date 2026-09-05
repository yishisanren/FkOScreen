package io.github.fkoscreen.hooks

import android.content.ContentResolver
import android.provider.Settings
import android.util.Pair
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.fkoscreen.ConfigManager

object ColorBallAnchorHook {
    private const val TAG = "FkOScreen:ColorBallAnchor"

    // 1. 在 com.android.settings 中解除置灰禁用
    fun initSettings(classLoader: ClassLoader) {
        // A. Hook ScreenColorTemperateBallPreference
        try {
            val prefClass = XposedHelpers.findClass(
                "com.oplus.settings.feature.display.screencolortemp.ScreenColorTemperateBallPreference",
                classLoader
            )
            XposedBridge.hookAllMethods(prefClass, "setEnabled", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (ConfigManager.isColorBallAnchorEnabled()) {
                        param.args[0] = true
                    }
                }
            })
            XposedBridge.hookAllMethods(prefClass, "onBindViewHolder", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!ConfigManager.isColorBallAnchorEnabled()) return
                    try {
                        XposedHelpers.callMethod(param.thisObject, "setEnabled", true)
                    } catch (e: Throwable) {
                        XposedBridge.log("$TAG: setEnabled true error: ${e.message}")
                    }
                }
            })
            XposedBridge.log("$TAG: Hooked ScreenColorTemperateBallPreference")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook ScreenColorTemperateBallPreference: ${t.message}")
        }

        // B. Hook ScreenColorTemperateBall (View 层)
        try {
            val ballClass = XposedHelpers.findClass(
                "com.oplus.settings.feature.display.screencolortemp.ScreenColorTemperateBall",
                classLoader
            )
            XposedBridge.hookAllMethods(ballClass, "setEnabled", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (ConfigManager.isColorBallAnchorEnabled()) {
                        param.args[0] = true
                    }
                }
            })
            XposedBridge.hookAllMethods(ballClass, "isEnabled", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (ConfigManager.isColorBallAnchorEnabled()) {
                        param.result = true
                    }
                }
            })
            XposedBridge.log("$TAG: Hooked ScreenColorTemperateBall view")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook ScreenColorTemperateBall: ${t.message}")
        }

        // C. Hook ColorModeFragment (Fragment 页面控制器层)
        try {
            val fragmentClass = XposedHelpers.findClass(
                "com.oplus.settings.feature.display.protecteyes.ColorModeFragment",
                classLoader
            )
            XposedBridge.hookAllMethods(fragmentClass, "updateScreenColorTemperatureState", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!ConfigManager.isColorBallAnchorEnabled()) return
                    try {
                        val ballPref = XposedHelpers.getObjectField(param.thisObject, "mScreenColorTempBallPref")
                        if (ballPref != null) {
                            XposedHelpers.callMethod(ballPref, "setEnabled", true)
                        }
                        // 隐藏置灰提示语（如“护眼模式开启时不可调节”或“环境色自适应开启时不可调节”）
                        val tipPref = XposedHelpers.getObjectField(param.thisObject, "mScreenColorSummary")
                        if (tipPref != null) {
                            XposedHelpers.callMethod(tipPref, "setVisible", false)
                        }
                    } catch (e: Throwable) {
                        XposedBridge.log("$TAG: updateScreenColorTemperatureState error: ${e.message}")
                    }
                }
            })
            XposedBridge.log("$TAG: Hooked ColorModeFragment.updateScreenColorTemperatureState")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook ColorModeFragment: ${t.message}")
        }
    }

    // 2. 在 system_server 中将色温球 RGB 矩阵与动态矩阵复合相乘 (M_final = M_adaptive * M_ball)
    fun initSystemServer(classLoader: ClassLoader) {
        try {
            val eyeProtectMgrClass = XposedHelpers.findClass(
                "com.android.server.display.color.eyeprotect.OplusEyeProtectManager",
                classLoader
            )
            XposedBridge.hookAllMethods(eyeProtectMgrClass, "applyAdjustValues", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!ConfigManager.isColorBallAnchorEnabled()) return
                    try {
                        val mainPair = param.args[0] as? Pair<*, *> ?: return
                        val firstMatrix = mainPair.first as? FloatArray ?: return
                        if (firstMatrix.size != 16) return

                        // 获取全局 Settings 中的 eyeprotect_rgb
                        val ctx = XposedHelpers.getObjectField(param.thisObject, "mContext") as? android.content.Context
                            ?: return
                        val rgbStr = Settings.System.getString(ctx.contentResolver, "eyeprotect_rgb") ?: return
                        val parts = rgbStr.split(",")
                        if (parts.size >= 3) {
                            val r = parts[0].toFloatOrNull() ?: 1000f
                            val g = parts[1].toFloatOrNull() ?: 1000f
                            val b = parts[2].toFloatOrNull() ?: 1000f

                            // 若非默认 1000, 1000, 1000，执行基底矩阵缩放 (R_out = R_in * r/1000)
                            if (r != 1000f || g != 1000f || b != 1000f) {
                                val rGain = (r / 1000f).coerceIn(0.5f, 1.0f)
                                val gGain = (g / 1000f).coerceIn(0.5f, 1.0f)
                                val bGain = (b / 1000f).coerceIn(0.5f, 1.0f)

                                firstMatrix[0] = (firstMatrix[0] * rGain).coerceIn(0f, 1f)
                                firstMatrix[5] = (firstMatrix[5] * gGain).coerceIn(0f, 1f)
                                firstMatrix[10] = (firstMatrix[10] * bGain).coerceIn(0f, 1f)
                            }
                        }
                    } catch (_: Throwable) {}
                }
            })
            XposedBridge.log("$TAG: Hooked OplusEyeProtectManager.applyAdjustValues")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook OplusEyeProtectManager: ${t.message}")
        }
    }
}
