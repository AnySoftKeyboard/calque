package com.anysoftkeyboard.calque.data.models

import com.anysoftkeyboard.calque.linguistics.UserWordStatus

/**
 * Domain model representing a learner's personal state and vault status for a vocabulary [lemma].
 */
data class UserWordState(
    val lemma: String,
    val status: UserWordStatus = UserWordStatus.NEW,
    val starred: Boolean = false,
    val savedContextSentence: String? = null,
    val encounterCount: Int = 0,
    val lastSeenTimestamp: Long = 0L,
)
