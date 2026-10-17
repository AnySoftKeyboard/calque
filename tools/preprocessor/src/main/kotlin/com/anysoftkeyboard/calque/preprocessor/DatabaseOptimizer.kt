package com.anysoftkeyboard.calque.preprocessor

/** Performs SQLite database optimization and compaction via ANALYZE and VACUUM. */
fun SeedDatabase.optimize() {
    connection.createStatement().use { stmt ->
        stmt.execute("ANALYZE")
        stmt.execute("VACUUM")
    }
}
