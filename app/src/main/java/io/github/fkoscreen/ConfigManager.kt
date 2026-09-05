package io.github.fkoscreen

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences
import java.io.File

object ConfigManager {
    const val PACKAGE_NAME = "io.github.fkoscreen"
    const val PREFS_NAME = "fkoscreen_prefs"

    const val KEY_MANUAL_1250 = "manual_1250_brightness"
    const val KEY_MANUAL_1600 = "manual_1600_brightness"
    const val KEY_FOSS_BYPASS = "foss_bypass"
    const val KEY_HDR_RATIO = "hdr_ratio_fix"
    const val KEY_DARK_MODE_STYLES = "dark_mode_styles"
    const val KEY_COLOR_BALL_ANCHOR = "color_ball_anchor"

    private var xPrefs: XSharedPreferences? = null

    fun getXPrefs(): XSharedPreferences {
        if (xPrefs == null) {
            val deFile = File("/data/user_de/0/$PACKAGE_NAME/shared_prefs/$PREFS_NAME.xml")
            val ceFile = File("/data/data/$PACKAGE_NAME/shared_prefs/$PREFS_NAME.xml")
            xPrefs = when {
                deFile.exists() -> XSharedPreferences(deFile)
                ceFile.exists() -> XSharedPreferences(ceFile)
                else -> XSharedPreferences(PACKAGE_NAME, PREFS_NAME)
            }
            xPrefs?.makeWorldReadable()
        } else {
            xPrefs?.reload()
        }
        return xPrefs!!
    }

    private fun getSettingInt(key: String): Int {
        return try {
            val atClass = Class.forName("android.app.ActivityThread")
            val app = atClass.getMethod("currentApplication").invoke(null) as? Context
            val cr = app?.contentResolver ?: return -1
            android.provider.Settings.System.getInt(cr, key, -1)
        } catch (_: Throwable) {
            -1
        }
    }

    fun isManual1250Enabled(): Boolean {
        val s = getSettingInt(KEY_MANUAL_1250)
        if (s != -1) return s == 1
        return try {
            getXPrefs().getBoolean(KEY_MANUAL_1250, true)
        } catch (_: Throwable) {
            true
        }
    }

    fun isManual1600Enabled(): Boolean {
        val s = getSettingInt(KEY_MANUAL_1600)
        if (s != -1) return s == 1
        return try {
            getXPrefs().getBoolean(KEY_MANUAL_1600, false)
        } catch (_: Throwable) {
            false
        }
    }

    fun isFossBypassEnabled(): Boolean {
        return try {
            getXPrefs().getBoolean(KEY_FOSS_BYPASS, true)
        } catch (_: Throwable) {
            true
        }
    }

    fun isHdrRatioEnabled(): Boolean {
        return try {
            getXPrefs().getBoolean(KEY_HDR_RATIO, true)
        } catch (_: Throwable) {
            true
        }
    }

    fun isDarkModeStylesEnabled(): Boolean {
        return try {
            getXPrefs().getBoolean(KEY_DARK_MODE_STYLES, true)
        } catch (_: Throwable) {
            true
        }
    }

    fun isColorBallAnchorEnabled(): Boolean {
        return try {
            getXPrefs().getBoolean(KEY_COLOR_BALL_ANCHOR, true)
        } catch (_: Throwable) {
            true
        }
    }

    fun getAppPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun fixPermissions(context: Context) {
        try {
            val prefsFile = File(context.filesDir.parentFile, "shared_prefs/$PREFS_NAME.xml")
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false)
                prefsFile.parentFile?.setReadable(true, false)
                prefsFile.parentFile?.setExecutable(true, false)
                // 确保 /data/data/io.github.fkoscreen 目录对其它 UID 具有 +x 遍历权限
                context.filesDir.parentFile?.setExecutable(true, false)
                context.filesDir.parentFile?.setReadable(true, false)
                try {
                    Runtime.getRuntime().exec(arrayOf("su", "-c", "chmod 666 ${prefsFile.absolutePath}")).waitFor()
                } catch (_: Throwable) {}
            }
        } catch (_: Exception) {}
    }
}
