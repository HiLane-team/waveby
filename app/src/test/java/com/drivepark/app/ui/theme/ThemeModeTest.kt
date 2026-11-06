package com.drivepark.app.ui.theme

import com.drivepark.app.domain.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemeModeTest {
    @Test
    fun system_followsPleos_otherwiseFixed() {
        assertEquals(true, resolveDark(ThemeMode.SYSTEM, systemDark = true))
        assertEquals(false, resolveDark(ThemeMode.SYSTEM, systemDark = false))
        assertEquals(false, resolveDark(ThemeMode.LIGHT, systemDark = true))
        assertEquals(true, resolveDark(ThemeMode.DARK, systemDark = false))
    }

    @Test
    fun splitScreen_usesSmallerScale() {
        assertEquals(1.5f, uiScaleFor(1682), 0f) // Pleos 기본(오른쪽 2/3)
        assertEquals(1.2f, uiScaleFor(841), 0f)  // 3분할(1/3)
    }

    @Test
    fun darkPalette_invertsBackgroundAndText_keepsAccent() {
        assertNotEquals(PleosLightPalette.background, PleosDarkPalette.background)
        assertNotEquals(PleosLightPalette.text, PleosDarkPalette.text)
        assertEquals(PleosLightPalette.accent, PleosDarkPalette.accent)
        // 반전 글자색은 같은 테마의 글자색과 달라야 읽힌다.
        listOf(PleosLightPalette, PleosDarkPalette).forEach { assertNotEquals(it.text, it.onText) }
    }
}
