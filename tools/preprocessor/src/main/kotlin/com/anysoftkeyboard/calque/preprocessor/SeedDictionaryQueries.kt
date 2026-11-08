package com.anysoftkeyboard.calque.preprocessor

import java.sql.ResultSet

// Read-side dictionary queries of [SeedDatabase]: the readings stored for a spelling and counts.

private const val PARAM_SURFACE = 1
private const val COLUMN_LEMMA = 1
private const val COLUMN_POS = 2
private const val COLUMN_ORDER = 3

/** Returns the primary reading stored for [surfaceForm], or null when there is none. */
fun SeedDatabase.queryDictionaryRow(surfaceForm: String): DictionaryRow? =
    queryDictionaryRows(surfaceForm).firstOrNull()

/** Returns every reading stored for [surfaceForm], primary first. */
fun SeedDatabase.queryDictionaryRows(surfaceForm: String): List<DictionaryRow> {
    val sql =
        "SELECT ${DICTIONARY_COLUMNS} FROM dictionary WHERE surface_form = ? " +
            "ORDER BY $READING_ORDER_SQL"
    val rows = mutableListOf<DictionaryRow>()
    connection.prepareStatement(sql).use { pstmt ->
        pstmt.setString(PARAM_SURFACE, surfaceForm)
        pstmt.executeQuery().use { rs -> while (rs.next()) rows.add(mapDictionaryRow(rs)) }
    }
    return rows
}

/** The stored `reading_order` of every reading of [surfaceForm], as (lemma, pos, order). */
fun SeedDatabase.queryReadingOrders(surfaceForm: String): List<Triple<String, String, Int>> {
    val sql =
        "SELECT lemma, pos, reading_order FROM dictionary WHERE surface_form = ? " +
            "ORDER BY reading_order, pos, lemma"
    val orders = mutableListOf<Triple<String, String, Int>>()
    connection.prepareStatement(sql).use { pstmt ->
        pstmt.setString(PARAM_SURFACE, surfaceForm)
        pstmt.executeQuery().use { rs ->
            while (rs.next()) orders.add(
                Triple(
                    rs.getString(COLUMN_LEMMA),
                    rs.getString(COLUMN_POS),
                    rs.getInt(COLUMN_ORDER)
                )
            )
        }
    }
    return orders
}

/** Returns how many spellings have more than one reading. */
fun SeedDatabase.countAmbiguousSpellings(): Int =
    connection.createStatement().use { stmt ->
        stmt
            .executeQuery(
                "SELECT COUNT(*) FROM (SELECT surface_form FROM dictionary " +
                    "GROUP BY surface_form HAVING COUNT(*) > 1)"
            )
            .use { rs ->
                rs.next()
                rs.getInt(1)
            }
    }

/** Returns the number of rows in the dictionary table. */
fun SeedDatabase.countDictionaryRows(): Int =
    connection.createStatement().use { stmt ->
        stmt.executeQuery("SELECT COUNT(*) FROM dictionary").use { rs ->
            rs.next()
            rs.getInt(1)
        }
    }

private fun mapDictionaryRow(rs: ResultSet): DictionaryRow {
    val freqObj = rs.getObject("frequency_rank")
    return DictionaryRow(
        surfaceForm = rs.getString("surface_form"),
        lemma = rs.getString("lemma"),
        pos = rs.getString("pos"),
        grammarInfo = rs.getString("grammar_info"),
        englishGloss = rs.getString("english_gloss"),
        isCognate = rs.getInt("is_cognate") == SeedDatabase.COGNATE_FLAG_TRUE,
        frequencyRank = if (freqObj != null) rs.getInt("frequency_rank") else null,
    )
}
