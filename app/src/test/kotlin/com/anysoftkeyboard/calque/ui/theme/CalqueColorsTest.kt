package com.anysoftkeyboard.calque.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalqueColorsTest {

    @Test
    fun testLightPaletteValuesMatchDesignDoc() {
        val light = CalqueColors.Light
        assertFalse(light.isDark)
        // Background: #FBF9F5
        assertEquals(0xFFFBF9F5.toInt(), light.background)
        // Background Secondary: #F3EFE6
        assertEquals(0xFFF3EFE6.toInt(), light.backgroundSecondary)
        // Surface: #FFFFFF
        assertEquals(0xFFFFFFFF.toInt(), light.surface)
        // Surface Elevated: #FFFFFF
        assertEquals(0xFFFFFFFF.toInt(), light.surfaceElevated)
        // Text primary (Ink): #1C1B18
        assertEquals(0xFF1C1B18.toInt(), light.textPrimary)
        // Text secondary: #6E6A61
        assertEquals(0xFF6E6A61.toInt(), light.textSecondary)
        // Slate Blue: #2B5B84
        assertEquals(0xFF2B5B84.toInt(), light.slateBlueAccent)
        // Cognate Emerald: #1E7A56
        assertEquals(0xFF1E7A56.toInt(), light.cognateEmerald)
        // Cognate Emerald Pill Fill: #E6F4EE
        assertEquals(0xFFE6F4EE.toInt(), light.cognateEmeraldFill)
        // False Cognate Amber: #B85D00
        assertEquals(0xFFB85D00.toInt(), light.falseCognateAmber)
        // False Cognate Amber Fill: #FFF3E0
        assertEquals(0xFFFFF3E0.toInt(), light.falseCognateAmberFill)
        // Vault Indigo: #4A55A2
        assertEquals(0xFF4A55A2.toInt(), light.vaultIndigo)
        // Vault Indigo Fill: #ECEFF9
        assertEquals(0xFFECEFF9.toInt(), light.vaultIndigoFill)
        // Border: #E5E0D8
        assertEquals(0xFFE5E0D8.toInt(), light.border)
        // Error Crimson: #BA1A1A
        assertEquals(0xFFBA1A1A.toInt(), light.errorCrimson)
        // Error Crimson Fill: #FFDAD6
        assertEquals(0xFFFFDAD6.toInt(), light.errorCrimsonFill)
    }

    @Test
    fun testDarkPaletteValuesMatchDesignDoc() {
        val dark = CalqueColors.Dark
        assertTrue(dark.isDark)
        // Background: #141413
        assertEquals(0xFF141413.toInt(), dark.background)
        // Background Secondary: #1F1E1B
        assertEquals(0xFF1F1E1B.toInt(), dark.backgroundSecondary)
        // Surface: #1E1D1B
        assertEquals(0xFF1E1D1B.toInt(), dark.surface)
        // Surface Elevated: #2A2925
        assertEquals(0xFF2A2925.toInt(), dark.surfaceElevated)
        // Text primary (Ink): #EAE6DF
        assertEquals(0xFFEAE6DF.toInt(), dark.textPrimary)
        // Text secondary: #9E9A90
        assertEquals(0xFF9E9A90.toInt(), dark.textSecondary)
        // Slate Blue: #6B9AC4
        assertEquals(0xFF6B9AC4.toInt(), dark.slateBlueAccent)
        // Cognate Emerald: #34D399
        assertEquals(0xFF34D399.toInt(), dark.cognateEmerald)
        // Cognate Emerald Fill: #133326
        assertEquals(0xFF133326.toInt(), dark.cognateEmeraldFill)
        // False Cognate Amber: #F59E0B
        assertEquals(0xFFF59E0B.toInt(), dark.falseCognateAmber)
        // False Cognate Amber Fill: #3A230A
        assertEquals(0xFF3A230A.toInt(), dark.falseCognateAmberFill)
        // Vault Indigo: #818CF8
        assertEquals(0xFF818CF8.toInt(), dark.vaultIndigo)
        // Vault Indigo Fill: #1F2547
        assertEquals(0xFF1F2547.toInt(), dark.vaultIndigoFill)
        // Border: #2E2C29
        assertEquals(0xFF2E2C29.toInt(), dark.border)
        // Error Crimson: #FFB4AB
        assertEquals(0xFFFFB4AB.toInt(), dark.errorCrimson)
        // Error Crimson Fill: #410E0B
        assertEquals(0xFF410E0B.toInt(), dark.errorCrimsonFill)
    }

    @Test
    fun testPalettesAreDistinct() {
        assertNotEquals(CalqueColors.Light.background, CalqueColors.Dark.background)
        assertNotEquals(CalqueColors.Light.textPrimary, CalqueColors.Dark.textPrimary)
        assertNotEquals(CalqueColors.Light.surface, CalqueColors.Dark.surface)
        assertNotEquals(CalqueColors.Light.slateBlueAccent, CalqueColors.Dark.slateBlueAccent)
        assertNotEquals(CalqueColors.Light.cognateEmerald, CalqueColors.Dark.cognateEmerald)
        assertNotEquals(CalqueColors.Light.falseCognateAmber, CalqueColors.Dark.falseCognateAmber)
        assertNotEquals(CalqueColors.Light.vaultIndigo, CalqueColors.Dark.vaultIndigo)
        assertNotEquals(CalqueColors.Light.border, CalqueColors.Dark.border)
        assertNotEquals(
            CalqueColors.Light.backgroundSecondary,
            CalqueColors.Dark.backgroundSecondary
        )
        assertNotEquals(CalqueColors.Light.surfaceElevated, CalqueColors.Dark.surfaceElevated)
    }

    @Test
    fun testToTokensProducesMatchingComposeColors() {
        val lightTokens = CalqueColors.Light.toTokens()
        assertFalse(lightTokens.isDark)
        assertEquals(Color(CalqueColors.Light.background), lightTokens.background)
        assertEquals(Color(CalqueColors.Light.surface), lightTokens.surface)
        assertEquals(Color(CalqueColors.Light.textPrimary), lightTokens.textPrimary)
        assertEquals(Color(CalqueColors.Light.slateBlueAccent), lightTokens.slateBlueAccent)
        assertEquals(Color(CalqueColors.Light.cognateEmerald), lightTokens.cognateEmerald)
        assertEquals(Color(CalqueColors.Light.falseCognateAmber), lightTokens.falseCognateAmber)
        assertEquals(Color(CalqueColors.Light.vaultIndigo), lightTokens.vaultIndigo)

        val darkTokens = CalqueColors.Dark.toTokens()
        assertTrue(darkTokens.isDark)
        assertEquals(Color(CalqueColors.Dark.background), darkTokens.background)
        assertEquals(Color(CalqueColors.Dark.surface), darkTokens.surface)
        assertEquals(Color(CalqueColors.Dark.textPrimary), darkTokens.textPrimary)
        assertEquals(Color(CalqueColors.Dark.slateBlueAccent), darkTokens.slateBlueAccent)
        assertEquals(Color(CalqueColors.Dark.cognateEmerald), darkTokens.cognateEmerald)
        assertEquals(Color(CalqueColors.Dark.falseCognateAmber), darkTokens.falseCognateAmber)
        assertEquals(Color(CalqueColors.Dark.vaultIndigo), darkTokens.vaultIndigo)
    }

    @Test
    fun testMaterialColorSchemesMapCorrectly() {
        assertEquals(Color(CalqueColors.Light.background), LightColorScheme.background)
        assertEquals(Color(CalqueColors.Light.textPrimary), LightColorScheme.onBackground)
        assertEquals(Color(CalqueColors.Light.surface), LightColorScheme.surface)
        assertEquals(Color(CalqueColors.Light.slateBlueAccent), LightColorScheme.primary)

        assertEquals(Color(CalqueColors.Dark.background), DarkColorScheme.background)
        assertEquals(Color(CalqueColors.Dark.textPrimary), DarkColorScheme.onBackground)
        assertEquals(Color(CalqueColors.Dark.surface), DarkColorScheme.surface)
        assertEquals(Color(CalqueColors.Dark.slateBlueAccent), DarkColorScheme.primary)
    }
}
