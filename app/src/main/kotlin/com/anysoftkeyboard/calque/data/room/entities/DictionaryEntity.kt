package com.anysoftkeyboard.calque.data.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.anysoftkeyboard.calque.linguistics.DictionaryEntry

/**
 * Room entity for the read-only [dictionary] seed table.
 *
 * A spelling can have several readings, one row each, keyed by (surface form, lemma, part of
 * speech). [readingOrder] numbers them per spelling: 0 is the primary reading.
 */
@Entity(
    tableName = "dictionary",
    primaryKeys = ["surface_form", "lemma", "pos"],
    indices = [Index(value = ["lemma"], name = "idx_dictionary_lemma")],
)
data class DictionaryEntity(
    @ColumnInfo(name = "surface_form") val surfaceForm: String,
    @ColumnInfo(name = "lemma") val lemma: String,
    @ColumnInfo(name = "pos") val pos: String,
    @ColumnInfo(name = "grammar_info") val grammarInfo: String?,
    @ColumnInfo(name = "english_gloss") val englishGloss: String,
    @ColumnInfo(name = "is_cognate") val isCognate: Int,
    @ColumnInfo(name = "frequency_rank") val frequencyRank: Int?,
    @ColumnInfo(name = "reading_order") val readingOrder: Int,
) {
    /** This single reading as a domain entry, without alternatives. */
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
        fun fromDomain(entry: DictionaryEntry, readingOrder: Int = 0): DictionaryEntity {
            return DictionaryEntity(
                surfaceForm = entry.surfaceForm,
                lemma = entry.lemma,
                pos = entry.pos,
                grammarInfo = entry.grammarInfo,
                englishGloss = entry.englishGloss,
                isCognate = if (entry.isCognate) 1 else 0,
                frequencyRank = entry.frequencyRank,
                readingOrder = readingOrder,
            )
        }
    }
}
