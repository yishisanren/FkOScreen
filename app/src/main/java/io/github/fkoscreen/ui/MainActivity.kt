package io.github.fkoscreen.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fkoscreen.ConfigManager
import io.github.fkoscreen.ui.miuix.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiuixTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val prefs = remember { ConfigManager.getAppPrefs(context) }

    var manual1250 by remember { mutableStateOf(prefs.getBoolean(ConfigManager.KEY_MANUAL_1250, true)) }
    var manual1600 by remember { mutableStateOf(prefs.getBoolean(ConfigManager.KEY_MANUAL_1600, false)) }
    var fossBypass by remember { mutableStateOf(prefs.getBoolean(ConfigManager.KEY_FOSS_BYPASS, true)) }
    var hdrRatio by remember { mutableStateOf(prefs.getBoolean(ConfigManager.KEY_HDR_RATIO, true)) }
    var darkModeStyles by remember { mutableStateOf(prefs.getBoolean(ConfigManager.KEY_DARK_MODE_STYLES, true)) }
    var colorBallAnchor by remember { mutableStateOf(prefs.getBoolean(ConfigManager.KEY_COLOR_BALL_ANCHOR, true)) }

    fun updatePref(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        ConfigManager.fixPermissions(context)
    }

    LaunchedEffect(Unit) {
        if (!prefs.contains(ConfigManager.KEY_MANUAL_1250)) {
            prefs.edit()
                .putBoolean(ConfigManager.KEY_MANUAL_1250, true)
                .putBoolean(ConfigManager.KEY_MANUAL_1600, false)
                .putBoolean(ConfigManager.KEY_FOSS_BYPASS, true)
                .putBoolean(ConfigManager.KEY_HDR_RATIO, true)
                .putBoolean(ConfigManager.KEY_DARK_MODE_STYLES, true)
                .putBoolean(ConfigManager.KEY_COLOR_BALL_ANCHOR, true)
                .apply()
        }
        ConfigManager.fixPermissions(context)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (isDark) MiuixColors.BackgroundDark else MiuixColors.BackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            MiuixTopAppBar(
                title = "FkOScreen",
                subtitle = "ColorOS 屏幕显示与色彩增强"
            )

            // 卡片 1：亮度与 HDR
            MiuixCard(title = "屏幕亮度与 HDR") {
                MiuixPreferenceItem(
                    title = "手动最高亮度提升",
                    summary = "将手动滑块上限提升至 1250 nits",
                    showDivider = true
                ) {
                    MiuixSwitch(
                        checked = manual1250,
                        onCheckedChange = {
                            manual1250 = it
                            updatePref(ConfigManager.KEY_MANUAL_1250, it)
                        }
                    )
                }

                MiuixPreferenceItem(
                    title = "解锁最高亮度限制 (1600 nits)",
                    summary = "试验性功能：将亮度滑块上限开放至屏幕硬件峰值 1600 nits",
                    showDivider = true
                ) {
                    MiuixSwitch(
                        checked = manual1600,
                        onCheckedChange = {
                            manual1600 = it
                            updatePref(ConfigManager.KEY_MANUAL_1600, it)
                        }
                    )
                }

                MiuixPreferenceItem(
                    title = "阻止 FOSS 自动降亮",
                    summary = "拦截针对特定应用的 15% 亮度扣减",
                    showDivider = true
                ) {
                    MiuixSwitch(
                        checked = fossBypass,
                        onCheckedChange = {
                            fossBypass = it
                            updatePref(ConfigManager.KEY_FOSS_BYPASS, it)
                        }
                    )
                }

                MiuixPreferenceItem(
                    title = "修复 HDR 亮度比例",
                    summary = "锁定 hdrSdrRatio 为 1.56，正常显示 Ultra HDR 高光",
                    showDivider = false
                ) {
                    MiuixSwitch(
                        checked = hdrRatio,
                        onCheckedChange = {
                            hdrRatio = it
                            updatePref(ConfigManager.KEY_HDR_RATIO, it)
                        }
                    )
                }
            }

            // 卡片 2：色彩与深色模式
            MiuixCard(title = "色彩与深色模式") {
                MiuixPreferenceItem(
                    title = "深色模式三档调节",
                    summary = "开启增强、适中、柔和三档深色样式选择及效果预览",
                    showDivider = true
                ) {
                    MiuixSwitch(
                        checked = darkModeStyles,
                        onCheckedChange = {
                            darkModeStyles = it
                            updatePref(ConfigManager.KEY_DARK_MODE_STYLES, it)
                        }
                    )
                }

                MiuixPreferenceItem(
                    title = "色温球作为自适应基底",
                    summary = "解除色温球调节限制，将护眼与环境色调节叠加在色温球白点上",
                    showDivider = false
                ) {
                    MiuixSwitch(
                        checked = colorBallAnchor,
                        onCheckedChange = {
                            colorBallAnchor = it
                            updatePref(ConfigManager.KEY_COLOR_BALL_ANCHOR, it)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 快捷热重载
            Button(
                onClick = {
                    try {
                        Runtime.getRuntime().exec(arrayOf("su", "-c", "killall com.android.settings com.android.systemui"))
                        Toast.makeText(context, "已重载系统设置与界面服务", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "执行热重载需要 Root 权限: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) MiuixColors.PrimaryDark else MiuixColors.PrimaryLight
                )
            ) {
                Text(
                    text = "重载系统设置与界面",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
