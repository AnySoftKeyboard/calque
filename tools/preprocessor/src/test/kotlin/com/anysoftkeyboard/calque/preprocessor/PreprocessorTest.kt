package com.anysoftkeyboard.calque.preprocessor

import com.anysoftkeyboard.calque.linguistics.CognateDetector
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreprocessorTest {

    @Test
    fun sublexReader_readsFrequenciesCorrectly() {
        val content =
            """
            Spelling	FREQcount	CD	Rank
            de	99999	100	1
            la	88888	95	2
            casa	5000	80	150
            """
                .trimIndent()

        val frequencies = SublexReader.readFrequencies(content.byteInputStream())

        assertEquals(3, frequencies.size)
        assertEquals(1, frequencies["de"])
        assertEquals(2, frequencies["la"])
        assertEquals(150, frequencies["casa"])
    }

    @Test
    fun kaikkiProcessor_parsesLemmasAndFormsWithCognateAndFreq() {
        val sublexFrequencies = mapOf("hablar" to 42, "conversación" to 110)
        val line1 =
            """{"word":"hablar","pos":"verb","senses":[{"glosses":["to speak"]}],""" +
                """"forms":[{"form":"hablo","tags":["first-person"]}]}"""
        val line2 =
            """{"word":"conversación","pos":"noun","senses":[{"glosses":["conversation"]}],""" +
                """"forms":[{"form":"conversaciones","tags":["plural"]}]}"""
        val kaikkiJson = "$line1\n$line2"

        val inMemoryDb = SeedDatabase.open()
        inMemoryDb.use { db ->
            val count =
                KaikkiProcessor.processStream(kaikkiJson.byteInputStream(), sublexFrequencies, db)

            // hablar: base + 1 form = 2 rows
            // conversación: base + 1 form = 2 rows
            assertEquals(4L, count)

            val hablarInfo = db.lookupWord("hablar")
            assertNotNull(hablarInfo)
            assertEquals(42, hablarInfo?.frequencyRank)
            assertEquals(false, hablarInfo?.isCognate)

            val conversacionInfo = db.lookupWord("conversación")
            assertNotNull(conversacionInfo)
            assertEquals(110, conversacionInfo?.frequencyRank)
            assertEquals(true, conversacionInfo?.isCognate)

            val habloInfo = db.lookupWord("hablo")
            assertNotNull(habloInfo)
            assertEquals(42, habloInfo?.frequencyRank)
            assertEquals("first-person", habloInfo?.grammar)
        }
    }

    @Test
    fun falseCognatesExpander_expandsAcrossDictionarySurfaces() {
        val inMemoryDb = SeedDatabase.open()
        inMemoryDb.use { db ->
            db.insertDictionaryBatch(
                listOf(
                    DictionaryRow(
                        surfaceForm = "carpeta",
                        lemma = "carpeta",
                        pos = "noun",
                        grammarInfo = "base",
                        englishGloss = "folder",
                        isCognate = false,
                        frequencyRank = 500,
                    ),
                    DictionaryRow(
                        surfaceForm = "carpetas",
                        lemma = "carpeta",
                        pos = "noun",
                        grammarInfo = "plural",
                        englishGloss = "folder",
                        isCognate = false,
                        frequencyRank = 500,
                    ),
                ),
            )

            val falseCognatesJson =
                """
                [
                  {
                    "lemma": "carpeta",
                    "apparent_english": "carpet",
                    "actual_english": "folder / binder",
                    "real_spanish_for_apparent": "alfombra",
                    "severity": "HIGH_RISK",
                    "disambiguation_tip": "Keep documents in a carpeta, walk on an alfombra.",
                    "example_es": "Pon el papel en la carpeta.",
                    "example_en": "Put the paper in the folder."
                  }
                ]
                """
                    .trimIndent()

            val expandedCount =
                FalseCognatesExpander.expand(falseCognatesJson.byteInputStream(), db)

            assertEquals(2, expandedCount)
        }
    }

    @Test
    fun curatedFalseCognatesSeed_hasValidFourPartCardMetadata() {
        val seedFile = File("data/seeds/false_cognates_seed.json")
        assertTrue("Expected seed file at ${seedFile.path}", seedFile.exists())

        val seeds = FileInputStream(seedFile).use { FalseCognatesExpander.parseSeeds(it) }
        assertEquals(25, seeds.size)

        val lemmas = seeds.map { it.lemma }.toSet()
        assertEquals(seeds.size, lemmas.size)
        val coreLemmas = setOf("éxito", "carpeta", "embarazada", "soportar", "realizar")
        assertTrue(lemmas.containsAll(coreLemmas))

        val validSeverities = setOf("HIGH_RISK", "MILD_NUANCE")
        assertTrue(seeds.any { it.severity == "HIGH_RISK" })
        assertTrue(seeds.any { it.severity == "MILD_NUANCE" })

        for (seed in seeds) {
            assertFalse(seed.lemma.isBlank())
            assertFalse(seed.apparentEnglish.isBlank())
            assertFalse(seed.actualEnglish.isBlank())
            assertFalse(seed.realSpanishForApparent.isBlank())
            assertTrue(seed.severity in validSeverities)
            assertFalse(seed.disambiguationTip.isBlank())
            assertFalse(seed.exampleEs.isBlank())
            assertFalse(seed.exampleEn.isBlank())
        }
    }

    @Test
    fun curatedFalseCognatesSeed_expandsInflectionsIntoDatabase() {
        val seedFile = File("data/seeds/false_cognates_seed.json")
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatch(
                listOf(
                    DictionaryRow(
                        "soporta",
                        "soportar",
                        "verb",
                        "3rd sg. present",
                        "tolerates",
                        false,
                        400,
                    ),
                    DictionaryRow("carpetas", "carpeta", "noun", "plural", "folders", false, 500),
                ),
            )

            val expandedCount =
                FileInputStream(seedFile).use { stream -> FalseCognatesExpander.expand(stream, db) }
            // 25 base lemmas + 2 inflected dictionary forms ("soporta", "carpetas") = 27 rows
            assertEquals(27, expandedCount)

            val storedRows = db.queryFalseCognates()
            assertEquals(27, storedRows.size)
            val surfaceMap = storedRows.associateBy { it.surfaceForm }
            assertEquals("soportar", surfaceMap["soporta"]?.lemma)
            assertEquals("carpeta", surfaceMap["carpetas"]?.lemma)
            assertEquals("HIGH_RISK", surfaceMap["éxito"]?.severity)
            assertEquals("MILD_NUANCE", surfaceMap["compromiso"]?.severity)
        }
    }

    @Test
    fun tatoebaProcessor_scoresAndBucketsSentences() {
        val inMemoryDb = SeedDatabase.open()
        inMemoryDb.use { db ->
            db.insertDictionaryBatch(
                listOf(
                    DictionaryRow("la", "la", "article", "base", "the", false, 2),
                    DictionaryRow("música", "música", "noun", "base", "music", true, 200),
                    DictionaryRow("es", "ser", "verb", "present", "is", false, 3),
                    DictionaryRow(
                        "fantástica",
                        "fantástico",
                        "adj",
                        "base",
                        "fantastic",
                        true,
                        300,
                    ),
                ),
            )

            val tatoebaContent =
                """
                La música es fantástica.	Music is fantastic.	Cognate Harmony	Adjective Agreement
                Hola.	Hello.
                """
                    .trimIndent()

            val lessonsCount =
                TatoebaProcessor.processSentences(tatoebaContent.byteInputStream(), db)

            assertEquals(1, lessonsCount)
        }
    }

    @Test
    fun curatedLessonsSeed_isValidAndPopulatesProgressionLevels() {
        val seedFile = File("data/seeds/curated_lessons_seed.json")
        assertTrue("Expected seed file at ${seedFile.path}", seedFile.exists())

        SeedDatabase.open().use { db ->
            val insertedCount =
                FileInputStream(seedFile).use { stream ->
                    CuratedLessonsProcessor.processLessons(stream, db)
                }
            assertEquals(8, insertedCount)

            val lessons = db.queryCuratedLessons()
            assertEquals(8, lessons.size)
            assertEquals(setOf(0, 1, 2), lessons.map { it.level }.toSet())

            val sentenceZeroLesson = lessons.first { it.lessonId == "lvl0_00_cognate_unlock" }
            val sentenceZeroItems =
                CuratedLessonsProcessor.parseLessonSentences(sentenceZeroLesson.contentJson)
            assertEquals(
                "El hotel moderno es excelente.",
                sentenceZeroItems.first().sentenceEs,
            )

            for (lesson in lessons) {
                assertFalse(lesson.title.isBlank())
                assertFalse(lesson.targetConcept.isBlank())
                val sentences = CuratedLessonsProcessor.parseLessonSentences(lesson.contentJson)
                assertTrue(sentences.isNotEmpty())
                for (sentence in sentences) {
                    assertFalse(sentence.sentenceEs.isBlank())
                    assertFalse(sentence.translationEn.isBlank())
                    assertTrue(sentence.interlinearGloss.isNotEmpty())
                    assertFalse(sentence.grammarCallout.isNullOrBlank())
                }
            }
        }
    }

    @Test
    fun cognateRulesProcessor_insertsRules() {
        val inMemoryDb = SeedDatabase.open()
        inMemoryDb.use { db ->
            val rulesJson =
                """
                [
                  {
                    "rule_id": "tion_to_cion",
                    "english_suffix": "-tion",
                    "spanish_suffix": "-ción",
                    "gender_rule": "Always feminine (la)",
                    "spelling_note": "Drop double consonants",
                    "examples": ["action -> acción", "nation -> nación"]
                  }
                ]
                """
                    .trimIndent()

            val rulesCount = CognateRulesProcessor.processRules(rulesJson.byteInputStream(), db)
            assertEquals(1, rulesCount)

            val storedRules = db.queryCognateRules()
            assertEquals(1, storedRules.size)
            assertEquals("tion_to_cion", storedRules[0].ruleId)
            val examples = CognateRulesProcessor.parseExamples(storedRules[0].examplesJson)
            assertEquals(2, examples.size)
            assertEquals(CognateRuleExample("action", "acción"), examples[0])
        }
    }

    @Test
    fun curatedCognateRulesSeed_isValidAndAlignedWithCognateDetector() {
        val seedFile = File("data/seeds/cognate_rules_seed.json")
        assertTrue(
            "Expected seed file at ${seedFile.path}",
            seedFile.exists(),
        )

        SeedDatabase.open().use { db ->
            val insertedCount =
                FileInputStream(seedFile).use { stream ->
                    CognateRulesProcessor.processRules(stream, db)
                }
            assertEquals(12, insertedCount)

            val rules = db.queryCognateRules()
            assertEquals(12, rules.size)

            val ruleIds = rules.map { it.ruleId }.toSet()
            val expectedIds =
                setOf(
                    "tion_to_cion",
                    "sion_to_sion",
                    "ty_to_dad",
                    "ly_to_mente",
                    "ous_to_oso",
                    "ic_to_ico",
                    "al_to_al",
                    "ct_to_cto",
                    "nce_to_ncia",
                    "ary_to_ario",
                    "ive_to_ivo",
                    "ble_to_ble",
                )
            assertEquals(expectedIds, ruleIds)

            val adverbRule = rules.first { it.ruleId == "ly_to_mente" }
            assertNull(adverbRule.genderRule)

            val nounRule = rules.first { it.ruleId == "tion_to_cion" }
            assertEquals("Always feminine (la)", nounRule.genderRule)

            for (rule in rules) {
                assertTrue(rule.englishSuffix.startsWith("-"))
                assertTrue(rule.spanishSuffix.startsWith("-"))
                assertFalse(rule.spellingNote.isBlank())

                val examples = CognateRulesProcessor.parseExamples(rule.examplesJson)
                assertTrue(
                    "Rule ${rule.ruleId} should have 5..10 examples, found ${examples.size}",
                    examples.size in 5..10,
                )

                for (example in examples) {
                    assertTrue(
                        "Expected (${example.spanish}, ${example.english}) in ${rule.ruleId} to match CognateDetector",
                        CognateDetector.matchesCognate(example.spanish, example.english),
                    )
                }
            }
        }
    }

    @Test
    fun dictionarySeedProcessor_parsesAndInsertsEntries() {
        val inMemoryDb = SeedDatabase.open()
        inMemoryDb.use { db ->
            val sampleJson =
                """
                [
                  {
                    "surface_form": "hotel",
                    "lemma": "hotel",
                    "pos": "noun",
                    "grammar_info": "masc. sing.",
                    "english_gloss": "hotel",
                    "is_cognate": true,
                    "frequency_rank": 500
                  }
                ]
                """
                    .trimIndent()

            val count =
                DictionarySeedProcessor.processDictionarySeed(sampleJson.byteInputStream(), db)
            assertEquals(1, count)

            val hotel = db.lookupWord("hotel")
            assertNotNull(hotel)
            assertTrue(hotel?.isCognate == true)
            assertEquals(500, hotel?.frequencyRank)
        }
    }

    @Test
    fun curatedDictionarySeed_isValidAndPopulatesDatabase() {
        val seedFile = File("data/seeds/dictionary_seed.json")
        assertTrue("Expected seed file at ${seedFile.path}", seedFile.exists())

        SeedDatabase.open().use { db ->
            val insertedCount =
                FileInputStream(seedFile).use { stream ->
                    DictionarySeedProcessor.processDictionarySeed(stream, db)
                }
            assertTrue("Expected >= 100 entries, found $insertedCount", insertedCount >= 100)

            val hotelInfo = db.lookupWord("hotel")
            assertNotNull(hotelInfo)
            assertTrue(hotelInfo?.isCognate == true)

            val esInfo = db.lookupWord("es")
            assertNotNull(esInfo)
            assertEquals("3rd sing. present", esInfo?.grammar)
        }
    }

    @Test
    fun pipeline_executesEndToEndSuccessfully() {
        val tempDir = createTempDirectory("calque_pipeline_test").toFile()
        val dbFile = File(tempDir, "seed.db")

        val sublexFile = File(tempDir, "sublex.tsv")
        sublexFile.writeText("hablar\t10\ncarpeta\t200\n", StandardCharsets.UTF_8)

        val sampleForm = """{"form":"carpetas","tags":["plural"]}"""
        val sampleKaikki =
            """{"word":"carpeta","pos":"noun","senses":[{"glosses":["folder"]}],"forms":[$sampleForm]}"""
        val kaikkiFile = File(tempDir, "kaikki.jsonl")
        kaikkiFile.writeText("$sampleKaikki\n", StandardCharsets.UTF_8)

        val dictionarySeedFile = File("data/seeds/dictionary_seed.json")
        val falseCognatesSeedFile = File("data/seeds/false_cognates_seed.json")
        val curatedLessonsSeedFile = File("data/seeds/curated_lessons_seed.json")
        val cognateRulesSeedFile = File("data/seeds/cognate_rules_seed.json")

        val config =
            PipelineConfig(
                dbFile = dbFile,
                kaikkiFile = kaikkiFile,
                sublexFile = sublexFile,
                dictionarySeedFile = dictionarySeedFile,
                falseCognatesFile = falseCognatesSeedFile,
                curatedLessonsFile = curatedLessonsSeedFile,
                cognateRulesFile = cognateRulesSeedFile,
            )

        val pipeline = PreprocessorPipeline(config)
        val summary = pipeline.execute()

        assertTrue(dbFile.exists())
        assertTrue(summary.dictionaryRows >= 100)
        // 25 base lemmas + "carpetas" plural from Kaikki/dictionary = at least 26 rows
        assertTrue(summary.falseCognateRows >= 26)
        assertEquals(8, summary.curatedLessonRows)
        assertEquals(12, summary.cognateRuleRows)

        tempDir.deleteRecursively()
    }
}
