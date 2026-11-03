package com.anysoftkeyboard.calque.data.seed

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SeedCognateRulesRepositoryTest {

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
        val sampleExamples =
            """
            [
              {"english": "nation", "spanish": "nación"},
              {"english": "action", "spanish": "acción"}
            ]
            """
                .trimIndent()

        val sql =
            """
            INSERT INTO cognate_rules (rule_id, english_suffix, spanish_suffix, gender_rule, spelling_note, examples_json)
            VALUES (?, ?, ?, ?, ?, ?)
            """
                .trimIndent()

        database.connection.prepareStatement(sql).use { stmt ->
            // Rule 1: tion -> ción
            stmt.setString(1, "tion_to_cion")
            stmt.setString(2, "-tion")
            stmt.setString(3, "-ción")
            stmt.setString(4, "Always feminine (la)")
            stmt.setString(5, "Drop double consonants")
            stmt.setString(6, sampleExamples)
            stmt.addBatch()

            // Rule 2: ty -> dad
            stmt.setString(1, "ty_to_dad")
            stmt.setString(2, "-ty")
            stmt.setString(3, "-dad")
            stmt.setString(4, "Always feminine (la)")
            stmt.setString(5, "Regular shift")
            stmt.setString(6, "[]")
            stmt.addBatch()

            // Rule 3: sion -> sión
            stmt.setString(1, "sion_to_sion")
            stmt.setString(2, "-sion")
            stmt.setString(3, "-sión")
            stmt.setString(4, "Always feminine (la)")
            stmt.setString(5, "Retains single s")
            stmt.setString(6, "[]")
            stmt.addBatch()

            stmt.executeBatch()
        }
    }

    @Test
    fun getAll_returnsAllRules() {
        val rules = database.cognateRules.getAll()
        assertEquals(3, rules.size)
        assertEquals("tion_to_cion", rules[0].ruleId)
        assertEquals("ty_to_dad", rules[1].ruleId)
        assertEquals("sion_to_sion", rules[2].ruleId)
    }

    @Test
    fun getById_andParseExamples() {
        val rule = database.cognateRules.getById("tion_to_cion")
        assertNotNull(rule)
        assertEquals("-tion", rule?.englishSuffix)
        assertEquals("-ción", rule?.spanishSuffix)
        assertEquals("Always feminine (la)", rule?.genderRule)

        val examples = rule?.parseExamples()
        assertNotNull(examples)
        assertEquals(2, examples?.size)
        assertEquals("nation", examples!![0].english)
        assertEquals("nación", examples[0].spanish)

        val notFound = database.cognateRules.getById("inexistent_rule")
        assertNull(notFound)
    }

    @Test
    fun findRuleForSpanishWord_matchesLongestSuffix() {
        val ruleNation = database.cognateRules.findRuleForSpanishWord("conversación")
        assertNotNull(ruleNation)
        assertEquals("tion_to_cion", ruleNation?.ruleId)

        val ruleCity = database.cognateRules.findRuleForSpanishWord("ciudad")
        assertNotNull(ruleCity)
        assertEquals("ty_to_dad", ruleCity?.ruleId)

        val noMatch = database.cognateRules.findRuleForSpanishWord("perro")
        assertNull(noMatch)
    }
}
