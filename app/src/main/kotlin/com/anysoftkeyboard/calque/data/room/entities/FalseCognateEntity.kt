package com.anysoftkeyboard.calque.data.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anysoftkeyboard.calque.linguistics.FalseCognateEntry

/** Room entity for the read-only [false_cognates] seed table. */
@Entity(
    tableName = "false_cognates",
    indices = [Index(value = ["lemma"], name = "idx_false_cognates_lemma")],
)
data class FalseCognateEntity(
    @PrimaryKey @ColumnInfo(name = "surface_form") val surfaceForm: String,
    @ColumnInfo(name = "lemma") val lemma: String,
    @ColumnInfo(name = "apparent_english") val apparentEnglish: String,
    @ColumnInfo(name = "actual_english") val actualEnglish: String,
    @ColumnInfo(name = "real_spanish_for_apparent") val realSpanishForApparent: String,
    @ColumnInfo(name = "severity") val severity: String,
    @ColumnInfo(name = "disambiguation_tip") val disambiguationTip: String,
    @ColumnInfo(name = "example_es") val exampleEs: String,
    @ColumnInfo(name = "example_en") val exampleEn: String,
) {
    fun toDomain(): FalseCognateEntry {
        return FalseCognateEntry(
            surfaceForm = surfaceForm,
            lemma = lemma,
            apparentEnglish = apparentEnglish,
            actualEnglish = actualEnglish,
            realSpanishForApparent = realSpanishForApparent,
            severity = severity,
            disambiguationTip = disambiguationTip,
            exampleEs = exampleEs,
            exampleEn = exampleEn,
        )
    }

    companion object {
        fun fromDomain(entry: FalseCognateEntry): FalseCognateEntity {
            return FalseCognateEntity(
                surfaceForm = entry.surfaceForm,
                lemma = entry.lemma,
                apparentEnglish = entry.apparentEnglish,
                actualEnglish = entry.actualEnglish,
                realSpanishForApparent = entry.realSpanishForApparent,
                severity = entry.severity,
                disambiguationTip = entry.disambiguationTip,
                exampleEs = entry.exampleEs,
                exampleEn = entry.exampleEn,
            )
        }
    }
}
