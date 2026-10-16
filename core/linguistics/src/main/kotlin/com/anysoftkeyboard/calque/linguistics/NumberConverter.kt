package com.anysoftkeyboard.calque.linguistics

/** Grammatical gender for number conversion. */
enum class GrammaticalGender {
    MASCULINE,
    FEMININE,
}

/**
 * Deterministic Spanish number to written words converter for integers 0..999,999.
 *
 * Implements the Deterministic Number Converter algorithm from docs/INGESTION.md.
 */
object NumberConverter {
    private const val MAX_SUPPORTED_NUMBER = 999_999
    private const val THOUSAND = 1000
    private const val HUNDRED = 100
    private const val TEN = 10

    private const val NUM_ZERO = 0
    private const val NUM_ONE = 1
    private const val NUM_FIFTEEN = 15
    private const val NUM_SIXTEEN = 16
    private const val NUM_TWENTY_ONE = 21
    private const val NUM_TWENTY_NINE = 29

    private val UNITS_TO_FIFTEEN =
        listOf(
            "cero",
            "uno",
            "dos",
            "tres",
            "cuatro",
            "cinco",
            "seis",
            "siete",
            "ocho",
            "nueve",
            "diez",
            "once",
            "doce",
            "trece",
            "catorce",
            "quince",
        )

    private val FUSED_TENS =
        listOf(
            "dieciséis",
            "diecisiete",
            "dieciocho",
            "diecinueve",
            "veinte",
            "veintiuno",
            "veintidós",
            "veintitrés",
            "veinticuatro",
            "veinticinco",
            "veintiséis",
            "veintisiete",
            "veintiocho",
            "veintinueve",
        )

    private val TENS_PREFIXES =
        listOf(
            "",
            "",
            "",
            "treinta",
            "cuarenta",
            "cincuenta",
            "sesenta",
            "setenta",
            "ochenta",
            "noventa",
        )

    private val HUNDREDS_MASC =
        listOf(
            "",
            "ciento",
            "doscientos",
            "trescientos",
            "cuatrocientos",
            "quinientos",
            "seiscientos",
            "setecientos",
            "ochocientos",
            "novecientos",
        )

    private val HUNDREDS_FEM =
        listOf(
            "",
            "ciento",
            "doscientas",
            "trescientas",
            "cuatrocientas",
            "quinientas",
            "seiscientas",
            "setecientas",
            "ochocientas",
            "novecientas",
        )

    /**
     * Converts an integer in range `0..999,999` to its written Spanish word equivalent.
     *
     * @param number The non-negative integer to convert.
     * @param gender Target grammatical gender for agreement.
     * @param isApocope Whether to truncate final "uno" -> "un" (e.g., before masculine nouns).
     */
    fun convert(
        number: Int,
        gender: GrammaticalGender = GrammaticalGender.MASCULINE,
        isApocope: Boolean = false,
    ): String {
        require(number in NUM_ZERO..MAX_SUPPORTED_NUMBER) {
            "Number $number is out of supported range 0..$MAX_SUPPORTED_NUMBER"
        }
        if (number == NUM_ZERO) return UNITS_TO_FIFTEEN[NUM_ZERO]

        val thousands = number / THOUSAND
        val remainder = number % THOUSAND

        val parts = mutableListOf<String>()
        if (thousands > 0) {
            if (thousands == NUM_ONE) {
                parts.add("mil")
            } else {
                parts.add("${convertUnderThousand(thousands, gender, isApocope = true)} mil")
            }
        }

        if (remainder > 0) {
            parts.add(convertUnderThousand(remainder, gender, isApocope))
        }

        return parts.joinToString(" ")
    }

    private fun convertUnderThousand(
        number: Int,
        gender: GrammaticalGender,
        isApocope: Boolean,
    ): String {
        if (number == HUNDRED) return "cien"

        val hundredsDigit = number / HUNDRED
        val remainder = number % HUNDRED

        val parts = mutableListOf<String>()
        if (hundredsDigit > 0) {
            val list = if (gender == GrammaticalGender.FEMININE) HUNDREDS_FEM else HUNDREDS_MASC
            parts.add(list[hundredsDigit])
        }

        if (remainder > 0) {
            parts.add(convertUnderHundred(remainder, gender, isApocope))
        }

        return parts.joinToString(" ")
    }

    private fun convertUnderHundred(
        number: Int,
        gender: GrammaticalGender,
        isApocope: Boolean,
    ): String {
        return when {
            number <= NUM_FIFTEEN -> resolveUnit(number, gender, isApocope)
            number in NUM_SIXTEEN..NUM_TWENTY_NINE -> resolveFusedTens(number, gender, isApocope)
            else -> resolveSeparatedTens(number, gender, isApocope)
        }
    }

    private fun resolveUnit(number: Int, gender: GrammaticalGender, isApocope: Boolean): String {
        if (number == NUM_ONE) {
            return when {
                isApocope -> "un"
                gender == GrammaticalGender.FEMININE -> "una"
                else -> "uno"
            }
        }
        return UNITS_TO_FIFTEEN[number]
    }

    private fun resolveFusedTens(
        number: Int,
        gender: GrammaticalGender,
        isApocope: Boolean
    ): String {
        if (number == NUM_TWENTY_ONE) {
            return when {
                isApocope -> "veintiún"
                gender == GrammaticalGender.FEMININE -> "veintiuna"
                else -> "veintiuno"
            }
        }
        val index = number - NUM_SIXTEEN
        return FUSED_TENS[index]
    }

    private fun resolveSeparatedTens(
        number: Int,
        gender: GrammaticalGender,
        isApocope: Boolean
    ): String {
        val tensDigit = number / TEN
        val unitDigit = number % TEN
        val tensWord = TENS_PREFIXES[tensDigit]

        return if (unitDigit == 0) {
            tensWord
        } else {
            val unitWord = resolveUnit(unitDigit, gender, isApocope)
            "$tensWord y $unitWord"
        }
    }
}
