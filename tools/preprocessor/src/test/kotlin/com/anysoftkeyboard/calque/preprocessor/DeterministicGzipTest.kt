package com.anysoftkeyboard.calque.preprocessor

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.GZIPInputStream
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DeterministicGzipTest {

    private fun compress(data: ByteArray): ByteArray =
        ByteArrayOutputStream().also { DeterministicGzip.compress(data, it) }.toByteArray()

    @Test
    fun compress_roundTripsThroughAStandardGzipReader() {
        val data = "Spanish: acción, ñandú, pingüino\n".repeat(500).toByteArray()

        assertArrayEquals(data, GZIPInputStream(compress(data).inputStream()).readBytes())
    }

    @Test
    fun compress_roundTripsEmptyInput() {
        assertEquals(0, GZIPInputStream(compress(ByteArray(0)).inputStream()).readBytes().size)
    }

    @Test
    fun compress_isByteIdenticalAcrossRuns() {
        val data = "same input".repeat(100).toByteArray()

        assertArrayEquals(compress(data), compress(data))
    }

    @Test
    fun compress_writesAFixedHeaderWithoutTimestampOrName() {
        val header = compress("x".toByteArray()).copyOfRange(0, 10)

        assertArrayEquals(
            byteArrayOf(0x1f, 0x8b.toByte(), 8, 0, 0, 0, 0, 0, 0, 0xff.toByte()),
            header,
        )
    }

    @Test
    fun compress_writesATrailerThatMatchesTheData() {
        val bytes = compress("hello".toByteArray())
        bytes[bytes.size - 5] = (bytes[bytes.size - 5] + 1).toByte() // corrupt the CRC

        assertThrows(IOException::class.java) { GZIPInputStream(bytes.inputStream()).readBytes() }
    }

    @Test
    fun openMaybeGzip_decompressesOnlyFilesNamedGz() {
        val dir = createTempDirectory("gzip-open").toFile()
        val plain = File(dir, "plain.txt").apply { writeText("plain") }
        val packed = File(dir, "packed.txt.gz")
        packed.outputStream().use { DeterministicGzip.compress("packed".toByteArray(), it) }

        assertEquals("plain", plain.openMaybeGzip().use { it.readBytes().toString(Charsets.UTF_8) })
        assertEquals(
            "packed",
            packed.openMaybeGzip().use { it.readBytes().toString(Charsets.UTF_8) }
        )
        dir.deleteRecursively()
    }
}
