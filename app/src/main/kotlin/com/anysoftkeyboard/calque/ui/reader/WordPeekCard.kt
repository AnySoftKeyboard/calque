package com.anysoftkeyboard.calque.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anysoftkeyboard.calque.linguistics.FalseCognateEntry
import com.anysoftkeyboard.calque.reader.TokenInspection
import com.anysoftkeyboard.calque.ui.theme.CalqueTheme

/** Peek inspection card presenting linguistic breakdown when a word token is tapped. */
@Composable
fun WordPeekCard(
    inspection: TokenInspection,
    onToggleStar: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CalqueTheme.colors

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 20.dp),
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = inspection.tokenSpan.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = colors.textPrimary,
                    )
                    val pos = inspection.pos ?: "word"
                    val status = inspection.userStatus.name.lowercase()
                    Text(
                        text = "Lemma: ${inspection.lemma} • $pos • $status",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                    )
                }

                // Star button
                Box(
                    modifier =
                        Modifier.clickable { onToggleStar(inspection.lemma) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = if (inspection.isStarred) "★ Starred" else "☆ Star",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color =
                            if (inspection.isStarred) {
                                colors.falseCognateAmber
                            } else {
                                colors.textSecondary
                            },
                    )
                }

                // Dismiss button
                Box(
                    modifier =
                        Modifier.clickable { onDismiss() }
                            .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                ) {
                    Text(text = "✕", fontSize = 16.sp, color = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Gloss Row
            val gloss = inspection.englishGloss ?: "—"
            Text(
                text = "Meaning: $gloss",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )

            // False Cognate Trap Section
            val trap = inspection.falseCognateTrap
            if (trap != null) {
                Spacer(modifier = Modifier.height(8.dp))
                FalseCognateTrapAlert(trap = trap)
            }

            // Clitic Section
            val clitic = inspection.cliticSurgery
            if (clitic != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text =
                        "✂️ Pronoun Surgery: Root \"${clitic.rootVerb}\" • Proclitic: \"${clitic.procliticEquivalent}\"",
                    fontSize = 12.sp,
                    color = colors.slateBlueAccent,
                )
            }

            // Cognate Rule Section
            val rule = inspection.cognateRuleNote
            if (rule != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "💡 Cognate Bridge: $rule",
                    fontSize = 12.sp,
                    color = colors.cognateEmerald,
                )
            }
        }
    }
}

@Composable
private fun FalseCognateTrapAlert(trap: FalseCognateEntry) {
    val colors = CalqueTheme.colors

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .background(
                    color = colors.falseCognateAmberFill,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = "⚠️ False Cognate Alert: Not \"${trap.apparentEnglish}\"!",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colors.falseCognateAmber,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text =
                "Actual meaning: ${trap.actualEnglish}\n" +
                    "To say \"${trap.apparentEnglish}\": use \"${trap.realSpanishForApparent}\"\n" +
                    "Tip: ${trap.disambiguationTip}",
            fontSize = 12.sp,
            color = colors.textPrimary,
        )
    }
}
