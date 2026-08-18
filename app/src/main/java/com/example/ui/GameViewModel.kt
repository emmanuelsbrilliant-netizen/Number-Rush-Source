package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundSynthesizer
import com.example.data.DateHelper
import com.example.data.UserStats
import com.example.data.UserStatsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Random

enum class GameScreen {
    Menu,
    Gameplay,
    GameOver,
    Paywall,
    Settings,
    AdInterstitial
}

enum class GameMode {
    Survival, // Standard with 10s countdown timer
    Zen,      // PRO Only: no timer, relaxed math
    Daily     // PRO Only: seeded random, play once daily
}

data class NumberBubble(
    val id: Int,
    val value: Int,
    var isUsed: Boolean = false
)

class GameViewModel(private val repository: UserStatsRepository) : ViewModel() {

    // Global Reactive Stats State
    val statsState: StateFlow<UserStats> = repository.userStatsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserStats()
        )

    // Screen navigation
    var currentScreen by mutableStateOf(GameScreen.Menu)
        private set

    // Game Mode
    var activeMode by mutableStateOf(GameMode.Survival)
        private set

    // Active Game Session Variables
    var currentScore by mutableStateOf(0)
        private set

    var currentCombo by mutableStateOf(0)
        private set

    var currentLives by mutableStateOf(3)
        private set

    var timerSeconds by mutableStateOf(10f)
        private set

    var targetNumber by mutableStateOf(0)
        private set

    var bubbles by mutableStateOf<List<NumberBubble>>(emptyList())
        private set

    var equationTokens by mutableStateOf<List<String>>(emptyList())
        private set

    // Shaking feedback & feedback text
    var shakeTrigger by mutableStateOf(0)
        private set

    var feedbackMessage by mutableStateOf("")
        private set

    var equationSuccess by mutableStateOf(false)
        private set

    // Theme Unlock & Popup State
    var showStreakUnlockPopup by mutableStateOf(false)
    var justUnlockedThemeName by mutableStateOf("")

    // Ad interstitial parameters
    var adRemainingSeconds by mutableStateOf(3)
        private set
    var roundsPlayedInSession = 0

    // Stats session recording
    private var solveStartTime = 0L

    // Timer coroutine job
    private var timerJob: Job? = null

    // Seeded generator for Daily Challenge
    private var dailyRandom: Random? = null
    private var dailyRoundCount = 0

    init {
        viewModelScope.launch {
            // Check streak resets on app start
            repository.checkStreakOnLaunch()
            // Reset daily lives count
            repository.checkAndResetDailyLives()
        }
    }

    fun navigateTo(screen: GameScreen) {
        SoundSynthesizer.playTap()
        
        // Block non-PRO users from Zen / Daily
        if (screen == GameScreen.Gameplay && !statsState.value.proStatus) {
            if (activeMode == GameMode.Zen || activeMode == GameMode.Daily) {
                currentScreen = GameScreen.Paywall
                return
            }
        }

        currentScreen = screen
        if (screen == GameScreen.Menu) {
            stopGameTimer()
        }
    }

    fun selectTheme(themeName: String) {
        viewModelScope.launch {
            repository.selectTheme(themeName)
            SoundSynthesizer.playTap()
        }
    }

    fun unlockPro() {
        viewModelScope.launch {
            repository.unlockPro()
            SoundSynthesizer.playCorrect()
        }
    }

    fun setGameModeAndStart(mode: GameMode) {
        SoundSynthesizer.playTap()
        activeMode = mode
        
        val stats = statsState.value
        if (!stats.proStatus) {
            // Check Zen / Daily restriction
            if (mode == GameMode.Zen || mode == GameMode.Daily) {
                currentScreen = GameScreen.Paywall
                return
            }
            // Check daily limit for free players: max 3 lives played today (i.e. if livesPlayedToday >= 3 and current session starts)
            if (stats.livesPlayedToday >= 3) {
                currentScreen = GameScreen.Paywall
                return
            }
        }

        startNewGame()
    }

    private fun startNewGame() {
        currentScore = 0
        currentCombo = 0
        currentLives = 3
        roundsPlayedInSession = 0
        feedbackMessage = ""
        equationSuccess = false

        if (activeMode == GameMode.Daily) {
            // Seed the generator with today's date
            val seed = (DateHelper.getTodayString() + "numberrush").hashCode().toLong()
            dailyRandom = Random(seed)
            dailyRoundCount = 0
        } else {
            dailyRandom = null
        }

        // Increment lives played count today for free players
        if (!statsState.value.proStatus) {
            viewModelScope.launch {
                repository.updateLivesPlayed(1)
            }
        }

        spawnNewTargetBoard()
        currentScreen = GameScreen.Gameplay
    }

    private fun spawnNewTargetBoard() {
        equationTokens = emptyList()
        feedbackMessage = ""
        equationSuccess = false
        solveStartTime = System.currentTimeMillis()

        val isPro = statsState.value.proStatus
        val streak = statsState.value.streak

        // Determine operator pool
        val hasMulDiv = streak >= 4 || isPro
        val operators = if (hasMulDiv) listOf("+", "-", "*", "/") else listOf("+", "-")

        var generatedTarget = 0
        val generatedBubbles = mutableListOf<Int>()
        var successGen = false

        val rand = dailyRandom ?: Random()

        // Loop to generate a valid, fair math equation to find our target
        for (attempt in 1..30) {
            val termCount = rand.nextInt(2) + 2 // 2 or 3 terms
            val terms = List(termCount) { rand.nextInt(20) + 1 } // numbers 1-20
            val ops = List(termCount - 1) { operators[rand.nextInt(operators.size)] }

            // Build tokens
            val exprTokens = mutableListOf<String>()
            for (i in 0 until termCount) {
                exprTokens.add(terms[i].toString())
                if (i < termCount - 1) exprTokens.add(ops[i])
            }

            val result = evaluateEquation(exprTokens)
            if (result != null && result >= 10.0 && result <= 100.0 && result % 1.0 == 0.0) {
                generatedTarget = result.toInt()
                generatedBubbles.addAll(terms)
                successGen = true
                break
            }
        }

        if (!successGen) {
            // Fallback
            generatedTarget = rand.nextInt(91) + 10 // 10 to 100
            generatedBubbles.add(rand.nextInt(20) + 1)
            generatedBubbles.add(rand.nextInt(20) + 1)
            generatedBubbles.add(rand.nextInt(20) + 1)
        }

        // Fill remaining spaces up to 6 bubbles
        while (generatedBubbles.size < 6) {
            generatedBubbles.add(rand.nextInt(20) + 1)
        }

        // Shuffle bubbles
        generatedBubbles.shuffle(rand)

        // Map to NumberBubble entities
        bubbles = generatedBubbles.mapIndexed { idx, value ->
            NumberBubble(id = idx, value = value, isUsed = false)
        }

        targetNumber = generatedTarget

        // Reset timer
        if (activeMode == GameMode.Survival) {
            timerSeconds = 10f
            startGameTimer()
        } else {
            stopGameTimer()
        }
    }

    private fun startGameTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (timerSeconds > 0) {
                delay(100)
                timerSeconds -= 0.1f
            }
            timerSeconds = 0f
            handleTimeoutOrWrong()
        }
    }

    private fun stopGameTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun handleTimeoutOrWrong() {
        SoundSynthesizer.playWrong()
        shakeTrigger++
        currentCombo = 0
        currentLives--
        feedbackMessage = "TIME OUT! -1 LIFE"

        viewModelScope.launch {
            repository.recordRound(isCorrect = false, solveTimeMs = 0, scoreEarned = currentScore)
        }

        if (currentLives <= 0) {
            endGame()
        } else {
            // Spawn new target after a short display delay
            viewModelScope.launch {
                delay(1200)
                spawnNewTargetBoard()
            }
        }
    }

    fun tapBubble(index: Int) {
        if (equationSuccess) return
        val bubble = bubbles[index]
        if (bubble.isUsed) return

        // Tap number is valid if equation is empty or ends with an operator
        val lastToken = equationTokens.lastOrNull()
        if (lastToken == null || isOperator(lastToken)) {
            SoundSynthesizer.playTap()
            // Modify bubble and list
            val newBubbles = bubbles.mapIndexed { idx, item ->
                if (idx == index) item.copy(isUsed = true) else item
            }
            bubbles = newBubbles
            equationTokens = equationTokens + bubble.value.toString()
        } else {
            // Shake or show error
            shakeTrigger++
            feedbackMessage = "TAP OPERATOR FIRST!"
        }
    }

    fun tapOperator(op: String) {
        if (equationSuccess) return
        val lastToken = equationTokens.lastOrNull()

        // Operator is valid only if last token is a number
        if (lastToken != null && !isOperator(lastToken)) {
            SoundSynthesizer.playTap()
            equationTokens = equationTokens + op
        } else {
            shakeTrigger++
            feedbackMessage = "TAP A NUMBER FIRST!"
        }
    }

    fun undo() {
        if (equationSuccess) return
        val lastToken = equationTokens.lastOrNull() ?: return
        SoundSynthesizer.playTap()

        if (!isOperator(lastToken)) {
            // Reactivate the number in bubbles
            val numVal = lastToken.toIntOrNull()
            if (numVal != null) {
                // Find first used bubble with this value and reactivate it
                var reactivated = false
                bubbles = bubbles.map { bubble ->
                    if (!reactivated && bubble.isUsed && bubble.value == numVal) {
                        reactivated = true
                        bubble.copy(isUsed = false)
                    } else {
                        bubble
                    }
                }
            }
        }

        equationTokens = equationTokens.dropLast(1)
        feedbackMessage = ""
    }

    fun clearEquation() {
        if (equationSuccess) return
        SoundSynthesizer.playTap()
        bubbles = bubbles.map { it.copy(isUsed = false) }
        equationTokens = emptyList()
        feedbackMessage = ""
    }

    fun submitEquation() {
        if (equationSuccess) return
        val finalVal = evaluateEquation(equationTokens)

        if (finalVal != null && finalVal.toInt() == targetNumber) {
            handleCorrectEquation()
        } else {
            handleIncorrectEquation()
        }
    }

    private fun handleCorrectEquation() {
        stopGameTimer()
        SoundSynthesizer.playCorrect()
        equationSuccess = true
        feedbackMessage = "CORRECT! +100 PTS"

        // Calculate score with Streak Multiplier
        // Day 1-3: 2x points multiplier
        val currentStreak = statsState.value.streak
        val multiplier = if (currentStreak in 1..3) 2 else 1
        val points = 100 * multiplier

        currentScore += points
        currentCombo += 1
        roundsPlayedInSession++

        val solveTimeMs = System.currentTimeMillis() - solveStartTime

        viewModelScope.launch {
            // Update streak, best combo, highscore
            val prevStreak = statsState.value.streak
            repository.recordRound(isCorrect = true, solveTimeMs = solveTimeMs, scoreEarned = currentScore)
            repository.updateBestCombo(currentCombo)

            // Celebrate Day 7 Streak theme unlock popup
            val freshStats = statsState.value
            if (freshStats.streak == 7 && prevStreak < 7) {
                justUnlockedThemeName = "Tree-Frame"
                showStreakUnlockPopup = true
            }

            delay(1000)

            spawnNewTargetBoard()
        }
    }

    private fun handleIncorrectEquation() {
        SoundSynthesizer.playWrong()
        shakeTrigger++
        currentCombo = 0
        currentLives--
        feedbackMessage = "WRONG EQUATION! -1 LIFE"

        if (currentLives <= 0) {
            endGame()
        } else {
            // Keep the same board, let them clear or retry
            // Or let them try again
        }
    }

    fun grantRewardedChancesAndResume() {
        SoundSynthesizer.playCorrect()
        currentLives = 3
        feedbackMessage = "+3 LIVES RESTORED!"
        currentScreen = GameScreen.Gameplay
        if (activeMode == GameMode.Survival) {
            timerSeconds = 10f
            startGameTimer()
        }
    }

    fun grantRewardedLifeAndResume() {
        grantRewardedChancesAndResume()
    }

    private fun startAdInterstitial() {
        adRemainingSeconds = 3
        currentScreen = GameScreen.AdInterstitial
        viewModelScope.launch {
            while (adRemainingSeconds > 0) {
                delay(1000)
                adRemainingSeconds--
            }
            // Ad completed, resume play
            spawnNewTargetBoard()
            currentScreen = GameScreen.Gameplay
        }
    }

    private fun endGame() {
        stopGameTimer()
        SoundSynthesizer.playGameOver()
        currentScreen = GameScreen.GameOver
    }

    private fun isOperator(token: String): Boolean {
        return token == "+" || token == "-" || token == "*" || token == "/"
    }

    // Mathematical evaluation supporting operator precedence
    fun evaluateEquation(tokens: List<String>): Double? {
        if (tokens.isEmpty()) return null
        val cleanTokens = if (isOperator(tokens.last())) tokens.dropLast(1) else tokens
        if (cleanTokens.isEmpty()) return null

        try {
            // Step 1: Precedence of * and /
            val step1 = mutableListOf<String>()
            var i = 0
            while (i < cleanTokens.size) {
                val token = cleanTokens[i]
                if (token == "*" || token == "/") {
                    if (step1.isEmpty() || i + 1 >= cleanTokens.size) return null
                    val prevVal = step1.removeAt(step1.size - 1).toDouble()
                    val nextVal = cleanTokens[i + 1].toDouble()
                    val result = if (token == "*") prevVal * nextVal else {
                        if (nextVal == 0.0) return null // Prevent division by zero
                        prevVal / nextVal
                    }
                    step1.add(result.toString())
                    i += 2
                } else {
                    step1.add(token)
                    i++
                }
            }

            // Step 2: Handle + and -
            if (step1.isEmpty()) return null
            var result = step1[0].toDouble()
            var j = 1
            while (j < step1.size) {
                val op = step1[j]
                if (j + 1 >= step1.size) return null
                val nextVal = step1[j + 1].toDouble()
                if (op == "+") {
                    result += nextVal
                } else if (op == "-") {
                    result -= nextVal
                } else {
                    return null
                }
                j += 2
            }
            return result
        } catch (e: Exception) {
            return null
        }
    }
}

@Suppress("UNCHECKED_CAST")
class GameViewModelFactory(private val repository: UserStatsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            return GameViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
