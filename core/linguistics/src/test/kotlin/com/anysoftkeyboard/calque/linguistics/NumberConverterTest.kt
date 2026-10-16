package com.anysoftkeyboard.calque.linguistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NumberConverterTest {

    @Test
    fun convert_zeroToFifteen() {
        assertEquals("cero", NumberConverter.convert(0))
        assertEquals("uno", NumberConverter.convert(1))
        assertEquals("dos", NumberConverter.convert(2))
        assertEquals("tres", NumberConverter.convert(3))
        assertEquals("cuatro", NumberConverter.convert(4))
        assertEquals("cinco", NumberConverter.convert(5))
        assertEquals("diez", NumberConverter.convert(10))
        assertEquals("quince", NumberConverter.convert(15))
    }

    @Test
    fun convert_fusedTens_withAccents() {
        assertEquals("dieciséis", NumberConverter.convert(16))
        assertEquals("diecisiete", NumberConverter.convert(17))
        assertEquals("veinte", NumberConverter.convert(20))
        assertEquals("veintiuno", NumberConverter.convert(21))
        assertEquals("veintidós", NumberConverter.convert(22))
        assertEquals("veintitrés", NumberConverter.convert(23))
        assertEquals("veinticuatro", NumberConverter.convert(24))
        assertEquals("veinticinco", NumberConverter.convert(25))
        assertEquals("veintiséis", NumberConverter.convert(26))
        assertEquals("veintinueve", NumberConverter.convert(29))
    }

    @Test
    fun convert_separatedTens() {
        assertEquals("treinta", NumberConverter.convert(30))
        assertEquals("treinta y uno", NumberConverter.convert(31))
        assertEquals("cuarenta y cinco", NumberConverter.convert(45))
        assertEquals("noventa y nueve", NumberConverter.convert(99))
    }

    @Test
    fun convert_hundreds() {
        assertEquals("cien", NumberConverter.convert(100))
        assertEquals("ciento uno", NumberConverter.convert(101))
        assertEquals("doscientos", NumberConverter.convert(200))
        assertEquals("quinientos", NumberConverter.convert(500))
        assertEquals("setecientos", NumberConverter.convert(700))
        assertEquals("novecientos noventa y nueve", NumberConverter.convert(999))
    }

    @Test
    fun convert_thousands() {
        assertEquals("mil", NumberConverter.convert(1000))
        assertEquals("mil uno", NumberConverter.convert(1001))
        assertEquals("dos mil", NumberConverter.convert(2000))
        assertEquals("veintiún mil", NumberConverter.convert(21000))
        assertEquals("cien mil", NumberConverter.convert(100000))
        assertEquals(
            "novecientos noventa y nueve mil novecientos noventa y nueve",
            NumberConverter.convert(999999)
        )
    }

    @Test
    fun convert_genderAndApocope() {
        assertEquals("una", NumberConverter.convert(1, gender = GrammaticalGender.FEMININE))
        assertEquals("un", NumberConverter.convert(1, isApocope = true))
        assertEquals("veintiuna", NumberConverter.convert(21, gender = GrammaticalGender.FEMININE))
        assertEquals("veintiún", NumberConverter.convert(21, isApocope = true))
        assertEquals(
            "doscientas",
            NumberConverter.convert(200, gender = GrammaticalGender.FEMININE)
        )
        assertEquals(
            "doscientas cincuenta",
            NumberConverter.convert(250, gender = GrammaticalGender.FEMININE)
        )
    }

    @Test
    fun convert_outOfRange_throwsException() {
        assertThrows(IllegalArgumentException::class.java) { NumberConverter.convert(-1) }
        assertThrows(IllegalArgumentException::class.java) { NumberConverter.convert(1_000_000) }
    }
}
