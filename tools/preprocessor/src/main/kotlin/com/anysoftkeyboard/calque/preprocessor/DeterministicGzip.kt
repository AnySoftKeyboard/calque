package com.anysoftkeyboard.calque.preprocessor

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPInputStream

/**
 * Writes gzip streams with a fixed header (no modification time, no file name, "unknown" OS), so
 * that re-running an extractor on the same input produces a byte-identical file and a regenerated,
 * unchanged subset does not show up as a diff.
 */
object DeterministicGzip {
    private const val ID1 = 0x1f
    private const val ID2 = 0x8b
    private const val METHOD_DEFLATE = 8
    private const val OS_UNKNOWN = 0xff
    private const val HEADER_SIZE = 10
    private const val TRAILER_SIZE = 8

    private val HEADER =
        ByteArray(HEADER_SIZE).also {
            it[0] = ID1.toByte()
            it[1] = ID2.toByte()
            it[2] = METHOD_DEFLATE.toByte()
            // Flags, 4 bytes of mtime and the extra-flags byte stay 0.
            it[HEADER_SIZE - 1] = OS_UNKNOWN.toByte()
        }

    /** Compresses [data] into [output] as a single gzip member. Does not close [output]. */
    fun compress(data: ByteArray, output: OutputStream) {
        output.write(HEADER)
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        try {
            val deflated = DeflaterOutputStream(output, deflater)
            deflated.write(data)
            deflated.finish()
        } finally {
            deflater.end()
        }
        val crc = CRC32().also { it.update(data) }
        val trailer = ByteBuffer.allocate(TRAILER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        trailer.putInt(crc.value.toInt())
        trailer.putInt(data.size)
        output.write(trailer.array())
        output.flush()
    }
}

/** Opens this file for reading, transparently decompressing it when its name ends in `.gz`. */
fun File.openMaybeGzip(): InputStream {
    val raw = inputStream().buffered()
    return if (name.endsWith(".gz")) GZIPInputStream(raw) else raw
}
