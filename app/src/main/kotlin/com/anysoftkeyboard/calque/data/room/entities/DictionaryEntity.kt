package com.anysoftkeyboard.calque.data.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anysoftkeyboard.calque.linguistics.DictionaryEntry

/** Room entity for the read-only [dictionary] seed table. */
@Entity(
    tableName = "dictionary",
    indices = [Index(value = ["lemma"], name = "idx_dictionary_lemma")],
)
data class DictionaryEntity(
    @PrimaryKey @ColumnInfo(name = "surface_form") val surfaceForm: String,
    @ColumnInfo(name = "lemma") val lemma: String,
    @ColumnInfo(name = "pos") val pos: String,
    @ColumnInfo(name = "grammar_info") val grammarInfo: String?,
    @ColumnInfo(name = "english_gloss") val englishGloss: String,
    @ColumnInfo(name = "is_cognate") val isCognate: Int,
    @ColumnInfo(name = "frequency_rank") val frequencyRank: Int?,
) {
    fun toDomain(): DictionaryEntry {
        return DictionaryEntry(
            surfaceForm = surfaceForm,
            lemma = lemma,
            pos = pos,
            grammarInfo = grammarInfo,
            englishGloss = englishGloss,
            isCognate = isCognate == 1,
            frequencyRank = frequencyRank,
        )
    }

    companion object {
        fun fromDomain(entry: DictionaryEntry): DictionaryEntity {
            return DictionaryEntity(
                surfaceForm = entry.surfaceForm,
                lemma = entry.lemma,
                pos = entry.pos,
                grammarInfo = entry.grammarInfo,
                englishGloss = entry.englishGloss,
                isCognate = if (entry.isCognate) 1 else 0,
                frequencyRank = entry.frequencyRank,
            )
        }
    }
}
