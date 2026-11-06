package com.drivepark.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.drivepark.app.domain.ThemeMode

/** 한 테마의 색 묶음. 라이트는 Pleos Connect 기본 화면, 다크는 Pleos 다크 화면을 기준으로 했다. */
data class PleosPalette(
    val background: Color,
    val surface: Color,
    val divider: Color,
    val text: Color,
    val textSecondary: Color,
    /** text 색 바탕 위에 올리는 글자색(반전 칩, "나" 이름표). */
    val onText: Color,
    val accent: Color,
    val scrim: Color,
    /** 캐릭터 뒤 조명(숨쉬기) 색. */
    val glow: Color,
    val isDark: Boolean,
)

val PleosLightPalette = PleosPalette(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF4F4F5),
    divider = Color(0xFFE4E4E7),
    text = Color(0xFF18181B),
    textSecondary = Color(0xFF71717A),
    onText = Color(0xFFFFFFFF),
    accent = Color(0xFFFF7A1A),
    scrim = Color(0xCC18181B),
    glow = Color(0xFFFFB27A), // 부드러운 살구빛(주황 강조색을 옅게)
    isDark = false,
)

val PleosDarkPalette = PleosPalette(
    background = Color(0xFF0F0F11),
    surface = Color(0xFF1E1E22),
    divider = Color(0xFF2E2E33),
    text = Color(0xFFF4F4F5),
    textSecondary = Color(0xFFA1A1AA),
    onText = Color(0xFF18181B),
    accent = Color(0xFFFF7A1A),
    scrim = Color(0xCC000000),
    glow = Color(0xFFFFC27A), // 밤 가로등 같은 따뜻한 호박빛
    isDark = true,
)

val LocalPleosPalette = staticCompositionLocalOf { PleosLightPalette }

/** 설정값과 시스템(Pleos) 다크 여부로 실제 다크 사용 여부를 정한다. */
fun resolveDark(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** 디자인 토큰. 색은 지금 테마(라이트/다크)에 맞춰 바뀐다. */
object Pleos {
    val Background: Color @Composable @ReadOnlyComposable get() = LocalPleosPalette.current.background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalPleosPalette.current.surface
    val Divider: Color @Composable @ReadOnlyComposable get() = LocalPleosPalette.current.divider
    val Text: Color @Composable @ReadOnlyComposable get() = LocalPleosPalette.current.text
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalPleosPalette.current.textSecondary
    val OnText: Color @Composable @ReadOnlyComposable get() = LocalPleosPalette.current.onText
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalPleosPalette.current.accent
    val Scrim: Color @Composable @ReadOnlyComposable get() = LocalPleosPalette.current.scrim
    val Glow: Color @Composable @ReadOnlyComposable get() = LocalPleosPalette.current.glow
    val IsDark: Boolean @Composable @ReadOnlyComposable get() = LocalPleosPalette.current.isDark

    val CardRadius = 20.dp
    val RailWidth = 132.dp
}
