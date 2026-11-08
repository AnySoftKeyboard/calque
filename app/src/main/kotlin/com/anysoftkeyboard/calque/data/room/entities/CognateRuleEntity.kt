package com.anysoftkeyboard.calque.data.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.anysoftkeyboard.calque.data.models.CognateRule

/** Room entity for the read-only [cognate_rules] seed table. */
@Entity(
    tableName = "cognate_rules",
)
data class CognateRuleEntity(
    @PrimaryKey @ColumnInfo(name = "rule_id") val ruleId: String,
    @ColumnInfo(name = "english_suffix") val englishSuffix: String,
    @ColumnInfo(name = "spanish_suffix") val spanishSuffix: String,
    @ColumnInfo(name = "gender_rule") val genderRule: String?,
    @ColumnInfo(name = "spelling_note") val spellingNote: String,
    @ColumnInfo(name = "examples_json") val examplesJson: String,
) {
    fun toDomain(): CognateRule {
        return CognateRule(
            ruleId = ruleId,
            englishSuffix = englishSuffix,
            spanishSuffix = spanishSuffix,
            genderRule = genderRule,
            spellingNote = spellingNote,
            examplesJson = examplesJson,
        )
    }

    companion object {
        fun fromDomain(rule: CognateRule): CognateRuleEntity {
            return CognateRuleEntity(
                ruleId = rule.ruleId,
                englishSuffix = rule.englishSuffix,
                spanishSuffix = rule.spanishSuffix,
                genderRule = rule.genderRule,
                spellingNote = rule.spellingNote,
                examplesJson = rule.examplesJson,
            )
        }
    }
}
