package com.anysoftkeyboard.calque.preprocessor

/**
 * A fixed hash of a sentence id (the SplitMix64 finalizer). It spreads consecutive ids evenly and
 * never changes between runs or JVMs, which makes "keep the N sentences with the smallest hash" an
 * unbiased and reproducible way to sample.
 */
internal object StableHash {
    private const val GOLDEN_GAMMA = -0x61c8864680b583ebL
    private const val MULTIPLIER_1 = -0x40a7b892e31b1a47L
    private const val MULTIPLIER_2 = -0x6b2fb644ecceee15L
    private const val SHIFT_1 = 30
    private const val SHIFT_2 = 27
    private const val SHIFT_3 = 31

    fun of(id: Long): Long {
        var z = id + GOLDEN_GAMMA
        z = (z xor (z ushr SHIFT_1)) * MULTIPLIER_1
        z = (z xor (z ushr SHIFT_2)) * MULTIPLIER_2
        return z xor (z ushr SHIFT_3)
    }
}
