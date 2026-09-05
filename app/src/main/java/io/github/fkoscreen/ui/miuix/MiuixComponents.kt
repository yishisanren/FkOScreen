package io.github.fkoscreen.ui.miuix

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MiuixSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()

    val trackColor by animateColorAsState(
        targetValue = if (checked) {
            if (isDark) MiuixColors.PrimaryDark else MiuixColors.PrimaryLight
        } else {
            if (isDark) MiuixColors.TrackUncheckedDark else MiuixColors.TrackUncheckedLight
        },
        animationSpec = spring(dampingRatio = 0.95f, stiffness = 400f),
        label = "switchTrackColor"
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 500f),
        label = "switchThumbOffset"
    )

    Box(
        modifier = modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor)
            .clickable(enabled = enabled) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .clip(CircleShape)
                .background(MiuixColors.Thumb)
        )
    }
}

@Composable
fun MiuixCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) MiuixColors.CardBackgroundDark else MiuixColors.CardBackgroundLight

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        if (title != null) {
            Text(
                text = title.uppercase(),
                color = if (isDark) MiuixColors.TextSecondaryDark else MiuixColors.TextSecondaryLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp, bottom = 6.dp, top = 6.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(bgColor)
                .padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
fun MiuixPreferenceItem(
    title: String,
    summary: String? = null,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    trailing: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val textPrimary = if (isDark) MiuixColors.TextPrimaryDark else MiuixColors.TextPrimaryLight
    val textSecondary = if (isDark) MiuixColors.TextSecondaryDark else MiuixColors.TextSecondaryLight
    val dividerColor = if (isDark) MiuixColors.DividerDark else MiuixColors.DividerLight

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title,
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (summary != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = summary,
                        color = textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    )
                }
            }
            trailing()
        }
        if (showDivider) {
            Divider(
                color = dividerColor,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp)
            )
        }
    }
}

@Composable
fun MiuixTopAppBar(
    title: String,
    subtitle: String? = null
) {
    val isDark = isSystemInDarkTheme()
    val textPrimary = if (isDark) MiuixColors.TextPrimaryDark else MiuixColors.TextPrimaryLight
    val textSecondary = if (isDark) MiuixColors.TextSecondaryDark else MiuixColors.TextSecondaryLight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(
            text = title,
            color = textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = textSecondary,
                fontSize = 14.sp
            )
        }
    }
}
