package com.anysoftkeyboard.calque.ui.theme

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
        )
}
