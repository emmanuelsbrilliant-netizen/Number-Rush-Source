package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserStatsRepository(
    private val userStatsDao: UserStatsDao,
    private val context: Context? = null
) {

    private val prefs: SharedPreferences? = context?.getSharedPreferences("number_rush_prefs", Context.MODE_PRIVATE)

    val userStatsFlow: Flow<UserStats> = userStatsDao.getUserStatsFlow().map { stats ->
        val currentStats = stats ?: UserStats()
        val isProInPrefs = prefs?.getBoolean("is_pro_user", false) ?: false
        if (isProInPrefs && !currentStats.proStatus) {
            currentStats.copy(proStatus = true)
        } else {
            currentStats
        }
    }

    suspend fun getOrCreateStats(): UserStats {
        val current = userStatsDao.getUserStats() ?: UserStats().also {
            userStatsDao.insertOrUpdate(it)
        }
        val isProInPrefs = prefs?.getBoolean("is_pro_user", false) ?: false
        return if (isProInPrefs && !current.proStatus) {
            val updated = current.copy(proStatus = true)
            userStatsDao.insertOrUpdate(updated)
            updated
        } else {
            current
        }
    }

    suspend fun updateStats(stats: UserStats) {
        if (stats.proStatus) {
            prefs?.edit()?.putBoolean("is_pro_user", true)?.apply()
        }
        userStatsDao.insertOrUpdate(stats)
    }

    suspend fun unlockPro() {
        prefs?.edit()?.putBoolean("is_pro_user", true)?.apply()
        val current = getOrCreateStats()
        // Unlocking pro grants unlimited lives, removes ads, and unlocks all themes
        val updated = current.copy(
            proStatus = true,
            unlockedThemes = if (current.unlockedThemes.contains("Tree-Frame")) {
                current.unlockedThemes
            } else {
                "${current.unlockedThemes},Tree-Frame,Cosmic Dust,Lantern Glow"
            }
        )
        userStatsDao.insertOrUpdate(updated)
    }

    suspend fun selectTheme(themeName: String) {
        val current = getOrCreateStats()
        if (current.isThemeUnlocked(themeName)) {
            userStatsDao.insertOrUpdate(current.copy(selectedTheme = themeName))
        }
    }

    /**
     * Resets daily lives count back to 0 if a new calendar day is detected.
     */
    suspend fun checkAndResetDailyLives(): UserStats {
        val current = getOrCreateStats()
        val today = DateHelper.getTodayString()
        return if (current.lastLivesResetDate != today) {
            val updated = current.copy(
                livesPlayedToday = 0,
                lastLivesResetDate = today
            )
            userStatsDao.insertOrUpdate(updated)
            updated
        } else {
            current
        }
    }

    /**
     * Checks if the streak was broken because a full day was missed.
     */
    suspend fun checkStreakOnLaunch(): UserStats {
        val current = getOrCreateStats()
        val today = DateHelper.getTodayString()
        if (current.lastPlayDate.isEmpty()) return current

        val diff = DateHelper.getDaysDifference(current.lastPlayDate, today)
        return if (diff > 1) {
            // Missed a day: reset streak to 0
            val updated = current.copy(streak = 0)
            userStatsDao.insertOrUpdate(updated)
            updated
        } else {
            current
        }
    }

    /**
     * Updates statistics on round completion.
     * @param isCorrect whether the player's equation was correct.
     * @param solveTimeMs time taken to solve the puzzle in milliseconds.
     */
    suspend fun recordRound(isCorrect: Boolean, solveTimeMs: Long, scoreEarned: Int) {
        val current = getOrCreateStats()
        val today = DateHelper.getTodayString()

        // 1. Calculate streak
        var newStreak = current.streak
        var newLastPlayDate = current.lastPlayDate
        var unlockedTreeFramePopup = false
        var newlyUnlockedThemes = current.unlockedThemes

        if (isCorrect) {
            if (current.lastPlayDate != today) {
                if (current.lastPlayDate == DateHelper.getYesterdayString()) {
                    newStreak += 1
                } else {
                    newStreak = 1
                }
                newLastPlayDate = today

                // Check 7 Day Streak Theme Unlock
                if (newStreak == 7 && !current.unlockedThemes.contains("Tree-Frame")) {
                    newlyUnlockedThemes = if (current.unlockedThemes.isEmpty()) "Tree-Frame" else "${current.unlockedThemes},Tree-Frame"
                    unlockedTreeFramePopup = true
                }
            }
        }

        // 2. High Score and Stat Tracking
        val newHighScore = if (scoreEarned > current.highScore) scoreEarned else current.highScore
        val newTotalAnswers = current.totalAnswers + 1
        val newCorrectAnswers = current.correctAnswers + (if (isCorrect) 1 else 0)
        val newAccuracy = if (newTotalAnswers > 0) {
            (newCorrectAnswers.toFloat() / newTotalAnswers.toFloat()) * 100f
        } else {
            100f
        }

        val newTotalSolveTime = current.totalSolveTime + (if (isCorrect) solveTimeMs else 0L)
        val newAvgSolveTime = if (newCorrectAnswers > 0) {
            newTotalSolveTime.toFloat() / newCorrectAnswers.toFloat() / 1000f // in seconds
        } else {
            0f
        }

        val updated = current.copy(
            highScore = newHighScore,
            streak = newStreak,
            lastPlayDate = newLastPlayDate,
            unlockedThemes = newlyUnlockedThemes,
            totalAnswers = newTotalAnswers,
            correctAnswers = newCorrectAnswers,
            accuracy = newAccuracy,
            totalSolveTime = newTotalSolveTime,
            avgSolveTime = newAvgSolveTime
        )

        userStatsDao.insertOrUpdate(updated)
    }

    suspend fun updateLivesPlayed(increment: Int) {
        val current = getOrCreateStats()
        val today = DateHelper.getTodayString()
        userStatsDao.insertOrUpdate(
            current.copy(
                livesPlayedToday = current.livesPlayedToday + increment,
                lastLivesResetDate = today
            )
        )
    }

    suspend fun updateBestCombo(combo: Int) {
        val current = getOrCreateStats()
        if (combo > current.bestCombo) {
            userStatsDao.insertOrUpdate(current.copy(bestCombo = combo))
        }
    }
}
