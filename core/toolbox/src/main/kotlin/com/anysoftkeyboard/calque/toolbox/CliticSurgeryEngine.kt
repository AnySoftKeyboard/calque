package com.anysoftkeyboard.calque.toolbox

import com.anysoftkeyboard.calque.linguistics.CliticDeconstructor
import com.anysoftkeyboard.calque.linguistics.WordLookupProvider
import java.util.Locale

/** Domain engine for the Clitic Unpacker ("Pronoun Surgery" Sandbox). */
object CliticSurgeryEngine {

    /**
     * Performs pronoun surgery on [word], breaking down root verb and attached enclitics into
     * structured, color-coded visual blocks and generating equivalent proclitic structures.
     */
    fun performSurgery(
        word: String,
        lookupProvider: WordLookupProvider,
    ): CliticSurgeryResult? {
        val breakdown = CliticDeconstructor.deconstruct(word, lookupProvider) ?: return null
        val rootVerb = breakdown.rootSurface
        val mood = determineVerbMood(rootVerb)
        val pronounBlocks =
            breakdown.attachedPronouns.map { pronoun -> createPronounBlock(pronoun) }

        val rootBlock =
            PronounBlock(
                type = PronounBlockType.ROOT_VERB,
                text = rootVerb,
                roleName = "Root Verb (${breakdown.rootLemma})",
                meaning = breakdown.rootEntry.englishGloss,
            )

        val allBlocks = listOf(rootBlock) + pronounBlocks
        val procliticEquivalent =
            buildProcliticEquivalent(rootVerb, breakdown.attachedPronouns, mood)
        val explanation = buildSurgeryExplanation(word, rootVerb, breakdown.attachedPronouns, mood)

        return CliticSurgeryResult(
            originalWord = word,
            rootVerb = rootVerb,
            rootMood = mood,
            blocks = allBlocks,
            separatedPronouns = breakdown.attachedPronouns,
            procliticEquivalent = procliticEquivalent,
            explanation = explanation,
        )
    }

    private fun determineVerbMood(rootVerb: String): String {
        val lower = rootVerb.lowercase(Locale.ROOT)
        return when {
            lower.endsWith("r") -> "infinitive"
            lower.endsWith("ndo") -> "gerund"
            else -> "imperative"
        }
    }

    private fun createPronounBlock(pronoun: String): PronounBlock {
        val lower = pronoun.lowercase(Locale.ROOT)
        val isDirect = lower in setOf("lo", "la", "los", "las")
        val isReflexive = lower == "se"

        val type =
            when {
                isDirect -> PronounBlockType.DIRECT_PRONOUN
                isReflexive -> PronounBlockType.REFLEXIVE_PRONOUN
                else -> PronounBlockType.INDIRECT_PRONOUN
            }

        val roleName =
            when {
                isDirect -> "Direct Object"
                isReflexive -> "Indirect / Reflexive"
                else -> "Indirect Object"
            }

        val meaning = getPronounMeaning(lower)
        return PronounBlock(type = type, text = pronoun, roleName = roleName, meaning = meaning)
    }

    private fun getPronounMeaning(pronoun: String): String {
        return when (pronoun) {
            "me" -> "to/for me"
            "te" -> "to/for you"
            "se" -> "to him/her/them (or reflexive)"
            "nos" -> "to/for us"
            "os" -> "to/for you all"
            "le",
            "les" -> "to him/her/them"
            "lo" -> "him / it (masc.)"
            "la" -> "her / it (fem.)"
            "los" -> "them (masc.)"
            "las" -> "them (fem.)"
            else -> pronoun
        }
    }

    private fun buildProcliticEquivalent(
        rootVerb: String,
        pronouns: List<String>,
        mood: String,
    ): String {
        val joinedPronouns = pronouns.joinToString(" ")
        return when (mood) {
            "infinitive" -> "$joinedPronouns va a $rootVerb"
            "gerund" -> "$joinedPronouns está $rootVerb"
            else -> "No $joinedPronouns $rootVerb"
        }
    }

    private fun buildSurgeryExplanation(
        originalWord: String,
        rootVerb: String,
        pronouns: List<String>,
        mood: String,
    ): String {
        val pronounCount = if (pronouns.size == 1) "1 pronoun" else "${pronouns.size} pronouns"
        val pronounList = pronouns.joinToString(", ") { "'$it'" }
        return "'$originalWord' attaches $pronounCount ($pronounList) to the $mood '$rootVerb'."
    }
}
