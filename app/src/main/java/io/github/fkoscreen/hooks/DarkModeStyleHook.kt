package io.github.fkoscreen.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.fkoscreen.ConfigManager

object DarkModeStyleHook {
    private const val TAG = "FkOScreen:DarkModeStyle"

    fun init(classLoader: ClassLoader) {
        try {
            val featureUtilsClass = XposedHelpers.findClass(
                "com.oplus.settings.utils.FeatureUtils",
                classLoader
            )
            XposedBridge.hookAllMethods(featureUtilsClass, "hasAppFeature", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!ConfigManager.isDarkModeStylesEnabled()) return
                    val featureName = param.args[0] as? String
                    if ("com.android.settings_dark_mode_style_enable" == featureName) {
                        param.result = true
                    }
                }
            })
            XposedBridge.log("$TAG: Hooked FeatureUtils.hasAppFeature for dark mode styles")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook FeatureUtils: ${t.message}")
        }
    }
}
