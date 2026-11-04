package com.anysoftkeyboard.calque.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Color palette specification defined in docs/DESIGN.md for Calque. */
data class CalquePalette(
    val background: Int,
    val surface: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val slateBlueAccent: Int,
    val cognateEmerald: Int,
    val falseCognateAmber: Int,
    val vaultIndigo: Int,
    val border: Int,
    val isDark: Boolean,
    val backgroundSecondary: Int = if (isDark) 0xFF1F1E1B.toInt() else 0xFFF3EFE6.toInt(),
    val surfaceElevated: Int = if (isDark) 0xFF2A2925.toInt() else 0xFFFFFFFF.toInt(),
    val cognateEmeraldFill: Int = if (isDark) 0xFF133326.toInt() else 0xFFE6F4EE.toInt(),
    val falseCognateAmberFill: Int = if (isDark) 0xFF3A230A.toInt() else 0xFFFFF3E0.toInt(),
    val vaultIndigoFill: Int = if (isDark) 0xFF1F2547.toInt() else 0xFFECEFF9.toInt(),
    val errorCrimson: Int = if (isDark) 0xFFFFB4AB.toInt() else 0xFFBA1A1A.toInt(),
    val errorCrimsonFill: Int = if (isDark) 0xFF410E0B.toInt() else 0xFFFFDAD6.toInt(),
) {
    fun toTokens(): CalqueColorTokens =
        CalqueColorTokens(
            background = Color(background),
            backgroundSecondary = Color(backgroundSecondary),
            surface = Color(surface),
            surfaceElevated = Color(surfaceElevated),
            textPrimary = Color(textPrimary),
            textSecondary = Color(textSecondary),
            slateBlueAccent = Color(slateBlueAccent),
            cognateEmerald = Color(cognateEmerald),
            cognateEmeraldFill = Color(cognateEmeraldFill),
            falseCognateAmber = Color(falseCognateAmber),
            falseCognateAmberFill = Color(falseCognateAmberFill),
            vaultIndigo = Color(vaultIndigo),
            vaultIndigoFill = Color(vaultIndigoFill),
            border = Color(border),
            errorCrimson = Color(errorCrimson),
            errorCrimsonFill = Color(errorCrimsonFill),
            isDark = isDark,
        )
}

/** Design system color tokens holding Compose Color instances. */
@Immutable
data class CalqueColorTokens(
    val background: Color,
    val backgroundSecondary: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val slateBlueAccent: Color,
    val cognateEmerald: Color,
    val cognateEmeraldFill: Color,
    val falseCognateAmber: Color,
    val falseCognateAmberFill: Color,
    val vaultIndigo: Color,
    val vaultIndigoFill: Color,
    val border: Color,
    val errorCrimson: Color,
    val errorCrimsonFill: Color,
    val isDark: Boolean,
)

/** Design system color tokens for Warm Paper light mode and Late Night Study dark mode. */
object CalqueColors {
    val Light =
        CalquePalette(
            background = 0xFFFBF9F5.toInt(),
            surface = 0xFFFFFFFF.toInt(),
            textPrimary = 0xFF1C1B18.toInt(),
            textSecondary = 0xFF6E6A61.toInt(),
            slateBlueAccent = 0xFF2B5B84.toInt(),
            cognateEmerald = 0xFF1E7A56.toInt(),
            falseCognateAmber = 0xFFB85D00.toInt(),
            vaultIndigo = 0xFF4A55A2.toInt(),
            border = 0xFFE5E0D8.toInt(),
            isDark = false,
            backgroundSecondary = 0xFFF3EFE6.toInt(),
            surfaceElevated = 0xFFFFFFFF.toInt(),
            cognateEmeraldFill = 0xFFE6F4EE.toInt(),
            falseCognateAmberFill = 0xFFFFF3E0.toInt(),
            vaultIndigoFill = 0xFFECEFF9.toInt(),
            errorCrimson = 0xFFBA1A1A.toInt(),
            errorCrimsonFill = 0xFFFFDAD6.toInt(),
        )

    val Dark =
        CalquePalette(
            background = 0xFF141413.toInt(),
            surface = 0xFF1E1D1B.toInt(),
            textPrimary = 0xFFEAE6DF.toInt(),
            textSecondary = 0xFF9E9A90.toInt(),
            slateBlueAccent = 0xFF6B9AC4.toInt(),
            cognateEmerald = 0xFF34D399.toInt(),
            falseCognateAmber = 0xFFF59E0B.toInt(),
            vaultIndigo = 0xFF818CF8.toInt(),
            border = 0xFF2E2C29.toInt(),
            isDark = true,
            backgroundSecondary = 0xFF1F1E1B.toInt(),
            surfaceElevated = 0xFF2A2925.toInt(),
            cognateEmeraldFill = 0xFF133326.toInt(),
            falseCognateAmberFill = 0xFF3A230A.toInt(),
            vaultIndigoFill = 0xFF1F2547.toInt(),
            errorCrimson = 0xFFFFB4AB.toInt(),
            errorCrimsonFill = 0xFF410E0B.toInt(),
        )
}
