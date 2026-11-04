package com.anysoftkeyboard.calque.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CalqueThemeTest {

    @Test
    fun testCalqueShapesTokens() {
        assertNotNull(CalqueShapes.small)
        assertNotNull(CalqueShapes.medium)
        assertNotNull(CalqueShapes.large)
    }

    @Test
    fun testComposeTypographyDefinitions() {
        val typography = CalqueComposeTypography
        // Headings & reading text must use Serif
        assertEquals(FontFamily.Serif, typography.headlineLarge.fontFamily)
        assertEquals(FontFamily.Serif, typography.headlineMedium.fontFamily)
        assertEquals(FontFamily.Serif, typography.titleLarge.fontFamily)
        assertEquals(FontFamily.Serif, typography.bodyLarge.fontFamily)

        // UI text must use SansSerif
        assertEquals(FontFamily.SansSerif, typography.titleMedium.fontFamily)
        assertEquals(FontFamily.SansSerif, typography.bodyMedium.fontFamily)
        assertEquals(FontFamily.SansSerif, typography.labelLarge.fontFamily)

        // Reading font sizing
        assertEquals(18.sp, typography.bodyLarge.fontSize)
        assertEquals(24.sp, typography.headlineLarge.fontSize)
    }

    @Test
    fun testCalqueColorTokensInitialization() {
        val lightTokens = CalqueColors.Light.toTokens()
        assertNotNull(lightTokens.background)
        assertNotNull(lightTokens.surface)
        assertNotNull(lightTokens.textPrimary)
        assertNotNull(lightTokens.slateBlueAccent)

        val darkTokens = CalqueColors.Dark.toTokens()
        assertNotNull(darkTokens.background)
        assertNotNull(darkTokens.surface)
        assertNotNull(darkTokens.textPrimary)
        assertNotNull(darkTokens.slateBlueAccent)
    }
}
