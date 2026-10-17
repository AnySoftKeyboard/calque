package com.anysoftkeyboard.calque.toolbox

import com.anysoftkeyboard.calque.linguistics.GrammaticalGender
import com.anysoftkeyboard.calque.linguistics.NumberConverter

/** Domain engine for the Number Odometer (Counting, Dates & Time Sandbox). */
object NumberOdometerEngine {

    private const val MAX_SUPPORTED_NUMBER = 999_999L
    private const val MAX_HOUR = 23
    private const val MAX_MINUTE = 59
    private const val MAX_MONTH = 12
    private const val NOON = 12
    private const val HALF_HOUR = 30
    private const val QUARTER = 15
    private const val THREE_QUARTERS = 45
    private const val MINUTES_PER_HOUR = 60

    private const val EARLY_MORNING_END = 5
    private const val MORNING_END = 11
    private const val AFTERNOON_END = 19

    private val MONTH_NAMES =
        listOf(
            "",
            "enero",
            "febrero",
            "marzo",
            "abril",
            "mayo",
            "junio",
            "julio",
            "agosto",
            "septiembre",
            "octubre",
            "noviembre",
            "diciembre",
        )

    private val HOUR_WORDS =
        listOf(
            "",
            "una",
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
        )

    /** Converts [number] (0..999,999) to written Spanish with masculine and feminine agreement. */
    fun formatNumberWithAgreement(number: Long): NumberAgreementResult {
        val clamped = number.coerceIn(0L, MAX_SUPPORTED_NUMBER).toInt()
        val masc = NumberConverter.convert(clamped, GrammaticalGender.MASCULINE, isApocope = true)
        val fem = NumberConverter.convert(clamped, GrammaticalGender.FEMININE, isApocope = false)
        return NumberAgreementResult(
            number = clamped.toLong(),
            masculineText = masc,
            feminineText = fem,
        )
    }

    /** Formats [hour] (0..23) and [minute] (0..59) into conversational Spanish clock time. */
    fun formatClockTime(hour: Int, minute: Int): ClockTimeResult {
        val normHour = hour.coerceIn(0, MAX_HOUR)
        val normMin = minute.coerceIn(0, MAX_MINUTE)
        val period = getPeriodOfDay(normHour)

        if (normHour == NOON && normMin == 0) {
            return ClockTimeResult(normHour, normMin, "Es mediodía", period)
        }
        if (normHour == 0 && normMin == 0) {
            return ClockTimeResult(normHour, normMin, "Es medianoche", period)
        }

        val clock12Hour = if (normHour % NOON == 0) NOON else normHour % NOON
        val phrase = buildTimePhrase(clock12Hour, normMin)
        return ClockTimeResult(normHour, normMin, "$phrase $period", period)
    }

    /** Formats a calendar date [day] (1..31), [month] (1..12), and optional [year]. */
    fun formatDate(day: Int, month: Int, year: Int? = null): DatePhraseResult {
        val validMonth = month.coerceIn(1, MAX_MONTH)
        val monthName = MONTH_NAMES[validMonth]
        val dayPhrase = if (day == 1) "el primero" else "el $day"

        val phrase =
            if (year != null) {
                "$dayPhrase de $monthName de $year"
            } else {
                "$dayPhrase de $monthName"
            }

        return DatePhraseResult(day, validMonth, year, phrase)
    }

    private fun hourPrefix(hour: Int): String =
        if (hour == 1) "Es la una" else "Son las ${hourToWord(hour)}"

    private fun buildTimePhrase(clockHour: Int, minute: Int): String {
        return when {
            minute == 0 -> "${hourPrefix(clockHour)} en punto"
            minute == QUARTER -> "${hourPrefix(clockHour)} y cuarto"
            minute == HALF_HOUR -> "${hourPrefix(clockHour)} y media"
            minute < HALF_HOUR -> {
                val minText = NumberConverter.convert(minute, GrammaticalGender.FEMININE)
                "${hourPrefix(clockHour)} y $minText"
            }
            minute == THREE_QUARTERS -> {
                val nextHour = if (clockHour == NOON) 1 else clockHour + 1
                "${hourPrefix(nextHour)} menos cuarto"
            }
            else -> {
                val nextHour = if (clockHour == NOON) 1 else clockHour + 1
                val diff = MINUTES_PER_HOUR - minute
                val minText = NumberConverter.convert(diff, GrammaticalGender.FEMININE)
                "${hourPrefix(nextHour)} menos $minText"
            }
        }
    }

    private fun hourToWord(hour: Int): String {
        return if (hour in 1..NOON) {
            HOUR_WORDS[hour]
        } else {
            hour.toString()
        }
    }

    private fun getPeriodOfDay(hour: Int): String {
        return when {
            hour in 1..EARLY_MORNING_END -> "de la madrugada"
            hour in (EARLY_MORNING_END + 1)..MORNING_END -> "de la mañana"
            hour in (MORNING_END + 1)..AFTERNOON_END -> "de la tarde"
            else -> "de la noche"
        }
    }
}
