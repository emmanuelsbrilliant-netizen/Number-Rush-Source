package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val highScore: Int = 0,
    val streak: Int = 0,
    val lastPlayDate: String = "",
    val proStatus: Boolean = false,
    val unlockedThemes: String = "Classic Ink", // Comma-separated list of unlocked themes: "Classic Ink", "Tree-Frame", etc.
    val selectedTheme: String = "Classic Ink", // Classic Ink, Tree-Frame, Cosmic Dust, Lantern Glow
    val avgSolveTime: Float = 0f,
    val accuracy: Float = 100f,
    val bestCombo: Int = 0,
    val totalAnswers: Int = 0,
    val correctAnswers: Int = 0,
    val totalSolveTime: Long = 0L,
    val livesPlayedToday: Int = 0,
    val lastLivesResetDate: String = ""
) {
    fun isThemeUnlocked(themeName: String): Boolean {
        if (themeName == "Classic Ink") return true
        if (proStatus && (themeName == "Cosmic Dust" || themeName == "Lantern Glow" || themeName == "Tree-Frame")) return true
        return unlockedThemes.split(",").map { it.trim() }.contains(themeName)
    }
}
