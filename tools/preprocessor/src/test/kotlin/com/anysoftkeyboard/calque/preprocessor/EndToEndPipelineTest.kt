package com.anysoftkeyboard.calque.preprocessor

import com.anysoftkeyboard.calque.linguistics.SentenceScorer
import java.io.File
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.io.path.createTempDirectory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Runs the whole chain on small fixtures, the way the checked-in subsets are produced and consumed:
 * SUBTLEX counts -> rank file, Kaikki dump -> lemma subset, Tatoeba exports -> candidate pool, and
 * finally the three subsets plus the curated seeds -> `seed.db`. The database is then checked for
 * the properties Room's `createFromAsset` needs, and for being byte-identical across runs.
 */
class EndToEndPipelineTest {

    private lateinit var dir: File

    @Before
    fun createDirectory() {
        dir = createTempDirectory("calque_e2e").toFile()
    }

    @After
    fun deleteDirectory() {
        dir.deleteRecursively()
    }

    // ----- fixtures -----------------------------------------------------------------------------

    private val counts =
        listOf(
            "el" to 1000L,
            "la" to 900L,
            "es" to 800L,
            "y" to 700L,
            "son" to 650L,
            "gato" to 500L,
            "come" to 480L,
            "comen" to 470L,
            "pan" to 460L,
            "perro" to 450L,
            "casa" to 300L,
            "grande" to 250L,
            "muy" to 200L,
            "hotel" to 150L,
            "grandes" to 90L,
            "tom" to 80L,
        )

    private val kaikkiDump =
        listOf(
            lemma("el", "article", "the"),
            lemma("la", "article", "the"),
            lemma("y", "conj", "and"),
            lemma("gato", "noun", "cat"),
            lemma("perro", "noun", "dog"),
            lemma("pan", "noun", "bread"),
            lemma("hotel", "noun", "inn"),
            lemma("mucho", "adv", "much, a lot"),
            lemma("casa", "noun", "house", form("casas", "plural")),
            lemma("grande", "adj", "big", form("grandes", "plural")),
            lemma("ser", "verb", "to be", form("es", "present"), form("son", "present")),
            lemma(
                "comer",
                "verb",
                "to eat",
                form("come", "third-person", "present"),
                form("comen", "third-person", "plural"),
                form("comerlo", "combined-form", "infinitive"),
                junk("es-conj", "inflection-template"),
                junk("irregular", "table-tags"),
            ),
            lemma("zarzaparrilla", "noun", "sarsaparilla"),
            formOf("muy", "adv", "apocopic form of mucho; very", "mucho"),
            formOf("casas", "noun", "plural of casa", "casa"),
            "not json at all",
        )

    private val spanishExport =
        listOf(
            "1\tspa\tEl gato come pan.",
            "2\tspa\tLa casa es grande.",
            "3\tspa\tEl perro es grande y el gato es muy grande.",
            "4\tspa\tEl gato y el perro comen pan.",
            "5\tspa\tEl hotel es muy grande.",
            "6\tspa\tLa casa y el hotel son grandes.",
            "7\tspa\tTom come pan en la casa.", // a proper name
            "8\tspa\tEl gato come zarzaparrilla.", // a word outside the rank file
            "9\tspa\tEl perro come pan y gato.", // no translation
        )

    private val englishExport =
        listOf(
            "101\teng\tThe cat eats bread.",
            "102\teng\tThe house is big.",
            "103\teng\tThe dog is big and the cat is very big.",
            "104\teng\tThe cat and the dog eat bread.",
            "105\teng\tThe hotel is very big.",
            "106\teng\tThe house and the hotel are big.",
            "107\teng\tTom eats bread in the house.",
            "108\teng\tThe cat eats sarsaparilla.",
        )

    private val links = (1..8).map { "$it\t${100 + it}" }

    private fun form(surface: String, vararg tags: String) =
        """{"form":"$surface","tags":[${tags.joinToString(",") { "\"$it\"" }}]}"""

    private fun junk(surface: String, tag: String) =
        """{"form":"$surface","source":"conjugation","tags":["$tag"]}"""

    private fun lemma(word: String, pos: String, gloss: String, vararg forms: String) =
        """{"word":"$word","pos":"$pos","senses":[{"glosses":["$gloss"]}]""" +
            (if (forms.isEmpty()) "" else ""","forms":[${forms.joinToString(",")}]""") +
            "}"

    private fun formOf(word: String, pos: String, gloss: String, target: String) =
        """{"word":"$word","pos":"$pos","senses":[{"glosses":["$gloss"],"tags":["form-of"],""" +
            """"form_of":[{"word":"$target"}]}]}"""

    private fun write(name: String, lines: List<String>) =
        File(dir, name).apply { writeText(lines.joinToString("\n") + "\n") }

    private fun seed(name: String, json: String) = File(dir, name).apply { writeText(json) }

    // ----- the chain ----------------------------------------------------------------------------

    private class Subsets(val ranks: File, val kaikki: File, val tatoeba: File)

    private fun extractSubsets(): Subsets {
        val ranks = File(dir, "ranks.tsv")
        ranks.outputStream().use { out ->
            SublexExtractor.write(
                SublexExtractor.rank(counts.map { SublexCount(it.first, it.second) }, 100),
                out
            )
        }
        val wanted = ranks.inputStream().use { SublexReader.readFrequencies(it).keys }

        val kaikki = File(dir, "spanish_lemmas.jsonl.gz")
        KaikkiSubsetFiles.extractToFiles(
            write("kaikki.jsonl", kaikkiDump),
            wanted,
            kaikki,
            File(dir, "spanish_lemmas.meta.json"),
            mapOf("source" to "kaikki.jsonl"),
        )

        val pool = File(dir, "pool.tsv.gz")
        TatoebaCandidateFiles.extractToFiles(
            TatoebaRun(
                inputs =
                    TatoebaInputs(
                        spanish = write("spa.tsv", spanishExport),
                        english = write("eng.tsv", englishExport),
                        links = write("links.tsv", links),
                    ),
                wantedWords = wanted,
                maxPairs = 100,
                output = pool,
                metadataFile = File(dir, "pool.meta.json"),
                sourceInfo = mapOf("source" to "spa.tsv"),
            )
        )
        return Subsets(ranks, kaikki, pool)
    }

    private fun config(db: File, subsets: Subsets): PipelineConfig =
        PipelineConfig(
            dbFile = db,
            kaikkiFile = subsets.kaikki,
            sublexFile = subsets.ranks,
            tatoebaFile = subsets.tatoeba,
            dictionarySeedFile =
                seed(
                    "dictionary_seed.json",
                    """[{"surface_form":"hotel","lemma":"hotel","pos":"noun","grammar_info":"masc. sing.",""" +
                        """"english_gloss":"hotel (curated)","is_cognate":true,"frequency_rank":50}]""",
                ),
            falseCognatesFile =
                seed(
                    "false_cognates_seed.json",
                    """[{"lemma":"casa","apparent_english":"cask","actual_english":"house",""" +
                        """"real_spanish_for_apparent":"barril","severity":"MILD_NUANCE",""" +
                        """"disambiguation_tip":"tip","example_es":"La casa.","example_en":"The house."}]""",
                ),
            curatedLessonsFile =
                seed(
                    "curated_lessons_seed.json",
                    """[{"lesson_id":"lvl0_03_curated","level":0,"sequence_order":3,"title":"T",""" +
                        """"target_concept":"C","content":{"sentences":[{"sentence_es":"Hola amigo.",""" +
                        """"translation_en":"Hello friend."}]}}]""",
                ),
            cognateRulesFile =
                seed(
                    "cognate_rules_seed.json",
                    """[{"rule_id":"tion_to_cion","english_suffix":"-tion","spanish_suffix":"-ci\u00f3n",""" +
                        """"gender_rule":"Always feminine (la)","spelling_note":"note",""" +
                        """"examples":[{"english":"nation","spanish":"naci\u00f3n"}]}]""",
                ),
            tatoebaPacks = TatoebaPackConfig(packSize = 2, packsPerLevel = 5),
        )

    private fun buildDatabase(name: String, subsets: Subsets): Pair<File, PipelineSummary> {
        val db = File(dir, name)
        return db to PreprocessorPipeline(config(db, subsets)).execute()
    }

    private fun <T> File.query(sql: String, read: (ResultSet) -> T): List<T> =
        DriverManager.getConnection("jdbc:sqlite:$absolutePath").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { rs -> rs.rows(read) }
            }
        }

    private fun <T> ResultSet.rows(read: (ResultSet) -> T): List<T> {
        val rows = mutableListOf<T>()
        while (next()) rows.add(read(this))
        return rows
    }

    private fun File.scalar(sql: String): String = query(sql) { it.getString(1) }.single()

    // ----- tests --------------------------------------------------------------------------------

    @Test
    fun theExtractorsKeepTheRightWordsAndDropTheRest() {
        val subsets = extractSubsets()

        val kept =
            java.util.zip.GZIPInputStream(subsets.kaikki.inputStream()).bufferedReader().readLines()
        val words = kept.map { Regex(""""word":"([^"]+)"""").find(it)!!.groupValues[1] }

        assertTrue("zarzaparrilla is not in the rank file", "zarzaparrilla" !in words)
        assertTrue(
            "casas is listed by casa, so no alias is kept",
            words.count { it == "casas" } == 0
        )
        assertTrue("muy" in words && "mucho" in words)

        val pool =
            TatoebaCandidateFiles.readPairs(
                java.util.zip.GZIPInputStream(subsets.tatoeba.inputStream())
            )
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L), pool.map { it.spanishId })
    }

    @Test
    fun theDictionaryHoldsLemmasFormsAndAliasesAndNoJunk() {
        val (db, summary) = buildDatabase("seed.db", extractSubsets())

        assertTrue(summary.dictionaryRows > 0)
        assertEquals("casa", db.scalar("SELECT lemma FROM dictionary WHERE surface_form = 'casas'"))
        assertEquals(
            "comer",
            db.scalar("SELECT lemma FROM dictionary WHERE surface_form = 'comen'")
        )
        assertEquals("mucho", db.scalar("SELECT lemma FROM dictionary WHERE surface_form = 'muy'"))
        assertEquals("ser", db.scalar("SELECT lemma FROM dictionary WHERE surface_form = 'son'"))
        for (junk in listOf("es-conj", "irregular", "comerlo", "zarzaparrilla")) {
            assertEquals(
                junk,
                "0",
                db.scalar("SELECT COUNT(*) FROM dictionary WHERE surface_form = '$junk'")
            )
        }
    }

    @Test
    fun frequencyRanksComeFromTheRankFileAndTheCuratedSeedWins() {
        val (db, _) = buildDatabase("seed.db", extractSubsets())

        assertEquals(
            "1",
            db.scalar("SELECT frequency_rank FROM dictionary WHERE surface_form = 'el'")
        )
        // "casas" has no rank of its own better than its lemma's, so it takes the lemma's.
        assertEquals(
            "hotel (curated)",
            db.scalar("SELECT english_gloss FROM dictionary WHERE surface_form = 'hotel'")
        )
        assertEquals(
            "50",
            db.scalar("SELECT frequency_rank FROM dictionary WHERE surface_form = 'hotel'")
        )
    }

    @Test
    fun theFalseCognateSeedIsExpandedAcrossTheKaikkiForms() {
        val (db, summary) = buildDatabase("seed.db", extractSubsets())

        assertEquals(2, summary.falseCognateRows)
        assertTrue(summary.falseCognateLemmasWithoutDictionaryRows.isEmpty())
        assertEquals(
            listOf("casa", "casas"),
            db.query("SELECT surface_form FROM false_cognates ORDER BY 1") { it.getString(1) }
        )
    }

    @Test
    fun theTatoebaLessonsContinueAfterTheCuratedLessonOfTheLevel() {
        val (db, summary) = buildDatabase("seed.db", extractSubsets())

        val lessons =
            db.query(
                "SELECT lesson_id, level, sequence_order FROM curated_lessons ORDER BY level, sequence_order"
            ) {
                Triple(it.getString(1), it.getInt(2), it.getInt(3))
            }
        // Five level-0 sentences make two packs of two (the fifth is dropped); the single level-1
        // sentence cannot fill a pack.
        assertEquals(
            listOf(
                Triple("lvl0_03_curated", 0, 3),
                Triple("tatoeba_l0_001", 0, 4),
                Triple("tatoeba_l0_002", 0, 5),
            ),
            lessons,
        )
        assertEquals(lessons.size, summary.curatedLessonRows)
    }

    @Test
    fun theDatabaseHasTheSchemaRoomExpects() {
        val (db, _) = buildDatabase("seed.db", extractSubsets())

        assertEquals(SeedDatabase.ROOM_SCHEMA_VERSION.toString(), db.scalar("PRAGMA user_version"))
        assertEquals("ok", db.scalar("PRAGMA integrity_check"))
        // ANALYZE adds sqlite_stat* bookkeeping tables; Room ignores sqlite_ internals.
        val tables =
            db.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' " +
                    "AND name NOT LIKE 'sqlite_%' ORDER BY name"
            ) {
                it.getString(1)
            }
        assertEquals(
            listOf("cognate_rules", "curated_lessons", "dictionary", "false_cognates"),
            tables
        )
        for (table in tables) {
            val primaryKeys =
                db.query("PRAGMA table_info($table)") {
                    Triple(it.getString("name"), it.getInt("notnull"), it.getInt("pk"))
                }
            assertTrue("$table has a primary key", primaryKeys.any { it.third > 0 })
            // Room validates NOT NULL on primary keys, which SQLite does not enforce by itself.
            assertTrue(
                "$table: primary key columns must be NOT NULL",
                primaryKeys.filter { it.third > 0 }.all { it.second == 1 }
            )
        }
    }

    @Test
    fun buildingTwiceProducesByteIdenticalDatabases() {
        val subsets = extractSubsets()
        val (first, _) = buildDatabase("first.db", subsets)
        val (second, _) = buildDatabase("second.db", subsets)

        assertTrue(
            "seed.db must be reproducible",
            first.readBytes().contentEquals(second.readBytes())
        )
    }

    @Test
    fun extractingTwiceProducesByteIdenticalSubsets() {
        val first = extractSubsets()
        val firstKaikki = first.kaikki.readBytes()
        val firstPool = first.tatoeba.readBytes()
        val firstRanks = first.ranks.readBytes()

        val second = extractSubsets()

        assertTrue(firstKaikki.contentEquals(second.kaikki.readBytes()))
        assertTrue(firstPool.contentEquals(second.tatoeba.readBytes()))
        assertTrue(firstRanks.contentEquals(second.ranks.readBytes()))
    }

    @Test
    fun everyShippedTatoebaWordResolvesInTheDictionary() {
        val (db, _) = buildDatabase("seed.db", extractSubsets())

        val sentences =
            db.query("SELECT content_json FROM curated_lessons WHERE lesson_id LIKE 'tatoeba%'") {
                it.getString(1)
            }
        assertFalse(sentences.isEmpty())
        val spanish = Regex(""""sentence_es":"([^"]+)"""")
        val words =
            sentences
                .flatMap { content -> spanish.findAll(content).map { it.groupValues[1] }.toList() }
                .flatMap { text -> SentenceScorer.tokenizeWords(text).map { word -> text to word } }
        assertTrue(words.isNotEmpty())
        SeedDatabase.open("jdbc:sqlite:${db.absolutePath}").use { database ->
            for ((text, word) in words) {
                assertNotNull("'$word' of '$text' must resolve", database.lookupWord(word))
            }
            assertNull(database.lookupWord("zarzaparrilla"))
        }
    }
}
