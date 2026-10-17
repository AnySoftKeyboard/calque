package com.anysoftkeyboard.calque.data.seed

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SeedCuratedLessonsRepositoryTest {

    private lateinit var database: SeedDatabase

    @Before
    fun setUp() {
        database = SeedDatabase.open()
        SeedDatabase.createTables(database.connection)
        insertTestData()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun insertTestData() {
        val sampleJson =
            """
            [
              {
                "sentenceEs": "El hotel es excelente.",
                "translationEn": "The hotel is excellent.",
                "interlinearGloss": [
                  {"es": "El", "en": "The"},
                  {"es": "hotel", "en": "hotel"},
                  {"es": "es", "en": "is"},
                  {"es": "excelente", "en": "excellent"}
                ],
                "grammarCallout": "Zero-translation cognates"
              }
            ]
            """
                .trimIndent()

        val sql =
            """
            INSERT INTO curated_lessons (lesson_id, level, sequence_order, title, target_concept, content_json)
            VALUES (?, ?, ?, ?, ?, ?)
            """
                .trimIndent()

        database.connection.prepareStatement(sql).use { stmt ->
            // Lesson 1: Level 0, seq 1
            stmt.setString(1, "lvl0_00_cognate_unlock")
            stmt.setInt(2, 0)
            stmt.setInt(3, 1)
            stmt.setString(4, "Cognate Unlock")
            stmt.setString(5, "Transparent cognates require zero translation effort.")
            stmt.setString(6, sampleJson)
            stmt.addBatch()

            // Lesson 2: Level 0, seq 2
            stmt.setString(1, "lvl0_01_svo_cognates")
            stmt.setInt(2, 0)
            stmt.setInt(3, 2)
            stmt.setString(4, "Simple SVO Sentences")
            stmt.setString(5, "Spanish declarative sentences follow Subject-Verb-Object.")
            stmt.setString(6, "[]")
            stmt.addBatch()

            // Lesson 3: Level 1, seq 1
            stmt.setString(1, "lvl1_01_oficina")
            stmt.setInt(2, 1)
            stmt.setInt(3, 1)
            stmt.setString(4, "El problema en la oficina")
            stmt.setString(5, "Present-tense narrative with false cognates.")
            stmt.setString(6, "[]")
            stmt.addBatch()

            stmt.executeBatch()
        }
    }

    @Test
    fun getAll_returnsOrderedByLevelThenSequence() {
        val lessons = database.curatedLessons.getAll()
        assertEquals(3, lessons.size)
        assertEquals("lvl0_00_cognate_unlock", lessons[0].lessonId)
        assertEquals("lvl0_01_svo_cognates", lessons[1].lessonId)
        assertEquals("lvl1_01_oficina", lessons[2].lessonId)
    }

    @Test
    fun getByLevel_filtersCorrectly() {
        val level0 = database.curatedLessons.getByLevel(0)
        assertEquals(2, level0.size)
        assertEquals("lvl0_00_cognate_unlock", level0[0].lessonId)
        assertEquals("lvl0_01_svo_cognates", level0[1].lessonId)

        val level1 = database.curatedLessons.getByLevel(1)
        assertEquals(1, level1.size)
        assertEquals("lvl1_01_oficina", level1[0].lessonId)

        val level99 = database.curatedLessons.getByLevel(99)
        assertTrue(level99.isEmpty())
    }

    @Test
    fun getById_andParseSentences() {
        val lesson = database.curatedLessons.getById("lvl0_00_cognate_unlock")
        assertNotNull(lesson)
        assertEquals("Cognate Unlock", lesson?.title)
        assertEquals(0, lesson?.level)
        assertEquals(1, lesson?.sequenceOrder)

        val sentences = lesson?.parseSentences()
        assertNotNull(sentences)
        assertEquals(1, sentences?.size)
        val s = sentences!![0]
        assertEquals("El hotel es excelente.", s.sentenceEs)
        assertEquals("The hotel is excellent.", s.translationEn)
        assertEquals(4, s.interlinearGloss.size)
        assertEquals("hotel", s.interlinearGloss[1].es)
        assertEquals("hotel", s.interlinearGloss[1].en)
        assertEquals("Zero-translation cognates", s.grammarCallout)

        val notFound = database.curatedLessons.getById("non_existent")
        assertNull(notFound)
    }
}
