package com.anysoftkeyboard.calque.toolbox

/** Conjugation lookup tables and regular rule inflector for Spanish reading tenses. */
object VerbConjugationTables {

    private const val MIN_LEMMA_LENGTH = 3

    /** Near future auxiliary form for "ir a + [lemma]" by subject pronoun. */
    fun getNearFutureAuxiliary(pronoun: SubjectPronoun): String =
        when (pronoun) {
            SubjectPronoun.YO -> "voy a"
            SubjectPronoun.TU -> "vas a"
            SubjectPronoun.EL_ELLA_USTED -> "va a"
            SubjectPronoun.NOSOTROS -> "vamos a"
            SubjectPronoun.ELLOS_USTEDES -> "van a"
        }

    /** Conjugates a regular verb lemma into the requested reading tense and subject pronoun. */
    fun conjugateRegular(
        lemma: String,
        pronoun: SubjectPronoun,
        tense: ReadingTense,
    ): ConjugatedVerbForm {
        if (tense == ReadingTense.NEAR_FUTURE) {
            val aux = getNearFutureAuxiliary(pronoun)
            return ConjugatedVerbForm(
                lemma = lemma,
                pronoun = pronoun,
                tense = tense,
                fullForm = "$aux $lemma",
                stem = lemma,
                suffix = "",
                auxiliary = aux,
            )
        }

        if (lemma.length < MIN_LEMMA_LENGTH) {
            return ConjugatedVerbForm(
                lemma = lemma,
                pronoun = pronoun,
                tense = tense,
                fullForm = lemma,
                stem = lemma,
                suffix = "",
            )
        }

        val ending = lemma.takeLast(2)
        val stem = lemma.dropLast(2)

        val suffix =
            when (tense) {
                ReadingTense.PRESENT -> getRegularPresentEnding(ending, pronoun)
                ReadingTense.PAST_COMPLETED -> getRegularPreteriteEnding(ending, pronoun)
                ReadingTense.PAST_ONGOING -> getRegularImperfectEnding(ending, pronoun)
                ReadingTense.NEAR_FUTURE -> ""
            }

        return ConjugatedVerbForm(
            lemma = lemma,
            pronoun = pronoun,
            tense = tense,
            fullForm = stem + suffix,
            stem = stem,
            suffix = suffix,
        )
    }

    private fun getArPresentEnding(pronoun: SubjectPronoun): String =
        when (pronoun) {
            SubjectPronoun.YO -> "o"
            SubjectPronoun.TU -> "as"
            SubjectPronoun.EL_ELLA_USTED -> "a"
            SubjectPronoun.NOSOTROS -> "amos"
            SubjectPronoun.ELLOS_USTEDES -> "an"
        }

    private fun getErPresentEnding(pronoun: SubjectPronoun): String =
        when (pronoun) {
            SubjectPronoun.YO -> "o"
            SubjectPronoun.TU -> "es"
            SubjectPronoun.EL_ELLA_USTED -> "e"
            SubjectPronoun.NOSOTROS -> "emos"
            SubjectPronoun.ELLOS_USTEDES -> "en"
        }

    private fun getIrPresentEnding(pronoun: SubjectPronoun): String =
        when (pronoun) {
            SubjectPronoun.YO -> "o"
            SubjectPronoun.TU -> "es"
            SubjectPronoun.EL_ELLA_USTED -> "e"
            SubjectPronoun.NOSOTROS -> "imos"
            SubjectPronoun.ELLOS_USTEDES -> "en"
        }

    private fun getRegularPresentEnding(ending: String, pronoun: SubjectPronoun): String =
        when (ending) {
            "ar" -> getArPresentEnding(pronoun)
            "er" -> getErPresentEnding(pronoun)
            else -> getIrPresentEnding(pronoun)
        }

    private fun getRegularPreteriteEnding(ending: String, pronoun: SubjectPronoun): String =
        when (ending) {
            "ar" ->
                when (pronoun) {
                    SubjectPronoun.YO -> "é"
                    SubjectPronoun.TU -> "aste"
                    SubjectPronoun.EL_ELLA_USTED -> "ó"
                    SubjectPronoun.NOSOTROS -> "amos"
                    SubjectPronoun.ELLOS_USTEDES -> "aron"
                }
            else ->
                when (pronoun) {
                    SubjectPronoun.YO -> "í"
                    SubjectPronoun.TU -> "iste"
                    SubjectPronoun.EL_ELLA_USTED -> "ió"
                    SubjectPronoun.NOSOTROS -> "imos"
                    SubjectPronoun.ELLOS_USTEDES -> "ieron"
                }
        }

    private fun getRegularImperfectEnding(ending: String, pronoun: SubjectPronoun): String =
        when (ending) {
            "ar" ->
                when (pronoun) {
                    SubjectPronoun.YO -> "aba"
                    SubjectPronoun.TU -> "abas"
                    SubjectPronoun.EL_ELLA_USTED -> "aba"
                    SubjectPronoun.NOSOTROS -> "ábamos"
                    SubjectPronoun.ELLOS_USTEDES -> "aban"
                }
            else ->
                when (pronoun) {
                    SubjectPronoun.YO -> "ía"
                    SubjectPronoun.TU -> "ías"
                    SubjectPronoun.EL_ELLA_USTED -> "ía"
                    SubjectPronoun.NOSOTROS -> "íamos"
                    SubjectPronoun.ELLOS_USTEDES -> "ían"
                }
        }
}
