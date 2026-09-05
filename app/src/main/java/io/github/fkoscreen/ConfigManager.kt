package io.github.fkoscreen

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences
import java.io.File

object ConfigManager {
    const val PACKAGE_NAME = "io.github.fkoscreen"
    const val PREFS_NAME = "fkoscreen_prefs"

    const val KEY_MANUAL_1250 = "manual_1250_brightness"
    const val KEY_FOSS_BYPASS = "foss_bypass"
    const val KEY_HDR_RATIO = "hdr_ratio_fix"
    const val KEY_DARK_MODE_STYLES = "dark_mode_styles"
    const val KEY_COLOR_BALL_ANCHOR = "color_ball_anchor"

    private var xPrefs: XSharedPreferences? = null

    fun getXPrefs(): XSharedPreferences {
        if (xPrefs == null) {
            xPrefs = XSharedPreferences(PACKAGE_NAME, PREFS_NAME)
            xPrefs?.makeWorldReadable()
        } else {
            xPrefs?.reload()
        }
        return xPrefs!!
    }

    fun isManual1250Enabled(): Boolean {
        return getXPrefs().getBoolean(KEY_MANUAL_1250, true)
    }

    fun isFossBypassEnabled(): Boolean {
        return getXPrefs().getBoolean(KEY_FOSS_BYPASS, true)
    }

    fun isHdrRatioEnabled(): Boolean {
        return getXPrefs().getBoolean(KEY_HDR_RATIO, true)
    }

    fun isDarkModeStylesEnabled(): Boolean {
        return getXPrefs().getBoolean(KEY_DARK_MODE_STYLES, true)
    }

    fun isColorBallAnchorEnabled(): Boolean {
        return getXPrefs().getBoolean(KEY_COLOR_BALL_ANCHOR, true)
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
            }
        } catch (_: Exception) {}
    }
}
