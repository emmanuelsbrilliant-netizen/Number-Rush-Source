package com.example

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import com.example.ads.UnityAdsManager
import com.example.audio.SoundSynthesizer
import com.example.data.AppDatabase
import com.example.data.DateHelper
import com.example.data.UserStats
import com.example.data.UserStatsRepository
import com.example.ui.GameMode
import com.example.ui.GameScreen
import com.example.ui.GameViewModel
import com.example.ui.GameViewModelFactory
import com.example.ui.theme.InkBlack
import com.example.ui.theme.InkGold
import com.example.ui.theme.InkGrayDark
import com.example.ui.theme.InkGrayLight
import com.example.ui.theme.InkGreen
import com.example.ui.theme.InkRed
import com.example.ui.theme.InkWhite
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Unity Ads SDK
        UnityAdsManager.initialize(this)

        // Initialize Room DB and Repository
        val database = AppDatabase.getDatabase(this)
        val repository = UserStatsRepository(database.userStatsDao(), this)
        
        // Instantiate ViewModel
        val viewModel = ViewModelProvider(
            this,
            GameViewModelFactory(repository)
        )[GameViewModel::class.java]

        setContent {
            MyApplicationTheme {
                NumberRushApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun NumberRushApp(viewModel: GameViewModel) {
    val stats by viewModel.statsState.collectAsState()
    val currentScreen = viewModel.currentScreen
    val shakeTrigger = viewModel.shakeTrigger
    val context = LocalContext.current

    // Screen Shake Animation offset
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0) {
            // High intensity spring oscillation
            repeat(5) { i ->
                val sign = if (i % 2 == 0) 1 else -1
                shakeOffset.animateTo(
                    targetValue = sign * 18f * (5 - i) / 5f,
                    animationSpec = spring(stiffness = 2000f)
                )
            }
            shakeOffset.animateTo(0f)
        }
    }

    // Root layout wrapped with gothic frame, responsive and offset-shaken
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBlack)
            .offset { IntOffset(shakeOffset.value.toInt(), 0) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        GothicInkFrame(
            theme = stats.selectedTheme,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Main Switchboard for Screens
                when (currentScreen) {
                    GameScreen.Menu -> MenuScreen(viewModel, stats)
                    GameScreen.Gameplay -> GameplayScreen(viewModel, stats)
                    GameScreen.GameOver -> GameOverScreen(viewModel, stats)
                    GameScreen.Paywall -> PaywallScreen(viewModel, stats)
                    GameScreen.Settings -> SettingsScreen(viewModel, stats)
                    GameScreen.AdInterstitial -> AdInterstitialScreen(viewModel)
                }

                // 7-day streak popup unlock alert
                if (viewModel.showStreakUnlockPopup) {
                    StreakUnlockDialog(
                        themeName = viewModel.justUnlockedThemeName,
                        onDismiss = {
                            viewModel.showStreakUnlockPopup = false
                            Toast.makeText(context, "Theme unlocked and equipped!", Toast.LENGTH_SHORT).show()
                            viewModel.selectTheme("Tree-Frame")
                        }
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 1: MAIN MENU
// ==========================================
@Composable
fun MenuScreen(viewModel: GameViewModel, stats: UserStats) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 500.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP UTILITIES BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Streak counter with Fire Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .border(BorderStroke(1.5.dp, InkWhite), RoundedCornerShape(0.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("streak_display")
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Streak Fire",
                    tint = if (stats.streak > 0) InkGold else InkWhite,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${stats.streak} DAYS",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = if (stats.streak > 0) InkGold else InkWhite
                )
            }

            // PRO indicator or GO PRO trigger
            if (stats.proStatus) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(InkGrayLight, RoundedCornerShape(0.dp))
                        .border(BorderStroke(1.5.dp, InkGold), RoundedCornerShape(0.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Pro Status",
                        tint = InkGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PRO MEMBER",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = InkGold
                    )
                }
            } else {
                InkButton(
                    text = "GO PRO",
                    onClick = { viewModel.navigateTo(GameScreen.Paywall) },
                    borderColor = InkGold,
                    textColor = InkGold,
                    modifier = Modifier.height(36.dp),
                    testTag = "go_pro_menu_button"
                )
            }
        }

        // CENTER TITLE AND ART SKETCH
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "NUMBER RUSH",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 38.sp,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "GOTHIC MATH PUZZLE RUNNER",
                fontSize = 11.sp,
                letterSpacing = 4.sp,
                color = InkWhite.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Hand-sketched interactive artwork
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .border(BorderStroke(1.5.dp, InkWhite), CircleShape)
                    .padding(8.dp)
                    .background(InkGrayDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    // Draw heavy crosshatch shadow lines inside art bubble
                    for (i in -40..40 step 8) {
                        drawLine(
                            color = InkWhite.copy(alpha = 0.15f),
                            start = Offset(center.x + i, center.y - r),
                            end = Offset(center.x + i - 40, center.y + r),
                            strokeWidth = 1f
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Rush",
                        fontSize = 32.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = InkWhite
                    )
                    Text(
                        text = "High Score: ${stats.highScore}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Serif,
                        color = InkGold
                    )
                }
            }
        }

        // BOTTOM MODE NAVIGATION BUTTONS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. SURVIVAL MODE (Free)
            InkButton(
                text = "SURVIVAL MODE",
                subText = "10s math rush • 3 lives",
                onClick = { viewModel.setGameModeAndStart(GameMode.Survival) },
                borderColor = InkWhite,
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = InkBlack) },
                modifier = Modifier.fillMaxWidth(0.95f),
                textColor = InkBlack, // Solid inverted visual prominence
                testTag = "play_survival_button"
            )

            // 2. ZEN MODE (PRO)
            InkButton(
                text = "ZEN MODE",
                subText = if (stats.proStatus) "No timer • Relaxed arithmetic" else "PRO ONLY • Relaxed arithmetic",
                onClick = { viewModel.setGameModeAndStart(GameMode.Zen) },
                borderColor = if (stats.proStatus) InkWhite else InkWhite.copy(alpha = 0.5f),
                textColor = if (stats.proStatus) InkWhite else InkWhite.copy(alpha = 0.5f),
                icon = { 
                    Icon(
                        imageVector = if (stats.proStatus) Icons.Default.Star else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (stats.proStatus) InkGold else InkWhite.copy(alpha = 0.5f)
                    ) 
                },
                modifier = Modifier.fillMaxWidth(0.95f),
                testTag = "play_zen_button"
            )

            // 3. DAILY CHALLENGE (PRO)
            InkButton(
                text = "DAILY CHALLENGE",
                subText = if (stats.proStatus) "Seeded same target for all players" else "PRO ONLY • Seeded same target",
                onClick = { viewModel.setGameModeAndStart(GameMode.Daily) },
                borderColor = if (stats.proStatus) InkWhite else InkWhite.copy(alpha = 0.5f),
                textColor = if (stats.proStatus) InkWhite else InkWhite.copy(alpha = 0.5f),
                icon = { 
                    Icon(
                        imageVector = if (stats.proStatus) Icons.Default.Star else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (stats.proStatus) InkGold else InkWhite.copy(alpha = 0.5f)
                    ) 
                },
                modifier = Modifier.fillMaxWidth(0.95f),
                testTag = "play_daily_button"
            )

            // Horizontal separators and utility settings row
            HorizontalDivider(color = InkWhite.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(0.95f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Theme selector and statistics view toggle
                InkButton(
                    text = "THEMES & STATS",
                    onClick = { viewModel.navigateTo(GameScreen.Settings) },
                    borderColor = InkWhite.copy(alpha = 0.8f),
                    textColor = InkWhite,
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = InkWhite) },
                    modifier = Modifier.weight(1f),
                    testTag = "settings_menu_button"
                )
            }
        }
    }
}

// ==========================================
// SCREEN 2: ACTIVE GAMEPLAY SCREEN
// ==========================================
@Composable
fun GameplayScreen(viewModel: GameViewModel, stats: UserStats) {
    val equationSuccess = viewModel.equationSuccess
    val currentMode = viewModel.activeMode
    val isStreakDoublePoints = stats.streak in 1..3

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 500.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP GAME STATE DASHBOARD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exit back to menu button
            IconButton(
                onClick = { viewModel.navigateTo(GameScreen.Menu) },
                modifier = Modifier
                    .border(BorderStroke(1.dp, InkWhite), CircleShape)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quit Game",
                    tint = InkWhite,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Game Mode label
            Text(
                text = when (currentMode) {
                    GameMode.Survival -> "SURVIVAL MODE"
                    GameMode.Zen -> "★ ZEN PLAY"
                    GameMode.Daily -> "★ DAILY SEED"
                },
                fontSize = 12.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = if (currentMode == GameMode.Survival) InkWhite else InkGold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Remaining life hearts
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .border(BorderStroke(1.dp, InkWhite), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                repeat(3) { index ->
                    Icon(
                        imageVector = if (index < viewModel.currentLives) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Hearts",
                        tint = if (index < viewModel.currentLives) InkRed else InkWhite.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(horizontal = 1.dp)
                    )
                }
            }
        }

        // STATS SUMMARY (SCORE, MULTIPLIER, COMBO)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SCORE",
                    fontSize = 11.sp,
                    color = InkWhite.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Serif
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${viewModel.currentScore}",
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = InkWhite
                    )
                    if (isStreakDoublePoints) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "2X MULTI",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = InkGold,
                            modifier = Modifier
                                .border(BorderStroke(1.dp, InkGold), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (viewModel.currentCombo > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "COMBO STREAK",
                        fontSize = 10.sp,
                        color = InkGold,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = "x${viewModel.currentCombo}",
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = InkGold
                    )
                }
            }
        }

        // CORE PUZZLE CENTERPIECE: TARGET NUMBER AND TIMER
        Box(
            modifier = Modifier
                .size(190.dp)
                .padding(8.dp)
                .drawBehind {
                    val r = size.minDimension / 2f
                    // Glowing/shadow outline rings from HTML shadow-[0_0_20px_rgba(255,255,255,0.2)]
                    drawCircle(color = InkWhite.copy(alpha = 0.04f), radius = r + 14.dp.toPx(), center = center)
                    drawCircle(color = InkWhite.copy(alpha = 0.08f), radius = r + 8.dp.toPx(), center = center)
                    drawCircle(color = InkWhite.copy(alpha = 0.12f), radius = r + 3.dp.toPx(), center = center)
                },
            contentAlignment = Alignment.Center
        ) {
            // Horizontal scanline pattern inside the target circle (from HTML repeating-linear-gradient)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = center.x
                val cy = center.y
                val R = size.minDimension / 2f - 6f
                var y = cy - R
                val step = 6.dp.toPx()
                while (y < cy + R) {
                    val dy = kotlin.math.abs(y - cy)
                    if (dy < R) {
                        val dx = kotlin.math.sqrt(R * R - dy * dy)
                        drawLine(
                            color = InkWhite.copy(alpha = 0.08f),
                            start = Offset(cx - dx, y),
                            end = Offset(cx + dx, y),
                            strokeWidth = 1.2f.dp.toPx()
                        )
                    }
                    y += step
                }
            }

            // Dynamic Ticking Circular Timer Outline
            if (currentMode == GameMode.Survival) {
                val animatedProgress by animateFloatAsState(
                    targetValue = viewModel.timerSeconds / 10f,
                    animationSpec = tween(100, easing = LinearEasing),
                    label = "TimerProgress"
                )
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Gray underlying circle
                    drawCircle(
                        color = InkWhite.copy(alpha = 0.1f),
                        radius = size.minDimension / 2f - 4f,
                        style = Stroke(width = 4f)
                    )
                    // White ticking progress circle
                    drawArc(
                        color = if (viewModel.timerSeconds <= 3f) InkRed else InkWhite,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 6f)
                    )
                }
            } else {
                // Static decorative heavy ink outline for Zen/Daily challenge
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = InkGold.copy(alpha = 0.6f),
                        radius = size.minDimension / 2f - 4f,
                        style = Stroke(width = 3f)
                    )
                }
            }
 
            // Central Target Display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "TARGET",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = InkWhite.copy(alpha = 0.6f),
                    letterSpacing = 3.sp
                )
                Text(
                    text = "${viewModel.targetNumber}",
                    fontSize = 62.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = if (equationSuccess) InkGreen else InkWhite,
                    modifier = Modifier.testTag("target_number")
                )
                if (currentMode == GameMode.Survival) {
                    Text(
                        text = String.format("%.1fs", viewModel.timerSeconds),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        color = if (viewModel.timerSeconds <= 3f) InkRed else InkWhite
                    )
                } else {
                    Text(
                        text = "INFINITY",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Serif,
                        color = InkGold
                    )
                }
            }
        }

        // REAL-TIME EQUATION BUILDER BAR
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Realtime Equation display row - Brutalist White Card from HTML
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        // Thick brutalist shadow at bottom and right (border-b-4 border-r-4 border-gray-600)
                        val shadowW = 5.dp.toPx()
                        // bottom shadow
                        drawRect(
                            color = Color(0xFF666666),
                            topLeft = Offset(shadowW, size.height),
                            size = Size(size.width, shadowW)
                        )
                        // right shadow
                        drawRect(
                            color = Color(0xFF666666),
                            topLeft = Offset(size.width, shadowW),
                            size = Size(shadowW, size.height)
                        )

                        // Rotating diamond vector at top-left corner (from HTML absolute -top-2 -left-2 rotate-45)
                        val dSize = 6.dp.toPx()
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, -dSize)
                            lineTo(dSize, 0f)
                            lineTo(0f, dSize)
                            lineTo(-dSize, 0f)
                            close()
                        }
                        path.translate(Offset(2.dp.toPx(), 2.dp.toPx()))
                        drawPath(path = path, color = InkBlack)
                        drawPath(path = path, color = InkWhite, style = Stroke(width = 1.5.dp.toPx()))
                    }
                    .background(Color.White)
                    .border(BorderStroke(2.dp, Color.Black))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.equationTokens.isEmpty()) {
                    Text(
                        text = "BUILD EQUATION EQUAL TO TARGET",
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        color = Color.Black.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                } else {
                    val formattedExpr = viewModel.equationTokens.joinToString(" ")
                    val evalVal = viewModel.evaluateEquation(viewModel.equationTokens)
                    val evalText = if (evalVal != null) {
                        if (evalVal % 1.0 == 0.0) " = ${evalVal.toInt()}" else " = " + String.format("%.2f", evalVal)
                    } else ""
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = formattedExpr,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = evalText,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100) // Deep warm contrast orange for eval value
                        )
                    }
                }
            }

            // Real-time success or wrong answer message banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.feedbackMessage.isNotEmpty()) {
                    Text(
                        text = viewModel.feedbackMessage,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            viewModel.feedbackMessage.contains("CORRECT") -> InkGreen
                            viewModel.feedbackMessage.contains("WRONG") || viewModel.feedbackMessage.contains("TIME") -> InkRed
                            else -> InkWhite
                        }
                    )
                }
            }
        }

        // TAPPABLE DIGITS AND OPERATORS COMPONENT
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Digits Grid (2 rows of 3)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(viewModel.bubbles) { index, bubble ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(2.1f)
                            .border(
                                BorderStroke(
                                    width = 1.5.dp,
                                    color = if (bubble.isUsed) InkWhite.copy(alpha = 0.2f) else InkWhite
                                ),
                                RoundedCornerShape(0.dp)
                            )
                            .background(
                                if (bubble.isUsed) InkGrayDark else InkBlack
                            )
                            .clickable(enabled = !bubble.isUsed && !equationSuccess) {
                                viewModel.tapBubble(index)
                            }
                            .testTag("bubble_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing crosshatching if selected/used
                        if (bubble.isUsed) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                for (p in 0..100 step 12) {
                                    drawLine(
                                        color = InkWhite.copy(alpha = 0.08f),
                                        start = Offset(p.toFloat(), 0f),
                                        end = Offset(0f, p.toFloat()),
                                        strokeWidth = 1f
                                    )
                                }
                            }
                        }
                        
                        Text(
                            text = "${bubble.value}",
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = if (bubble.isUsed) InkWhite.copy(alpha = 0.2f) else InkWhite
                        )
                    }
                }
            }

            // Operators Row (+, -, *, /)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val operatorsList = listOf("+", "-", "*", "/")
                val isStreakUnlocked = stats.streak >= 4
                val isPro = stats.proStatus
                
                operatorsList.forEach { op ->
                    val isLocked = (op == "*" || op == "/") && !isStreakUnlocked && !isPro
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .border(
                                BorderStroke(
                                    width = 1.5.dp,
                                    color = if (isLocked) InkWhite.copy(alpha = 0.25f) else InkWhite
                                ),
                                RoundedCornerShape(0.dp)
                            )
                            .background(InkBlack)
                            .clickable(enabled = !isLocked && !equationSuccess) {
                                viewModel.tapOperator(op)
                            }
                            .testTag("operator_$op"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked Operator",
                                tint = InkWhite.copy(alpha = 0.3f),
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = op,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = InkWhite
                            )
                        }
                    }
                }
            }
        }

        // LOWER ACTION CONTROLS ROW (CLEAR, UNDO, SUBMIT)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Clear equation button
            IconButton(
                onClick = { viewModel.clearEquation() },
                modifier = Modifier
                    .weight(1.1f)
                    .height(48.dp)
                    .border(BorderStroke(1.5.dp, InkWhite), RoundedCornerShape(0.dp))
                    .testTag("clear_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = InkWhite, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CLEAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkWhite, fontFamily = FontFamily.Serif)
                }
            }

            // Backspace/Undo button
            IconButton(
                onClick = { viewModel.undo() },
                modifier = Modifier
                    .weight(1.1f)
                    .height(48.dp)
                    .border(BorderStroke(1.5.dp, InkWhite), RoundedCornerShape(0.dp))
                    .testTag("undo_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Undo", tint = InkWhite, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("UNDO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkWhite, fontFamily = FontFamily.Serif)
                }
            }

            // Heavy Submit Button (Visual Inverted Outline style)
            Button(
                onClick = { viewModel.submitEquation() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = InkWhite,
                    contentColor = InkBlack
                ),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .weight(1.8f)
                    .height(48.dp)
                    .testTag("submit_button"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = InkBlack, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SUBMIT",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = InkBlack
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3: GAME OVER DIALOG
// ==========================================
@Composable
fun GameOverScreen(viewModel: GameViewModel, stats: UserStats) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        // Pre-load Unity ads for quick readiness
        UnityAdsManager.loadRewardedAd()
        UnityAdsManager.loadInterstitialAd()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 440.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GAME OVER",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 42.sp,
                color = InkRed,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "THE INK RUNS DRY...",
            fontSize = 12.sp,
            letterSpacing = 3.sp,
            color = InkWhite.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 28.dp)
        )

        // SCORE CARD WITH CROSSHATCH SHADOW
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(BorderStroke(2.dp, InkWhite), RoundedCornerShape(12.dp))
                .background(InkGrayDark)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "FINAL SCORE",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif,
                    color = InkWhite.copy(alpha = 0.5f)
                )
                Text(
                    text = "${viewModel.currentScore}",
                    fontSize = 48.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = InkWhite
                )
                
                HorizontalDivider(color = InkWhite.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
                
                Text(
                    text = "Personal Best: ${stats.highScore}",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    color = InkGold
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // FREE LIVES LIMIT OVERLAY UPSELL
        if (!stats.proStatus && stats.livesPlayedToday >= 3) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .border(BorderStroke(1.5.dp, InkGold), RoundedCornerShape(8.dp))
                    .background(InkGrayDark)
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DAILY LIVES EXHAUSTED",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = InkGold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Free players get max 3 lives per day.\nGo PRO for infinite plays, stats & exclusive visual themes!",
                    fontSize = 11.sp,
                    color = InkWhite.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                InkButton(
                    text = "UNLOCK PRO - $3.99",
                    onClick = { viewModel.navigateTo(GameScreen.Paywall) },
                    borderColor = InkGold,
                    textColor = InkGold,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // NAVIGATION ACTIONS ROW
        Column(
            modifier = Modifier.fillMaxWidth(0.9f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val isBlocked = !stats.proStatus && stats.livesPlayedToday >= 3
            
            // SIDE-BY-SIDE BUTTONS: "WATCH AD FOR +3 LIVES" & "TRY AGAIN"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. WATCH AD FOR +3 LIVES (Unity Rewarded Ad, keeps puzzle progress)
                InkButton(
                    text = "Watch Ad for +3 Lives",
                    subText = "Keep Progress 🎬",
                    onClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            UnityAdsManager.showRewardedAd(
                                activity = activity,
                                onRewardEarned = {
                                    activity.runOnUiThread {
                                        viewModel.grantRewardedChancesAndResume()
                                        Toast.makeText(context, "+3 Lives Restored! Resuming puzzle...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onAdClosedOrSkipped = {
                                    activity.runOnUiThread {
                                        Toast.makeText(context, "Watch full ad to get +3 lives.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onAdFailed = { msg ->
                                    activity.runOnUiThread {
                                        Toast.makeText(context, "Ad loading, please try again in a moment...", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        } else {
                            viewModel.grantRewardedChancesAndResume()
                        }
                    },
                    borderColor = InkGold,
                    textColor = InkGold,
                    modifier = Modifier.weight(1f),
                    testTag = "watch_ad_for_lives_button"
                )

                // 2. TRY AGAIN (Unity Interstitial Ad before level restart from scratch)
                InkButton(
                    text = "Try Again",
                    subText = "Restart Level",
                    onClick = {
                        val activity = context as? Activity
                        val restartGame = {
                            activity?.runOnUiThread {
                                viewModel.setGameModeAndStart(viewModel.activeMode)
                            } ?: viewModel.setGameModeAndStart(viewModel.activeMode)
                        }

                        if (activity != null && !stats.proStatus) {
                            UnityAdsManager.showInterstitialAd(
                                activity = activity,
                                onCompleteOrDismissed = {
                                    restartGame()
                                }
                            )
                        } else {
                            restartGame()
                        }
                    },
                    borderColor = if (isBlocked) InkWhite.copy(alpha = 0.3f) else InkWhite,
                    textColor = if (isBlocked) InkWhite.copy(alpha = 0.3f) else InkBlack,
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (isBlocked) InkWhite.copy(alpha = 0.3f) else InkBlack) },
                    testTag = "restart_game_button"
                )
            }

            InkButton(
                text = "MAIN MENU",
                onClick = { viewModel.navigateTo(GameScreen.Menu) },
                borderColor = InkWhite,
                textColor = InkWhite,
                modifier = Modifier.fillMaxWidth(),
                testTag = "back_to_menu_button"
            )
        }
    }
}

// ==========================================
// SCREEN 4: $3.99 PRO PAYWALL SCREEN
// ==========================================
@Composable
fun PaywallScreen(viewModel: GameViewModel, stats: UserStats) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 440.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // HEADER ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(GameScreen.Menu) },
                modifier = Modifier
                    .border(BorderStroke(1.dp, InkWhite), CircleShape)
                    .size(32.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = InkWhite, modifier = Modifier.size(16.dp))
            }
            
            Text(
                text = "PRO MEMBER BENEFITS",
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = InkGold,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(32.dp)) // Anchor balance
        }

        // UPPER DECORATIVE HEADER
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "NUMBER RUSH PRO",
                fontSize = 28.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = InkGold,
                modifier = Modifier.testTag("paywall_title")
            )
            Text(
                text = "✦ UNLEASH THE COMPLETE MATH EXPERIENCE ✦",
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                color = InkWhite.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // BULLETED LIST OF PRO BENEFITS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.5.dp, InkWhite), RoundedCornerShape(10.dp))
                .background(InkGrayDark)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val benefits = listOf(
                "No Ads Ever" to "Pure uninterrupted math flow without waiting timers.",
                "Unlimited Lives" to "Play infinitely daily, bypassing the free 3 lives limit.",
                "Exclusive Zen Mode" to "Solve puzzles with absolutely no stress and no ticking timer.",
                "Daily Seed Challenge" to "Compete on the identical daily board generator with friends.",
                "Full Stat Tracking" to "Analyze your average solve speeds, accuracy, and combo streaks.",
                "Exclusive Ink Themes" to "Unlock beautiful custom backgrounds: Cosmic Dust and Lantern Glow!"
            )

            benefits.forEach { (title, desc) ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "✦ ",
                        fontSize = 14.sp,
                        color = InkGold,
                        fontWeight = FontWeight.Bold
                    )
                    Column {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = InkWhite
                        )
                        Text(
                            text = desc,
                            fontSize = 11.sp,
                            color = InkWhite.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // GOLD GO-PRO PURCHASE BUTTON
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (stats.proStatus) {
                Text(
                    text = "✦ YOU ARE ALREADY A PRO MEMBER! ✦",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = InkGold
                )
            } else {
                Button(
                    onClick = {
                        viewModel.unlockPro()
                        Toast.makeText(context, "Welcome to PRO! All features unlocked.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InkGold,
                        contentColor = InkBlack
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(2.dp, InkWhite),
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(56.dp)
                        .testTag("subscribe_pro_button"),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "UPGRADE TO PRO",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = InkBlack
                        )
                        Text(
                            text = "$3.99/MONTH • CANCEL ANYTIME",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkBlack.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Text(
                    text = "Restore Purchase",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    textDecoration = TextDecoration.Underline,
                    color = InkWhite.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable {
                            Toast.makeText(context, "Purchases Restored.", Toast.LENGTH_SHORT).show()
                        }
                )
            }
        }
    }
}

// ==========================================
// SCREEN 5: SETTINGS & HISTORICAL STATS
// ==========================================
@Composable
fun SettingsScreen(viewModel: GameViewModel, stats: UserStats) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 440.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(GameScreen.Menu) },
                modifier = Modifier
                    .border(BorderStroke(1.dp, InkWhite), CircleShape)
                    .size(32.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = InkWhite, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "THEMES & STATISTICS",
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
        }

        // MAIN OPTIONS LAYOUT Scrollable/Stacked
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: VISUAL THEME SELECTOR
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "COSMETIC SKIN THEME",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = InkGold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val themes = listOf(
                    "Classic Ink" to "Default high-contrast monochrome design",
                    "Tree-Frame" to "Dense branching roots around borders (Day 7 / PRO)",
                    "Cosmic Dust" to "Deep cosmic purples & sparkling dust starflows (PRO)",
                    "Lantern Glow" to "Flickering warm lantern candlelight glow overlays (PRO)"
                )

                themes.forEach { (themeName, desc) ->
                    val isUnlocked = stats.isThemeUnlocked(themeName)
                    val isSelected = stats.selectedTheme == themeName

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(
                                BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) InkGold else if (isUnlocked) InkWhite else InkWhite.copy(alpha = 0.25f)
                                ),
                                RoundedCornerShape(8.dp)
                            )
                            .background(if (isSelected) InkGrayLight else InkBlack)
                            .clickable(enabled = isUnlocked) {
                                viewModel.selectTheme(themeName)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = themeName,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) InkWhite else InkWhite.copy(alpha = 0.4f)
                            )
                            Text(
                                text = desc,
                                fontSize = 10.sp,
                                color = if (isUnlocked) InkWhite.copy(alpha = 0.7f) else InkWhite.copy(alpha = 0.3f)
                            )
                        }

                        if (!isUnlocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Theme Locked",
                                tint = InkWhite.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        } else if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = InkGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // SECTION 2: STAT TRACKING (PRO exclusive analytics overlay)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "HISTORICAL METRICS",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = InkGold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.5.dp, InkWhite), RoundedCornerShape(8.dp))
                        .background(InkGrayDark)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (stats.proStatus) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatRow("Average Solve Speed", String.format("%.2fs", stats.avgSolveTime))
                            StatRow("Equation Accuracy", String.format("%.1f %%", stats.accuracy))
                            StatRow("Highest Combo Peak", "${stats.bestCombo}")
                            StatRow("Puzzles Solved", "${stats.correctAnswers} / ${stats.totalAnswers}")
                        }
                    } else {
                        // Custom heavy shadowed lock cover
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked stats",
                                tint = InkGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "ANALYTICS LOCKED",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = InkGold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Go PRO to analyze your math speed,\naccuracy curves, and peaks.",
                                fontSize = 11.sp,
                                color = InkWhite.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            InkButton(
                                text = "UPGRADE NOW",
                                onClick = { viewModel.navigateTo(GameScreen.Paywall) },
                                borderColor = InkGold,
                                textColor = InkGold,
                                modifier = Modifier.height(34.dp)
                            )
                        }
                    }
                }
            }
        }

        // BACK TO MENU AT THE BOTTOM
        InkButton(
            text = "BACK TO MENU",
            onClick = { viewModel.navigateTo(GameScreen.Menu) },
            borderColor = InkWhite,
            textColor = InkWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            testTag = "settings_back_button"
        )
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = InkWhite.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = InkWhite
        )
    }
}

// ==========================================
// SCREEN 6: INTERSTITIAL COUNTDOWN ADS SCREEN
// ==========================================
@Composable
fun AdInterstitialScreen(viewModel: GameViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 440.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "ADVERTISEMENT",
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            color = InkWhite.copy(alpha = 0.4f),
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(top = 16.dp)
        )

        // FUNNY GOTHIC MERCHANT BRAND CARD
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Hand sketch of an inkwell
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .border(BorderStroke(1.5.dp, InkWhite), RoundedCornerShape(8.dp))
                    .background(InkGrayDark),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw inkwell sketch crosshatch lines
                    for (i in 0..120 step 10) {
                        drawLine(
                            color = InkWhite.copy(alpha = 0.1f),
                            start = Offset(i.toFloat(), 0f),
                            end = Offset(120f, (120 - i).toFloat()),
                            strokeWidth = 1f
                        )
                    }
                    // Inkwell cap outline
                    drawRect(
                        color = InkWhite,
                        topLeft = Offset(size.width * 0.35f, size.height * 0.15f),
                        size = Size(size.width * 0.3f, size.height * 0.15f),
                        style = Stroke(width = 2f)
                    )
                }
                Text(
                    text = "INK",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif,
                    color = InkWhite
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "INK-BOTTLERS CO.",
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = InkWhite
            )
            
            Text(
                text = "\"Providing pristine charcoal fluid for all\nyour math and equation logs since 1826.\"",
                fontSize = 11.sp,
                color = InkWhite.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Upgrade to PRO to remove these annoying ads forever!",
                fontSize = 10.sp,
                color = InkGold,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Serif
            )
        }

        // Countdown clock loader at the bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = InkWhite,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Resuming game in ${viewModel.adRemainingSeconds}...",
                fontSize = 12.sp,
                fontFamily = FontFamily.Serif,
                color = InkWhite
            )
        }
    }
}

// ==========================================
// SUB-COMPONENT: REUSABLE GRAPHIC NOVEL FRAME
// ==========================================
@Composable
fun GothicInkFrame(
    theme: String, // "Classic Ink", "Tree-Frame", "Cosmic Dust", "Lantern Glow"
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(InkBlack)
            .drawBehind {
                val width = size.width
                val height = size.height

                // Render repeating 45-degree diagonal ink-line texture in the background (from HTML's repeating-linear-gradient)
                val spacingPx = 12.dp.toPx()
                val lineThickness = 1.dp.toPx()
                val diagonalSum = width + height
                var offset = 0f
                while (offset < diagonalSum) {
                    drawLine(
                        color = InkWhite.copy(alpha = 0.04f),
                        start = Offset(offset, 0f),
                        end = Offset(0f, offset),
                        strokeWidth = lineThickness
                    )
                    offset += spacingPx
                }

                // Render radial gradient fading towards the corners (from HTML's radial-gradient)
                val radialBrush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(Color.Transparent, InkBlack.copy(alpha = 0.55f)),
                    center = center,
                    radius = size.maxDimension * 0.75f
                )
                drawRect(brush = radialBrush)

                val lineColor = InkWhite
                val glowColor = when (theme) {
                    "Lantern Glow" -> Color(0xFFFFB300).copy(alpha = 0.22f) // Warm candle amber
                    "Cosmic Dust" -> Color(0xFF8E24AA).copy(alpha = 0.16f)  // Dark nebula violet
                    else -> Color.Transparent
                }

                // Render back atmosphere glow for Lantern/Cosmic
                if (glowColor != Color.Transparent) {
                    drawCircle(
                        color = glowColor,
                        radius = size.minDimension * 0.65f,
                        center = center
                    )
                }

                // Draw standard geometric high-contrast border
                val outerMargin = 12f
                val strokeW = 3.5f
                
                drawRect(
                    color = lineColor,
                    topLeft = Offset(outerMargin, outerMargin),
                    size = Size(width - outerMargin * 2, height - outerMargin * 2),
                    style = Stroke(width = strokeW)
                )

                // Draw secondary hand-inked line frame
                val innerMargin = 22f
                drawRect(
                    color = lineColor.copy(alpha = 0.4f),
                    topLeft = Offset(innerMargin, innerMargin),
                    size = Size(width - innerMargin * 2, height - innerMargin * 2),
                    style = Stroke(width = 1.5f)
                )

                // Drawing tree framing roots if selected
                if (theme == "Tree-Frame" || theme == "Cosmic Dust" || theme == "Lantern Glow") {
                    val path = androidx.compose.ui.graphics.Path()
                    
                    // Top-Left thick curved limb
                    path.moveTo(0f, 0f)
                    path.cubicTo(width * 0.2f, height * 0.05f, width * 0.05f, height * 0.2f, 0f, height * 0.25f)
                    path.moveTo(0f, 0f)
                    path.cubicTo(width * 0.25f, height * 0.08f, width * 0.08f, height * 0.25f, 0f, height * 0.32f)

                    // Bottom-Right thick root limbs
                    path.moveTo(width, height)
                    path.cubicTo(width * 0.8f, height * 0.95f, width * 0.95f, height * 0.8f, width, height * 0.75f)
                    path.moveTo(width, height)
                    path.cubicTo(width * 0.75f, height * 0.92f, width * 0.92f, height * 0.75f, width, height * 0.68f)

                    // Top-Right thick curved limb
                    path.moveTo(width, 0f)
                    path.cubicTo(width * 0.8f, height * 0.05f, width * 0.95f, height * 0.2f, width, height * 0.25f)

                    // Bottom-Left roots
                    path.moveTo(0f, height)
                    path.cubicTo(width * 0.2f, height * 0.95f, width * 0.05f, height * 0.8f, 0f, height * 0.75f)

                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 3f)
                    )
                }

                // CORNER SHADOW CROSSHATCHING
                // 1. Top Left corner crosshatch
                for (d in 0..110 step 15) {
                    drawLine(
                        color = lineColor.copy(alpha = 0.3f),
                        start = Offset(d.toFloat(), 0f),
                        end = Offset(0f, d.toFloat()),
                        strokeWidth = 1.5f
                    )
                }

                // 2. Bottom Right corner crosshatch
                for (d in 0..110 step 15) {
                    drawLine(
                        color = lineColor.copy(alpha = 0.3f),
                        start = Offset(width - d.toFloat(), height),
                        end = Offset(width, height - d.toFloat()),
                        strokeWidth = 1.5f
                    )
                }

                // 3. Top Right corner crosshatch
                for (d in 0..110 step 15) {
                    drawLine(
                        color = lineColor.copy(alpha = 0.3f),
                        start = Offset(width - d.toFloat(), 0f),
                        end = Offset(width, d.toFloat()),
                        strokeWidth = 1.5f
                    )
                }

                // 4. Bottom Left corner crosshatch
                for (d in 0..110 step 15) {
                    drawLine(
                        color = lineColor.copy(alpha = 0.3f),
                        start = Offset(d.toFloat(), height),
                        end = Offset(0f, height - d.toFloat()),
                        strokeWidth = 1.5f
                    )
                }

                // Draw floating cosmic stars if Cosmic Dust is active
                if (theme == "Cosmic Dust") {
                    val rand = java.util.Random(42)
                    for (k in 0 until 35) {
                        val px = rand.nextFloat() * width
                        val py = rand.nextFloat() * height
                        val sizeP = rand.nextFloat() * 4.5f + 1f
                        drawCircle(
                            color = InkWhite.copy(alpha = rand.nextFloat() * 0.75f + 0.25f),
                            radius = sizeP,
                            center = Offset(px, py)
                        )
                    }
                }
            }
    ) {
        content()
    }
}

// ==========================================
// SUB-COMPONENT: OUTLINE REUSABLE INK BUTTON
// ==========================================
@Composable
fun InkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = InkWhite,
    textColor: Color = InkWhite,
    subText: String? = null,
    icon: @Composable (() -> Unit)? = null,
    testTag: String = ""
) {
    Button(
        onClick = {
            SoundSynthesizer.playTap()
            onClick()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (textColor == InkBlack) InkWhite else InkBlack,
            contentColor = textColor
        ),
        border = BorderStroke(2.dp, borderColor),
        shape = RoundedCornerShape(0.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(6.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = text,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = textColor
                )
                if (subText != null) {
                    Text(
                        text = subText,
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ==========================================
// DIALOG COMPONENT: STREAK UNLOCKED OVERLAY
// ==========================================
@Composable
fun StreakUnlockDialog(themeName: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBlack.copy(alpha = 0.85f))
            .clickable(enabled = false) {}, // Scrim
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(BorderStroke(2.5.dp, InkGold), RoundedCornerShape(12.dp))
                .background(InkGrayDark)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "Theme Unlocked",
                tint = InkGold,
                modifier = Modifier.size(56.dp)
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Text(
                text = "7 DAY STREAK!",
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = InkGold
            )
            
            Text(
                text = "THEME UNLOCKED",
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = InkWhite,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "You have unlocked the exclusive '$themeName' cosmetic theme! Gnarled tree border roots are now fully equipped.",
                fontSize = 12.sp,
                color = InkWhite.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            InkButton(
                text = "EQUIP NOW",
                onClick = onDismiss,
                borderColor = InkGold,
                textColor = InkGold,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
