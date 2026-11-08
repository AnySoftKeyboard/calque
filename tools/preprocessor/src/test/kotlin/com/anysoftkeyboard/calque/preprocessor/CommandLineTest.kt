package com.anysoftkeyboard.calque.preprocessor

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CommandLineTest {

    @Test
    fun parseCommandLineArgs_readsNameValuePairs() {
        val options = parseCommandLineArgs(listOf("--input", "a.xlsx", "--limit", "10"))

        assertEquals(mapOf("--input" to "a.xlsx", "--limit" to "10"), options)
    }

    @Test
    fun parseCommandLineArgs_ignoresFlagsWithoutAValueAndStrayArguments() {
        val options =
            parseCommandLineArgs(listOf("stray", "--flag", "--output", "out.tsv", "--last"))

        assertEquals(mapOf("--output" to "out.tsv"), options)
    }

    @Test
    fun parseCommandLineArgs_returnsNothingForNoArguments() {
        assertEquals(emptyMap<String, String>(), parseCommandLineArgs(emptyList()))
    }

    @Test
    fun resolveFile_returnsNullForNullAndAbsolutePathsUnchanged() {
        assertNull(resolveFile(null))
        assertEquals(File("/tmp/x.tsv"), resolveFile("/tmp/x.tsv"))
    }

    @Test
    fun requireFile_throwsAUsageExceptionNamingTheMissingOption() {
        val error =
            assertThrows(UsageException::class.java) {
                emptyMap<String, String>().requireFile("--input")
            }

        assertEquals("Missing required option --input", error.message)
    }

    @Test
    fun positiveInt_usesTheDefaultWhenAbsentAndParsesWhenPresent() {
        assertEquals(25_000, emptyMap<String, String>().positiveInt("--limit", 25_000))
        assertEquals(7, mapOf("--limit" to "7").positiveInt("--limit", 25_000))
    }

    @Test
    fun positiveInt_rejectsZeroNegativeAndNonNumericValues() {
        for (bad in listOf("0", "-3", "many", "")) {
            assertThrows(UsageException::class.java) {
                mapOf("--limit" to bad).positiveInt("--limit", 1)
            }
        }
    }
}
