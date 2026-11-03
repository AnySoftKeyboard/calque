package com.anysoftkeyboard.calque.toolbox

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberOdometerEngineTest {

    @Test
    fun formatNumberWithAgreement_oneAndTwentyOne() {
        val result1 = NumberOdometerEngine.formatNumberWithAgreement(1)
        assertEquals("un", result1.masculineText)
        assertEquals("una", result1.feminineText)

        val result21 = NumberOdometerEngine.formatNumberWithAgreement(21)
        assertEquals("veintiún", result21.masculineText)
        assertEquals("veintiuna", result21.feminineText)
    }

    @Test
    fun formatNumberWithAgreement_hundredsAgreement() {
        val result200 = NumberOdometerEngine.formatNumberWithAgreement(200)
        assertEquals("doscientos", result200.masculineText)
        assertEquals("doscientas", result200.feminineText)

        val result501 = NumberOdometerEngine.formatNumberWithAgreement(501)
        assertEquals("quinientos un", result501.masculineText)
        assertEquals("quinientas una", result501.feminineText)
    }

    @Test
    fun formatClockTime_conventions() {
        // Noon & Midnight
        assertEquals("Es mediodía", NumberOdometerEngine.formatClockTime(12, 0).phrase)
        assertEquals("Es medianoche", NumberOdometerEngine.formatClockTime(0, 0).phrase)

        // Exact hours
        assertEquals(
            "Es la una en punto de la madrugada",
            NumberOdometerEngine.formatClockTime(1, 0).phrase
        )
        assertEquals(
            "Son las dos en punto de la madrugada",
            NumberOdometerEngine.formatClockTime(2, 0).phrase
        )

        // Quarters and Half
        assertEquals(
            "Es la una y cuarto de la madrugada",
            NumberOdometerEngine.formatClockTime(1, 15).phrase
        )
        assertEquals(
            "Son las tres y media de la tarde",
            NumberOdometerEngine.formatClockTime(15, 30).phrase
        )

        // Less than quarter (next hour)
        assertEquals(
            "Son las cuatro menos cuarto de la tarde",
            NumberOdometerEngine.formatClockTime(15, 45).phrase
        )
        assertEquals(
            "Son las cinco menos diez de la tarde",
            NumberOdometerEngine.formatClockTime(16, 50).phrase
        )
    }

    @Test
    fun formatDate_formatsFirstAndOtherDays() {
        assertEquals("el primero de enero", NumberOdometerEngine.formatDate(1, 1).phrase)
        assertEquals("el 5 de mayo", NumberOdometerEngine.formatDate(5, 5).phrase)
        assertEquals(
            "el 12 de octubre de 1492",
            NumberOdometerEngine.formatDate(12, 10, 1492).phrase
        )
    }
}
