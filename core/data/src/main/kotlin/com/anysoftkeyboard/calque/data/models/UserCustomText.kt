package com.anysoftkeyboard.calque.data.models

/** Domain model representing user-imported reading passages and optional AI-simplified versions. */
data class UserCustomText(
    val textId: Long = 0L,
    val title: String,
    val originalSpanish: String,
    val simplifiedSpanish: String? = null,
    val readabilityLevel: Int = 0,
    val createdTimestamp: Long = 0L,
)
