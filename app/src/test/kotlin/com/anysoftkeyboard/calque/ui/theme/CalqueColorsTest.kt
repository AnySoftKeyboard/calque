package com.anysoftkeyboard.calque.ui.theme

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
        // Text primary (Ink): #1C1B18
        assertEquals(0xFF1C1B18.toInt(), light.textPrimary)
        // Slate Blue: #2B5B84
        assertEquals(0xFF2B5B84.toInt(), light.slateBlueAccent)
        // Cognate Emerald: #1E7A56
        assertEquals(0xFF1E7A56.toInt(), light.cognateEmerald)
        // False Cognate Amber: #B85D00
        assertEquals(0xFFB85D00.toInt(), light.falseCognateAmber)
        // Vault Indigo: #4A55A2
        assertEquals(0xFF4A55A2.toInt(), light.vaultIndigo)
    }

    @Test
    fun testDarkPaletteValuesMatchDesignDoc() {
        val dark = CalqueColors.Dark
        assertTrue(dark.isDark)
        // Background: #141413
        assertEquals(0xFF141413.toInt(), dark.background)
        // Text primary (Ink): #EAE6DF
        assertEquals(0xFFEAE6DF.toInt(), dark.textPrimary)
        // Slate Blue: #6B9AC4
        assertEquals(0xFF6B9AC4.toInt(), dark.slateBlueAccent)
        // Cognate Emerald: #34D399
        assertEquals(0xFF34D399.toInt(), dark.cognateEmerald)
        // False Cognate Amber: #F59E0B
        assertEquals(0xFFF59E0B.toInt(), dark.falseCognateAmber)
        // Vault Indigo: #818CF8
        assertEquals(0xFF818CF8.toInt(), dark.vaultIndigo)
    }

    @Test
    fun testPalettesAreDistinct() {
        assertNotEquals(CalqueColors.Light.background, CalqueColors.Dark.background)
        assertNotEquals(CalqueColors.Light.textPrimary, CalqueColors.Dark.textPrimary)
    }
}
