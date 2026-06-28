package com.dopalette.app

import android.os.Bundle
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dopalette.app.data.ArtworkStore
import com.dopalette.app.data.AchievementStore
import com.dopalette.app.data.ProfileStore
import com.dopalette.app.data.MonetizationStore
import com.dopalette.app.data.AdMobManager
import com.dopalette.app.data.BillingManager
import com.dopalette.app.data.GoogleAuthController
import com.dopalette.app.ui.editor.EditorScreen
import com.dopalette.app.ui.home.HomeDashboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF2E6BFF),
                    secondary = Color(0xFF7C9CFF),
                    background = Color(0xFFDCE4EE),
                    surface = Color(0xFFEFF4FA),
                    onPrimary = Color(0xFFEFF4FA),
                    onSecondary = Color(0xFF18212F),
                    onBackground = Color(0xFF18212F),
                    onSurface = Color(0xFF18212F)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFDCE4EE)
                ) {
                    var screen by rememberSaveable { mutableStateOf("Home") }
                    var activeArtwork by rememberSaveable { mutableStateOf("APPLE") }
                    var editingFinishedId by rememberSaveable { mutableStateOf<String?>(null) }
                    var returnCategory by rememberSaveable { mutableStateOf("") }
                    var appReady by remember { mutableStateOf(false) }
                    var showLoading by rememberSaveable { mutableStateOf(true) }

                    val context = LocalContext.current.applicationContext

                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) {
                            ArtworkStore.initialize(context)
                            AchievementStore.initialize(context)
                            ProfileStore.initialize(context)
                            MonetizationStore.initialize(context)
                            AdMobManager.initialize(context)
                            BillingManager.initialize(context)
                            GoogleAuthController.refreshCurrentAccount(context)?.let { account ->
                                BillingManager.restoreOwnedPremium(context, account)
                            }
                        }
                        appReady = true
                    }

                    BackHandler(enabled = screen == "Editor" && !showLoading) {
                        editingFinishedId = null
                        screen = "Home"
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (!showLoading) {
                            when (screen) {
                                "Editor" -> EditorScreen(
                                    artworkTitle = activeArtwork,
                                    artworkCategory = returnCategory,
                                    finishedArtworkId = editingFinishedId,
                                    onBack = {
                                        editingFinishedId = null
                                        screen = "Home"
                                    }
                                )

                                else -> HomeDashboard(
                                    initialGenre = returnCategory,
                                    onOpenEditor = { artwork, category ->
                                        activeArtwork = artwork
                                        returnCategory = category ?: returnCategory
                                        editingFinishedId = null
                                        screen = "Editor"
                                    },
                                    onEditFinished = { artwork ->
                                        activeArtwork = artwork.title
                                        editingFinishedId = artwork.id
                                        screen = "Editor"
                                    }
                                )
                            }
                        }

                        if (showLoading) {
                            DoPaletteLoadingScreen(
                                appReady = appReady,
                                onFinished = { showLoading = false }
                            )
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun DoPaletteLoadingScreen(
    appReady: Boolean,
    onFinished: () -> Unit
) {
    val entranceProgress = remember { Animatable(0f) }
    val fadeProgress = remember { Animatable(1f) }
    var wordFormed by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        entranceProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1150, easing = FastOutSlowInEasing)
        )
        wordFormed = true
    }

    LaunchedEffect(appReady, wordFormed) {
        if (!wordFormed || finishing) return@LaunchedEffect
        val minimumVisibleTimeMillis = 1400L
        delay((minimumVisibleTimeMillis - 1150L).coerceAtLeast(0L))
        while (!appReady) {
            delay(90L)
        }
        if (!finishing) {
            finishing = true
            delay(320)
            fadeProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
            )
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(fadeProgress.value)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFEFF6FF),
                        Color(0xFFDCE8F7),
                        Color(0xFFC9D7EA)
                    ),
                    center = Offset(0.5f, 0.28f),
                    radius = 1100f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DoPaletteAnimatedWord(
                progress = entranceProgress.value,
                wordFormed = wordFormed,
                shouldWave = wordFormed
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Getting DoPalette ready…",
                color = Color(0xFF4D5B72),
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(if (entranceProgress.value > 0.55f) 1f else 0f)
            )
        }
    }
}

@Composable
private fun DoPaletteAnimatedWord(
    progress: Float,
    wordFormed: Boolean,
    shouldWave: Boolean
) {
    val letters = "DOPALETTE"
    val colors = listOf(
        Color(0xFF2563EB),
        Color(0xFFFF5F93),
        Color(0xFFFF9144),
        Color(0xFF20B486),
        Color(0xFF7C5CFF),
        Color(0xFF2563EB),
        Color(0xFFFF5F93),
        Color(0xFFFF9144),
        Color(0xFF20B486)
    )
    val startOffsets = listOf(
        -230.dp to -250.dp,
        -120.dp to -290.dp,
        45.dp to -255.dp,
        230.dp to -190.dp,
        -285.dp to -25.dp,
        285.dp to 25.dp,
        -180.dp to 255.dp,
        35.dp to 295.dp,
        245.dp to 230.dp
    )
    val startRotations = listOf(-24f, 18f, -16f, 22f, -12f, 15f, -20f, 13f, 24f)
    val wave = rememberInfiniteTransition(label = "dopalette-single-wave")
    val wavePhase by wave.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1180, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dopalette-wave-phase"
    )

    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val letterSize = if (maxWidth < 360.dp) 39.sp else 50.sp
        val slotWidth = if (maxWidth < 360.dp) 35.dp else 44.dp
        val lineHeight = if (maxWidth < 360.dp) 48.sp else 60.sp
        val eased = (progress * progress * (3f - (2f * progress))).coerceIn(0f, 1f)
        val wordWidth = slotWidth * letters.length
        val firstX = (wordWidth / -2f) + (slotWidth / 2f)
        val arrivalPop = if (progress < 0.72f) {
            0f
        } else {
            sin(((progress - 0.72f) / 0.28f).coerceIn(0f, 1f) * PI).toFloat()
        }

        Box(
            modifier = Modifier
                .height(86.dp)
                .width(wordWidth + 36.dp),
            contentAlignment = Alignment.Center
        ) {
            letters.forEachIndexed { index, char ->
                val wavePosition = ((wavePhase * letters.length) - index).coerceIn(-1f, 1f)
                val waveLift = if (shouldWave && kotlin.math.abs(wavePosition) < 1f) {
                    -18f * (1f - kotlin.math.abs(wavePosition))
                } else 0f
                val start = startOffsets[index]
                val finalX = firstX + (slotWidth * index.toFloat())
                val enterX = (start.first * (1f - eased)) + (finalX * eased)
                val enterY = (start.second * (1f - eased)) + waveLift.dp
                val pop = 1f + (arrivalPop * (0.06f + (index % 3) * 0.018f))
                val entranceWobble = if (!wordFormed) {
                    sin((progress * PI * 2.2f) + index).toFloat() * (1f - eased) * 5f
                } else 0f
                val rotationValue = (startRotations[index] * (1f - eased)) + entranceWobble
                val scaleValue = (0.64f + (0.36f * eased)) * pop
                Text(
                    text = char.toString(),
                    color = colors[index],
                    fontSize = letterSize,
                    lineHeight = lineHeight,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(slotWidth)
                        .offset(x = enterX, y = enterY)
                        .graphicsLayer {
                            alpha = (0.06f + progress * 0.94f).coerceIn(0f, 1f)
                            scaleX = scaleValue
                            scaleY = scaleValue * (1f + arrivalPop * 0.035f)
                            rotationZ = rotationValue
                        }
                )
            }
        }
    }
}
