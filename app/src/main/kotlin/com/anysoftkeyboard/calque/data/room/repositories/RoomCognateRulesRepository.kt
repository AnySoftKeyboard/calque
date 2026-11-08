package com.anysoftkeyboard.calque.data.room.repositories

import com.anysoftkeyboard.calque.data.models.CognateRule
import com.anysoftkeyboard.calque.data.room.dao.CognateRulesDao
import com.anysoftkeyboard.calque.data.seed.CognateRulesRepository
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** Room-backed implementation of [CognateRulesRepository] using [CognateRulesDao]. */
class RoomCognateRulesRepository(
    val dao: CognateRulesDao,
) : CognateRulesRepository {

    override fun getAll(): List<CognateRule> {
        return runBlocking(Dispatchers.IO) { dao.getAll().map { it.toDomain() } }
    }

    override fun getById(ruleId: String): CognateRule? {
        val trimmed = ruleId.trim()
        if (trimmed.isEmpty()) return null
        return runBlocking(Dispatchers.IO) { dao.getById(trimmed)?.toDomain() }
    }

    override fun findRuleForSpanishWord(spanishWord: String): CognateRule? {
        val normalized = spanishWord.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return null

        val rules = getAll()
        return rules
            .filter { rule ->
                val clean = cleanSuffix(rule.spanishSuffix)
                clean.isNotEmpty() && normalized.endsWith(clean)
            }
            .maxByOrNull { rule -> cleanSuffix(rule.spanishSuffix).length }
    }

    private fun cleanSuffix(suffix: String): String {
        return suffix.lowercase(Locale.ROOT).trim().removePrefix("-")
    }
}
