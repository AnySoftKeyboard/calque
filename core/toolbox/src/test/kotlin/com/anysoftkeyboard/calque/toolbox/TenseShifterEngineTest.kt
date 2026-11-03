package com.anysoftkeyboard.calque.toolbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TenseShifterEngineTest {

    @Test
    fun conjugateRegularAr_present() {
        assertEquals(
            "hablo",
            TenseShifterEngine.conjugate("hablar", SubjectPronoun.YO, ReadingTense.PRESENT).fullForm
        )
        assertEquals(
            "hablas",
            TenseShifterEngine.conjugate("hablar", SubjectPronoun.TU, ReadingTense.PRESENT).fullForm
        )
        assertEquals(
            "habla",
            TenseShifterEngine.conjugate(
                    "hablar",
                    SubjectPronoun.EL_ELLA_USTED,
                    ReadingTense.PRESENT
                )
                .fullForm
        )
        assertEquals(
            "hablamos",
            TenseShifterEngine.conjugate("hablar", SubjectPronoun.NOSOTROS, ReadingTense.PRESENT)
                .fullForm
        )
        assertEquals(
            "hablan",
            TenseShifterEngine.conjugate(
                    "hablar",
                    SubjectPronoun.ELLOS_USTEDES,
                    ReadingTense.PRESENT
                )
                .fullForm
        )
    }

    @Test
    fun conjugateRegularAr_pastAndFuture() {
        // Preterite
        assertEquals(
            "hablé",
            TenseShifterEngine.conjugate("hablar", SubjectPronoun.YO, ReadingTense.PAST_COMPLETED)
                .fullForm
        )
        assertEquals(
            "habló",
            TenseShifterEngine.conjugate(
                    "hablar",
                    SubjectPronoun.EL_ELLA_USTED,
                    ReadingTense.PAST_COMPLETED
                )
                .fullForm
        )

        // Imperfect
        assertEquals(
            "hablaba",
            TenseShifterEngine.conjugate("hablar", SubjectPronoun.YO, ReadingTense.PAST_ONGOING)
                .fullForm
        )
        assertEquals(
            "hablábamos",
            TenseShifterEngine.conjugate(
                    "hablar",
                    SubjectPronoun.NOSOTROS,
                    ReadingTense.PAST_ONGOING
                )
                .fullForm
        )

        // Near Future
        assertEquals(
            "voy a hablar",
            TenseShifterEngine.conjugate("hablar", SubjectPronoun.YO, ReadingTense.NEAR_FUTURE)
                .fullForm
        )
        assertEquals(
            "vamos a hablar",
            TenseShifterEngine.conjugate(
                    "hablar",
                    SubjectPronoun.NOSOTROS,
                    ReadingTense.NEAR_FUTURE
                )
                .fullForm
        )
    }

    @Test
    fun conjugateRegularEr_comer() {
        assertEquals(
            "como",
            TenseShifterEngine.conjugate("comer", SubjectPronoun.YO, ReadingTense.PRESENT).fullForm
        )
        assertEquals(
            "comió",
            TenseShifterEngine.conjugate(
                    "comer",
                    SubjectPronoun.EL_ELLA_USTED,
                    ReadingTense.PAST_COMPLETED
                )
                .fullForm
        )
        assertEquals(
            "comía",
            TenseShifterEngine.conjugate("comer", SubjectPronoun.YO, ReadingTense.PAST_ONGOING)
                .fullForm
        )
        assertEquals(
            "va a comer",
            TenseShifterEngine.conjugate(
                    "comer",
                    SubjectPronoun.EL_ELLA_USTED,
                    ReadingTense.NEAR_FUTURE
                )
                .fullForm
        )
    }

    @Test
    fun conjugateRegularIr_vivir() {
        assertEquals(
            "vivo",
            TenseShifterEngine.conjugate("vivir", SubjectPronoun.YO, ReadingTense.PRESENT).fullForm
        )
        assertEquals(
            "vivieron",
            TenseShifterEngine.conjugate(
                    "vivir",
                    SubjectPronoun.ELLOS_USTEDES,
                    ReadingTense.PAST_COMPLETED
                )
                .fullForm
        )
        assertEquals(
            "vivíamos",
            TenseShifterEngine.conjugate(
                    "vivir",
                    SubjectPronoun.NOSOTROS,
                    ReadingTense.PAST_ONGOING
                )
                .fullForm
        )
        assertEquals(
            "van a vivir",
            TenseShifterEngine.conjugate(
                    "vivir",
                    SubjectPronoun.ELLOS_USTEDES,
                    ReadingTense.NEAR_FUTURE
                )
                .fullForm
        )
    }

    @Test
    fun conjugateIrregular_serAndEstar() {
        // ser
        assertEquals(
            "soy",
            TenseShifterEngine.conjugate("ser", SubjectPronoun.YO, ReadingTense.PRESENT).fullForm
        )
        assertEquals(
            "fui",
            TenseShifterEngine.conjugate("ser", SubjectPronoun.YO, ReadingTense.PAST_COMPLETED)
                .fullForm
        )
        assertEquals(
            "era",
            TenseShifterEngine.conjugate("ser", SubjectPronoun.YO, ReadingTense.PAST_ONGOING)
                .fullForm
        )

        // estar
        assertEquals(
            "estoy",
            TenseShifterEngine.conjugate("estar", SubjectPronoun.YO, ReadingTense.PRESENT).fullForm
        )
        assertEquals(
            "estuve",
            TenseShifterEngine.conjugate("estar", SubjectPronoun.YO, ReadingTense.PAST_COMPLETED)
                .fullForm
        )
        assertEquals(
            "estaba",
            TenseShifterEngine.conjugate("estar", SubjectPronoun.YO, ReadingTense.PAST_ONGOING)
                .fullForm
        )
    }

    @Test
    fun conjugateIrregular_irAndTener() {
        // ir
        assertEquals(
            "voy",
            TenseShifterEngine.conjugate("ir", SubjectPronoun.YO, ReadingTense.PRESENT).fullForm
        )
        assertEquals(
            "fueron",
            TenseShifterEngine.conjugate(
                    "ir",
                    SubjectPronoun.ELLOS_USTEDES,
                    ReadingTense.PAST_COMPLETED
                )
                .fullForm
        )
        assertEquals(
            "iba",
            TenseShifterEngine.conjugate("ir", SubjectPronoun.YO, ReadingTense.PAST_ONGOING)
                .fullForm
        )

        // tener
        assertEquals(
            "tengo",
            TenseShifterEngine.conjugate("tener", SubjectPronoun.YO, ReadingTense.PRESENT).fullForm
        )
        assertEquals(
            "tuvo",
            TenseShifterEngine.conjugate(
                    "tener",
                    SubjectPronoun.EL_ELLA_USTED,
                    ReadingTense.PAST_COMPLETED
                )
                .fullForm
        )
        assertEquals(
            "tenían",
            TenseShifterEngine.conjugate(
                    "tener",
                    SubjectPronoun.ELLOS_USTEDES,
                    ReadingTense.PAST_ONGOING
                )
                .fullForm
        )
    }

    @Test
    fun shiftSentence_replacesVerbCorrectly() {
        val result =
            TenseShifterEngine.shiftSentence(
                sentence = "Yo hablo español.",
                lemma = "hablar",
                pronoun = SubjectPronoun.YO,
                tense = ReadingTense.PAST_COMPLETED,
            )

        assertEquals("hablé", result.newVerb)
        assertEquals("Yo hablé español.", result.shiftedSentence)
    }

    @Test
    fun supportedVerbs_includesCommonIrregulars() {
        val verbs = TenseShifterEngine.supportedVerbs()
        assertTrue(verbs.contains("hablar"))
        assertTrue(verbs.contains("ser"))
        assertTrue(verbs.contains("estar"))
        assertTrue(verbs.contains("tener"))
        assertTrue(verbs.contains("hacer"))
    }
}
