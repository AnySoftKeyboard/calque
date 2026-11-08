package com.anysoftkeyboard.calque.data.room

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * Test-only adapter exposing a plain sqlite-jdbc [Connection] as an AndroidX [SQLiteConnection], so
 * Room's generated schema creation and validation code can run on a regular JVM without Android.
 */
internal class JdbcSqliteConnection(private val connection: Connection) : SQLiteConnection {

    override fun prepare(sql: String): SQLiteStatement =
        JdbcSqliteStatement(connection.prepareStatement(sql))

    override fun close() {
        connection.close()
    }
}

/**
 * JDBC-backed [SQLiteStatement]. Bind indices are 1-based and column indices 0-based, matching the
 * AndroidX contract.
 */
internal class JdbcSqliteStatement(private val statement: PreparedStatement) : SQLiteStatement {

    private var resultSet: ResultSet? = null
    private var columnNames: List<String> = emptyList()
    private var executed = false

    private fun ensureExecuted() {
        if (executed) return
        executed = true
        if (statement.execute()) {
            val rs = statement.resultSet
            resultSet = rs
            columnNames = (1..rs.metaData.columnCount).map { rs.metaData.getColumnName(it) }
        }
    }

    private fun rs(): ResultSet = checkNotNull(resultSet) { "Statement has no current row" }

    override fun bindBlob(index: Int, value: ByteArray) = statement.setBytes(index, value)

    override fun bindDouble(index: Int, value: Double) = statement.setDouble(index, value)

    override fun bindLong(index: Int, value: Long) = statement.setLong(index, value)

    override fun bindText(index: Int, value: String) = statement.setString(index, value)

    override fun bindNull(index: Int) = statement.setNull(index, Types.NULL)

    override fun getBlob(index: Int): ByteArray = rs().getBytes(index + 1)

    override fun getDouble(index: Int): Double = rs().getDouble(index + 1)

    override fun getLong(index: Int): Long = rs().getLong(index + 1)

    override fun getText(index: Int): String = rs().getString(index + 1)

    override fun isNull(index: Int): Boolean = rs().getObject(index + 1) == null

    override fun getColumnCount(): Int {
        ensureExecuted()
        return columnNames.size
    }

    override fun getColumnName(index: Int): String {
        ensureExecuted()
        return columnNames[index]
    }

    override fun getColumnType(index: Int): Int =
        when (rs().getObject(index + 1)) {
            null -> TYPE_NULL
            is Long,
            is Int -> TYPE_INTEGER
            is Double,
            is Float -> TYPE_FLOAT
            is ByteArray -> TYPE_BLOB
            else -> TYPE_TEXT
        }

    override fun step(): Boolean {
        ensureExecuted()
        return resultSet?.next() ?: false
    }

    override fun reset() {
        resultSet?.close()
        resultSet = null
        executed = false
    }

    override fun clearBindings() = statement.clearParameters()

    override fun close() {
        resultSet?.close()
        statement.close()
    }

    private companion object {
        const val TYPE_INTEGER = 1
        const val TYPE_FLOAT = 2
        const val TYPE_TEXT = 3
        const val TYPE_BLOB = 4
        const val TYPE_NULL = 5
    }
}
