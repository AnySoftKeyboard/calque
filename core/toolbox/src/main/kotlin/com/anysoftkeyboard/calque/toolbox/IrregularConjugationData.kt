package com.anysoftkeyboard.calque.toolbox

/** Irregular conjugation data for common Spanish verbs. */
internal object IrregularConjugationData {

    val IRREGULARS: Map<String, Map<ReadingTense, Map<SubjectPronoun, String>>> =
        mapOf(
            "ser" to
                mapOf(
                    ReadingTense.PRESENT to
                        mapOf(
                            SubjectPronoun.YO to "soy",
                            SubjectPronoun.TU to "eres",
                            SubjectPronoun.EL_ELLA_USTED to "es",
                            SubjectPronoun.NOSOTROS to "somos",
                            SubjectPronoun.ELLOS_USTEDES to "son",
                        ),
                    ReadingTense.PAST_COMPLETED to
                        mapOf(
                            SubjectPronoun.YO to "fui",
                            SubjectPronoun.TU to "fuiste",
                            SubjectPronoun.EL_ELLA_USTED to "fue",
                            SubjectPronoun.NOSOTROS to "fuimos",
                            SubjectPronoun.ELLOS_USTEDES to "fueron",
                        ),
                    ReadingTense.PAST_ONGOING to
                        mapOf(
                            SubjectPronoun.YO to "era",
                            SubjectPronoun.TU to "eras",
                            SubjectPronoun.EL_ELLA_USTED to "era",
                            SubjectPronoun.NOSOTROS to "éramos",
                            SubjectPronoun.ELLOS_USTEDES to "eran",
                        ),
                ),
            "estar" to
                mapOf(
                    ReadingTense.PRESENT to
                        mapOf(
                            SubjectPronoun.YO to "estoy",
                            SubjectPronoun.TU to "estás",
                            SubjectPronoun.EL_ELLA_USTED to "está",
                            SubjectPronoun.NOSOTROS to "estamos",
                            SubjectPronoun.ELLOS_USTEDES to "están",
                        ),
                    ReadingTense.PAST_COMPLETED to
                        mapOf(
                            SubjectPronoun.YO to "estuve",
                            SubjectPronoun.TU to "estuviste",
                            SubjectPronoun.EL_ELLA_USTED to "estuvo",
                            SubjectPronoun.NOSOTROS to "estuvimos",
                            SubjectPronoun.ELLOS_USTEDES to "estuvieron",
                        ),
                    ReadingTense.PAST_ONGOING to
                        mapOf(
                            SubjectPronoun.YO to "estaba",
                            SubjectPronoun.TU to "estabas",
                            SubjectPronoun.EL_ELLA_USTED to "estaba",
                            SubjectPronoun.NOSOTROS to "estábamos",
                            SubjectPronoun.ELLOS_USTEDES to "estaban",
                        ),
                ),
            "ir" to
                mapOf(
                    ReadingTense.PRESENT to
                        mapOf(
                            SubjectPronoun.YO to "voy",
                            SubjectPronoun.TU to "vas",
                            SubjectPronoun.EL_ELLA_USTED to "va",
                            SubjectPronoun.NOSOTROS to "vamos",
                            SubjectPronoun.ELLOS_USTEDES to "van",
                        ),
                    ReadingTense.PAST_COMPLETED to
                        mapOf(
                            SubjectPronoun.YO to "fui",
                            SubjectPronoun.TU to "fuiste",
                            SubjectPronoun.EL_ELLA_USTED to "fue",
                            SubjectPronoun.NOSOTROS to "fuimos",
                            SubjectPronoun.ELLOS_USTEDES to "fueron",
                        ),
                    ReadingTense.PAST_ONGOING to
                        mapOf(
                            SubjectPronoun.YO to "iba",
                            SubjectPronoun.TU to "ibas",
                            SubjectPronoun.EL_ELLA_USTED to "iba",
                            SubjectPronoun.NOSOTROS to "íbamos",
                            SubjectPronoun.ELLOS_USTEDES to "iban",
                        ),
                ),
            "tener" to
                mapOf(
                    ReadingTense.PRESENT to
                        mapOf(
                            SubjectPronoun.YO to "tengo",
                            SubjectPronoun.TU to "tienes",
                            SubjectPronoun.EL_ELLA_USTED to "tiene",
                            SubjectPronoun.NOSOTROS to "tenemos",
                            SubjectPronoun.ELLOS_USTEDES to "tienen",
                        ),
                    ReadingTense.PAST_COMPLETED to
                        mapOf(
                            SubjectPronoun.YO to "tuve",
                            SubjectPronoun.TU to "tuviste",
                            SubjectPronoun.EL_ELLA_USTED to "tuvo",
                            SubjectPronoun.NOSOTROS to "tuvimos",
                            SubjectPronoun.ELLOS_USTEDES to "tuvieron",
                        ),
                    ReadingTense.PAST_ONGOING to
                        mapOf(
                            SubjectPronoun.YO to "tenía",
                            SubjectPronoun.TU to "tenías",
                            SubjectPronoun.EL_ELLA_USTED to "tenía",
                            SubjectPronoun.NOSOTROS to "teníamos",
                            SubjectPronoun.ELLOS_USTEDES to "tenían",
                        ),
                ),
            "hacer" to
                mapOf(
                    ReadingTense.PRESENT to
                        mapOf(
                            SubjectPronoun.YO to "hago",
                            SubjectPronoun.TU to "haces",
                            SubjectPronoun.EL_ELLA_USTED to "hace",
                            SubjectPronoun.NOSOTROS to "hacemos",
                            SubjectPronoun.ELLOS_USTEDES to "hacen",
                        ),
                    ReadingTense.PAST_COMPLETED to
                        mapOf(
                            SubjectPronoun.YO to "hice",
                            SubjectPronoun.TU to "hiciste",
                            SubjectPronoun.EL_ELLA_USTED to "hizo",
                            SubjectPronoun.NOSOTROS to "hicimos",
                            SubjectPronoun.ELLOS_USTEDES to "hicieron",
                        ),
                    ReadingTense.PAST_ONGOING to
                        mapOf(
                            SubjectPronoun.YO to "hacía",
                            SubjectPronoun.TU to "hacías",
                            SubjectPronoun.EL_ELLA_USTED to "hacía",
                            SubjectPronoun.NOSOTROS to "hacíamos",
                            SubjectPronoun.ELLOS_USTEDES to "hacían",
                        ),
                ),
            "poder" to
                mapOf(
                    ReadingTense.PRESENT to
                        mapOf(
                            SubjectPronoun.YO to "puedo",
                            SubjectPronoun.TU to "puedes",
                            SubjectPronoun.EL_ELLA_USTED to "puede",
                            SubjectPronoun.NOSOTROS to "podemos",
                            SubjectPronoun.ELLOS_USTEDES to "pueden",
                        ),
                    ReadingTense.PAST_COMPLETED to
                        mapOf(
                            SubjectPronoun.YO to "pude",
                            SubjectPronoun.TU to "pudiste",
                            SubjectPronoun.EL_ELLA_USTED to "pudo",
                            SubjectPronoun.NOSOTROS to "pudimos",
                            SubjectPronoun.ELLOS_USTEDES to "pudieron",
                        ),
                    ReadingTense.PAST_ONGOING to
                        mapOf(
                            SubjectPronoun.YO to "podía",
                            SubjectPronoun.TU to "podías",
                            SubjectPronoun.EL_ELLA_USTED to "podía",
                            SubjectPronoun.NOSOTROS to "podíamos",
                            SubjectPronoun.ELLOS_USTEDES to "podían",
                        ),
                ),
            "querer" to
                mapOf(
                    ReadingTense.PRESENT to
                        mapOf(
                            SubjectPronoun.YO to "quiero",
                            SubjectPronoun.TU to "quieres",
                            SubjectPronoun.EL_ELLA_USTED to "quiere",
                            SubjectPronoun.NOSOTROS to "queremos",
                            SubjectPronoun.ELLOS_USTEDES to "quieren",
                        ),
                    ReadingTense.PAST_COMPLETED to
                        mapOf(
                            SubjectPronoun.YO to "quise",
                            SubjectPronoun.TU to "quisiste",
                            SubjectPronoun.EL_ELLA_USTED to "quiso",
                            SubjectPronoun.NOSOTROS to "quisimos",
                            SubjectPronoun.ELLOS_USTEDES to "quisieron",
                        ),
                    ReadingTense.PAST_ONGOING to
                        mapOf(
                            SubjectPronoun.YO to "quería",
                            SubjectPronoun.TU to "querías",
                            SubjectPronoun.EL_ELLA_USTED to "quería",
                            SubjectPronoun.NOSOTROS to "queríamos",
                            SubjectPronoun.ELLOS_USTEDES to "querían",
                        ),
                ),
            "ver" to
                mapOf(
                    ReadingTense.PRESENT to
                        mapOf(
                            SubjectPronoun.YO to "veo",
                            SubjectPronoun.TU to "ves",
                            SubjectPronoun.EL_ELLA_USTED to "ve",
                            SubjectPronoun.NOSOTROS to "vemos",
                            SubjectPronoun.ELLOS_USTEDES to "ven",
                        ),
                    ReadingTense.PAST_COMPLETED to
                        mapOf(
                            SubjectPronoun.YO to "vi",
                            SubjectPronoun.TU to "viste",
                            SubjectPronoun.EL_ELLA_USTED to "vio",
                            SubjectPronoun.NOSOTROS to "vimos",
                            SubjectPronoun.ELLOS_USTEDES to "vieron",
                        ),
                    ReadingTense.PAST_ONGOING to
                        mapOf(
                            SubjectPronoun.YO to "veía",
                            SubjectPronoun.TU to "veías",
                            SubjectPronoun.EL_ELLA_USTED to "veía",
                            SubjectPronoun.NOSOTROS to "veíamos",
                            SubjectPronoun.ELLOS_USTEDES to "veían",
                        ),
                ),
            "saber" to
                mapOf(
                    ReadingTense.PRESENT to
                        mapOf(
                            SubjectPronoun.YO to "sé",
                            SubjectPronoun.TU to "sabes",
                            SubjectPronoun.EL_ELLA_USTED to "sabe",
                            SubjectPronoun.NOSOTROS to "sabemos",
                            SubjectPronoun.ELLOS_USTEDES to "saben",
                        ),
                    ReadingTense.PAST_COMPLETED to
                        mapOf(
                            SubjectPronoun.YO to "supe",
                            SubjectPronoun.TU to "supiste",
                            SubjectPronoun.EL_ELLA_USTED to "supo",
                            SubjectPronoun.NOSOTROS to "supimos",
                            SubjectPronoun.ELLOS_USTEDES to "supieron",
                        ),
                    ReadingTense.PAST_ONGOING to
                        mapOf(
                            SubjectPronoun.YO to "sabía",
                            SubjectPronoun.TU to "sabías",
                            SubjectPronoun.EL_ELLA_USTED to "sabía",
                            SubjectPronoun.NOSOTROS to "sabíamos",
                            SubjectPronoun.ELLOS_USTEDES to "sabían",
                        ),
                ),
            "decir" to
                mapOf(
                    ReadingTense.PRESENT to
                        mapOf(
                            SubjectPronoun.YO to "digo",
                            SubjectPronoun.TU to "dices",
                            SubjectPronoun.EL_ELLA_USTED to "dice",
                            SubjectPronoun.NOSOTROS to "decimos",
                            SubjectPronoun.ELLOS_USTEDES to "dicen",
                        ),
                    ReadingTense.PAST_COMPLETED to
                        mapOf(
                            SubjectPronoun.YO to "dije",
                            SubjectPronoun.TU to "dijiste",
                            SubjectPronoun.EL_ELLA_USTED to "dijo",
                            SubjectPronoun.NOSOTROS to "dijimos",
                            SubjectPronoun.ELLOS_USTEDES to "dijeron",
                        ),
                    ReadingTense.PAST_ONGOING to
                        mapOf(
                            SubjectPronoun.YO to "decía",
                            SubjectPronoun.TU to "decías",
                            SubjectPronoun.EL_ELLA_USTED to "decía",
                            SubjectPronoun.NOSOTROS to "decíamos",
                            SubjectPronoun.ELLOS_USTEDES to "decían",
                        ),
                ),
            "dar" to
                mapOf(
                    ReadingTense.PRESENT to
                        mapOf(
                            SubjectPronoun.YO to "doy",
                            SubjectPronoun.TU to "das",
                            SubjectPronoun.EL_ELLA_USTED to "da",
                            SubjectPronoun.NOSOTROS to "damos",
                            SubjectPronoun.ELLOS_USTEDES to "dan",
                        ),
                    ReadingTense.PAST_COMPLETED to
                        mapOf(
                            SubjectPronoun.YO to "di",
                            SubjectPronoun.TU to "diste",
                            SubjectPronoun.EL_ELLA_USTED to "dio",
                            SubjectPronoun.NOSOTROS to "dimos",
                            SubjectPronoun.ELLOS_USTEDES to "dieron",
                        ),
                    ReadingTense.PAST_ONGOING to
                        mapOf(
                            SubjectPronoun.YO to "daba",
                            SubjectPronoun.TU to "dabas",
                            SubjectPronoun.EL_ELLA_USTED to "daba",
                            SubjectPronoun.NOSOTROS to "dábamos",
                            SubjectPronoun.ELLOS_USTEDES to "daban",
                        ),
                ),
        )

    fun getConjugation(
        lemma: String,
        pronoun: SubjectPronoun,
        tense: ReadingTense,
    ): ConjugatedVerbForm? {
        val form = IRREGULARS[lemma]?.get(tense)?.get(pronoun) ?: return null
        return ConjugatedVerbForm(
            lemma = lemma,
            pronoun = pronoun,
            tense = tense,
            fullForm = form,
            stem = lemma,
            suffix = "",
        )
    }
}
