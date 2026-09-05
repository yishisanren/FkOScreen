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
    var fossBypass by remember { mutableStateOf(prefs.getBoolean(ConfigManager.KEY_FOSS_BYPASS, true)) }
    var hdrRatio by remember { mutableStateOf(prefs.getBoolean(ConfigManager.KEY_HDR_RATIO, true)) }
    var darkModeStyles by remember { mutableStateOf(prefs.getBoolean(ConfigManager.KEY_DARK_MODE_STYLES, true)) }
    var colorBallAnchor by remember { mutableStateOf(prefs.getBoolean(ConfigManager.KEY_COLOR_BALL_ANCHOR, true)) }

    fun updatePref(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
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
                subtitle = "OPPO Pad Mini 显示与色彩科学增强 (ColorOS 16)"
            )

            // 卡片 1：亮度与画质引擎
            MiuixCard(title = "亮度与画质引擎") {
                MiuixPreferenceItem(
                    title = "手动亮度解限至 1250 nits",
                    summary = "将手动滑块正常上限由 800 nits (4095) 拓宽至杜比视界上限 1250 nits (4543)",
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
                    title = "阻断 FOSS 15% 亮度暗扣",
                    summary = "拦截 ColorOS 对微信、B站、微博、抖音等应用的后台自动偷扣 15% 亮度惩罚",
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
                    title = "修复 AOSP hdrSdrRatio",
                    summary = "修复 Android 16 比例死锁在 1.0 的缺陷，精准锚定 1.56，激发 Ultra HDR 照片高光",
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

            // 卡片 2：色彩科学与界面控制
            MiuixCard(title = "色彩科学与界面") {
                MiuixPreferenceItem(
                    title = "深色模式三档调节",
                    summary = "击穿 API 36 出厂隐藏门禁，复原原生“增强(纯黑) / 适中(深灰) / 柔和(中灰)”及图片预览",
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
                    summary = "解除自适应/护眼时色温球的禁用置灰，将环境自适应与护眼变化叠加在用户校准白点底色之上",
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

            // 卡片 3：安全与架构准则
            MiuixCard(title = "架构与安全边界") {
                MiuixPreferenceItem(
                    title = "温控保护保持原厂",
                    summary = "本模块坚决不改动任何温控逻辑与降频限额，极端发热时由原厂温控全权保护硬件",
                    showDivider = true
                ) {
                    Text(
                        text = "100% 原厂",
                        color = Color(0xFF34C759),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                MiuixPreferenceItem(
                    title = "纯内存运行 / 完全可逆",
                    summary = "绝不修改 /system 或 /my_product 任何物理分区，在 LSPosed 管理器中可随时停用",
                    showDivider = false
                ) {
                    Text(
                        text = "Systemless",
                        color = Color(0xFF0D84FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 快捷热重启
            Button(
                onClick = {
                    try {
                        Runtime.getRuntime().exec(arrayOf("su", "-c", "killall com.android.settings com.android.systemui"))
                        Toast.makeText(context, "已重载系统设置与 SystemUI 作用域", Toast.LENGTH_SHORT).show()
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
                    text = "热重载系统设置与界面 (无需重启平板)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
