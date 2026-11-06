package com.drivepark.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.drivepark.app.domain.ThemeMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val UI_SCALE = 1.5f

private fun colorsOf(p: PleosPalette) = (if (p.isDark) darkColorScheme() else lightColorScheme()).copy(
    primary = p.text,
    onPrimary = p.onText,
    secondary = p.accent,
    onSecondary = Color.White,
    background = p.background,
    onBackground = p.text,
    surface = p.background,
    onSurface = p.text,
    surfaceVariant = p.surface,
    onSurfaceVariant = p.textSecondary,
    secondaryContainer = p.surface,
    onSecondaryContainer = p.text,
    primaryContainer = p.surface,
    onPrimaryContainer = p.text,
    surfaceContainer = p.surface,
    surfaceContainerLow = p.surface,
    surfaceContainerHigh = p.surface,
    surfaceContainerHighest = p.surface,
    outline = p.divider,
    outlineVariant = p.divider,
)

private val lightColors = colorsOf(PleosLightPalette)
private val darkColors = colorsOf(PleosDarkPalette)

// 굵기는 Regular와 SemiBold 두 가지만 쓴다. 차량 화면이라 기본보다 크게.
private val typography = Typography(
    headlineMedium = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 20.sp),
    bodyMedium = TextStyle(fontSize = 18.sp),
    labelLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 15.sp),
)

private val shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(Pleos.CardRadius),
    large = RoundedCornerShape(Pleos.CardRadius),
)

/** 앱 창 폭(기본 dp)이 이보다 좁으면 Pleos 3분할(앱 1/3) 화면으로 본다. 기본 화면은 1682, 3분할은 841. */
const val SPLIT_MAX_WIDTH_DP = 1200

/** 3분할에서는 같은 배율이면 내용이 넘치므로 조금 줄인다(841/1.2 = 약 700dp). */
fun uiScaleFor(windowWidthDp: Int): Float = if (windowWidthDp < SPLIT_MAX_WIDTH_DP) 1.2f else UI_SCALE

/** 지금 화면이 3분할처럼 좁은지. 화면들이 배치를 바꿀 때 쓴다. */
val LocalCompact = staticCompositionLocalOf { false }

/** 앱 테마. mode가 SYSTEM이면 Pleos(시스템) 다크모드를 따라간다. */
@Composable
fun DriveParkTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    // Pleos 차량 화면은 2560x1440에 160dpi라 dp 기준 UI가 작게 보인다. 순정 앱과 비슷한 크기로 1.5배 키운다.
    // 3분할(창 폭 841)에서는 1.2배로 줄이고 화면들이 좁은 배치를 쓰게 한다.
    val base = LocalDensity.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val dark = resolveDark(mode, isSystemInDarkTheme())
    val palette = if (dark) PleosDarkPalette else PleosLightPalette
    CompositionLocalProvider(
        LocalDensity provides Density(base.density * uiScaleFor(widthDp), base.fontScale),
        LocalPleosPalette provides palette,
        LocalCompact provides (widthDp < SPLIT_MAX_WIDTH_DP),
    ) {
        MaterialTheme(colorScheme = if (dark) darkColors else lightColors, typography = typography, shapes = shapes) {
            Surface(color = palette.background, content = content)
        }
    }
}
