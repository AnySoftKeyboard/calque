package com.anysoftkeyboard.calque.data.seed

import com.anysoftkeyboard.calque.data.models.CognateRule
import java.sql.Connection
import java.sql.ResultSet
import java.util.Locale

/** Repository interface for accessing cognate transformation rules. */
interface CognateRulesRepository {
    /** Retrieves all cognate rules. */
    fun getAll(): List<CognateRule>

    /** Retrieves a cognate rule by its unique identifier (e.g. "tion_to_cion"). */
    fun getById(ruleId: String): CognateRule?

    /**
     * Finds the most specific cognate rule applicable to a Spanish word, matching the longest
     * [CognateRule.spanishSuffix].
     */
    fun findRuleForSpanishWord(spanishWord: String): CognateRule?
}

/** SQLite-backed implementation of [CognateRulesRepository]. */
class SqliteCognateRulesRepository(
    private val connection: Connection,
) : CognateRulesRepository {

    override fun getAll(): List<CognateRule> {
        val sql =
            """
            SELECT rule_id, english_suffix, spanish_suffix, gender_rule, spelling_note, examples_json
            FROM cognate_rules
            ORDER BY rowid ASC
            """
                .trimIndent()

        val results = mutableListOf<CognateRule>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                while (rs.next()) {
                    results.add(mapRow(rs))
                }
            }
        }
        return results
    }

    override fun getById(ruleId: String): CognateRule? {
        val trimmed = ruleId.trim()
        if (trimmed.isEmpty()) return null

        val sql =
            """
            SELECT rule_id, english_suffix, spanish_suffix, gender_rule, spelling_note, examples_json
            FROM cognate_rules
            WHERE rule_id = ?
            LIMIT 1
            """
                .trimIndent()

        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, trimmed)
            stmt.executeQuery().use { rs ->
                return if (rs.next()) mapRow(rs) else null
            }
        }
    }

    override fun findRuleForSpanishWord(spanishWord: String): CognateRule? {
        val normalized = spanishWord.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return null

        val rules = getAll()
        return rules
            .filter { rule ->
                val cleanSuffix = cleanSuffix(rule.spanishSuffix)
                cleanSuffix.isNotEmpty() && normalized.endsWith(cleanSuffix)
            }
            .maxByOrNull { rule -> cleanSuffix(rule.spanishSuffix).length }
    }

    private fun cleanSuffix(suffix: String): String {
        return suffix.lowercase(Locale.ROOT).trim().removePrefix("-")
    }

    private fun mapRow(rs: ResultSet): CognateRule {
        return CognateRule(
            ruleId = rs.getString("rule_id"),
            englishSuffix = rs.getString("english_suffix"),
            spanishSuffix = rs.getString("spanish_suffix"),
            genderRule = rs.getString("gender_rule"),
            spellingNote = rs.getString("spelling_note"),
            examplesJson = rs.getString("examples_json"),
        )
    }
}
