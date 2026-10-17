package com.anysoftkeyboard.calque.data.user

import com.anysoftkeyboard.calque.data.models.UserCustomText
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserCustomTextRepositoryTest {

    private lateinit var database: UserDatabase

    @Before
    fun setUp() {
        database = UserDatabase.open()
        UserDatabase.createTables(database.connection)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetById_autoGeneratesIdAndRetrieves() {
        val entry =
            UserCustomText(
                title = "Noticia breve",
                originalSpanish = "El presidente anunció nuevas medidas económicas hoy.",
                simplifiedSpanish = "El líder presentó nuevas reglas de dinero hoy.",
                readabilityLevel = 1,
                createdTimestamp = 1000L,
            )

        val id = database.customTexts.insert(entry)
        assertTrue(id > 0L)

        val retrieved = database.customTexts.getById(id)
        assertNotNull(retrieved)
        assertEquals(id, retrieved?.textId)
        assertEquals("Noticia breve", retrieved?.title)
        assertEquals(
            "El presidente anunció nuevas medidas económicas hoy.",
            retrieved?.originalSpanish
        )
        assertEquals("El líder presentó nuevas reglas de dinero hoy.", retrieved?.simplifiedSpanish)
        assertEquals(1, retrieved?.readabilityLevel)
        assertEquals(1000L, retrieved?.createdTimestamp)
    }

    @Test
    fun update_modifiesFields() {
        val id =
            database.customTexts.insert(
                UserCustomText(
                    title = "Borrador",
                    originalSpanish = "Texto sin simplificar.",
                    readabilityLevel = 2,
                    createdTimestamp = 500L,
                )
            )

        val updated =
            UserCustomText(
                textId = id,
                title = "Borrador Final",
                originalSpanish = "Texto sin simplificar.",
                simplifiedSpanish = "Texto fácil.",
                readabilityLevel = 0,
                createdTimestamp = 500L,
            )

        assertTrue(database.customTexts.update(updated))

        val retrieved = database.customTexts.getById(id)
        assertEquals("Borrador Final", retrieved?.title)
        assertEquals("Texto fácil.", retrieved?.simplifiedSpanish)
        assertEquals(0, retrieved?.readabilityLevel)
    }

    @Test
    fun getAll_returnsOrderedNewestFirst() {
        database.customTexts.insert(
            UserCustomText(
                title = "Primero",
                originalSpanish = "Texto 1",
                createdTimestamp = 100L,
            )
        )
        database.customTexts.insert(
            UserCustomText(
                title = "Segundo",
                originalSpanish = "Texto 2",
                createdTimestamp = 300L,
            )
        )
        database.customTexts.insert(
            UserCustomText(
                title = "Tercero",
                originalSpanish = "Texto 3",
                createdTimestamp = 200L,
            )
        )

        val all = database.customTexts.getAll()
        assertEquals(3, all.size)
        // Order by created_timestamp DESC
        assertEquals("Segundo", all[0].title)
        assertEquals("Tercero", all[1].title)
        assertEquals("Primero", all[2].title)
    }

    @Test
    fun delete_removesEntry() {
        val id =
            database.customTexts.insert(
                UserCustomText(
                    title = "Para eliminar",
                    originalSpanish = "Texto",
                    createdTimestamp = 100L,
                )
            )

        assertTrue(database.customTexts.delete(id))
        assertNull(database.customTexts.getById(id))
        assertFalse(database.customTexts.delete(id))
    }
}
