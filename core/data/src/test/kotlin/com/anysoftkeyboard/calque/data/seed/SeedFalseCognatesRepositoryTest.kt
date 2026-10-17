package com.anysoftkeyboard.calque.data.seed

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SeedFalseCognatesRepositoryTest {

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
        val sql =
            """
            INSERT INTO false_cognates (
                surface_form, lemma, apparent_english, actual_english, real_spanish_for_apparent,
                severity, disambiguation_tip, example_es, example_en
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
                .trimIndent()

        database.connection.prepareStatement(sql).use { stmt ->
            // Entry 1: exito
            stmt.setString(1, "éxito")
            stmt.setString(2, "éxito")
            stmt.setString(3, "exit")
            stmt.setString(4, "success")
            stmt.setString(5, "salida")
            stmt.setString(6, "HIGH_RISK")
            stmt.setString(7, "Think: 'Exit' is 'salida'; 'éxito' is success.")
            stmt.setString(8, "El concierto fue un gran éxito.")
            stmt.setString(9, "The concert was a great success.")
            stmt.addBatch()

            // Entry 2: exitos (plural)
            stmt.setString(1, "éxitos")
            stmt.setString(2, "éxito")
            stmt.setString(3, "exit")
            stmt.setString(4, "success")
            stmt.setString(5, "salida")
            stmt.setString(6, "HIGH_RISK")
            stmt.setString(7, "Think: 'Exit' is 'salida'; 'éxito' is success.")
            stmt.setString(8, "Muchos éxitos en tu nuevo trabajo.")
            stmt.setString(9, "Much success in your new job.")
            stmt.addBatch()

            // Entry 3: carpeta
            stmt.setString(1, "carpeta")
            stmt.setString(2, "carpeta")
            stmt.setString(3, "carpet")
            stmt.setString(4, "folder / binder")
            stmt.setString(5, "alfombra")
            stmt.setString(6, "HIGH_RISK")
            stmt.setString(7, "Carpeta holds documents; carpet is alfombra.")
            stmt.setString(8, "Guarda los papeles en la carpeta.")
            stmt.setString(9, "Keep the papers in the folder.")
            stmt.addBatch()

            stmt.executeBatch()
        }
    }

    @Test
    fun getBySurfaceForm_exactAndCaseInsensitive() {
        val entry = database.falseCognates.getBySurfaceForm("éxito")
        assertNotNull(entry)
        assertEquals("éxito", entry?.surfaceForm)
        assertEquals("éxito", entry?.lemma)
        assertEquals("exit", entry?.apparentEnglish)
        assertEquals("success", entry?.actualEnglish)
        assertEquals("salida", entry?.realSpanishForApparent)
        assertEquals("HIGH_RISK", entry?.severity)

        val caseInsensitive = database.falseCognates.getBySurfaceForm("  CARPETA  ")
        assertNotNull(caseInsensitive)
        assertEquals("carpeta", caseInsensitive?.surfaceForm)
        assertEquals("carpet", caseInsensitive?.apparentEnglish)

        val notFound = database.falseCognates.getBySurfaceForm("inexistente")
        assertNull(notFound)
    }

    @Test
    fun getBySurfaceForms_batchQuery() {
        val results = database.falseCognates.getBySurfaceForms(setOf("éxito", "carpeta", "no_trap"))
        assertEquals(2, results.size)
        assertTrue(results.containsKey("éxito"))
        assertTrue(results.containsKey("carpeta"))
    }

    @Test
    fun getByLemma_returnsAllInflectionsOfTrap() {
        val entries = database.falseCognates.getByLemma("éxito")
        assertEquals(2, entries.size)
        assertEquals(listOf("éxito", "éxitos"), entries.map { it.surfaceForm })
    }

    @Test
    fun getAll_returnsAllTrapsSorted() {
        val all = database.falseCognates.getAll()
        assertEquals(3, all.size)
        assertEquals("carpeta", all[0].surfaceForm)
        assertEquals("éxito", all[1].surfaceForm)
        assertEquals("éxitos", all[2].surfaceForm)
    }
}
