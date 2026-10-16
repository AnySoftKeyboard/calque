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
                        300
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

        val fcFile = File(tempDir, "false_cognates.json")
        fcFile.writeText(
            """
            [
              {
                "lemma": "carpeta",
                "apparent_english": "carpet",
                "actual_english": "folder",
                "real_spanish_for_apparent": "alfombra",
                "severity": "HIGH_RISK",
                "disambiguation_tip": "A folder, not a carpet.",
                "example_es": "Mi carpeta.",
                "example_en": "My folder."
              }
            ]
            """
                .trimIndent(),
            StandardCharsets.UTF_8,
        )

        val cognateRulesSeedFile = File("data/seeds/cognate_rules_seed.json")

        val config =
            PipelineConfig(
                dbFile = dbFile,
                kaikkiFile = kaikkiFile,
                sublexFile = sublexFile,
                falseCognatesFile = fcFile,
                cognateRulesFile = cognateRulesSeedFile,
            )

        val pipeline = PreprocessorPipeline(config)
        val summary = pipeline.execute()

        assertTrue(dbFile.exists())
        assertTrue(summary.dictionaryRows >= 2)
        assertEquals(2, summary.falseCognateRows)
        assertEquals(12, summary.cognateRuleRows)

        tempDir.deleteRecursively()
    }
}
