package com.anysoftkeyboard.calque.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.anysoftkeyboard.calque.data.models.CuratedLesson
import com.anysoftkeyboard.calque.ui.theme.CalqueTheme

/** Curriculum landing screen listing structured syntactic progression modules. */
@Composable
fun CurriculumScreen(
    lessons: List<CuratedLesson>,
    onSelectLesson: (CuratedLesson) -> Unit,
    onStartCustomText: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CalqueTheme.colors

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        // Curriculum Header
        Text(
            text = "Syntactic Progression Path",
            fontSize = 18.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Decode Spanish naturally using cognates, syntax patterns, and glosses.",
            fontSize = 13.sp,
            color = colors.textSecondary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Starter Card
        Card(
            modifier =
                Modifier.fillMaxWidth().clickable {
                    onStartCustomText(
                        "Sentence Zero",
                        "El hotel moderno es excelente y la música es fantástica.",
                    )
                },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = "📖 Try Sample: Sentence Zero",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.slateBlueAccent,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "El hotel moderno es excelente y la música es fantástica.",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lessons List
        if (lessons.isEmpty()) {
            Text(
                text = "No curated lessons available in database.",
                fontSize = 14.sp,
                color = colors.textSecondary,
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(vertical = 32.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally),
            )
        } else {
            for (lesson in lessons) {
                Card(
                    modifier =
                        Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable {
                            onSelectLesson(lesson)
                        },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(
                            text = "LEVEL ${lesson.level} • MODULE ${lesson.sequenceOrder}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.cognateEmerald,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = lesson.title,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Focus: ${lesson.targetConcept}",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}
