package io.github.fkoscreen.util

import android.content.Context
import android.provider.Settings
import java.math.BigDecimal
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class ColorBallState(
    val cct: Float,       // [-1.0f, 1.0f]: -1.0 为极暖(暖橙), +1.0 为极冷(浅蓝), 0.0 为标准白点
    val tint: Float,      // [-1.0f, 1.0f]: -1.0 为偏青绿, +1.0 为偏品红, 0.0 为无偏色
    val x750: Float,      // 750 基准设计坐标系 X
    val y750: Float,      // 750 基准设计坐标系 Y
    val r: Int,           // 红色通道增益 (900~1000)
    val g: Int,           // 绿色通道增益 (900~1000)
    val b: Int,           // 蓝色通道增益 (900~1000)
    val rgbStr: String    // ColorOS 格式字符串，如 "1000,980,928,1,1"
)

object ColorBallCalculator {
    const val CENTER_750 = 375.0f
    const val RADIUS_750 = 315.0f
    private val SQRT_2 = sqrt(2.0f)

    private const val KEY_POINT_X = "color_ball_last_pointx"
    private const val KEY_POINT_Y = "color_ball_last_pointy"
    private const val KEY_EYEPROTECT_RGB = "eyeprotect_rgb"
    private const val KEY_BALL_MODE = "color_temperate_ball_mode"

    private var cachedMinVal: Int = -1
    private var cachedMaxVal: Int = -1

    fun getSystemRgbMin(): Int {
        if (cachedMinVal != -1) return cachedMinVal
        val v = getSystemProp("ro.oplus.display.rgb_ball_min_value", 900)
        cachedMinVal = v
        return v
    }

    fun getSystemRgbMax(): Int {
        if (cachedMaxVal != -1) return cachedMaxVal
        val v = getSystemProp("ro.oplus.display.rgb_ball_max_value", 1000)
        cachedMaxVal = v
        return v
    }

    private fun getSystemProp(key: String, def: Int): Int {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val getInt = clazz.getMethod("getInt", String::class.java, Int::class.javaPrimitiveType)
            getInt.invoke(null, key, def) as Int
        } catch (_: Throwable) {
            def
        }
    }

    /**
     * 官方 ColorOS 原厂 6 扇区算法复刻 (来自 ScreenColorTemperateBall.java)
     */
    fun calculateRange(angle: Float, dx: Float, dy: Float, radius: Float): String {
        val minVal = getSystemRgbMin()
        val maxVal = getSystemRgbMax()
        val rangeDis = maxVal - minVal

        val dist = hypot(dx.toDouble(), dy.toDouble())
        val pointPercent = (dist / radius.toDouble()).coerceIn(0.0, 1.0)

        // 弧度转角度 (-180.0 ~ 180.0)
        val deg = (180.0 * angle.toDouble()) / Math.PI
        val dAbs = Math.abs(deg)
        val bd = BigDecimal(deg)

        var rVal: Int
        var gVal: Int
        var bVal: Int

        if (bd.compareTo(BigDecimal(-90.0)) <= 0 || bd.compareTo(BigDecimal(-30.0)) > 0) {
            if (bd.compareTo(BigDecimal(-150.0)) <= 0 || bd.compareTo(BigDecimal(-90.0)) > 0) {
                if (bd.compareTo(BigDecimal(-180.0)) > 0 && bd.compareTo(BigDecimal(-150.0)) <= 0) {
                    val d4 = maxVal.toDouble()
                    val d5 = rangeDis.toDouble()
                    val i6 = (d4 - ((d4 - (d4 - (((dAbs - 150.0) / 60.0) * d5))) * pointPercent)).toInt()
                    val i7 = (d4 - (d5 * pointPercent)).toInt()
                    rVal = i6
                    gVal = i6
                    bVal = i7
                } else if (bd.compareTo(BigDecimal(150.0)) > 0 && bd.compareTo(BigDecimal(180.0)) <= 0) {
                    val d6 = rangeDis.toDouble()
                    val d7 = minVal.toDouble() + (((dAbs - 150.0) / 60.0) * d6)
                    val d8 = maxVal.toDouble()
                    val i6 = (d8 - ((d8 - d7) * pointPercent)).toInt()
                    val i7 = (d8 - (d6 * pointPercent)).toInt()
                    rVal = i6
                    gVal = i6
                    bVal = i7
                } else if (bd.compareTo(BigDecimal(90.0)) <= 0 || bd.compareTo(BigDecimal(150.0)) > 0) {
                    val d2: Double
                    val i4: Int
                    if (bd.compareTo(BigDecimal(30.0)) > 0 && bd.compareTo(BigDecimal(90.0)) <= 0) {
                        val d9 = rangeDis.toDouble()
                        val d10 = minVal.toDouble() + (((dAbs - 30.0) / 60.0) * d9)
                        val d11 = maxVal.toDouble()
                        i4 = (d11 - (d9 * pointPercent)).toInt()
                        d2 = d11 - ((d11 - d10) * pointPercent)
                    } else if (bd.compareTo(BigDecimal(0.0)) > 0 && bd.compareTo(BigDecimal(30.0)) <= 0) {
                        val d12 = (30.0 - dAbs) / 60.0
                        val d13 = rangeDis.toDouble()
                        val d14 = maxVal.toDouble()
                        i4 = (d14 - ((d14 - (minVal.toDouble() + (d12 * d13))) * pointPercent)).toInt()
                        d2 = (d14 - (d13 * pointPercent))
                    } else if (bd.compareTo(BigDecimal(-30.0)) <= 0 || bd.compareTo(BigDecimal(0.0)) > 0) {
                        i4 = maxVal
                        d2 = maxVal.toDouble()
                    } else {
                        val d15 = rangeDis.toDouble()
                        val d16 = minVal.toDouble() + (((dAbs + 30.0) / 60.0) * d15)
                        val d17 = maxVal.toDouble()
                        i4 = (d17 - ((d17 - d16) * pointPercent)).toInt()
                        d2 = d17 - (d15 * pointPercent)
                    }
                    rVal = i4
                    gVal = d2.toInt()
                    bVal = maxVal
                } else {
                    val d18 = (150.0 - dAbs) / 60.0
                    val d19 = rangeDis.toDouble()
                    val d20 = minVal.toDouble() + (d18 * d19)
                    val d21 = maxVal.toDouble()
                    val i6 = (d21 - (d19 * pointPercent)).toInt()
                    val i7 = (d21 - ((d21 - d20) * pointPercent)).toInt()
                    rVal = i6
                    gVal = i6
                    bVal = i7
                }
            } else {
                val d22 = rangeDis.toDouble()
                val d23 = minVal.toDouble() + (((dAbs - 90.0) / 60.0) * d22)
                val d24 = maxVal.toDouble()
                val i8 = (d24 - ((d24 - d23) * pointPercent)).toInt()
                val i9 = (d24 - (d22 * pointPercent)).toInt()
                rVal = maxVal
                gVal = i8
                bVal = i9
            }
        } else {
            val d25 = (90.0 - dAbs) / 60.0
            val d26 = rangeDis.toDouble()
            val d27 = minVal.toDouble() + (d25 * d26)
            val d28 = maxVal.toDouble()
            val i8 = (d28 - (d26 * pointPercent)).toInt()
            val i9 = (d28 - ((d28 - d27) * pointPercent)).toInt()
            rVal = maxVal
            gVal = i8
            bVal = i9
        }

        rVal = rVal.coerceIn(minVal, maxVal)
        gVal = gVal.coerceIn(minVal, maxVal)
        bVal = bVal.coerceIn(minVal, maxVal)

        return "$rVal,$gVal,$bVal,1,1"
    }

    /**
     * 正向合成：由冷暖 (cct) 和青品 (tint) 滑杆数值计算 750 坐标系位置与 RGB
     */
    fun compose(cct: Float, tint: Float): ColorBallState {
        var c = cct.coerceIn(-1.0f, 1.0f)
        var t = tint.coerceIn(-1.0f, 1.0f)

        // 若矢量模长超过圆盘半径，进行径向归一化限位
        val mag = hypot(c.toDouble(), t.toDouble()).toFloat()
        if (mag > 1.0f) {
            c /= mag
            t /= mag
        }

        val dx = ((c + t) / SQRT_2) * RADIUS_750
        val dy = ((c - t) / SQRT_2) * RADIUS_750

        val x750 = CENTER_750 + dx
        val y750 = CENTER_750 + dy

        val angle = atan2(dy, dx)
        val rgbStr = if (hypot(dx.toDouble(), dy.toDouble()) < 0.5) {
            val maxV = getSystemRgbMax()
            "$maxV,$maxV,$maxV,1,1"
        } else {
            calculateRange(angle, dx, dy, RADIUS_750)
        }

        val parts = rgbStr.split(",")
        val r = parts.getOrNull(0)?.toIntOrNull() ?: 1000
        val g = parts.getOrNull(1)?.toIntOrNull() ?: 1000
        val b = parts.getOrNull(2)?.toIntOrNull() ?: 1000

        return ColorBallState(
            cct = c,
            tint = t,
            x750 = x750,
            y750 = y750,
            r = r,
            g = g,
            b = b,
            rgbStr = rgbStr
        )
    }

    /**
     * 逆向反解：由 750 坐标系 (x750, y750) 反解出冷暖 (cct) 和青品 (tint)
     */
    fun decompose(x750: Float, y750: Float): ColorBallState {
        val dx = x750 - CENTER_750
        val dy = y750 - CENTER_750

        val cct = ((dx + dy) / (SQRT_2 * RADIUS_750)).coerceIn(-1.0f, 1.0f)
        val tint = ((dx - dy) / (SQRT_2 * RADIUS_750)).coerceIn(-1.0f, 1.0f)

        val angle = atan2(dy, dx)
        val rgbStr = if (hypot(dx.toDouble(), dy.toDouble()) < 0.5) {
            val maxV = getSystemRgbMax()
            "$maxV,$maxV,$maxV,1,1"
        } else {
            calculateRange(angle, dx, dy, RADIUS_750)
        }

        val parts = rgbStr.split(",")
        val r = parts.getOrNull(0)?.toIntOrNull() ?: 1000
        val g = parts.getOrNull(1)?.toIntOrNull() ?: 1000
        val b = parts.getOrNull(2)?.toIntOrNull() ?: 1000

        return ColorBallState(
            cct = cct,
            tint = tint,
            x750 = x750,
            y750 = y750,
            r = r,
            g = g,
            b = b,
            rgbStr = rgbStr
        )
    }

    /**
     * 从系统读取当前色温球状态
     */
    fun readFromSystem(context: Context): ColorBallState {
        val cr = context.contentResolver
        val x = try {
            Settings.System.getFloat(cr, KEY_POINT_X, -1.0f)
        } catch (_: Throwable) { -1.0f }

        val y = try {
            Settings.System.getFloat(cr, KEY_POINT_Y, -1.0f)
        } catch (_: Throwable) { -1.0f }

        if (x <= 0.0f || y <= 0.0f) {
            // 处于默认白点中心
            return compose(0.0f, 0.0f)
        }

        return decompose(x, y)
    }

    private val rootExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
    @Volatile
    private var pendingRootState: ColorBallState? = null
    @Volatile
    private var isRootWorkerRunning = false

    /**
     * 下发至系统管线与 Settings
     */
    fun applyToSystem(context: Context, state: ColorBallState) {
        val cr = context.contentResolver
        var directSuccess = false
        try {
            Settings.System.putString(cr, KEY_EYEPROTECT_RGB, state.rgbStr)
            Settings.System.putFloat(cr, KEY_POINT_X, state.x750)
            Settings.System.putFloat(cr, KEY_POINT_Y, state.y750)

            val isCenter = Math.abs(state.cct) < 0.005f && Math.abs(state.tint) < 0.005f
            Settings.System.putInt(cr, KEY_BALL_MODE, if (isCenter) 2 else 4)
            directSuccess = true
        } catch (_: Throwable) {
            directSuccess = false
        }

        if (!directSuccess) {
            // 权限后备方案：通过异步队列执行 su，避免阻塞 UI 渲染
            pendingRootState = state
            synchronized(rootExecutor) {
                if (!isRootWorkerRunning) {
                    isRootWorkerRunning = true
                    rootExecutor.execute {
                        while (true) {
                            val targetState = pendingRootState ?: break
                            pendingRootState = null
                            try {
                                val isCenter = Math.abs(targetState.cct) < 0.005f && Math.abs(targetState.tint) < 0.005f
                                val cmd = "settings put system $KEY_EYEPROTECT_RGB '${targetState.rgbStr}'; " +
                                        "settings put system $KEY_POINT_X ${targetState.x750}; " +
                                        "settings put system $KEY_POINT_Y ${targetState.y750}; " +
                                        "settings put system $KEY_BALL_MODE ${if (isCenter) 2 else 4}"
                                Runtime.getRuntime().exec(arrayOf("su", "-c", cmd)).waitFor()
                            } catch (_: Throwable) {}
                        }
                        isRootWorkerRunning = false
                    }
                }
            }
        }
    }
}
