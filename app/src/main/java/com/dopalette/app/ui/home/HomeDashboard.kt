package com.dopalette.app.ui.home

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.content.ContextWrapper
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.dopalette.app.data.AchievementStore
import com.dopalette.app.data.ProfileStore
import com.dopalette.app.data.GoogleAuthController
import com.dopalette.app.data.AccountProfileSync
import com.dopalette.app.data.AppResetManager
import com.dopalette.app.data.MonetizationStore
import com.dopalette.app.data.AdMobManager
import com.dopalette.app.data.BillingManager

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import java.util.Locale
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.io.File
import java.net.URL
import com.dopalette.app.R
import com.dopalette.app.data.ArtworkStore
import com.dopalette.app.data.ArtworkCloudSync
import com.dopalette.app.data.ArtworkRenderer
import com.dopalette.app.data.BrushStyle
import com.dopalette.app.data.FinishedArtwork
import kotlin.math.max
import kotlin.math.min
import com.dopalette.app.data.StrokeData
import com.dopalette.app.data.SelectionArtworkAssets
import com.dopalette.app.data.GoogleFormSupportSender
import com.dopalette.app.data.CommunityRepository
import kotlin.random.Random


private val DoseviaPinkPrimary = Color(0xFF2E6BFF)
private val DoseviaPinkDark = Color(0xFFE2E8F0)
private val DoseviaOrangeAccent = Color(0xFFF17878)
private val DoseviaSurface = Color(0xFFD8DCE4)
private val DoseviaCard = Color(0xFFFFFFFF)
private val DoseviaSoftPanel = Color(0xFFE9EEF6)
private val DoseviaText = Color(0xFF001233)
private val DoseviaMuted = Color(0xFF53657C)
private val DoseviaBorder = Color(0xFFC4CEDD)
private val DoseviaGreen = Color(0xFFE8CA4C)
private val DoseviaRed = Color(0xFFF17878)
private val DoseviaBackgroundBrush = Brush.linearGradient(
    listOf(
        Color(0xFFD8DCE4),
        Color(0xFFE7ECF3),
        Color(0xFFD0D6E1)
    )
)
private val DoseviaAccentBrush = Brush.linearGradient(listOf(Color(0xFF2E6BFF), Color(0xFFF27C78), Color(0xFFE7C94F)))

private val defaultGenres = listOf("Fruits", "Vegetables", "Animals", "Objects", "Flowers", "Vehicles", "Sports")
private val specialGenres = listOf("Dinosaurs", "Dragons", "Space", "Desserts", "Fantasy", "Sea Life")
private fun isSpecialGenre(genre: String) = specialGenres.any { it.equals(genre, ignoreCase = true) }
private val tabs = listOf("Selections", "Community", "Me")
private const val ALL_CATEGORIES_ROUTE = "__ALL_CATEGORIES__"
private const val SPECIAL_CATEGORIES_ROUTE = "__SPECIAL_CATEGORIES__"

private data class ProfileBorderSpec(val id: String, val title: String, val assetName: String, val animated: Boolean = false)
private val profileBorderSpecs = listOf(
    ProfileBorderSpec("default", "None", "none.png", false),
    ProfileBorderSpec("fruit_border", "Fruit Border", "fruit_border.png"),
    ProfileBorderSpec("vegetable_border", "Vegetable Border", "vegetable_border.png"),
    ProfileBorderSpec("animal_border", "Animal Border", "animal_border.png"),
    ProfileBorderSpec("flower_border", "Flower Border", "flower_border.png"),
    ProfileBorderSpec("vehicle_border", "Vehicle Border", "vehicle_border.png"),
    ProfileBorderSpec("sports_border", "Sports Border", "sports_border.png"),
    ProfileBorderSpec("dinosaur_border", "Dinosaur Border", "dinosaur_border.png"),
    ProfileBorderSpec("dragon_border", "Dragon Border", "dragon_border.png"),
    ProfileBorderSpec("space_border", "Space Border", "space_border.png"),
    ProfileBorderSpec("dessert_border", "Dessert Border", "dessert_border.png"),
    ProfileBorderSpec("fantasy_border", "Fantasy Border", "fantasy_border.png"),
    ProfileBorderSpec("ocean_border", "Ocean Border", "ocean_border.png"),
    ProfileBorderSpec("rainbow_border", "Rainbow Border", "rainbow_border.png"),
    ProfileBorderSpec("gold_master_border", "Gold Master Border", "gold_master_border.png"),
    ProfileBorderSpec("legendary_border", "Legendary Border", "legendary_border.png"),
    ProfileBorderSpec("bronze_border", "Bronze Border", "bronze_border.png", false),
    ProfileBorderSpec("silver_border", "Silver Border", "silver_border.png", false),
    ProfileBorderSpec("diamond_border", "Diamond Border", "diamond_border.png"),
    ProfileBorderSpec("founder_border", "Founder Border", "founder_border.png"),
    ProfileBorderSpec("completionist_border", "Completionist Border", "completionist_border.png")
)
private fun profileBorderSpec(id: String): ProfileBorderSpec = profileBorderSpecs.firstOrNull { it.id == id } ?: profileBorderSpecs.first()
private fun profileBorderTitle(id: String): String = profileBorderSpec(id).title
private fun normalizeRewardBorderId(reward: String): String? {
    val clean = reward.trim().lowercase(Locale.US)
    if (!clean.contains("border")) return null
    return when {
        clean.contains("fruit") -> "fruit_border"
        clean.contains("vegetable") -> "vegetable_border"
        clean.contains("animal") -> "animal_border"
        clean.contains("flower") -> "flower_border"
        clean.contains("vehicle") -> "vehicle_border"
        clean.contains("sport") -> "sports_border"
        clean.contains("dinosaur") -> "dinosaur_border"
        clean.contains("dragon") -> "dragon_border"
        clean.contains("space") -> "space_border"
        clean.contains("dessert") -> "dessert_border"
        clean.contains("fantasy") -> "fantasy_border"
        clean.contains("ocean") || clean.contains("sea") -> "ocean_border"
        clean.contains("rainbow") -> "rainbow_border"
        clean.contains("legendary") -> "legendary_border"
        clean.contains("diamond") -> "diamond_border"
        clean.contains("completionist") -> "completionist_border"
        clean.contains("gold") || clean.contains("master") -> "gold_master_border"
        else -> null
    }
}
private data class ProfileBadgeSpec(val id: String, val title: String, val subtitle: String, val minLevel: Int, val section: String = "Progress")
private val profileBadgeSpecs = listOf(
    ProfileBadgeSpec("starter", "Fresh Canvas", "New DoPalette artist", 1, "Progress"),
    ProfileBadgeSpec("premium_artist", "Premium Artist", "DoPalette Premium supporter", 1, "Premium"),
    ProfileBadgeSpec("beginner", "Beginner Artist", "Reach Level 1", 1, "Progress"),
    ProfileBadgeSpec("explorer", "Color Explorer", "Reach Level 5", 5, "Progress"),
    ProfileBadgeSpec("pro", "Creative Pro", "Reach Level 10", 10, "Progress"),
    ProfileBadgeSpec("champion", "Palette Champion", "Reach Level 25", 25, "Progress"),
    ProfileBadgeSpec("master", "DoPalette Master", "Reach Level 50", 50, "Progress"),
    ProfileBadgeSpec("legend", "Completionist Legend", "Reach Level 100", 100, "Progress"),

    ProfileBadgeSpec("fresh_picker", "Fresh Picker", "Finish your first Fruits artwork", 1, "Fruits"),
    ProfileBadgeSpec("fruit_explorer", "Fruit Explorer", "Finish 5 Fruits artworks", 1, "Fruits"),
    ProfileBadgeSpec("fruit", "Orchard Master", "Complete Fruits", 1, "Fruits"),

    ProfileBadgeSpec("seed_starter", "Seed Starter", "Finish your first Vegetables artwork", 1, "Vegetables"),
    ProfileBadgeSpec("harvest_hero", "Harvest Hero", "Finish 5 Vegetables artworks", 1, "Vegetables"),
    ProfileBadgeSpec("garden", "Garden Guardian", "Complete Vegetables", 1, "Vegetables"),

    ProfileBadgeSpec("animal_friend", "Wildlife Scout", "Finish your first Animals artwork", 1, "Animals"),
    ProfileBadgeSpec("wild_explorer", "Beast Tamer", "Finish 5 Animals artworks", 1, "Animals"),
    ProfileBadgeSpec("jungle", "King of the Jungle", "Complete Animals", 1, "Animals"),

    ProfileBadgeSpec("collector_rookie", "Treasure Finder", "Finish your first Objects artwork", 1, "Objects"),
    ProfileBadgeSpec("object_finder", "Daily Collector", "Finish 5 Objects artworks", 1, "Objects"),
    ProfileBadgeSpec("everyday_master", "Everyday Master", "Complete Objects", 1, "Objects"),

    ProfileBadgeSpec("little_gardener", "Bloom Seeker", "Finish your first Flowers artwork", 1, "Flowers"),
    ProfileBadgeSpec("petal_collector", "Garden Keeper", "Finish 5 Flowers artworks", 1, "Flowers"),
    ProfileBadgeSpec("bloom", "Floral Monarch", "Complete Flowers", 1, "Flowers"),

    ProfileBadgeSpec("first_driver", "Road Rookie", "Finish your first Vehicles artwork", 1, "Vehicles"),
    ProfileBadgeSpec("speed_explorer", "Highway Hero", "Finish 5 Vehicles artworks", 1, "Vehicles"),
    ProfileBadgeSpec("road", "Road Champion", "Complete Vehicles", 1, "Vehicles"),

    ProfileBadgeSpec("rookie_athlete", "Rising Athlete", "Finish your first Sports artwork", 1, "Sports"),
    ProfileBadgeSpec("team_player", "Arena Champion", "Finish 5 Sports artworks", 1, "Sports"),
    ProfileBadgeSpec("allstar", "Sports Legend", "Complete Sports", 1, "Sports"),

    ProfileBadgeSpec("dino_rookie", "Fossil Hunter", "Finish your first Dinosaurs artwork", 1, "Dinosaurs"),
    ProfileBadgeSpec("fossil_hunter", "Jurassic Explorer", "Finish 5 Dinosaurs artworks", 1, "Dinosaurs"),
    ProfileBadgeSpec("prehistoric", "Prehistoric Master", "Complete Dinosaurs", 1, "Dinosaurs"),

    ProfileBadgeSpec("dragon_rider", "Dragon Rider", "Finish your first Dragons artwork", 1, "Dragons"),
    ProfileBadgeSpec("flame_keeper", "Flame Keeper", "Finish 5 Dragons artworks", 1, "Dragons"),
    ProfileBadgeSpec("dragon_lord", "Dragon Lord", "Complete Dragons", 1, "Dragons"),

    ProfileBadgeSpec("star_voyager", "Star Voyager", "Finish your first Space artwork", 1, "Space"),
    ProfileBadgeSpec("galaxy_explorer", "Galaxy Explorer", "Finish 5 Space artworks", 1, "Space"),
    ProfileBadgeSpec("cosmic_legend", "Cosmic Legend", "Complete Space", 1, "Space"),

    ProfileBadgeSpec("sweet_starter", "Sweet Starter", "Finish your first Desserts artwork", 1, "Desserts"),
    ProfileBadgeSpec("sugar_artist", "Sugar Artist", "Finish 5 Desserts artworks", 1, "Desserts"),
    ProfileBadgeSpec("dessert_master", "Dessert Royalty", "Complete Desserts", 1, "Desserts"),

    ProfileBadgeSpec("first_share", "Gallery Debut", "Share your first artwork", 1, "Community"),
    ProfileBadgeSpec("showcase", "Showcase Artist", "Share 10 Community artworks", 1, "Community"),
    ProfileBadgeSpec("community_star", "Community Star", "Receive 100 likes", 1, "Community"),
    ProfileBadgeSpec("free_master", "Free Collection Master", "Complete free categories", 1, "Collection"),
    ProfileBadgeSpec("special_master", "Special Collection Master", "Complete special categories", 1, "Collection"),
    ProfileBadgeSpec("completionist", "DoPalette Completionist", "Complete everything", 1, "Collection")
).distinctBy { it.id }
private fun profileBadgeSpec(id: String): ProfileBadgeSpec = profileBadgeSpecs.firstOrNull { it.id == id } ?: profileBadgeSpecs.first()
private fun profileBadgeTitle(id: String): String = profileBadgeSpec(id).title
private fun levelForBadgeId(id: String): Int = when (id) {
    "starter", "beginner" -> 1
    "explorer" -> 5
    "pro" -> 10
    "champion" -> 25
    "master" -> 50
    "legend" -> 100
    else -> 1
}
private fun achievementIdForBadge(id: String): String? = when (id) {
    "animal_friend" -> "animals_first"
    "wild_explorer" -> "animals_five"
    "jungle" -> "animals_master"
    "little_gardener" -> "flowers_first"
    "petal_collector" -> "flowers_five"
    "bloom" -> "flowers_master"
    "first_driver" -> "vehicles_first"
    "speed_explorer" -> "vehicles_five"
    "road" -> "vehicles_master"
    "dino_rookie" -> "dinosaurs_first"
    "fossil_hunter" -> "dinosaurs_five"
    "prehistoric" -> "dinosaurs_master"
    "seed_starter" -> "vegetables_first"
    "harvest_hero" -> "vegetables_five"
    "garden" -> "vegetables_master"
    "fresh_picker" -> "fruits_first"
    "fruit_explorer" -> "fruits_five"
    "fruit" -> "fruits_master"
    "collector_rookie" -> "objects_first"
    "object_finder" -> "objects_five"
    "everyday_master" -> "objects_master"
    "rookie_athlete" -> "sports_first"
    "team_player" -> "sports_five"
    "allstar" -> "sports_master"
    "first_share" -> "first_community_share"
    "showcase" -> "community_ten_posts"
    "community_star" -> "community_star"
    "free_master" -> "complete_free_categories"
    "special_master" -> "complete_special_categories"
    "dragon_rider" -> "dragons_first"
    "flame_keeper" -> "dragons_five"
    "dragon_lord" -> "dragons_master"
    "star_voyager" -> "space_first"
    "galaxy_explorer" -> "space_five"
    "cosmic_legend" -> "space_master"
    "sweet_starter" -> "desserts_first"
    "sugar_artist" -> "desserts_five"
    "dessert_master" -> "desserts_master"
    "completionist" -> "completionist"
    else -> null
}
private fun isBadgeOwned(id: String): Boolean {
    val clean = id.ifBlank { "starter" }
    if (clean == "premium_artist" && MonetizationStore.isPremiumUser.value) return true
    if (clean == "starter" || clean == "beginner") return true
    val level = artistLevelFor(AchievementStore.totalXp()).level
    if (clean in setOf("explorer", "pro", "champion", "master", "legend")) return level >= levelForBadgeId(clean)
    val achievementId = achievementIdForBadge(clean) ?: return false
    return AchievementStore.isUnlocked(achievementId)
}
private fun unlockedBadgeSpecs(): List<ProfileBadgeSpec> = profileBadgeSpecs.filter { isBadgeOwned(it.id) }

private fun achievementIdForBorder(id: String): String? = when (id) {
    "fruit_border" -> "fruits_master"
    "vegetable_border" -> "vegetables_master"
    "animal_border" -> "animals_master"
    "flower_border" -> "flowers_master"
    "vehicle_border" -> "vehicles_master"
    "sports_border" -> "sports_master"
    "dinosaur_border" -> "dinosaurs_master"
    "dragon_border" -> "dragons_master"
    "space_border" -> "space_master"
    "dessert_border" -> "desserts_master"
    "fantasy_border" -> "fantasy_master"
    "ocean_border" -> "sea_life_master"
    "bronze_border" -> "level_5"
    "silver_border" -> "level_10"
    "diamond_border" -> "level_25"
    "gold_master_border" -> "level_50"
    "legendary_border" -> "level_100"
    "completionist_border" -> "completionist"
    "rainbow_border" -> "completionist"
    else -> null
}
private fun isBorderOwned(id: String): Boolean {
    val clean = id.ifBlank { "default" }
    if (clean == "rainbow_border" && MonetizationStore.isPremiumUser.value) return true
    if (clean == "default") return true
    val achievementId = achievementIdForBorder(clean) ?: return false
    return AchievementStore.isUnlocked(achievementId)
}
private fun unlockedBorderSpecs(): List<ProfileBorderSpec> = profileBorderSpecs.filter { isBorderOwned(it.id) }


private object CommunityPreviewMemoryCache {
    private const val MAX_MEMORY_ITEMS = 40
    private const val DISK_DIR = "community_preview_cache"
    private val cache = LinkedHashMap<String, ImageBitmap>(MAX_MEMORY_ITEMS, 0.75f, true)

    fun get(key: String): ImageBitmap? = synchronized(cache) { cache[key] }

    fun put(key: String, image: ImageBitmap) {
        synchronized(cache) {
            cache[key] = image
            while (cache.size > MAX_MEMORY_ITEMS) {
                val first = cache.entries.firstOrNull()?.key ?: break
                cache.remove(first)
            }
        }
    }

    fun loadDisk(context: android.content.Context, key: String): ImageBitmap? {
        return runCatching {
            val file = diskFile(context, key)
            if (!file.exists()) return null
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        }.getOrNull()
    }

    fun saveDisk(context: android.content.Context, key: String, bitmap: Bitmap) {
        runCatching {
            val file = diskFile(context, key)
            file.parentFile?.mkdirs()
            file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        }
    }

    private fun diskFile(context: android.content.Context, key: String): File {
        val safeName = key.hashCode().toString() + ".png"
        return File(File(context.cacheDir, DISK_DIR), safeName)
    }
}

private data class CatalogItem(
    val title: String,
    val category: String,
    val accent: Color
)

private val fallbackCatalogItems = listOf(
    CatalogItem("APPLE", "Fruits", DoseviaPinkPrimary)
)

@Composable
fun HomeDashboard(
    initialGenre: String = "",
    onOpenEditor: (String, String?) -> Unit,
    onEditFinished: (FinishedArtwork) -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    var activeGenre by rememberSaveable(initialGenre) { mutableStateOf(initialGenre) }
    var activeTab by remember { mutableStateOf("Selections") }
    var showComingSoon by remember { mutableStateOf<String?>(null) }
    var choiceItem by remember { mutableStateOf<CatalogItem?>(null) }
    var pendingStartNewItem by remember { mutableStateOf<CatalogItem?>(null) }
    var catalogItems by remember { mutableStateOf(fallbackCatalogItems) }
    var genres by remember { mutableStateOf(defaultGenres) }
    var useShowcaseUi by rememberSaveable { mutableStateOf(false) }
    var pickLaunchItem by remember { mutableStateOf<CatalogItem?>(null) }
    var pickRunId by remember { mutableIntStateOf(0) }
    var pickIsOpening by remember { mutableStateOf(false) }
    var showPremiumModal by remember { mutableStateOf(false) }
    var showPremiumUnlockedModal by rememberSaveable { mutableStateOf(false) }

    val storeVersion = ArtworkStore.globalUpdateTick.intValue

    LaunchedEffect(Unit) {
        val loadedCatalog = withContext(Dispatchers.IO) { SelectionArtworkAssets.loadCatalog(context) }
        if (loadedCatalog.isNotEmpty()) {
            // Keep startup light. Do not render every artwork preview during app launch.
            // Thumbnails are created lazily by ArtworkThumbnail and cached afterward.
            catalogItems = loadedCatalog.map { entry ->
                CatalogItem(entry.title, entry.category, accentForCategory(entry.category))
            }
            val loadedGenres = loadedCatalog.map { it.category }.distinct()
            genres = (loadedGenres + defaultGenres + specialGenres).distinct()
        } else {
            genres = defaultGenres
        }
    }

    val visibleItems = remember(activeGenre, catalogItems) {
        if (activeGenre.isBlank() || activeGenre == ALL_CATEGORIES_ROUTE || activeGenre == SPECIAL_CATEGORIES_ROUTE) emptyList() else catalogItems.filter { it.category == activeGenre }
    }
    val availablePickItems = remember(catalogItems) {
        catalogItems.filter { candidate -> candidate.title.isNotBlank() && candidate.category.isNotBlank() && !isSpecialGenre(candidate.category) }
    }

    LaunchedEffect(pickRunId) {
        val picked = pickLaunchItem
        if (picked != null) {
            pickIsOpening = false
            // Let the user clearly see the selected artwork before loading it.
            delay(2200)
            pickIsOpening = true
            delay(260)
            onOpenEditor(picked.title, picked.category)
            pickLaunchItem = null
            pickIsOpening = false
        }
    }

    // Pre-rendering every thumbnail here made the app slow/heavy at startup.
    // Home previews now render lazily per visible card and reuse HomePreviewCache.

    BackHandler(enabled = activeGenre.isNotBlank() || activeTab != "Selections") {
        if (activeTab != "Selections") {
            activeTab = "Selections"
        } else {
            activeGenre = ""
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DoseviaSurface)
    ) {
        val wide = maxWidth >= 600.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (wide) 28.dp else 16.dp, vertical = 16.dp)
                .navigationBarsPadding()
        ) {
            CompactTopHeader(wide = wide, onProfileClick = { activeTab = "Profile" }, onPremiumClick = { showPremiumModal = true })
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    "Community" -> CommunityTab(onComingSoon = { showComingSoon = it })
                    "Profile" -> ProfileTab(onBack = { activeTab = "Selections" }, onComingSoon = { showComingSoon = it })
                    "Me" -> ProgressTab(
                        onOpenEditor = { title -> onOpenEditor(title, null) },
                        onEditFinished = onEditFinished
                    )
                    else -> SelectionsTab(
                        activeGenre = activeGenre,
                        genres = genres,
                        catalogItems = catalogItems,
                        useShowcaseUi = useShowcaseUi,
                        onGenreSelected = { activeGenre = it },
                        items = visibleItems,
                        onItemClick = { item ->
                            if (ArtworkStore.hasDraft(item.title) || ArtworkStore.finishedFor(item.title).isNotEmpty()) {
                                choiceItem = item
                            } else {
                                onOpenEditor(item.title, item.category)
                            }
                        },
                        onPickForMe = {
                            if (availablePickItems.isNotEmpty() && pickLaunchItem == null) {
                                pickIsOpening = false
                                pickLaunchItem = availablePickItems.random()
                                pickRunId += 1
                            }
                        },
                        pickForMeEnabled = availablePickItems.isNotEmpty() && pickLaunchItem == null
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            BottomTabs(activeTab = activeTab, onSelect = { activeTab = it })
        }

        PickForMeDiceOverlay(
            item = pickLaunchItem,
            runId = pickRunId,
            isOpening = pickIsOpening,
            modifier = Modifier.fillMaxSize()
        )

        AchievementRewardOverlay(
            reward = AchievementStore.lastReward.value,
            onDismiss = { AchievementStore.dismissReward() },
            modifier = Modifier.fillMaxSize()
        )
    }

    choiceItem?.let { item ->
        ArtworkChoiceDialog(
            item = item,
            canStartNew = ArtworkStore.canAddFinished(item.title),
            onDismiss = { choiceItem = null },
            onContinueDraft = {
                choiceItem = null
                onOpenEditor(item.title, item.category)
            },
            onStartNew = {
                pendingStartNewItem = item
            },
            onShowFinished = {
                choiceItem = null
                activeTab = "Me"
            }
        )
    }

    pendingStartNewItem?.let { item ->
        PremiumChoiceModal(
            eyebrow = "New canvas",
            title = "Start fresh?",
            body = "This clears only the current ${item.title} draft. Your finished layers stay safe in your library.",
            primaryText = "Start New",
            secondaryText = "Cancel",
            danger = false,
            onPrimary = {
                ArtworkStore.clearDraft(item.title)
                pendingStartNewItem = null
                choiceItem = null
                onOpenEditor(item.title, item.category)
            },
            onSecondary = { pendingStartNewItem = null },
            onDismiss = { pendingStartNewItem = null }
        )
    }

    showComingSoon?.let { title ->
        ComingSoonDialog(title = title, onDismiss = { showComingSoon = null })
    }

    if (showPremiumModal) {
        DoPalettePremiumModal(
            onDismiss = { showPremiumModal = false },
            onUpgradePlaceholder = {
                val account = GoogleAuthController.refreshCurrentAccount(context)
                val hostActivity = activity
                if (account == null) {
                    MonetizationStore.requestPremiumSignIn()
                    activeTab = "Profile"
                    showPremiumModal = false
                } else if (hostActivity == null) {
                    showPremiumModal = false
                } else {
                    BillingManager.launchPremiumPurchase(
                        activity = hostActivity,
                        account = account,
                        onActivated = {
                            scope.launch {
                                AccountProfileSync.syncLightweightProfile(account)
                                CommunityRepository.patchCachedOwnerStyle(context)
                                showPremiumModal = false
                                showPremiumUnlockedModal = true
                            }
                        },
                        onError = {
                            // Keep the modal open so users can try again when Google Play is ready.
                        }
                    )
                }
            }
        )
    }

    if (showPremiumUnlockedModal) {
        PremiumUnlockedModal(onDismiss = { showPremiumUnlockedModal = false })
    }

}



@Composable
private fun rememberProfileBorderImage(borderId: String): ImageBitmap? {
    val context = LocalContext.current
    val spec = remember(borderId) { profileBorderSpec(borderId) }
    return remember(spec.assetName) {
        runCatching {
            if (spec.id == "default") null else context.assets.open("profile_borders/${spec.assetName}").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        }.getOrNull()
    }
}

@Composable
private fun ProfileBorderOverlay(borderId: String, modifier: Modifier = Modifier) {
    val image = rememberProfileBorderImage(borderId) ?: return
    val adjustedModifier = when (borderId) {
        "gold_master_border", "legendary_border" -> modifier.graphicsLayer(scaleY = 1.07f).offset(y = (-3).dp)
        else -> modifier
    }
    Image(
        bitmap = image,
        contentDescription = profileBorderTitle(borderId),
        modifier = adjustedModifier,
        contentScale = ContentScale.FillBounds
    )
}

@Composable
private fun ProfileAvatarWithBorder(size: Dp, textSize: Dp, borderId: String = ProfileStore.borderId.value, avatarScale: Float = 0.72f) {
    val path = ProfileStore.avatarFilePath.value
    val avatarVersion = ProfileStore.avatarVersion.intValue
    val image = remember(path, avatarVersion) { try { path?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() } } catch (_: Throwable) { null } }
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxSize(avatarScale).background(DoseviaAccentBrush, CircleShape).clip(CircleShape), contentAlignment = Alignment.Center) {
            if (image != null) Image(bitmap = image, contentDescription = "Profile picture", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Text(ProfileStore.displayName.value.firstOrNull()?.uppercase() ?: "D", color = Color.White, fontSize = textSize.value.sp, fontWeight = FontWeight.Black)
        }
        ProfileBorderOverlay(borderId, Modifier.fillMaxSize())
    }
}

@Composable
private fun CompactTopHeader(wide: Boolean, onProfileClick: () -> Unit, onPremiumClick: () -> Unit) {
    var currentHour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }

    LaunchedEffect(Unit) {
        while (true) {
            currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            delay(60_000L)
        }
    }

    val greeting = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
    }
    val displayName = ProfileStore.displayName.value.ifBlank { "DoPalette Artist" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (wide) 58.dp else 54.dp)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            ProfileAvatarWithBorder(size = if (wide) 58.dp else 54.dp, textSize = if (wide) 19.dp else 18.dp)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onProfileClick),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "$greeting ✦",
                color = Color(0xFF51627A),
                fontSize = if (wide) 14.sp else 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = displayName,
                color = Color(0xFF001233),
                fontSize = if (wide) 32.sp else 28.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
        if (!MonetizationStore.isPremiumUser.value) {
            PremiumGiftButton(isPremium = false, onClick = onPremiumClick)
        }
    }
}


@Composable
private fun ProfileAvatarMini() {
    ProfileAvatarWithBorder(size = 48.dp, textSize = 18.dp)
}

@Composable
private fun CompactSelectionHeader(
    title: String,
    subtitle: String,
    icon: String,
    showChangeCategory: Boolean,
    onChangeCategory: () -> Unit
) {
    if (!showChangeCategory && title == "Selections") return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = DoseviaText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = subtitle,
                color = DoseviaMuted,
                fontSize = 13.sp
            )
        }
        if (showChangeCategory) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onChangeCategory),
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFFEAF0FA),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC7D2E4)),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "‹",
                        color = DoseviaPinkPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Back",
                        color = DoseviaText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionsTab(
    activeGenre: String,
    genres: List<String>,
    catalogItems: List<CatalogItem>,
    useShowcaseUi: Boolean,
    onGenreSelected: (String) -> Unit,
    items: List<CatalogItem>,
    onItemClick: (CatalogItem) -> Unit,
    onPickForMe: () -> Unit = {},
    pickForMeEnabled: Boolean = true
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    val isChoosingCategory = activeGenre.isBlank()
    val isAllCategories = activeGenre == ALL_CATEGORIES_ROUTE
    val isSpecialCategories = activeGenre == SPECIAL_CATEGORIES_ROUTE
    val storeVersion = ArtworkStore.globalUpdateTick.intValue
    var itemCycleTick by remember { mutableIntStateOf(0) }
    var pendingRewardGenre by remember { mutableStateOf<String?>(null) }
    val monetizationTick = MonetizationStore.updateTick.intValue
    val adTick = AdMobManager.adTick.intValue
    val rewardedReady = AdMobManager.isRewardedReady() || !MonetizationStore.adsEnabled()
    val rewardedLoading = AdMobManager.isRewardedLoading()

    LaunchedEffect(Unit, monetizationTick) {
        AdMobManager.preloadRewarded(context, force = true)
        AdMobManager.preloadInterstitial(context, force = true)
    }

    fun openGenreWithMonetization(genre: String, realCount: Int) {
        if (realCount <= 0) return
        if (isSpecialGenre(genre) && MonetizationStore.shouldRequireRewardForSpecial(genre)) {
            pendingRewardGenre = genre
        } else {
            onGenreSelected(genre)
        }
    }

    LaunchedEffect(catalogItems, activeGenre) {
        if (activeGenre.isNotBlank()) return@LaunchedEffect
        while (true) {
            delay(5000)
            itemCycleTick += 1
        }
    }

    val itemsByCategory = remember(catalogItems) { catalogItems.groupBy { it.category } }
    val freeGenres = remember(genres) { (genres + defaultGenres).distinct().filterNot { isSpecialGenre(it) } }
    val visibleSpecialGenres = remember(genres, catalogItems) { (specialGenres + genres.filter { isSpecialGenre(it) }).distinct() }
    val representativeByCategory = remember(genres, itemsByCategory, itemCycleTick) {
        (genres + defaultGenres + specialGenres).distinct().associateWith { genre ->
            randomPreviewItemForCategory(
                genre = genre,
                realItems = itemsByCategory[genre].orEmpty(),
                tick = itemCycleTick
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CompactSelectionHeader(
            title = when {
                isChoosingCategory -> "Selections"
                isAllCategories -> "All Categories"
                isSpecialCategories -> "Special Categories"
                else -> activeGenre
            },
            subtitle = when {
                isChoosingCategory -> "Pick a category to browse pages."
                isAllCategories -> "Choose a category to browse its pages."
                isSpecialCategories -> "Special collections and future premium pages."
                else -> if (isSpecialGenre(activeGenre)) {
                    "${items.size} page${if (items.size == 1) "" else "s"} • ${MonetizationStore.specialUnlockRemainingText(activeGenre)}"
                } else {
                    "${items.size} page${if (items.size == 1) "" else "s"} available"
                }
            },
            icon = if (isChoosingCategory || isAllCategories || isSpecialCategories) "🎨" else categoryIcon(activeGenre),
            showChangeCategory = !isChoosingCategory,
            onChangeCategory = { onGenreSelected("") }
        )

        if (isChoosingCategory) {
            val availableGenres = freeGenres
            PremiumHomeLanding(
                genres = availableGenres,
                specialGenres = visibleSpecialGenres,
                catalogItems = catalogItems,
                itemsByCategory = itemsByCategory,
                representativeByCategory = representativeByCategory,
                itemCycleTick = itemCycleTick,
                onGenreSelected = onGenreSelected,
                onOpenGenre = ::openGenreWithMonetization,
                onSeeAllCategories = { onGenreSelected(ALL_CATEGORIES_ROUTE) },
                onSeeAllSpecialCategories = { onGenreSelected(SPECIAL_CATEGORIES_ROUTE) },
                onItemClick = onItemClick,
                onPickForMe = onPickForMe,
                pickForMeEnabled = pickForMeEnabled
            )
        } else if (isAllCategories || isSpecialCategories) {
            val allGenres = if (isSpecialCategories) visibleSpecialGenres else freeGenres
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                allGenres.chunked(2).forEach { rowGenres ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        rowGenres.forEach { genre ->
                            val realCount = catalogItems.count { it.category == genre }
                            val comingSoon = realCount <= 0
                            CategoryTile(
                                genre = genre,
                                itemCount = realCount.takeIf { it > 0 } ?: previewPoolForCategory(genre, itemsByCategory[genre].orEmpty()).size,
                                representative = if (realCount > 0) representativeByCategory[genre] else null,
                                selected = false,
                                isComingSoon = comingSoon,
                                onClick = { openGenreWithMonetization(genre, realCount) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowGenres.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(120.dp))
            }
        } else {

            if (items.isEmpty()) {
                InfoPanel("No Pages Yet", "$activeGenre pages can be added here when new line art is included.")
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            rowItems.forEach { item ->
                                CatalogCard(
                                    item = item,
                                    onClick = { onItemClick(item) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }

    pendingRewardGenre?.let { genre ->
        SpecialRewardUnlockModal(
            genre = genre,
            adReady = rewardedReady,
            adLoading = rewardedLoading,
            onDismiss = { pendingRewardGenre = null },
            onWatchPlaceholder = {
                val hostActivity = activity
                if (hostActivity == null) {
                    AdMobManager.preloadRewarded(context, force = true)
                } else if (!AdMobManager.isRewardedReady()) {
                    AdMobManager.preloadRewarded(context, force = true)
                } else {
                    AdMobManager.showRewardedForSpecial(
                        activity = hostActivity,
                        onRewardEarned = {
                            MonetizationStore.unlockSpecialFor24Hours(genre)
                            pendingRewardGenre = null
                            onGenreSelected(genre)
                        },
                        onClosedWithoutReward = {
                            AdMobManager.preloadRewarded(context, force = true)
                        }
                    )
                }
            }
        )
    }
}


@Composable
private fun PremiumHomeLanding(
    genres: List<String>,
    specialGenres: List<String>,
    catalogItems: List<CatalogItem>,
    itemsByCategory: Map<String, List<CatalogItem>>,
    representativeByCategory: Map<String, CatalogItem?>,
    itemCycleTick: Int,
    onGenreSelected: (String) -> Unit,
    onOpenGenre: (String, Int) -> Unit,
    onSeeAllCategories: () -> Unit,
    onSeeAllSpecialCategories: () -> Unit,
    onItemClick: (CatalogItem) -> Unit,
    onPickForMe: () -> Unit,
    pickForMeEnabled: Boolean
) {
    if (genres.isEmpty()) {
        InfoPanel("No Categories Yet", "Add artwork inside the selections folders to show categories here.")
        return
    }
    val heroGenres = genres.filter { genre -> catalogItems.count { it.category == genre } > 0 }
        .ifEmpty { genres.filter { genre -> catalogItems.any { it.category == genre } } }

    if (heroGenres.isEmpty()) {
        InfoPanel("No Preview Yet", "Add artwork to a category to show it in the swipe preview.")
        return
    }

    var heroIndex by rememberSaveable(heroGenres.joinToString("|")) { mutableStateOf(0) }
    var dragDirection by remember { mutableStateOf(1) }
    fun go(delta: Int) {
        dragDirection = if (delta >= 0) 1 else -1
        heroIndex = (heroIndex + delta + heroGenres.size) % heroGenres.size
    }

    val currentGenre = heroGenres[heroIndex.coerceIn(0, heroGenres.lastIndex)]
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (screenWidth >= 700.dp) 390.dp else 500.dp, max = if (screenWidth >= 700.dp) 500.dp else 620.dp)
                .pointerInput(heroGenres) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            totalDrag += dragAmount
                            change.consume()
                        },
                        onDragEnd = {
                            if (totalDrag < -45f) go(1)
                            if (totalDrag > 45f) go(-1)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val rawHeroWidth = when {
                maxWidth >= 900.dp -> maxWidth * 0.40f
                maxWidth >= 700.dp -> maxWidth * 0.48f
                maxWidth >= 460.dp -> maxWidth * 0.78f
                else -> maxWidth * 0.90f
            }
            val maxHeroWidthByHeight = maxHeight / 1.46f
            val heroWidth = min(rawHeroWidth.value, maxHeroWidthByHeight.value).dp
            val heroHeight = heroWidth * 1.46f
            val heroImageHeight = heroHeight * 0.70f
            val nextGenre = heroGenres[(heroIndex + 1) % heroGenres.size]
            val thirdGenre = heroGenres[(heroIndex + 2) % heroGenres.size]

            val isTabletDeck = maxWidth >= 700.dp
            val activeDeckOffsetX = if (isTabletDeck) heroWidth * -0.42f else 0.dp
            val secondDeckOffsetX = if (isTabletDeck) heroWidth * 0.04f else heroWidth * -0.10f
            val thirdDeckOffsetX = if (isTabletDeck) heroWidth * 0.50f else heroWidth * -0.19f
            val secondDeckOffsetY = if (isTabletDeck) 0.dp else 22.dp
            val thirdDeckOffsetY = if (isTabletDeck) 0.dp else 44.dp

            PremiumHeroCard(
                genre = thirdGenre,
                item = randomPreviewItemForCategory(thirdGenre, itemsByCategory[thirdGenre].orEmpty(), itemCycleTick + 2),
                pageCount = previewPoolForCategory(thirdGenre, itemsByCategory[thirdGenre].orEmpty()).size,
                onOpenCategory = { onGenreSelected(thirdGenre) },
                onContinue = { onGenreSelected(thirdGenre) },
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = thirdDeckOffsetX, y = thirdDeckOffsetY)
                    .graphicsLayer {
                        scaleX = if (isTabletDeck) 0.92f else 0.88f
                        scaleY = if (isTabletDeck) 0.92f else 0.88f
                        alpha = 0.46f
                    },
                cardWidth = heroWidth,
                cardHeight = heroHeight,
                imageHeight = heroImageHeight
            )

            PremiumHeroCard(
                genre = nextGenre,
                item = randomPreviewItemForCategory(nextGenre, itemsByCategory[nextGenre].orEmpty(), itemCycleTick + 1),
                pageCount = previewPoolForCategory(nextGenre, itemsByCategory[nextGenre].orEmpty()).size,
                onOpenCategory = { onGenreSelected(nextGenre) },
                onContinue = { onGenreSelected(nextGenre) },
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = secondDeckOffsetX, y = secondDeckOffsetY)
                    .graphicsLayer {
                        scaleX = if (isTabletDeck) 0.96f else 0.94f
                        scaleY = if (isTabletDeck) 0.96f else 0.94f
                        alpha = 0.72f
                    },
                cardWidth = heroWidth,
                cardHeight = heroHeight,
                imageHeight = heroImageHeight
            )

            AnimatedContent(
                targetState = currentGenre,
                transitionSpec = {
                    val enterFrom: (Int) -> Int = if (dragDirection >= 0) ({ width -> width }) else ({ width -> -width })
                    val exitTo: (Int) -> Int = if (dragDirection >= 0) ({ width -> -width }) else ({ width -> width })
                    (slideInHorizontally(animationSpec = tween(420), initialOffsetX = enterFrom) + fadeIn(animationSpec = tween(160))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(420), targetOffsetX = exitTo) + fadeOut(animationSpec = tween(160)))
                },
                label = "stacked-home-hero"
            ) { genre ->
                val item = randomPreviewItemForCategory(genre, itemsByCategory[genre].orEmpty(), itemCycleTick)
                PremiumHeroCard(
                    genre = genre,
                    item = item,
                    pageCount = previewPoolForCategory(genre, itemsByCategory[genre].orEmpty()).size,
                    onOpenCategory = { onGenreSelected(genre) },
                    onContinue = { onGenreSelected(genre) },
                    modifier = Modifier.align(Alignment.Center).offset(x = activeDeckOffsetX),
                    cardWidth = heroWidth,
                    cardHeight = heroHeight,
                    imageHeight = heroImageHeight
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Popular Categories", color = DoseviaText, fontSize = 20.sp, fontWeight = FontWeight.Black)
            TextButton(onClick = onSeeAllCategories) {
                Text("See all", color = DoseviaPinkPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            genres.forEach { genre ->
                PremiumCategoryStripCard(
                    genre = genre,
                    item = representativeByCategory[genre],
                    count = catalogItems.count { it.category == genre }.takeIf { it > 0 } ?: previewPoolForCategory(genre, itemsByCategory[genre].orEmpty()).size,
                    isComingSoon = catalogItems.count { it.category == genre } <= 0,
                    onClick = { onOpenGenre(genre, catalogItems.count { it.category == genre }) }
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
        }

        if (specialGenres.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Special Categories", color = DoseviaText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    TextButton(onClick = onSeeAllSpecialCategories) {
                        Text("View all", color = DoseviaPinkPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    specialGenres.forEach { genre ->
                        PremiumCategoryStripCard(
                            genre = genre,
                            item = representativeByCategory[genre],
                            count = catalogItems.count { it.category == genre }.takeIf { it > 0 } ?: previewPoolForCategory(genre, itemsByCategory[genre].orEmpty()).size,
                            isComingSoon = catalogItems.count { it.category == genre } <= 0,
                            onClick = { onOpenGenre(genre, catalogItems.count { it.category == genre }) }
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                }
            }
        }

        MischiefPickForMeButton(
            enabled = pickForMeEnabled,
            onClick = onPickForMe
        )

        Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PickForMeDiceOverlay(
    item: CatalogItem?,
    runId: Int,
    isOpening: Boolean,
    modifier: Modifier = Modifier
) {
    if (item == null) return

    var started by remember(runId) { mutableStateOf(false) }
    LaunchedEffect(runId) { started = true }

    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
        label = "pick-for-me-dice-reveal"
    )
    val showArtwork = progress > 0.35f
    val overlayAlpha = when {
        progress < 0.08f -> progress / 0.08f
        else -> 1f
    }.coerceIn(0f, 1f)
    val rollPhase = progress.coerceIn(0f, 0.35f) / 0.35f
    val revealPhase = ((progress - 0.35f) / 0.18f).coerceIn(0f, 1f)
    val holdBounce = if (showArtwork) kotlin.math.sin(((progress - 0.55f).coerceAtLeast(0f)) * 22f) * 0.035f else 0f
    val cardScale = if (showArtwork) (0.72f + revealPhase * 0.33f + holdBounce).coerceIn(0.72f, 1.08f) else (0.8f + rollPhase * 0.22f)
    val diceRotation = 1440f * rollPhase

    Box(
        modifier = modifier
            .background(Color(0xFF061127).copy(alpha = 0.62f * overlayAlpha)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .graphicsLayer { alpha = overlayAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        rotationZ = if (showArtwork) 0f else diceRotation
                        scaleX = cardScale
                        scaleY = cardScale
                        shadowElevation = 30f
                    }
                    .clip(RoundedCornerShape(34.dp))
                    .background(Color.White)
                    .border(3.dp, Color(0xFF2E6BFF).copy(alpha = 0.65f), RoundedCornerShape(34.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showArtwork && SelectionArtworkAssets.hasBaseArtworkForTitle(LocalContext.current, item.title)) {
                    ArtworkThumbnail(
                        title = item.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(26.dp)),
                        preferStaticBase = false,
                        showPanel = false,
                        fillPreview = true
                    )
                } else {
                    Text("🎲", fontSize = 92.sp)
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = if (isOpening) "Loading..." else if (showArtwork) item.title else "Rolling...",
                color = Color.White,
                fontSize = if (showArtwork) 28.sp else 24.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isOpening) "Preparing your canvas" else if (showArtwork) "Opening your random page" else "Picking a surprise artwork",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MischiefPickForMeButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "pick-for-me-mischief")
    val wiggle by infinite.animateFloat(
        initialValue = -0.8f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pick-for-me-wiggle"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .graphicsLayer { rotationZ = if (enabled) wiggle else 0f }
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (enabled) {
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF2E6BFF),
                            Color(0xFF7C3AED),
                            Color(0xFFFF5F93),
                            Color(0xFFFF9144)
                        )
                    )
                } else {
                    Brush.linearGradient(listOf(DoseviaMuted.copy(alpha = 0.35f), DoseviaMuted.copy(alpha = 0.22f)))
                }
            )
            .border(
                width = 2.dp,
                color = Color.White.copy(alpha = if (enabled) 0.55f else 0.25f),
                shape = RoundedCornerShape(28.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "🎲", fontSize = 25.sp, fontWeight = FontWeight.Black)
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Pick For Me",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Surprise me with a random page",
                    color = Color.White.copy(alpha = 0.86f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(text = "✨", fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PremiumHeroCard(
    genre: String,
    item: CatalogItem?,
    pageCount: Int,
    onOpenCategory: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 330.dp,
    cardHeight: Dp = 505.dp,
    imageHeight: Dp = 350.dp
) {
    Surface(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .clip(RoundedCornerShape(30.dp))
            .clickable(onClick = onOpenCategory),
        shape = RoundedCornerShape(30.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.72f)),
        shadowElevation = 22.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = item?.title ?: genre,
                    transitionSpec = { fadeIn(animationSpec = tween(360)) togetherWith fadeOut(animationSpec = tween(220)) },
                    label = "premium-hero-item-cycle"
                ) { shownTitle ->
                    if (item != null && SelectionArtworkAssets.hasBaseArtworkForTitle(LocalContext.current, shownTitle)) {
                        ArtworkThumbnail(
                            title = shownTitle,
                            modifier = Modifier.fillMaxSize(),
                            preferStaticBase = false,
                            showPanel = false,
                            fillPreview = true
                        )
                    } else {
                        PlaceholderCategoryImage(genre = shownTitle, modifier = Modifier.fillMaxSize())
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item?.title ?: genre, color = Color(0xFF001233), fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text("$pageCount pages · $genre", color = Color(0xFF5F6E82), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == 4) 18.dp else 7.dp, 7.dp)
                                    .background(if (index == 4) DoseviaPinkPrimary else Color(0xFFC8CDD6), RoundedCornerShape(50.dp))
                            )
                        }
                    }
                }
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary, contentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)
                ) {
                    Text("Continue", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun PremiumSidePeek(genre: String, item: CatalogItem?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .width(118.dp)
            .height(260.dp)
            .clip(RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)),
        shadowElevation = 10.dp
    ) {
        Box(modifier = Modifier.background(previewPanelTintForCategory(genre)), contentAlignment = Alignment.Center) {
            if (item != null && SelectionArtworkAssets.hasBaseArtworkForTitle(LocalContext.current, item.title)) {
                ArtworkThumbnail(title = item.title, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)), showPanel = false, fillPreview = true)
            } else {
                PlaceholderCategoryImage(genre = item?.title ?: genre, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun PremiumCategoryStripCard(
    genre: String,
    item: CatalogItem?,
    count: Int,
    isComingSoon: Boolean = false,
    onClick: () -> Unit
) {
    val accent = accentForCategory(genre)
    Surface(
        modifier = Modifier
            .width(162.dp)
            .height(260.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = !isComingSoon, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = if (isComingSoon) Color(0xFFE9EEF6) else cardTintForCategory(genre),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isComingSoon) Color(0xFFB5C0CF) else DoseviaBorder),
        shadowElevation = if (isComingSoon) 4.dp else 10.dp
    ) {
        if (isComingSoon) {
            ComingSoonFullCategoryCardContent(
                genre = genre,
                modifier = Modifier.fillMaxSize().padding(10.dp),
                titleFontSize = 18.sp,
                subtitleFontSize = 12.sp,
                panelCorner = 20.dp
            )
        } else {
            Column(modifier = Modifier.padding(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(142.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(1.dp, DoseviaBorder, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (item != null && SelectionArtworkAssets.hasBaseArtworkForTitle(LocalContext.current, item.title)) {
                            ArtworkThumbnail(
                                title = item.title,
                                modifier = Modifier.fillMaxSize(),
                                preferStaticBase = false,
                                showPanel = false,
                                fillPreview = true
                            )
                        } else {
                            PlaceholderCategoryImage(genre = item?.title ?: genre, modifier = Modifier.fillMaxSize())
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color(0xF2EDF4FF), RoundedCornerShape(50.dp))
                            .border(1.dp, accent.copy(alpha = 0.58f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$count page", color = DoseviaPinkPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(genre, color = DoseviaText, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSpecialGenre(genre)) MonetizationStore.specialUnlockRemainingText(genre) else "$count page${if (count == 1) "" else "s"} available",
                        color = if (isSpecialGenre(genre) && MonetizationStore.specialUnlockRemainingText(genre) != "Watch ad to open") DoseviaGreen else DoseviaMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.size(12.dp).background(accent, RoundedCornerShape(50.dp)))
                }
            }
        }
    }
}

private fun heroGradientForCategory(category: String): Brush {
    return Brush.linearGradient(listOf(Color(0xFFF7DE59), Color(0xFFF3B84B)))
}

private fun categoryIcon(genre: String): String {
    return when {
        genre.equals("Dinosaurs", ignoreCase = true) -> "🦖"
        genre.equals("Sports", ignoreCase = true) -> "🏆"
        genre.equals("Dragons", ignoreCase = true) -> "🐉"
        genre.equals("Space", ignoreCase = true) -> "🚀"
        genre.equals("Desserts", ignoreCase = true) -> "🧁"
        else -> "🍎"
    }
}

private fun categorySubtitle(genre: String): String {
    return when {
        genre.equals("Dinosaurs", ignoreCase = true) -> "Special prehistoric pages"
        genre.equals("Sports", ignoreCase = true) -> "Free action and game pages"
        genre.equals("Desserts", ignoreCase = true) -> "Sweet food pages"
        else -> "Coloring pages"
    }
}

private fun accentForCategory(category: String): Color {
    return when {
        category.equals("Dinosaurs", ignoreCase = true) -> Color(0xFF7C5CFF)
        category.equals("Sports", ignoreCase = true) -> Color(0xFF2E6BFF)
        category.equals("Dragons", ignoreCase = true) -> Color(0xFFF17878)
        category.equals("Space", ignoreCase = true) -> Color(0xFF4B69FF)
        category.equals("Desserts", ignoreCase = true) -> Color(0xFFF27C78)
        else -> DoseviaPinkPrimary
    }
}

private fun cardTintForCategory(category: String): Color {
    return when {
        category.equals("Dinosaurs", ignoreCase = true) -> Color(0xFFEDE7FF)
        category.equals("Sports", ignoreCase = true) -> Color(0xFFEAF1FF)
        category.equals("Dragons", ignoreCase = true) -> Color(0xFFFFECEA)
        category.equals("Space", ignoreCase = true) -> Color(0xFFE9EDFF)
        category.equals("Desserts", ignoreCase = true) -> Color(0xFFFFF1EC)
        else -> Color(0xFFFFF0D8)
    }
}

private fun previewPanelTintForCategory(category: String): Color {
    return when {
        category.equals("Dinosaurs", ignoreCase = true) -> Color(0xFFE7E0FF)
        category.equals("Sports", ignoreCase = true) -> Color(0xFFF4F8FF)
        category.equals("Dragons", ignoreCase = true) -> Color(0xFFFFF3F1)
        category.equals("Space", ignoreCase = true) -> Color(0xFFF2F5FF)
        category.equals("Desserts", ignoreCase = true) -> Color(0xFFFFF6F1)
        else -> Color(0xFFFFF7CC)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun CategoryTile(
    genre: String,
    itemCount: Int,
    representative: CatalogItem?,
    selected: Boolean,
    isComingSoon: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = accentForCategory(genre)
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(274.dp)
            .clip(shape)
            .clickable(enabled = !isComingSoon, onClick = onClick),
        shape = shape,
        color = if (isComingSoon) Color(0xFFE9EEF6) else cardTintForCategory(genre),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = when {
                selected -> accent.copy(alpha = 0.85f)
                isComingSoon -> Color(0xFFB5C0CF)
                else -> DoseviaBorder
            }
        ),
        shadowElevation = if (isComingSoon) 4.dp else 10.dp
    ) {
        if (isComingSoon) {
            ComingSoonFullCategoryCardContent(
                genre = genre,
                modifier = Modifier.fillMaxSize().padding(12.dp),
                titleFontSize = 16.sp,
                subtitleFontSize = 11.sp,
                panelCorner = 18.dp
            )
        } else {
            Column(modifier = Modifier.padding(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(1.dp, DoseviaBorder, RoundedCornerShape(18.dp))
                ) {
                    AnimatedContent(
                        targetState = representative?.title ?: genre,
                        transitionSpec = { fadeIn(animationSpec = tween(260)) togetherWith fadeOut(animationSpec = tween(220)) },
                        label = "category-item-cycle"
                    ) { shownTitle ->
                        if (representative != null) {
                            ArtworkThumbnail(
                                title = shownTitle,
                                modifier = Modifier.fillMaxSize(),
                                preferStaticBase = false,
                                showPanel = false
                            )
                        } else {
                            PlaceholderCategoryImage(genre = genre, modifier = Modifier.fillMaxSize())
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color(0xF2EDF4FF), RoundedCornerShape(50.dp))
                            .border(1.dp, accent.copy(alpha = 0.58f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$itemCount page", color = DoseviaPinkPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    genre,
                    color = DoseviaText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val specialStatus = if (isSpecialGenre(genre)) MonetizationStore.specialUnlockRemainingText(genre) else null
                    Text(
                        text = specialStatus ?: "$itemCount page${if (itemCount == 1) "" else "s"} available",
                        color = if (specialStatus != null && specialStatus != "Watch ad to open") DoseviaGreen else DoseviaMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(modifier = Modifier.size(10.dp).background(accent, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun ComingSoonFullCategoryCardContent(
    genre: String,
    modifier: Modifier = Modifier,
    titleFontSize: androidx.compose.ui.unit.TextUnit,
    subtitleFontSize: androidx.compose.ui.unit.TextUnit,
    panelCorner: Dp
) {
    val panelShape = RoundedCornerShape(panelCorner)
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(panelShape)
                .background(Color.White)
                .border(1.dp, DoseviaBorder, panelShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(panelShape)
                    .background(Color(0xEE061225))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🔒", fontSize = 27.sp, maxLines = 1)
                    Text("COMING SOON", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 1)
                    Text("Not available yet", color = Color.White.copy(alpha = 0.84f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            genre,
            color = DoseviaText,
            fontWeight = FontWeight.ExtraBold,
            fontSize = titleFontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Coming soon",
                color = DoseviaMuted,
                fontSize = subtitleFontSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(modifier = Modifier.size(12.dp).background(Color(0xFF8A94A6), CircleShape))
        }
    }
}

@Composable
private fun ComingSoonCardOverlay() {
    ComingSoonFullCardOverlay()
}

@Composable
private fun ComingSoonFullCardOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
            .clip(RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 86.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(Color(0xEE061225))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🔒", fontSize = 24.sp, maxLines = 1)
                Text("COMING SOON", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 1)
                Text("Not available yet", color = Color.White.copy(alpha = 0.82f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PlaceholderCategoryImage(genre: String, modifier: Modifier = Modifier) {
    val accent = accentForCategory(genre)
    Box(
        modifier = modifier.background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            val stroke = Stroke(width = size.minDimension * 0.035f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val w = size.width
            val h = size.height
            drawRoundRect(color = Color.White.copy(alpha = 0.16f), size = size.copy(width = w * 0.74f, height = h * 0.62f), topLeft = Offset(w * 0.13f, h * 0.18f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f), style = stroke)
            drawCircle(color = Color.White.copy(alpha = 0.12f), radius = w * 0.22f, center = Offset(w * 0.45f, h * 0.45f), style = stroke)
            drawLine(color = Color.White.copy(alpha = 0.16f), start = Offset(w * 0.25f, h * 0.68f), end = Offset(w * 0.75f, h * 0.36f), strokeWidth = w * 0.025f, cap = StrokeCap.Round)
        }
        Text(genre.take(1), color = Color.White.copy(alpha = 0.85f), fontSize = 46.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun LoadingWaveIndicator(modifier: Modifier = Modifier) {
    OperationWaveOverlay(word = "LOADING", modifier = modifier, dimBackground = false)
}

@Composable
private fun HelpGuidePage(onBack: () -> Unit) {
    val guideImages = remember {
        listOf(
            R.drawable.help_color_with_one_finger,
            R.drawable.help_zoom_and_move_with_two_fingers,
            R.drawable.help_pick_a_color,
            R.drawable.help_undo_and_redo,
            R.drawable.help_finish_and_download,
            R.drawable.help_finished_layer_limit,
            R.drawable.help_draft_auto_save,
            R.drawable.help_share_to_community,
            R.drawable.help_browse_community,
            R.drawable.help_view_artwork_details
        )
    }
    var expandedImage by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 1.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
            ) {
                Text(
                    text = "Help & Guide",
                    color = DoseviaText,
                    fontSize = 29.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tap any guide card to view it larger and zoom in.",
                    color = DoseviaMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Surface(
                onClick = onBack,
                modifier = Modifier.heightIn(min = 44.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "‹",
                        color = DoseviaPinkPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Back",
                        color = DoseviaText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        guideImages.forEach { imageRes ->
            HelpGuideImageCard(
                imageRes = imageRes,
                onClick = { expandedImage = imageRes }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
    }

    expandedImage?.let { imageRes ->
        HelpGuideImagePreviewDialog(
            imageRes = imageRes,
            onDismiss = { expandedImage = null }
        )
    }
}

@Composable
private fun HelpGuideImageCard(
    imageRes: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(1.dp, DoseviaBorder, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f),
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
private fun HelpGuideImagePreviewDialog(
    imageRes: Int,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEE08101F))
                .pointerInput(imageRes) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 4f)
                        val maxX = size.width * (newScale - 1f) * 0.5f
                        val maxY = size.height * (newScale - 1f) * 0.5f
                        scale = newScale
                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                            y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.98f)
                    .aspectRatio(1.5f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Fit,
            )

            Surface(
                onClick = {
                    scale = 1f
                    offset = Offset.Zero
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.96f),
                border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)
            ) {
                Text(
                    text = "Reset",
                    color = DoseviaText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            Surface(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.96f),
                border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)
            ) {
                Text(
                    text = "Close",
                    color = DoseviaText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun ProgressTab(
    onOpenEditor: (String) -> Unit,
    onEditFinished: (FinishedArtwork) -> Unit
) {
    val version = ArtworkStore.globalUpdateTick.intValue
    val titles = remember(version) { ArtworkStore.allProgressTitles() }
    var showHelpPage by remember { mutableStateOf(false) }
    var selectedLibraryTab by rememberSaveable { mutableStateOf("All") }

    if (showHelpPage) {
        BackHandler { showHelpPage = false }
        HelpGuidePage(onBack = { showHelpPage = false })
        return
    }

    val draftTitles = remember(titles, version) { titles.filter { ArtworkStore.hasDraft(it) } }
    val finishedTitles = remember(titles, version) { titles.filter { ArtworkStore.hasFinished(it) } }
    val sharedCount = AchievementStore.counter("community_shares")
    val visibleTitles = remember(selectedLibraryTab, titles, draftTitles, finishedTitles) {
        when (selectedLibraryTab) {
            "Drafts" -> draftTitles
            "Finished" -> finishedTitles
            else -> titles
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProfileSectionHeader("HELP")
        HelpAndGuideCard(onOpen = { showHelpPage = true })
        AdBannerPlaceholder()

        ProfileSectionHeader("MY ARTWORKS")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = DoseviaCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileStat("Drafts", draftTitles.size.toString(), Modifier.weight(1f))
                    ProfileStat("Finished", finishedTitles.size.toString(), Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Drafts", "Finished").forEach { tab ->
                        MyArtworkTabChip(
                            title = tab,
                            selected = selectedLibraryTab == tab,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedLibraryTab = tab }
                        )
                    }
                }
            }
        }

        ProfileSectionHeader(
            when (selectedLibraryTab) {
                "Drafts" -> "DRAFT ARTWORK"
                "Finished" -> "FINISHED ARTWORK"
                else -> "ALL ARTWORK PROGRESS"
            }
        )

        when {
            visibleTitles.isEmpty() -> {
                InfoPanel(
                    when (selectedLibraryTab) {
                        "Drafts" -> "No Drafts Yet"
                        "Finished" -> "No Finished Artwork Yet"
                        else -> "No Progress Yet"
                    },
                    when (selectedLibraryTab) {
                        "Drafts" -> "Start coloring a page and leave the editor to create a draft."
                        "Finished" -> "Finish and save an artwork to see it here."
                        else -> "Start an artwork from Selections and your progress will appear here."
                    }
                )
            }
            else -> visibleTitles.forEach { title ->
                ProgressCard(
                    title = title,
                    onOpenEditor = onOpenEditor,
                    onEditFinished = onEditFinished
                )
            }
        }
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun MyArtworkTabChip(title: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .heightIn(min = 46.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) DoseviaPinkPrimary else DoseviaSoftPanel,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) DoseviaPinkPrimary else DoseviaBorder),
        shadowElevation = if (selected) 5.dp else 1.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 11.dp), contentAlignment = Alignment.Center) {
            Text(
                title,
                color = if (selected) Color.White else DoseviaText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HelpAndGuideCard(onOpen: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = DoseviaCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Help & Guide",
                color = DoseviaText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Coloring, zooming, saving, finished layers, downloads, and draft tips are kept here in Me.",
                color = DoseviaMuted,
                fontSize = 14.sp
            )
            Button(
                onClick = onOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary)
            ) {
                Text(
                    text = "Open Help & Guide",
                    color = Color(0xFFEFF4FA),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProgressCard(
    title: String,
    onOpenEditor: (String) -> Unit,
    onEditFinished: (FinishedArtwork) -> Unit
) {
    val draftCount = ArtworkStore.draftStrokeCount(title)
    val finishedCount = ArtworkStore.finishedFor(title).size
    var showFinishedLayers by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DoseviaCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArtworkThumbnail(
                title = title,
                modifier = Modifier
                    .width(60.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = DoseviaText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(if (draftCount < 0) "Draft saved" else "$draftCount draft strokes", color = DoseviaMuted, fontSize = 11.sp)
                Text("$finishedCount finished layers", color = DoseviaGreen, fontSize = 11.sp)
                if (finishedCount > 0) {
                    TextButton(
                        onClick = { showFinishedLayers = !showFinishedLayers },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("View All", color = DoseviaPinkPrimary, fontSize = 10.sp)
                    }
                }
            }
            Button(
                onClick = { onOpenEditor(title) },
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Open", color = Color(0xFFEFF4FA), fontSize = 13.sp)
            }
        }
    }

    if (showFinishedLayers) {
        FinishedLayersDialog(
            title = title,
            onDismiss = { showFinishedLayers = false },
            onEditFinished = onEditFinished
        )
    }
}

@Composable
private fun FinishedLayersDialog(
    title: String,
    onDismiss: () -> Unit,
    onEditFinished: (FinishedArtwork) -> Unit
) {
    val version = ArtworkStore.globalUpdateTick.intValue
    val finishedLayers = remember(title, version) { ArtworkStore.finishedFor(title).sortedByDescending { it.timestamp } }
    var pendingDeleteArtwork by remember { mutableStateOf<FinishedArtwork?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(34.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .background(DoseviaCard, RoundedCornerShape(34.dp))
                    .border(1.dp, DoseviaBorder, RoundedCornerShape(34.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .heightIn(max = 600.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("FINISHED LIBRARY", color = DoseviaGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$title Layers", color = DoseviaText, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Choose a saved version to show, edit, or delete.", color = DoseviaMuted, fontSize = 13.sp)
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(DoseviaGreen.copy(alpha = 0.16f), CircleShape)
                                .border(1.dp, DoseviaGreen.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(finishedLayers.size.toString(), color = DoseviaGreen, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }

                    if (finishedLayers.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = DoseviaCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "No finished layers saved yet.",
                                color = DoseviaMuted,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    } else {
                        finishedLayers.forEachIndexed { index, artwork ->
                            FinishedLayerItem(
                                artwork = artwork,
                                number = index + 1,
                                onEdit = {
                                    val originalIndex = ArtworkStore.finishedFor(artwork.title).indexOfFirst { it.id == artwork.id }
                                    ArtworkStore.displayFinished(artwork.title, originalIndex)
                                    onDismiss()
                                    onEditFinished(artwork)
                                },
                                onShowOnHome = {
                                    val originalIndex = ArtworkStore.finishedFor(artwork.title).indexOfFirst { it.id == artwork.id }
                                    ArtworkStore.displayFinished(artwork.title, originalIndex)
                                },
                                onDelete = { pendingDeleteArtwork = artwork }
                            )
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DoseviaSoftPanel, contentColor = DoseviaText)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    pendingDeleteArtwork?.let { artwork ->
        PremiumChoiceModal(
            eyebrow = "Delete layer",
            title = "Delete ${artwork.layerName}?",
            body = "This permanently removes this finished layer. Your draft and other finished versions will not be touched.",
            primaryText = "Delete",
            secondaryText = "Cancel",
            danger = true,
            onPrimary = {
                ArtworkStore.deleteFinishedArtwork(artwork.id)
                pendingDeleteArtwork = null
                if (ArtworkStore.finishedFor(title).isEmpty()) onDismiss()
            },
            onSecondary = { pendingDeleteArtwork = null },
            onDismiss = { pendingDeleteArtwork = null }
        )
    }
}

@Composable
private fun FinishedLayerItem(
    artwork: FinishedArtwork,
    number: Int,
    onEdit: () -> Unit,
    onShowOnHome: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var communityMessage by remember { mutableStateOf<String?>(null) }
    var showShareConfirm by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = DoseviaCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArtworkThumbnail(
                    title = artwork.title,
                    strokesOverride = artwork.strokes,
                    modifier = Modifier
                        .width(68.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Layer $number", color = DoseviaGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(artwork.layerName, color = DoseviaText, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Finished artwork", color = DoseviaMuted, fontSize = 12.sp)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DoseviaGreen, contentColor = Color.White)
                ) { Text("Edit", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
                Button(
                    onClick = onShowOnHome,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary, contentColor = Color.White)
                ) { Text("Show", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
                Button(
                    onClick = {
                        if (GoogleAuthController.refreshCurrentAccount(context) == null) {
                            communityMessage = "Please sign in to share your artwork."
                        } else {
                            showShareConfirm = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DoseviaSoftPanel, contentColor = DoseviaText)
                ) { Text(if (GoogleAuthController.refreshCurrentAccount(context) == null) "Sign In" else "Share to Community", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DoseviaRed, contentColor = Color.White)
                ) { Text("Delete", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
            }
        }
    }
    if (showShareConfirm) {
        PremiumChoiceModal(
            eyebrow = "Community",
            title = "Share ${artwork.title}?",
            body = "Share this finished artwork to your public Community gallery.",
            primaryText = "Share",
            secondaryText = "Cancel",
            onPrimary = {
                showShareConfirm = false
                scope.launch {
                    runCatching {
                        val result = withContext(Dispatchers.IO) {
                            CommunityRepository.shareFinishedArtwork(context, artwork, null)
                        }
                        communityMessage = result.message
                    }.onFailure { error ->
                        communityMessage = "Could not share to Community: ${error.message ?: error.javaClass.simpleName}"
                    }
                }
            },
            onSecondary = { showShareConfirm = false },
            danger = false,
            onDismiss = { showShareConfirm = false }
        )
    }
    communityMessage?.let { text ->
        CommunityMessageDialog(message = text, onDismiss = { communityMessage = null })
    }
}

@Composable
private fun CatalogCard(
    item: CatalogItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = cardTintForCategory(item.category),
        border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
        shadowElevation = 10.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(previewPanelTintForCategory(item.category))
                    .border(1.dp, DoseviaBorder, RoundedCornerShape(18.dp))
            ) {
                ArtworkThumbnail(
                    title = item.title,
                    modifier = Modifier.fillMaxSize(),
                    showPanel = false
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color(0xDDE6EEF8), RoundedCornerShape(50.dp))
                        .border(1.dp, item.accent.copy(alpha = 0.65f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.category, color = DoseviaPinkPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.title,
                color = DoseviaText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusText(item.title),
                    color = DoseviaMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(item.accent, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ArtworkThumbnail(
    title: String,
    modifier: Modifier = Modifier,
    strokesOverride: List<StrokeData>? = null,
    preferStaticBase: Boolean = false,
    showPanel: Boolean = true,
    fillPreview: Boolean = false
) {
    val context = LocalContext.current
    val version = ArtworkStore.globalUpdateTick.intValue
    val previewStrokes = remember(title, version, strokesOverride, preferStaticBase) {
        if (preferStaticBase) emptyList() else (strokesOverride ?: ArtworkStore.previewStrokes(title))
    }
    val diskPreviewVersion = remember(title, version, preferStaticBase) {
        if (preferStaticBase) 0L else ArtworkStore.previewVersion(title)
    }
    val previewSignature = remember(previewStrokes, version, diskPreviewVersion, preferStaticBase) {
        if (preferStaticBase) {
            -1
        } else {
            val strokeSignature = previewStrokes.sumOf { stroke -> stroke.serializableActions.size } + (previewStrokes.size * 10_000)
            (31L * strokeSignature.toLong() + diskPreviewVersion).hashCode()
        }
    }
    val initialBitmap = remember(title, previewSignature, strokesOverride, preferStaticBase) {
        HomePreviewCache.get(title, previewSignature, preferStaticBase)
    }
    val initialBaseBitmap = remember(title) {
        HomePreviewCache.get(title, 0, true)
    }
    var thumbnailBitmap by remember(title, previewSignature, strokesOverride, preferStaticBase) { mutableStateOf(initialBitmap) }
    var baseBitmap by remember(title) { mutableStateOf(initialBaseBitmap) }
    var isThumbnailLoading by remember(title, previewSignature, strokesOverride, preferStaticBase) { mutableStateOf(initialBitmap == null) }

    LaunchedEffect(title) {
        if (baseBitmap == null) {
            baseBitmap = withContext(Dispatchers.IO) {
                HomePreviewCache.getOrCreate(
                    context = context.applicationContext,
                    title = title,
                    strokes = emptyList(),
                    signature = 0,
                    staticBase = true
                )
            }
        }
    }

    LaunchedEffect(title, previewSignature, strokesOverride, preferStaticBase) {
        val cached = HomePreviewCache.get(title, previewSignature, preferStaticBase)
        if (cached != null) {
            thumbnailBitmap = cached
            isThumbnailLoading = false
        } else {
            isThumbnailLoading = true
            thumbnailBitmap = withContext(Dispatchers.IO) {
                HomePreviewCache.getOrCreate(
                    context = context.applicationContext,
                    title = title,
                    strokes = previewStrokes,
                    signature = previewSignature,
                    staticBase = preferStaticBase
                )
            }
            isThumbnailLoading = !preferStaticBase && thumbnailBitmap == null && ArtworkStore.hasAnyProgress(title)
        }
    }

    val thumbnailModifier = if (showPanel) {
        modifier
            .background(Color(0xFFF5F7FB))
            .border(1.dp, DoseviaBorder, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
    } else {
        modifier
    }
    Box(
        modifier = thumbnailModifier,
        contentAlignment = Alignment.Center
    ) {
        val safeThumbnailBitmap = thumbnailBitmap
        val safeBaseBitmap = baseBitmap
        if (safeThumbnailBitmap != null) {
            Image(
                bitmap = safeThumbnailBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = if (fillPreview) ContentScale.FillBounds else ContentScale.Fit,
            )
        } else if (safeBaseBitmap != null) {
            Image(
                bitmap = safeBaseBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = if (fillPreview) ContentScale.FillBounds else ContentScale.Fit,
            )
        } else {
            PlaceholderCategoryImage(genre = title, modifier = Modifier.fillMaxSize())
        }

        if (isThumbnailLoading && !preferStaticBase) {
            ThumbnailLoadingOverlay(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ThumbnailLoadingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.58f))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.9f),
            shape = RoundedCornerShape(999.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                MiniInlineWaveWord("LOADING", fontSize = 11)
            }
        }
    }
}

@Composable
private fun OperationWaveOverlay(word: String, modifier: Modifier = Modifier, dimBackground: Boolean = true) {
    if (!dimBackground) {
        OperationWaveOverlayContent(word = word, modifier = modifier, dimBackground = false)
        return
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        OperationWaveOverlayContent(
            word = word,
            modifier = Modifier.fillMaxSize(),
            dimBackground = true
        )
    }
}

@Composable
private fun OperationWaveOverlayContent(word: String, modifier: Modifier = Modifier, dimBackground: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "operation-wave")
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(if (dimBackground) Color(0xE605070D) else Color(0xF2E6EEF8)),
        contentAlignment = Alignment.Center
    ) {
        val panelWidth = (maxWidth * 0.78f).coerceAtMost(330.dp)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.width(panelWidth),
                shape = RoundedCornerShape(26.dp),
                color = DoseviaCard.copy(alpha = 0.98f),
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    word.forEachIndexed { index, char ->
                        val y by transition.animateFloat(
                            initialValue = 0f,
                            targetValue = -9f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 520, delayMillis = index * 45),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "operation-wave-char-$index"
                        )
                        Text(
                            text = char.toString(),
                            color = DoseviaPinkPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.graphicsLayer { translationY = y }
                        )
                        if (char != ' ') Spacer(Modifier.width(3.dp))
                        else Spacer(Modifier.width(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniInlineWaveWord(word: String, fontSize: Int) {
    val transition = rememberInfiniteTransition(label = "mini-inline-wave")
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.CenterVertically) {
        word.forEachIndexed { index, char ->
            val y by transition.animateFloat(
                initialValue = 0f,
                targetValue = -5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 520, delayMillis = index * 45),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "mini-inline-wave-char-$index"
            )
            Text(
                text = char.toString(),
                color = DoseviaPinkPrimary,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.graphicsLayer { translationY = y }
            )
        }
    }
}


object HomePreviewCache {
    private val bitmaps = linkedMapOf<String, android.graphics.Bitmap>()
    private const val MAX_ENTRIES = 24

    @Synchronized
    fun clear() {
        bitmaps.clear()
    }

    @Synchronized
    fun get(title: String, signature: Int, staticBase: Boolean): android.graphics.Bitmap? {
        val key = key(title, signature, staticBase)
        return bitmaps[key]?.takeIf { !it.isRecycled }
    }

    @Synchronized
    fun put(title: String, signature: Int, staticBase: Boolean, bitmap: android.graphics.Bitmap) {
        val key = key(title, signature, staticBase)
        bitmaps[key] = bitmap
        while (bitmaps.size > MAX_ENTRIES) {
            val first = bitmaps.keys.firstOrNull() ?: break
            bitmaps.remove(first)
        }
    }


    /**
     * Intentionally no-op. Preloading every category artwork made app launch heavy.
     * Previews are now created lazily only for visible cards.
     */
    fun preloadStaticBaseBitmaps(context: android.content.Context, entries: List<SelectionArtworkAssets.CatalogEntry>) = Unit

    fun getOrCreate(
        context: android.content.Context,
        title: String,
        strokes: List<StrokeData>,
        signature: Int,
        staticBase: Boolean
    ): android.graphics.Bitmap? {
        get(title, signature, staticBase)?.let { return it }

        if (!staticBase) {
            val savedOrRebuilt = ArtworkStore.ensureFreshDisplayPreviewBitmap(title)
            if (savedOrRebuilt != null) {
                put(title, signature, staticBase, savedOrRebuilt)
                return savedOrRebuilt
            }
        }

        val sourceStrokes = if (!staticBase && strokes.isEmpty()) {
            ArtworkStore.loadThumbnailSourceStrokes(title)
        } else {
            strokes
        }

        if (!staticBase && sourceStrokes.isEmpty() && ArtworkStore.hasAnyProgress(title)) {
            return null
        }

        val created = try {
            if (staticBase || sourceStrokes.isEmpty()) {
                SelectionArtworkAssets.loadBasePreviewBitmap(context, title)?.let { source ->
                    scaleBitmapForHomePreview(source)
                }
            } else {
                val (baseWidth, baseHeight) = SelectionArtworkAssets.baseSize(context, title)
                val previewWidth = 768
                val previewHeight = ((previewWidth.toFloat() * baseHeight.toFloat() / baseWidth.toFloat().coerceAtLeast(1f)).roundToInt()).coerceAtLeast(1)
                ArtworkRenderer.renderArtworkBitmap(context, sourceStrokes, previewWidth, previewHeight, title)
                    ?: SelectionArtworkAssets.loadBasePreviewBitmap(context, title)?.let { source -> scaleBitmapForHomePreview(source) }
            }
        } catch (error: Throwable) {
            // Never let a thumbnail/preview render crash app startup. Fall back to safe line art.
            SelectionArtworkAssets.loadBasePreviewBitmap(context, title)
        }

        // Do not crop thumbnails to the detected ink bounds. Cropping created non-square
        // preview bitmaps that left colored side bars inside category cards and made
        // edited artwork look inconsistent compared with untouched artwork.
        val previewReady = created
        if (previewReady != null) put(title, signature, staticBase, previewReady)
        return previewReady
    }


    private fun scaleBitmapForHomePreview(source: android.graphics.Bitmap): android.graphics.Bitmap {
        if (source.isRecycled) return source
        val targetWidth = 768
        if (source.width <= targetWidth) return source
        val targetHeight = ((targetWidth.toFloat() * source.height.toFloat() / source.width.toFloat()).roundToInt()).coerceAtLeast(1)
        return android.graphics.Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    private fun cropToVisibleArtworkBounds(bitmap: android.graphics.Bitmap): android.graphics.Bitmap {
        if (bitmap.width <= 2 || bitmap.height <= 2 || bitmap.isRecycled) return bitmap

        var left = bitmap.width
        var top = bitmap.height
        var right = -1
        var bottom = -1
        val pixels = IntArray(bitmap.width)

        for (y in 0 until bitmap.height) {
            bitmap.getPixels(pixels, 0, bitmap.width, 0, y, bitmap.width, 1)
            for (x in 0 until bitmap.width) {
                val color = pixels[x]
                val alpha = color ushr 24
                val red = (color ushr 16) and 0xFF
                val green = (color ushr 8) and 0xFF
                val blue = color and 0xFF
                val visibleInkOrColor = alpha > 18 && (red < 245 || green < 245 || blue < 245)
                if (visibleInkOrColor) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }

        if (right <= left || bottom <= top) return bitmap

        val rawWidth = right - left + 1
        val rawHeight = bottom - top + 1
        val padding = ((minOf(rawWidth, rawHeight) * 0.08f).roundToInt()).coerceIn(10, 42)
        val cropLeft = (left - padding).coerceAtLeast(0)
        val cropTop = (top - padding).coerceAtLeast(0)
        val cropRight = (right + padding).coerceAtMost(bitmap.width - 1)
        val cropBottom = (bottom + padding).coerceAtMost(bitmap.height - 1)
        val cropWidth = cropRight - cropLeft + 1
        val cropHeight = cropBottom - cropTop + 1

        if (cropWidth >= bitmap.width * 0.96f && cropHeight >= bitmap.height * 0.96f) return bitmap

        return try {
            android.graphics.Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)
        } catch (error: Throwable) {
            bitmap
        }
    }

    private fun key(title: String, signature: Int, staticBase: Boolean): String =
        title + "_" + signature + "_" + staticBase
}

private fun placeholderItemsForCategory(genre: String, accent: Color = accentForCategory(genre)): List<CatalogItem> {
    val names = when (genre.lowercase()) {
        "fruits" -> listOf("APPLE", "BANANA", "CHERRY", "DRAGON FRUIT", "ELDERBERRY", "FIG FRUIT", "GRAPES", "HONEYDEW", "INDIAN FIG", "JACKFRUIT", "KIWI FRUIT", "LEMON", "MANGO", "NECTARINE", "ORANGE", "PEAR", "QUINCE", "RASPBERRY", "STRAWBERRY", "TOMATO", "UGLI FRUIT", "VALENCIA ORANGE", "WATERMELON", "XIMENIA", "YUZU", "ZIZIPHUS")
        "vegetables" -> listOf("Carrot", "Broccoli", "Corn", "Pumpkin", "Potato", "Peas")
        "objects" -> listOf("Ball", "Cup", "Book", "Lamp", "Clock", "Chair")
        "animals" -> listOf("Cat", "Dog", "Bird", "Fish", "Bear", "Rabbit")
        "buildings" -> listOf("House", "Castle", "School", "Shop", "Tower")
        "characters" -> listOf("Hero", "Princess", "Robot", "Wizard", "Pirate")
        "flowers" -> listOf("Rose", "Tulip", "Sunflower", "Daisy", "Lily")
        "vehicles" -> listOf("Car", "Bus", "Train", "Plane", "Boat")
        else -> listOf("Preview 1", "Preview 2", "Preview 3", "Preview 4", "Preview 5")
    }
    return names.map { CatalogItem(it, genre, accent) }
}

private fun previewPoolForCategory(genre: String, realItems: List<CatalogItem>): List<CatalogItem> {
    // Real assets must always win. Mixing placeholder names with real asset names caused
    // category/home previews to sometimes select artwork that did not exist, which made
    // Kiwi and other pages appear missing or disappear during recomposition.
    if (realItems.isNotEmpty()) return realItems.distinctBy { it.title.lowercase() }.sortedBy { it.title.lowercase() }

    val accent = accentForCategory(genre)
    return placeholderItemsForCategory(genre, accent).distinctBy { it.title.lowercase() }
}

private object HomeRandomPreviewState {
    private val random = Random(System.currentTimeMillis())
    private val lastTickByGenre = mutableMapOf<String, Int>()
    private val lastIndexByGenre = mutableMapOf<String, Int>()

    fun pick(genre: String, pool: List<CatalogItem>, tick: Int): CatalogItem? {
        if (pool.isEmpty()) return null

        val previousTick = lastTickByGenre[genre]
        val previousIndex = lastIndexByGenre[genre]
        if (previousTick == tick && previousIndex != null && previousIndex in pool.indices) {
            return pool[previousIndex]
        }

        var nextIndex = random.nextInt(pool.size)
        if (pool.size > 1 && previousIndex != null) {
            var attempts = 0
            while (nextIndex == previousIndex && attempts < 8) {
                nextIndex = random.nextInt(pool.size)
                attempts += 1
            }
        }

        lastTickByGenre[genre] = tick
        lastIndexByGenre[genre] = nextIndex
        return pool[nextIndex]
    }
}

private fun randomPreviewItemForCategory(genre: String, realItems: List<CatalogItem>, tick: Int): CatalogItem? {
    val pool = previewPoolForCategory(genre, realItems)
    return HomeRandomPreviewState.pick(genre, pool, tick)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ShowcaseCategoryBrowser(
    genres: List<String>,
    itemsByCategory: Map<String, List<CatalogItem>>,
    itemCycleTick: Int,
    onGenreSelected: (String) -> Unit
) {
    if (genres.isEmpty()) {
        InfoPanel("No Pages Yet", "Add artwork inside the selections folders to show categories here.")
        return
    }

    var index by rememberSaveable(genres.joinToString("|")) { mutableStateOf(0) }
    var dragDirection by remember { mutableStateOf(1) }
    fun go(delta: Int) {
        dragDirection = if (delta >= 0) 1 else -1
        index = (index + delta + genres.size) % genres.size
    }

    fun showcaseItemsFor(genre: String): List<CatalogItem> {
        return previewPoolForCategory(genre, itemsByCategory[genre].orEmpty()).shuffled(
            Random(genre.hashCode().toLong() + itemCycleTick.toLong() * 7919L)
        )
    }

    val currentGenre = genres[index.coerceIn(0, genres.lastIndex)]
    val previousGenre = genres[(index - 1 + genres.size) % genres.size]
    val nextGenre = genres[(index + 1) % genres.size]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(550.dp)
                .pointerInput(genres) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            totalDrag += dragAmount
                            change.consume()
                        },
                        onDragEnd = {
                            if (totalDrag < -40f) go(1)
                            if (totalDrag > 40f) go(-1)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            MiniCylinderPreview(
                genre = previousGenre,
                item = randomPreviewItemForCategory(previousGenre, itemsByCategory[previousGenre].orEmpty(), itemCycleTick),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 2.dp)
                    .graphicsLayer {
                        scaleX = 0.72f
                        scaleY = 0.72f
                        rotationY = 52f
                        alpha = 0.55f
                        cameraDistance = 16f * density
                    }
            )
            MiniCylinderPreview(
                genre = nextGenre,
                item = randomPreviewItemForCategory(nextGenre, itemsByCategory[nextGenre].orEmpty(), itemCycleTick),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp)
                    .graphicsLayer {
                        scaleX = 0.72f
                        scaleY = 0.72f
                        rotationY = -52f
                        alpha = 0.55f
                        cameraDistance = 16f * density
                    }
            )

            AnimatedContent(
                targetState = currentGenre,
                transitionSpec = {
                    val enterFrom: (Int) -> Int = if (dragDirection >= 0) ({ width -> width }) else ({ width -> -width })
                    val exitTo: (Int) -> Int = if (dragDirection >= 0) ({ width -> -width }) else ({ width -> width })
                    (slideInHorizontally(animationSpec = tween(520), initialOffsetX = enterFrom) + fadeIn(animationSpec = tween(240))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(520), targetOffsetX = exitTo) + fadeOut(animationSpec = tween(180)))
                },
                label = "category-cylinder-showcase"
            ) { genre ->
                val item = randomPreviewItemForCategory(genre, itemsByCategory[genre].orEmpty(), itemCycleTick)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .widthIn(max = 430.dp)
                        .graphicsLayer {
                            rotationY = if (dragDirection >= 0) -9f else 9f
                            cameraDistance = 14f * density
                        }
                        .clip(RoundedCornerShape(34.dp))
                        .clickable { onGenreSelected(genre) },
                    shape = RoundedCornerShape(34.dp),
                    color = cardTintForCategory(genre),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
                    shadowElevation = 14.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(28.dp))
                                .background(previewPanelTintForCategory(genre))
                                .border(1.dp, DoseviaBorder, RoundedCornerShape(28.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = item?.title ?: genre,
                                transitionSpec = { fadeIn(animationSpec = tween(320)) togetherWith fadeOut(animationSpec = tween(220)) },
                                label = "showcase-item-cycle"
                            ) { shownTitle ->
                                if (item != null && SelectionArtworkAssets.hasBaseArtworkForTitle(context = LocalContext.current, title = shownTitle)) {
                                    ArtworkThumbnail(
                                        title = shownTitle,
                                        modifier = Modifier.fillMaxSize(),
                                        preferStaticBase = false,
                                        showPanel = false
                                    )
                                } else {
                                    PlaceholderCategoryImage(genre = shownTitle, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                        Text(genre, color = DoseviaText, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            TextButton(
                onClick = { go(-1) },
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
            ) { Text("‹", color = DoseviaText, fontSize = 42.sp, fontWeight = FontWeight.Black) }
            TextButton(
                onClick = { go(1) },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
            ) { Text("›", color = DoseviaText, fontSize = 42.sp, fontWeight = FontWeight.Black) }
        }

        Text("Swipe left and right to show other categories", color = DoseviaMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MiniCylinderPreview(
    genre: String,
    item: CatalogItem?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(118.dp)
            .height(210.dp)
            .clip(RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = cardTintForCategory(genre),
        border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
        shadowElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (item != null && SelectionArtworkAssets.hasBaseArtworkForTitle(LocalContext.current, item.title)) {
                ArtworkThumbnail(title = item.title, modifier = Modifier.fillMaxSize(), showPanel = false)
            } else {
                PlaceholderCategoryImage(genre = item?.title ?: genre, modifier = Modifier.fillMaxSize())
            }
        }
    }
}


@Composable
private fun GenrePill(genre: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) DoseviaPinkPrimary else DoseviaSoftPanel,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) Color(0xFF8FB1FF) else DoseviaBorder
        ),
        shadowElevation = if (selected) 8.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (selected) DoseviaText else DoseviaPinkPrimary, CircleShape)
            )
            Text(
                text = genre,
                color = if (selected) DoseviaText else DoseviaMuted,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun BackPillButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 38.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DoseviaCard, contentColor = DoseviaPinkPrimary),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("Back", fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ProfileTab(
    onBack: () -> Unit,
    onComingSoon: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var googleStatusMessage by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf("Profile") }
    val premiumSignInRequestTick = MonetizationStore.premiumSignInRequestTick.intValue
    var showNameDialog by remember { mutableStateOf(false) }
    var showPictureDialog by remember { mutableStateOf(false) }
    var avatarPendingCloudSave by remember { mutableStateOf(false) }
    var avatarSaving by remember { mutableStateOf(false) }
    var avatarSaveMessage by remember { mutableStateOf<String?>(null) }
    var showEraseDialog by remember { mutableStateOf(false) }
    var eraseText by remember { mutableStateOf("") }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteAccountText by remember { mutableStateOf("") }
    var deletingAccount by remember { mutableStateOf(false) }
    var signingOut by remember { mutableStateOf(false) }
    var preparingGoogleSignIn by remember { mutableStateOf(false) }
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        scope.launch {
            googleStatusMessage = "Signing in…"
            googleStatusMessage = withContext(Dispatchers.IO) { GoogleAuthController.handleSignInResult(context, result.data) }
            GoogleAuthController.refreshCurrentAccount(context)
            preparingGoogleSignIn = false
        }
    }
    val storeVersion = ArtworkStore.globalUpdateTick.intValue
    var catalogItems by remember { mutableStateOf<List<CatalogItem>>(emptyList()) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val saved = ProfileStore.saveAvatarFromUri(uri)
            showPictureDialog = true
            if (saved) {
                avatarSaving = false
                avatarPendingCloudSave = GoogleAuthController.account.value != null
                avatarSaveMessage = if (GoogleAuthController.account.value != null) {
                    "Preview ready. Press Save to update your account."
                } else {
                    "Preview ready. Sign in to keep it safe."
                }
                googleStatusMessage = null
            } else {
                avatarPendingCloudSave = false
                avatarSaving = false
                avatarSaveMessage = "Could not open that image. Please try another one."
                googleStatusMessage = avatarSaveMessage
            }
        }
    }

    LaunchedEffect(Unit) {
        GoogleAuthController.initialize(context)
        ProfileStore.repairInvalidState()
        AchievementStore.repairInvalidState()
        catalogItems = withContext(Dispatchers.IO) {
            SelectionArtworkAssets.loadCatalog(context).map { CatalogItem(it.title, it.category, accentForCategory(it.category)) }
        }
    }

    LaunchedEffect(premiumSignInRequestTick) {
        if (MonetizationStore.consumePremiumSignInRequest()) {
            page = "Google"
            googleStatusMessage = "Sign in first to unlock Premium on your account."
        }
    }

    LaunchedEffect(
        GoogleAuthController.account.value?.uid,
        GoogleAuthController.accountStateTick.intValue,
        AchievementStore.updateTick.intValue,
        ProfileStore.displayName.value,
        ProfileStore.badgeId.value,
        ProfileStore.borderId.value
    ) {
        GoogleAuthController.account.value?.let { account ->
            val result = withContext(Dispatchers.IO) { AccountProfileSync.syncLightweightProfile(account) }
            if (!result.success) googleStatusMessage = result.message
        }
    }

    BackHandler(enabled = page != "Profile") { page = "Profile" }

    when (page) {
        "Achievements" -> AchievementScreen(catalogItems = catalogItems, onBack = { page = "Profile" })
        "Borders" -> ProfileBordersScreen(onBack = { page = "Profile" })
        "Badges" -> ProfileBadgesScreen(onBack = { page = "Profile" })
        "MyArtworks" -> MyArtworksScreen(catalogItems = catalogItems, onBack = { page = "Profile" })
        "Google" -> GoogleSignInScreen(
            statusMessage = googleStatusMessage,
            onBack = { page = "Profile" },
            onSignIn = {
                if (!preparingGoogleSignIn) {
                    preparingGoogleSignIn = true
                    googleStatusMessage = "Opening Google account chooser…"
                    scope.launch {
                        val intent = withContext(Dispatchers.IO) { GoogleAuthController.getAccountChooserSignInIntent(context) }
                        if (intent == null) {
                            preparingGoogleSignIn = false
                            googleStatusMessage = "Sign in is not ready yet."
                        } else {
                            googleLauncher.launch(intent)
                        }
                    }
                }
            },
            onSignOut = {
                if (!signingOut) {
                    signingOut = true
                    googleStatusMessage = "Saving your changes…"
                    scope.launch {
                        val syncResult = withContext(Dispatchers.IO) { ArtworkCloudSync.syncLocalBeforeLeaving() }
                        if (syncResult.success) {
                            GoogleAuthController.signOut(context) {
                                googleStatusMessage = "Signed out."
                                page = "Profile"
                            }
                        } else {
                            googleStatusMessage = syncResult.message
                            signingOut = false
                        }
                    }
                }
            },
            onDeleteAccount = { showDeleteAccountDialog = true },
            deletingAccount = deletingAccount || signingOut || preparingGoogleSignIn,
            signingIn = preparingGoogleSignIn
        )
        "BlockedUsers" -> BlockedUsersScreen(onBack = { page = "Profile" })
        "Feedback" -> FeedbackScreen(onBack = { page = "Profile" })
        "About" -> AboutDoPaletteScreen(catalogItems = catalogItems, onBack = { page = "Profile" })
        "Privacy" -> PrivacyPolicyScreen(onBack = { page = "Profile" })
        "Terms" -> TermsOfServiceScreen(onBack = { page = "Profile" })
        else -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PROFILE", color = DoseviaGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Your DoPalette space", color = DoseviaText, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("Profile, achievements, and account.", color = DoseviaMuted, fontSize = 13.sp)
                }
                BackPillButton(onClick = onBack)
            }

            AdBannerPlaceholder()

            val achievements = remember(catalogItems, AchievementStore.updateTick.intValue, ArtworkStore.globalUpdateTick.intValue) { buildAchievements(catalogItems) }
            val totalBadges = achievements.count { it.unlocked }
            val level = artistLevelFor(AchievementStore.totalXp())
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = DoseviaCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
                shadowElevation = 10.dp
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProfileAvatarLarge()
                    Text(ProfileStore.displayName.value, color = DoseviaText, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text("Member since ${ProfileStore.memberSince.value}", color = DoseviaMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileStat("Level", "${level.level}", Modifier.weight(1f))
                        ProfileStat("XP", "${AchievementStore.totalXp()}", Modifier.weight(1f))
                        ProfileStat("Badges", totalBadges.toString(), Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileBadgeSummary(ProfileStore.badgeId.value, Modifier.weight(1f))
                        ProfileBorderSummary(profileBorderTitle(ProfileStore.borderId.value), Modifier.weight(1f))
                    }
                }
            }

            val signedInAccount = GoogleAuthController.account.value
            ProfileSectionHeader("CUSTOMIZATION")
            ProfileActionButton("Profile Borders", "Pick and preview your unlocked avatar border.") { page = "Borders" }
            ProfileActionButton("Profile Badge", "Pick what experience title to display.") { page = "Badges" }
            ProfileActionButton("Achievements", "View earned and locked DoPalette rewards.") { page = "Achievements" }

            ProfileSectionHeader("PROFILE")
            ProfileActionButton("Change Name", if (signedInAccount != null) "Backed up to your account." else "Saved on this device.") { showNameDialog = true }
            ProfileActionButton("Change Profile Picture", if (signedInAccount != null) "Backed up to your account." else "Saved on this device.") { showPictureDialog = true }
            ProfileActionButton("Google Account", if (signedInAccount != null) "Signed in. Manage your account." else "Sign in to back up your profile and artwork.") { page = "Google" }

            ProfileSectionHeader("SUPPORT")
            ProfileActionButton("Blocked Users", "View and unblock Community users you have hidden.") { page = "BlockedUsers" }
            ProfileActionButton("Feedback", "Send bug reports, feature ideas, or artwork issues.") { page = "Feedback" }
            ProfileActionButton("About DoPalette", "Version, content, and artwork system information.") { page = "About" }
            ProfileActionButton("Privacy Policy", "How DoPalette handles your information.") { page = "Privacy" }
            ProfileActionButton("Terms of Service", "Rules for using DoPalette.") { page = "Terms" }
            if (signedInAccount == null) {
                ProfileActionButton("Erase App Data", "Only for local guest data. Resets drafts, finished artworks, profile, and badges.", danger = true) { showEraseDialog = true }
            }
        }
    }

    if (showNameDialog) ChangeNameDialog(
        onDismiss = { showNameDialog = false },
        onSave = { clean ->
            if (ProfileStore.setDisplayName(clean)) {
                showNameDialog = false
                GoogleAuthController.account.value?.let { account ->
                    scope.launch {
                        googleStatusMessage = withContext(Dispatchers.IO) {
                            AccountProfileSync.saveDisplayName(account, clean).message
                        }
                    }
                }
            }
        }
    )
    if (showPictureDialog) ProfilePictureDialog(
        signedIn = GoogleAuthController.account.value != null,
        pendingCloudSave = avatarPendingCloudSave,
        saving = avatarSaving,
        message = avatarSaveMessage,
        onPick = {
            imagePicker.launch("image/*")
        },
        onSave = {
            val account = GoogleAuthController.account.value
            if (account == null) {
                avatarSaveMessage = "Sign in first to save this profile picture online."
                googleStatusMessage = avatarSaveMessage
            } else {
                scope.launch {
                    avatarSaving = true
                    avatarSaveMessage = "Saving…"
                    googleStatusMessage = avatarSaveMessage
                    val result = withContext(Dispatchers.IO) {
                        AccountProfileSync.uploadCurrentAvatar(account)
                    }
                    avatarSaving = false
                    avatarPendingCloudSave = !result.success
                    avatarSaveMessage = result.message
                    googleStatusMessage = result.message
                    if (result.success) showPictureDialog = false
                }
            }
        },
        onRemove = {
            val account = GoogleAuthController.account.value
            ProfileStore.removeAvatar()
            avatarPendingCloudSave = false
            if (account != null) {
                scope.launch {
                    avatarSaving = true
                    avatarSaveMessage = "Saving…"
                    val result = withContext(Dispatchers.IO) {
                        AccountProfileSync.removeCloudAvatar(account)
                    }
                    avatarSaving = false
                    avatarSaveMessage = result.message
                    googleStatusMessage = result.message
                    if (result.success) showPictureDialog = false
                }
            } else {
                avatarSaveMessage = "Profile picture removed."
                showPictureDialog = false
            }
        },
        onDismiss = { showPictureDialog = false }
    )
    if (showEraseDialog) EraseAppDataDialog(
        typed = eraseText,
        onTyped = { eraseText = it },
        onDismiss = { showEraseDialog = false; eraseText = "" },
        onErase = {
            AppResetManager.freshInstallLocalReset(context)
            googleStatusMessage = "Local app data erased. Fresh start restored."
            showEraseDialog = false
            eraseText = ""
            page = "Profile"
        }
    )
    if (showDeleteAccountDialog) DeleteAccountDialog(
        typed = deleteAccountText,
        deleting = deletingAccount,
        onTyped = { deleteAccountText = it },
        onDismiss = {
            if (!deletingAccount) {
                showDeleteAccountDialog = false
                deleteAccountText = ""
            }
        },
        onDelete = {
            scope.launch {
                deletingAccount = true
                googleStatusMessage = "Deleting…"
                val result = withContext(Dispatchers.IO) {
                    GoogleAuthController.deleteCurrentAccountDataAndAuth(context)
                }
                deletingAccount = false
                googleStatusMessage = result
                showDeleteAccountDialog = false
                deleteAccountText = ""
            }
        }
    )

    if (signingOut) {
        OperationWaveOverlay(word = "SIGNING OUT")
    }
    if (deletingAccount) {
        OperationWaveOverlay(word = "DELETING")
    }
    if (avatarSaving) {
        OperationWaveOverlay(word = "SAVING")
    }
    if (preparingGoogleSignIn) {
        OperationWaveOverlay(word = "SIGNING IN")
    }
}


@Composable
private fun GoogleSignInScreen(
    statusMessage: String?,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    deletingAccount: Boolean,
    signingIn: Boolean = false
) {
    val account = GoogleAuthController.account.value
    val isConfigured = GoogleAuthController.configured.value
    val showSetupMessage = !isConfigured
    val showErrorMessage = statusMessage?.takeIf {
        it.contains("failed", ignoreCase = true) ||
            it.contains("not ready", ignoreCase = true) ||
            it.contains("not synced", ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("ACCOUNT", color = DoseviaGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text("Google Account", color = DoseviaText, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("Sign in or manage your account.", color = DoseviaMuted, fontSize = 13.sp)
            }
            BackPillButton(onClick = onBack)
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 8.dp),
            shape = RoundedCornerShape(28.dp),
            color = DoseviaCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfileAvatarLarge()

                if (account == null) {
                    Text("Sign in with Google", color = DoseviaText, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text(
                        "Sign in to back up your profile and artwork.",
                        color = DoseviaMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    if (ProfileStore.hasLocalProfileData() || AchievementStore.hasAnyLocalProgress() || ArtworkStore.hasAnyLocalArtwork()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFFFF6D8),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8CA4C))
                        ) {
                            Text(
                                "You have saved work on this device. Sign in to back it up.",
                                modifier = Modifier.padding(14.dp),
                                color = DoseviaText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (showSetupMessage) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFFFF6D8),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8CA4C))
                        ) {
                            Text(
                                "Sign in is not ready yet.",
                                modifier = Modifier.padding(14.dp),
                                color = DoseviaText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    GoogleContinueButton(enabled = !deletingAccount && !signingIn && isConfigured, loading = signingIn, onClick = onSignIn)
                } else {
                    Text(ProfileStore.displayName.value, color = DoseviaText, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text(account.email, color = DoseviaMuted, fontSize = 13.sp, textAlign = TextAlign.Center)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = DoseviaSoftPanel
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Signed In", color = DoseviaText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            Text("Your profile and artwork are backed up.", color = DoseviaMuted, fontSize = 13.sp)
                        }
                    }

                    ArtworkSyncStatusCard()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileStat("Level", "${artistLevelFor(AchievementStore.totalXp()).level}", Modifier.weight(1f))
                        ProfileStat("XP", "${AchievementStore.totalXp()}", Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileBadgeSummary(ProfileStore.badgeId.value, Modifier.weight(1f))
                        ProfileBorderSummary(profileBorderTitle(ProfileStore.borderId.value), Modifier.weight(1f))
                    }

                    Button(
                        onClick = onSignOut,
                        enabled = !deletingAccount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DoseviaSoftPanel, contentColor = DoseviaText)
                    ) {
                        Text("Sign Out", fontWeight = FontWeight.Black)
                    }

                    Button(
                        onClick = onDeleteAccount,
                        enabled = !deletingAccount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEEF1), contentColor = DoseviaRed)
                    ) {
                        Text(if (deletingAccount) "Deleting..." else "Delete Account", fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        showErrorMessage?.let { message ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFFFE8E8),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE57373))
            ) {
                Text(
                    message,
                    modifier = Modifier.padding(14.dp),
                    color = DoseviaText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun GoogleContinueButton(enabled: Boolean = true, loading: Boolean = false, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E7F0)),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_google_g),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                if (loading) "Signing in…" else "Continue with Google",
                color = DoseviaText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}


@Composable
private fun ArtworkSyncStatusCard() {
    val state = ArtworkCloudSync.syncState.value
    val status = when (state) {
        ArtworkCloudSync.SyncState.SYNCED -> "Saved"
        ArtworkCloudSync.SyncState.SYNCING -> "Saving"
        ArtworkCloudSync.SyncState.NOT_SYNCED -> "Needs Attention"
        ArtworkCloudSync.SyncState.LOCAL_ONLY -> "Saved on this device"
    }
    val helper = when (state) {
        ArtworkCloudSync.SyncState.NOT_SYNCED -> "We’ll keep it safe."
        ArtworkCloudSync.SyncState.LOCAL_ONLY -> "Sign in to back it up."
        else -> null
    }
    val bg = when (state) {
        ArtworkCloudSync.SyncState.SYNCED -> Color(0xFFE9FBEF)
        ArtworkCloudSync.SyncState.SYNCING -> Color(0xFFEAF1FF)
        ArtworkCloudSync.SyncState.NOT_SYNCED -> Color(0xFFFFF6D8)
        ArtworkCloudSync.SyncState.LOCAL_ONLY -> DoseviaSoftPanel
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state == ArtworkCloudSync.SyncState.SYNCING) {
                MiniWaveWord("SAVING")
            } else {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Cloud Backup", color = DoseviaText, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text(status, color = DoseviaMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    helper?.let { Text(it, color = DoseviaMuted, fontSize = 12.sp) }
                }
            }
        }
    }
}

@Composable
private fun MiniWaveWord(word: String) {
    val transition = rememberInfiniteTransition(label = "mini-wave")
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Cloud Backup", color = DoseviaText, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.CenterVertically) {
            word.forEachIndexed { index, char ->
                val y by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = -5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 520, delayMillis = index * 45),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "mini-wave-char-$index"
                )
                Text(
                    text = char.toString(),
                    color = DoseviaPinkPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.graphicsLayer { translationY = y }
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatarLarge() {
    ProfileAvatarWithBorder(size = 126.dp, textSize = 38.dp)
}


@Composable
private fun ProfileBordersScreen(onBack: () -> Unit) {
    val selected = ProfileStore.borderId.value
    val context = LocalContext.current
    val syncScope = rememberCoroutineScope()
    var filter by rememberSaveable { mutableStateOf("All") }

    fun equipBorderOnline(id: String) {
        if (!isBorderOwned(id)) return
        ProfileStore.setBorderId(id)
        CommunityRepository.patchCachedOwnerStyle(context)
        GoogleAuthController.account.value?.let { account ->
            syncScope.launch {
                withContext(Dispatchers.IO) {
                    AccountProfileSync.syncProfileStyleAndProgress(account)
                }
                CommunityRepository.patchCachedOwnerStyle(context)
            }
        }
    }

    val specs = remember(AchievementStore.updateTick.intValue, filter) {
        profileBorderSpecs.filter { spec ->
            when (filter) {
                "Unlocked" -> isBorderOwned(spec.id)
                "Locked" -> !isBorderOwned(spec.id)
                else -> true
            }
        }
    }
    val sections = listOf(
        "Category Borders" to listOf("fruit_border", "vegetable_border", "animal_border", "flower_border", "vehicle_border", "sports_border"),
        "Special Borders" to listOf("dinosaur_border", "dragon_border", "space_border", "dessert_border", "fantasy_border", "ocean_border"),
        "Prestige Borders" to listOf("default", "bronze_border", "silver_border", "diamond_border", "gold_master_border", "legendary_border", "rainbow_border", "founder_border", "completionist_border")
    )

    Column(modifier = Modifier.fillMaxSize().background(DoseviaBackgroundBrush).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("PROFILE BORDERS", color = DoseviaGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text("Choose your frame", color = DoseviaText, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text("Unlocked borders can be equipped and sync online immediately.", color = DoseviaMuted, fontSize = 13.sp)
            }
            BackPillButton(onClick = onBack)
        }
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = DoseviaCard, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder), shadowElevation = 10.dp) {
            Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileAvatarWithBorder(size = 140.dp, textSize = 40.dp, borderId = selected)
                Text(profileBorderTitle(selected), color = DoseviaText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("Shown on your profile and Community posts.", color = DoseviaMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Unlocked", "Locked").forEach { label ->
                Surface(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50.dp)).clickable { filter = label },
                    shape = RoundedCornerShape(50.dp),
                    color = if (filter == label) DoseviaPinkPrimary else DoseviaCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (filter == label) DoseviaPinkPrimary else DoseviaBorder),
                    shadowElevation = if (filter == label) 6.dp else 2.dp
                ) {
                    Text(label, modifier = Modifier.padding(vertical = 11.dp), textAlign = TextAlign.Center, color = if (filter == label) Color.White else DoseviaText, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
        sections.forEach { (sectionTitle, ids) ->
            val group = ids.mapNotNull { id -> specs.firstOrNull { it.id == id } }
            if (group.isNotEmpty()) {
                Text(sectionTitle.uppercase(Locale.US), color = DoseviaGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
                group.chunked(2).forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowItems.forEach { spec ->
                            val isSelected = selected == spec.id
                            val isUnlocked = isBorderOwned(spec.id)
                            val borderColor = when {
                                isSelected -> DoseviaPinkPrimary
                                isUnlocked -> DoseviaBorder
                                else -> Color(0xFFB6C1D0)
                            }
                            val bgColor = when {
                                isSelected -> Color(0xFFEAF1FF)
                                isUnlocked -> DoseviaCard
                                else -> Color(0xFFE3E9F2)
                            }
                            Surface(
                                modifier = Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(22.dp)).clickable(enabled = isUnlocked) { equipBorderOnline(spec.id) },
                                shape = RoundedCornerShape(22.dp),
                                color = bgColor,
                                border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                                shadowElevation = if (isSelected) 9.dp else if (isUnlocked) 4.dp else 1.dp
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.graphicsLayer { alpha = if (isUnlocked) 1f else 0.38f }) {
                                        ProfileAvatarWithBorder(size = 82.dp, textSize = 20.dp, borderId = spec.id)
                                    }
                                    Text(spec.title, color = if (isUnlocked) DoseviaText else DoseviaMuted, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                    Text(if (isSelected) "Equipped" else if (isUnlocked) "Available" else "Locked", color = if (isSelected) DoseviaPinkPrimary else if (isUnlocked) Color(0xFF2F6FED) else DoseviaMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun ProfileSectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp),
        color = DoseviaGreen,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
private fun MyArtworksScreen(catalogItems: List<CatalogItem>, onBack: () -> Unit) {
    val finished = catalogItems.count { ArtworkStore.hasFinished(it.title) }
    val drafts = catalogItems.count { ArtworkStore.hasDraft(it.title) }
    Column(
        modifier = Modifier.fillMaxSize().background(DoseviaBackgroundBrush).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("MY ARTWORKS", color = DoseviaGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text("Your creations", color = DoseviaText, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text("Drafts and finished artwork are organized here.", color = DoseviaMuted, fontSize = 13.sp)
            }
            BackPillButton(onClick = onBack)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileStat("Drafts", drafts.toString(), Modifier.weight(1f))
            ProfileStat("Finished", finished.toString(), Modifier.weight(1f))
            ProfileStat("Total", catalogItems.size.toString(), Modifier.weight(1f))
        }
        ArtworkLibraryActionCard("Drafts", "Continue artwork you started but have not finished yet.", drafts, enabled = drafts > 0)
        ArtworkLibraryActionCard("Finished", "View your saved finished layers and choose what to share.", finished, enabled = finished > 0)
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun ArtworkLibraryActionCard(title: String, subtitle: String, count: Int, enabled: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (enabled) DoseviaCard else Color(0xFFE6ECF5),
        border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
        shadowElevation = if (enabled) 8.dp else 2.dp
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.size(52.dp).background(if (enabled) Color(0xFFEAF1FF) else Color(0xFFDDE5EF), CircleShape), contentAlignment = Alignment.Center) {
                Text(count.toString(), color = if (enabled) DoseviaPinkPrimary else DoseviaMuted, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = DoseviaText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(if (enabled) subtitle else "Nothing here yet.", color = DoseviaMuted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ProfileBadgesScreen(onBack: () -> Unit) {
    val selected = ProfileStore.badgeId.value
    val context = LocalContext.current
    val syncScope = rememberCoroutineScope()
    var filter by rememberSaveable { mutableStateOf("All") }
    fun equipBadgeOnline(id: String) {
        if (!isBadgeOwned(id)) return
        ProfileStore.setBadgeId(id)
        CommunityRepository.patchCachedOwnerStyle(context)
        GoogleAuthController.account.value?.let { account ->
            syncScope.launch {
                withContext(Dispatchers.IO) { AccountProfileSync.syncProfileStyleAndProgress(account) }
                CommunityRepository.patchCachedOwnerStyle(context)
            }
        }
    }
    val levelInfo = artistLevelFor(AchievementStore.totalXp())
    val specs = remember(AchievementStore.updateTick.intValue, filter) {
        profileBadgeSpecs.filter { spec ->
            when (filter) {
                "Unlocked" -> isBadgeOwned(spec.id)
                "Locked" -> !isBadgeOwned(spec.id)
                else -> true
            }
        }
    }
    val sections = listOf("Premium", "Progress", "Fruits", "Vegetables", "Animals", "Objects", "Flowers", "Vehicles", "Sports", "Dinosaurs", "Dragons", "Space", "Desserts", "Community", "Collection")
    Column(modifier = Modifier.fillMaxSize().background(DoseviaBackgroundBrush).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("PROFILE BADGES", color = DoseviaGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text("Choose your title", color = DoseviaText, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text("Titles are earned from XP, categories, and achievements.", color = DoseviaMuted, fontSize = 13.sp)
            }
            BackPillButton(onClick = onBack)
        }
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = DoseviaCard, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder), shadowElevation = 10.dp) {
            Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(profileBadgeTitle(selected), color = badgeAccent(selected), fontSize = 25.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Box(modifier = Modifier.background(badgeAccent(selected).copy(alpha = 0.12f), RoundedCornerShape(50.dp)).border(1.dp, badgeAccent(selected).copy(alpha = 0.35f), RoundedCornerShape(50.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("Level ${levelInfo.level} • ${AchievementStore.totalXp()} XP • ${profileBadgeSpec(selected).subtitle}", color = DoseviaText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("This title displays on your profile and Community posts.", color = DoseviaMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Unlocked", "Locked").forEach { label ->
                Surface(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50.dp)).clickable { filter = label },
                    shape = RoundedCornerShape(50.dp),
                    color = if (filter == label) DoseviaPinkPrimary else DoseviaCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (filter == label) DoseviaPinkPrimary else DoseviaBorder),
                    shadowElevation = if (filter == label) 6.dp else 2.dp
                ) {
                    Text(label, modifier = Modifier.padding(vertical = 11.dp), textAlign = TextAlign.Center, color = if (filter == label) Color.White else DoseviaText, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
        sections.forEach { section ->
            val group = specs.filter { it.section == section }
            if (group.isNotEmpty()) {
                Text(section.uppercase(Locale.US), color = DoseviaGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
                group.forEach { spec ->
                    val isUnlocked = isBadgeOwned(spec.id)
                    val isSelected = selected == spec.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(enabled = isUnlocked) { equipBadgeOnline(spec.id) },
                        shape = RoundedCornerShape(24.dp),
                        color = if (isUnlocked) DoseviaCard else Color(0xFFE7EDF6),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) badgeAccent(spec.id) else DoseviaBorder
                        ),
                        shadowElevation = if (isSelected) 8.dp else if (isUnlocked) 4.dp else 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(if (isUnlocked) badgeAccent(spec.id).copy(alpha = 0.14f) else Color(0xFFD9E1EC), CircleShape)
                                    .border(1.dp, if (isUnlocked) badgeAccent(spec.id).copy(alpha = 0.30f) else Color(0xFFC1CBD9), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (spec.id == "premium_artist") "👑" else "★", fontSize = 22.sp)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    spec.title,
                                    color = if (isUnlocked) DoseviaText else DoseviaMuted,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    spec.subtitle,
                                    color = DoseviaMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) badgeAccent(spec.id).copy(alpha = 0.14f) else if (isUnlocked) Color(0xFFEAF1FF) else Color(0xFFDDE5EF),
                                        RoundedCornerShape(999.dp)
                                    )
                                    .padding(horizontal = 11.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (isSelected) "Equipped" else if (isUnlocked) "Equip" else "Locked",
                                    color = if (isSelected) badgeAccent(spec.id) else if (isUnlocked) DoseviaPinkPrimary else DoseviaMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun ProfileBadgeSummary(badgeId: String, modifier: Modifier = Modifier) {
    val accent = badgeAccent(badgeId)
    Surface(
        modifier = modifier.heightIn(min = 86.dp),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    profileBadgeTitle(badgeId),
                    color = accent,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Title",
                    color = DoseviaMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ProfileBorderSummary(borderTitle: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 86.dp),
        shape = RoundedCornerShape(18.dp),
        color = DoseviaSoftPanel,
        border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    borderTitle,
                    color = DoseviaText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text("Border", color = DoseviaMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = DoseviaSoftPanel,
        border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = DoseviaText, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(label, color = DoseviaMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ProfileActionButton(title: String, subtitle: String, danger: Boolean = false, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (danger) Color(0xFFFFEEF1) else DoseviaCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (danger) DoseviaRed.copy(alpha = 0.45f) else DoseviaBorder),
        shadowElevation = if (danger) 2.dp else 6.dp
    ) {
        val icon = when {
            title.contains("Delete", ignoreCase = true) -> "🗑"
            title.contains("Erase", ignoreCase = true) -> "🧹"
            title.contains("Google", ignoreCase = true) -> "G"
            title.contains("Picture", ignoreCase = true) -> "🖼"
            title.contains("Name", ignoreCase = true) -> "✏️"
            title.contains("Badge", ignoreCase = true) -> "🏆"
            title.contains("Border", ignoreCase = true) -> "🖼"
            title.contains("Achievements", ignoreCase = true) -> "🏆"
            title.contains("Shared", ignoreCase = true) -> "👥"
            title.contains("Liked", ignoreCase = true) -> "♥"
            title.contains("Feedback", ignoreCase = true) -> "💬"
            title.contains("Privacy", ignoreCase = true) -> "🛡"
            title.contains("Terms", ignoreCase = true) -> "📄"
            title.contains("Guidelines", ignoreCase = true) -> "⚖"
            title.contains("Blocked", ignoreCase = true) -> "🚫"
            title.contains("Report", ignoreCase = true) -> "🚩"
            title.contains("About", ignoreCase = true) -> "ⓘ"
            else -> "•"
        }
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileActionIcon(icon = icon, danger = danger)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = if (danger) DoseviaRed else DoseviaText, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = DoseviaMuted, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(DoseviaSoftPanel, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("›", color = DoseviaMuted, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun ProfileActionIcon(icon: String, danger: Boolean) {
    val accent = if (danger) DoseviaRed else DoseviaPinkPrimary
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.22f),
                        if (danger) Color(0xFFFFE3E8) else Color(0xFFEAF1FF)
                    )
                ),
                CircleShape
            )
            .border(1.dp, accent.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(icon, color = accent, fontWeight = FontWeight.Black, fontSize = if (icon == "G" || icon == "ⓘ") 18.sp else 20.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ChangeNameDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(ProfileStore.displayName.value) }
    val clean = name.trim().replace(Regex("\\s+"), " ")
    val valid = clean.length in 2..24
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = DoseviaCard, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)) {
            Column(modifier = Modifier.padding(22.dp).widthIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Change Name", color = DoseviaText, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("When signed in, this name is saved to your account and loads again after sign in.", color = DoseviaMuted, fontSize = 13.sp)
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 24) name = it },
                    singleLine = true,
                    label = { Text("Display name") },
                    supportingText = { Text("${clean.length}/24 characters • 2 minimum") },
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(shape = RoundedCornerShape(18.dp), color = DoseviaSoftPanel, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.size(42.dp).background(DoseviaAccentBrush, CircleShape), contentAlignment = Alignment.Center) {
                            Text(clean.firstOrNull()?.uppercase() ?: "D", color = Color.White, fontWeight = FontWeight.Black)
                        }
                        Column {
                            Text(if (clean.isBlank()) "DoPalette Artist" else clean, color = DoseviaText, fontWeight = FontWeight.Black)
                            Text("Live profile preview", color = DoseviaMuted, fontSize = 12.sp)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaSoftPanel, contentColor = DoseviaText)) { Text("Cancel", fontWeight = FontWeight.Bold) }
                    Button(onClick = { onSave(clean) }, enabled = valid, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary)) { Text("Save", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun ProfilePictureDialog(
    signedIn: Boolean,
    pendingCloudSave: Boolean,
    saving: Boolean,
    message: String?,
    onPick: () -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = DoseviaCard, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)) {
            Column(modifier = Modifier.padding(22.dp).widthIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Profile Picture", color = DoseviaText, fontSize = 24.sp, fontWeight = FontWeight.Black)
                ProfileAvatarLarge()
                Spacer(Modifier.height(2.dp))
                Button(onClick = onPick, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary)) { Text("Choose From Gallery", color = Color.White, fontWeight = FontWeight.ExtraBold) }
                if (signedIn) {
                    Button(onClick = onSave, enabled = !saving && pendingCloudSave, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary)) {
                        if (saving) Text("Saving...", color = Color.White, fontWeight = FontWeight.ExtraBold) else Text("Save", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Button(onClick = onRemove, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEEF1), contentColor = DoseviaRed)) { Text("Remove Picture", fontWeight = FontWeight.ExtraBold) }
                TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel", color = DoseviaMuted, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    typed: String,
    deleting: Boolean,
    onTyped: (String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = { if (!deleting) onDismiss() }) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFFFFF7F8), border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaRed.copy(alpha = 0.45f))) {
            Column(modifier = Modifier.padding(22.dp).widthIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Delete Account?", color = DoseviaRed, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("This deletes your signed-in account data and then restarts DoPalette with the same clean state as a fresh install.", color = DoseviaText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                InfoSection("Will be removed", listOf("Account info", "Profile info", "Profile picture", "Local profile info", "Local XP, badges, borders, and achievements", "Local drafts, finished artwork, and previews"))
                InfoSection("Will not be removed", listOf("The DoPalette app installation", "Built-in category artwork", "Base and mask assets packaged with the app", "Images already downloaded outside the app"))
                OutlinedTextField(value = typed, onValueChange = onTyped, enabled = !deleting, singleLine = true, label = { Text("Type DELETE to confirm") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onDismiss, enabled = !deleting, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaSoftPanel, contentColor = DoseviaText)) { Text("Cancel", fontWeight = FontWeight.Bold) }
                    Button(onClick = onDelete, enabled = typed == "DELETE" && !deleting, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaRed)) { Text(if (deleting) "Deleting..." else "Delete", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun EraseAppDataDialog(typed: String, onTyped: (String) -> Unit, onDismiss: () -> Unit, onErase: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFFFFF7F8), border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaRed.copy(alpha = 0.45f))) {
            Column(modifier = Modifier.padding(22.dp).widthIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Erase App Data?", color = DoseviaRed, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("This permanently removes local DoPalette data from this device. This action cannot be undone.", color = DoseviaText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                InfoSection("Will be removed", listOf("All draft artwork progress", "All finished artwork saved inside the app", "Generated preview thumbnails", "Profile name and profile picture", "Local achievement unlock data"))
                InfoSection("Will not be removed", listOf("The DoPalette app installation", "The built-in category artwork", "Images you already downloaded outside the app"))
                OutlinedTextField(value = typed, onValueChange = onTyped, singleLine = true, label = { Text("Type ERASE to confirm") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaSoftPanel, contentColor = DoseviaText)) { Text("Cancel", fontWeight = FontWeight.Bold) }
                    Button(onClick = onErase, enabled = typed == "ERASE", modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaRed)) { Text("Erase", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

private data class AchievementUi(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val rarity: String,
    val xp: Int,
    val reward: String,
    val progress: Int,
    val target: Int,
    val unlocked: Boolean
)

private data class ArtistLevel(
    val level: Int,
    val title: String,
    val currentXp: Int,
    val currentLevelXp: Int,
    val nextLevelXp: Int
)

private fun artistLevelFor(xp: Int): ArtistLevel {
    var level = 1
    var currentStart = 0
    var next = 100
    var step = 150
    val safeXp = xp.coerceAtLeast(0)
    while (safeXp >= next && level < 1000) {
        level += 1
        currentStart = next
        step = (step + 50).coerceAtMost(2000)
        next += step
    }
    val title = when {
        level >= 100 -> "Completionist Legend"
        level >= 50 -> "DoPalette Master"
        level >= 25 -> "Palette Champion"
        level >= 10 -> "Creative Pro"
        level >= 5 -> "Color Explorer"
        else -> "Beginner Artist"
    }
    return ArtistLevel(level, title, safeXp, currentStart, next)
}


private fun categoryBadgeReward(category: String, tier: String): String {
    val c = category.lowercase(Locale.US)
    return when {
        c.contains("fruit") -> if (tier == "master") "Orchard Master Title" else if (tier == "five") "Fruit Explorer Title" else "Fresh Picker Title"
        c.contains("vegetable") -> if (tier == "master") "Garden Guardian Title" else if (tier == "five") "Harvest Hero Title" else "Seed Starter Title"
        c.contains("animal") -> if (tier == "master") "King of the Jungle Title" else if (tier == "five") "Beast Tamer Title" else "Wildlife Scout Title"
        c.contains("flower") -> if (tier == "master") "Floral Monarch Title" else if (tier == "five") "Garden Keeper Title" else "Bloom Seeker Title"
        c.contains("vehicle") -> if (tier == "master") "Road Champion Title" else if (tier == "five") "Highway Hero Title" else "Road Rookie Title"
        c.contains("sport") -> if (tier == "master") "Sports Legend Title" else if (tier == "five") "Arena Champion Title" else "Rising Athlete Title"
        c.contains("dinosaur") -> if (tier == "master") "Prehistoric Master Title" else if (tier == "five") "Jurassic Explorer Title" else "Fossil Hunter Title"
        c.contains("dragon") -> if (tier == "master") "Dragon Lord Title" else if (tier == "five") "Flame Keeper Title" else "Dragon Rider Title"
        c.contains("space") -> if (tier == "master") "Cosmic Legend Title" else if (tier == "five") "Galaxy Explorer Title" else "Star Voyager Title"
        c.contains("dessert") -> if (tier == "master") "Dessert Royalty Title" else if (tier == "five") "Sugar Artist Title" else "Sweet Starter Title"
        else -> if (tier == "master") "$category Master Title" else if (tier == "five") "$category Explorer Title" else "$category Starter Title"
    }
}

private fun buildAchievements(catalogItems: List<CatalogItem>): List<AchievementUi> {
    val tick = AchievementStore.updateTick.intValue
    val storeTick = ArtworkStore.globalUpdateTick.intValue
    val touched = AchievementStore.counter("artworks_touched")
    val uniqueFinished = AchievementStore.counter("unique_finished")
    val finishedTotal = AchievementStore.counter("finished_total")
    val downloads = AchievementStore.counter("downloads")
    val fills = AchievementStore.counter("bucket_fills")
    val brushes = AchievementStore.counter("brush_strokes")
    val clears = AchievementStore.counter("clear_canvas")
    val undo = AchievementStore.counter("undo")
    val openDays = AchievementStore.counter("open_days")
    val communityShares = max(AchievementStore.counter("community_shares"), AchievementStore.counter("community_public_posts_max"))
    val communityLikesGiven = AchievementStore.counter("community_likes_given")
    val communityLikesReceived = AchievementStore.counter("community_likes_received_max")
    val catalog = catalogItems.ifEmpty { fallbackCatalogItems }
    val categoryNames = catalog.map { it.category }.distinct()
    fun unlocked(id: String, progress: Int, target: Int, title: String, xp: Int, reward: String): Boolean {
        // Profile/Achievement UI must be read-only. Unlocking while Compose is building the
        // Profile screen can mutate state during composition after Delete Account and crash.
        // Real unlocks are recorded by the action paths: brush, fill, save finished, download.
        return AchievementStore.isUnlocked(id) || progress >= target
    }
    fun item(id: String, title: String, description: String, category: String, rarity: String, xp: Int, reward: String, progress: Int, target: Int): AchievementUi {
        val safeTarget = target.coerceAtLeast(1)
        return AchievementUi(id, title, description, category, rarity, xp, reward, progress.coerceAtLeast(0), safeTarget, unlocked(id, progress, safeTarget, title, xp, reward))
    }

    val result = mutableListOf<AchievementUi>()

    // COLORING: simple milestones that make XP meaningful without turning it into currency.
    listOf(
        1 to Triple("first_finished", "First Artwork", 100),
        10 to Triple("colored_10", "10 Colored", 250),
        25 to Triple("colored_25", "25 Colored", 600),
        50 to Triple("colored_50", "50 Colored", 1200),
        100 to Triple("colored_100", "100 Colored", 2500),
        250 to Triple("colored_250", "250 Colored", 6000)
    ).forEach { (target, info) ->
        result += item(info.first, info.second, "Finish and save $target artwork${if (target == 1) "" else "s"}.", "Coloring", if (target >= 100) "Legendary" else if (target >= 50) "Epic" else if (target >= 10) "Rare" else "Common", info.third, "Coloring Milestone Title", uniqueFinished, target)
    }

    // CATEGORY MASTERY: complete every artwork in a category to earn its border/badge.
    categoryNames.forEach { category ->
        val items = catalog.filter { it.category == category }
        val finishedInCategory = items.count { ArtworkStore.hasFinished(it.title) }
        val safeCount = items.size.coerceAtLeast(1)
        val clean = category.lowercase(Locale.US).replace(" ", "_")
        val label = category.removeSuffix("s")
        result += item("${clean}_first", "$label Apprentice", "Finish your first $category artwork.", "Category", "Common", 75, categoryBadgeReward(category, "first"), finishedInCategory, 1)
        result += item("${clean}_five", "$label Explorer", "Finish 5 artworks in $category.", "Category", "Rare", 180, categoryBadgeReward(category, "five"), finishedInCategory, min(5, safeCount))
        result += item("${clean}_master", "$category Master", "Complete every $category artwork.", "Category", "Legendary", safeCount * 30, "$category Border + ${categoryBadgeReward(category, "master")}", finishedInCategory, safeCount)
    }

    // LEVEL: XP shows long-term dedication and unlocks prestige borders later.
    val currentLevel = artistLevelFor(AchievementStore.totalXp()).level
    listOf(
        5 to Triple("level_5", "Reach Level 5", 250),
        10 to Triple("level_10", "Reach Level 10", 500),
        25 to Triple("level_25", "Reach Level 25", 1000),
        50 to Triple("level_50", "Reach Level 50", 2000),
        100 to Triple("level_100", "Reach Level 100", 5000)
    ).forEach { (target, info) ->
        result += item(info.first, info.second, "Reach artist level $target.", "Progress", if (target >= 50) "Legendary" else if (target >= 25) "Epic" else "Rare", info.third, "Level $target Border", currentLevel, target)
    }

    // COMMUNITY: gives profile/community progression without forcing coins or shops.
    result += item("first_community_share", "First Share", "Share your first finished artwork to Community.", "Community", "Rare", 50, "Gallery Debut Title", communityShares, 1)
    result += item("community_ten_posts", "10 Shares", "Share 10 public artworks.", "Community", "Epic", 300, "Showcase Artist Title", communityShares, 10)
    result += item("community_50_posts", "50 Shares", "Share 50 public artworks.", "Community", "Legendary", 900, "Community Showcase Border", communityShares, 50)
    result += item("community_100_likes", "100 Likes", "Receive 100 likes on your public artworks.", "Community", "Legendary", 700, "Community Star Border", communityLikesReceived, 100)
    result += item("community_star", "Community Star", "Receive 100 likes and share at least 10 artworks.", "Community", "Legendary", 1000, "Community Star Title", min(communityLikesReceived, 100) + min(communityShares, 10), 110)

    // COLLECTION: overall completion goals. Missing future categories will not block current builds.
    val completedCategories = catalog.groupBy { it.category }.filter { (_, items) -> items.isNotEmpty() && items.all { ArtworkStore.hasFinished(it.title) } }.keys.map { it.lowercase(Locale.US).replace(" ", "_") }.toSet()
    val freeCategories = setOf("fruits", "vegetables", "animals", "objects", "flowers", "vehicles", "sports")
    val specialCategories = setOf("dinosaurs", "dragons", "space", "desserts", "fantasy", "sea_life")
    val availableFree = freeCategories.filter { wanted -> catalog.any { it.category.lowercase(Locale.US).replace(" ", "_") == wanted } }
    val availableSpecial = specialCategories.filter { wanted -> catalog.any { it.category.lowercase(Locale.US).replace(" ", "_") == wanted } }
    result += item("complete_free_categories", "Complete Free Categories", "Complete every available free category.", "Collection", "Legendary", 3000, "Free Master Border", availableFree.count { it in completedCategories }, availableFree.size.coerceAtLeast(1))
    result += item("complete_special_categories", "Complete Special Categories", "Complete every available special category.", "Collection", "Legendary", 3000, "Special Master Border", availableSpecial.count { it in completedCategories }, availableSpecial.size.coerceAtLeast(1))
    result += item("completionist", "Completionist", "Complete every artwork in DoPalette.", "Collection", "Legendary", 10000, "Completionist Border", catalog.count { ArtworkStore.hasFinished(it.title) }, catalog.size.coerceAtLeast(1))

    return result.sortedWith(compareBy<AchievementUi> { it.category }.thenBy { it.unlocked.not() }.thenBy { it.title })
}

@Composable
private fun AchievementScreen(catalogItems: List<CatalogItem>, onBack: () -> Unit) {
    var filter by remember { mutableStateOf("All") }
    val tick = AchievementStore.updateTick.intValue + ArtworkStore.globalUpdateTick.intValue
    val achievements = remember(filter, tick, catalogItems) { buildAchievements(catalogItems) }
    val shown = remember(filter, achievements) {
        when (filter) {
            "Unlocked" -> achievements.filter { it.unlocked }
            "Locked" -> achievements.filter { !it.unlocked }
            else -> achievements
        }
    }
    val unlockedCount = achievements.count { it.unlocked }
    val xp = AchievementStore.totalXp()
    val level = artistLevelFor(xp)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("ACHIEVEMENTS", color = DoseviaGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text("Level ${level.level} ${level.title}", color = DoseviaText, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("$unlockedCount / ${achievements.size} unlocked • $xp XP", color = DoseviaMuted, fontSize = 13.sp)
            }
            BackPillButton(onClick = onBack)
        }
        XpProgressCard(level = level, unlocked = unlockedCount, total = achievements.size)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Unlocked", "Locked").forEach { item ->
                Button(
                    onClick = { filter = item },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (filter == item) DoseviaPinkPrimary else DoseviaCard, contentColor = if (filter == item) Color.White else DoseviaText)
                ) { Text(item, fontWeight = FontWeight.Bold) }
            }
        }
        shown.groupBy { it.category }.forEach { (section, entries) ->
            Text(section.uppercase(Locale.US), color = DoseviaGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
            entries.forEach { achievement -> AchievementCard(achievement) }
        }
    }
}

@Composable
private fun XpProgressCard(level: ArtistLevel, unlocked: Int, total: Int) {
    val progress = ((level.currentXp - level.currentLevelXp).toFloat() / (level.nextLevelXp - level.currentLevelXp).toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(900, easing = FastOutSlowInEasing), label = "xpProgress")
    val animatedXp by animateIntAsState(targetValue = level.currentXp, animationSpec = tween(900, easing = FastOutSlowInEasing), label = "xpCount")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = DoseviaCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Artist Level ${level.level}", color = DoseviaText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(level.title, color = DoseviaMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("$unlocked / $total", color = DoseviaPinkPrimary, fontWeight = FontWeight.Black)
            }
            LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(99.dp)), color = DoseviaPinkPrimary, trackColor = DoseviaSoftPanel)
            Text("$animatedXp XP • ${level.nextLevelXp - animatedXp} XP to next level", color = DoseviaMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AchievementCard(achievement: AchievementUi) {
    var expanded by remember { mutableStateOf(false) }
    val rarityColor = when (achievement.rarity) {
        "Legendary" -> Color(0xFFE7C94F)
        "Epic" -> Color(0xFF9B6BFF)
        "Rare" -> DoseviaPinkPrimary
        else -> DoseviaMuted
    }
    val progress = (achievement.progress.toFloat() / achievement.target.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(700, easing = FastOutSlowInEasing), label = "achProgress")
    val scale by animateFloatAsState(targetValue = if (expanded) 1.015f else 1f, animationSpec = tween(220, easing = FastOutSlowInEasing), label = "achScale")
    Surface(
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(20.dp)).clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        color = if (achievement.unlocked) DoseviaCard else DoseviaSoftPanel,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (achievement.unlocked) rarityColor.copy(alpha = 0.65f) else DoseviaBorder),
        shadowElevation = if (achievement.unlocked) 6.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(if (achievement.unlocked) rarityColor.copy(alpha = 0.18f) else Color(0xFFD7DEE9), CircleShape)
                        .border(2.dp, if (achievement.unlocked) rarityColor.copy(alpha = 0.55f) else DoseviaBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(if (achievement.unlocked) "🏆" else "🔒", fontSize = 24.sp) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(achievement.title, color = DoseviaText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text(achievement.description, color = DoseviaMuted, fontSize = 12.sp)
                    Text("Reward: +${achievement.xp} XP • ${achievement.reward}", color = rarityColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
            LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)), color = rarityColor, trackColor = Color(0xFFE1E8F2))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${achievement.progress.coerceAtMost(achievement.target)} / ${achievement.target}", color = DoseviaMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(if (achievement.unlocked) "UNLOCKED" else achievement.rarity.uppercase(Locale.US), color = rarityColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
            if (expanded) {
                Text(
                    text = if (achievement.unlocked) "Benefit active on your profile: ${achievement.reward}." else "Keep coloring to unlock this reward and add XP to your Artist Level.",
                    color = DoseviaMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun AchievementRewardOverlay(reward: com.dopalette.app.data.AchievementReward?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    if (reward == null) return
    val haptic = LocalHapticFeedback.current
    val entrance = remember(reward.title) { Animatable(0f) }
    LaunchedEffect(reward) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        entrance.animateTo(1f, animationSpec = tween(420, easing = FastOutSlowInEasing))
    }
    Box(modifier = modifier.padding(16.dp), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .offset(y = ((-90 + entrance.value * 112).dp))
                .graphicsLayer { alpha = entrance.value; scaleX = 0.92f + entrance.value * 0.08f; scaleY = 0.92f + entrance.value * 0.08f },
            shape = RoundedCornerShape(24.dp),
            color = DoseviaCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaPinkPrimary.copy(alpha = 0.45f)),
            shadowElevation = 14.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val borderRewardId = normalizeRewardBorderId(reward.reward)
                    if (borderRewardId != null) ProfileAvatarWithBorder(size = 58.dp, textSize = 16.dp, borderId = borderRewardId)
                    else Box(modifier = Modifier.size(52.dp).background(DoseviaAccentBrush, CircleShape), contentAlignment = Alignment.Center) { Text("🏆", fontSize = 25.sp) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Achievement Unlocked!", color = DoseviaGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        Text(reward.title, color = DoseviaText, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Text("+${reward.xp} XP • ${reward.reward}", color = DoseviaPinkPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(42.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary, contentColor = Color.White)) { Text("OK", fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun FeedbackScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    var email by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var submitAttempted by rememberSaveable { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var sendError by remember { mutableStateOf<String?>(null) }

    val emailTrim = email.trim()
    val topicTrim = topic.trim()
    val messageTrim = message.trim()
    val emailBlank = submitAttempted && emailTrim.isEmpty()
    val emailInvalid = submitAttempted && emailTrim.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(emailTrim).matches()
    val topicBlank = submitAttempted && topicTrim.isEmpty()
    val messageBlank = submitAttempted && messageTrim.isEmpty()
    val isEmailValid = emailTrim.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(emailTrim).matches()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DoseviaBackgroundBrush)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Feedback", color = DoseviaText, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text("Send bug reports, suggestions, or artwork issues.", color = DoseviaMuted, fontSize = 13.sp)
            }
            BackPillButton(onClick = onBack)
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = DoseviaCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Contact DoPalette", color = DoseviaText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tell us what happened or what you want improved. This sends directly to your DoPalette Google Form.",
                    color = DoseviaMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(14.dp))

                DoseviaFeedbackField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email (required)",
                    icon = "✉️",
                    singleLine = true,
                    isError = emailBlank || emailInvalid,
                    supportingText = when {
                        emailBlank -> "Email is required."
                        emailInvalid -> "Please enter a valid email address."
                        else -> null
                    }
                )

                Spacer(Modifier.height(10.dp))

                DoseviaFeedbackField(
                    value = topic,
                    onValueChange = { new -> if (wordCount(new) <= 50) topic = new },
                    label = "Subject (required)",
                    icon = "📝",
                    singleLine = true,
                    isError = topicBlank,
                    supportingText = if (topicBlank) "Subject is required." else "${wordCount(topic)}/50 words"
                )

                Spacer(Modifier.height(10.dp))

                DoseviaFeedbackField(
                    value = message,
                    onValueChange = { new -> if (wordCount(new) <= 200) message = new },
                    label = "Message (required)",
                    icon = "💬",
                    singleLine = false,
                    minLines = 5,
                    isError = messageBlank,
                    supportingText = if (messageBlank) "Message is required." else "${wordCount(message)}/200 words"
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (sending) return@Button
                        submitAttempted = true
                        successMessage = null
                        sendError = null

                        if (!isEmailValid || topicTrim.isEmpty() || messageTrim.isEmpty()) {
                            return@Button
                        }

                        sending = true
                        scope.launch(Dispatchers.IO) {
                            val result = GoogleFormSupportSender.submit(emailTrim, topicTrim, messageTrim)
                            withContext(Dispatchers.Main) {
                                sending = false
                                if (result.isSuccess) {
                                    topic = ""
                                    message = ""
                                    submitAttempted = false
                                    successMessage = "Sent! Thanks — we’ll review it."
                                } else {
                                    sendError = result.exceptionOrNull()?.message ?: "Failed to send. Please try again."
                                }
                            }
                        }
                    },
                    enabled = !sending,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary, contentColor = Color.White)
                ) {
                    if (sending) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(10.dp))
                        Text("Sending…", fontWeight = FontWeight.Bold)
                    } else {
                        Text("➤", fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("Send", fontWeight = FontWeight.Bold)
                    }
                }

                successMessage?.let { text ->
                    Spacer(Modifier.height(10.dp))
                    Text(text, color = DoseviaGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                sendError?.let { text ->
                    Spacer(Modifier.height(10.dp))
                    Text(text, color = DoseviaRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

            }
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun DoseviaFeedbackField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: String,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Text(icon, fontSize = 18.sp) },
        singleLine = singleLine,
        minLines = minLines,
        isError = isError,
        supportingText = supportingText?.let { text -> ({ Text(text) }) },
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = DoseviaSoftPanel,
            unfocusedContainerColor = DoseviaSoftPanel,
            disabledContainerColor = DoseviaSoftPanel,
            errorContainerColor = Color(0xFFFFEEF1),
            focusedIndicatorColor = DoseviaPinkPrimary,
            unfocusedIndicatorColor = DoseviaBorder,
            errorIndicatorColor = DoseviaRed,
            focusedLabelColor = DoseviaPinkPrimary,
            unfocusedLabelColor = DoseviaMuted,
            errorLabelColor = DoseviaRed,
            cursorColor = DoseviaPinkPrimary,
            errorCursorColor = DoseviaRed,
            errorSupportingTextColor = DoseviaRed
        )
    )
}

private fun wordCount(text: String): Int = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size

@Composable
private fun AboutDoPaletteScreen(catalogItems: List<CatalogItem>, onBack: () -> Unit) {
    val byCategory = catalogItems.groupBy { it.category }.toSortedMap()
    ProfessionalInfoPage(
        title = "About DoPalette",
        eyebrow = "VERSION 1.0",
        body = "DoPalette is a fun coloring app with smooth brushes, easy fill, clean pages, and simple categories.",
        sections = listOf(
            "Current categories" to byCategory.map { (cat, items) -> "$cat: ${items.map { it.title }.distinct().size} pages" },
            "How coloring works" to listOf("Pick a page and start coloring.", "Your colors stay inside the artwork as much as possible.", "The black outlines stay clear and smooth."),
            "What you can do" to listOf("Save your draft", "Finish artwork", "Change your profile name and picture", "Earn achievements", "Download your art")
        ),
        onBack = onBack
    )
}

@Composable
private fun PrivacyPolicyScreen(onBack: () -> Unit) {
    ProfessionalInfoPage(
        title = "Privacy Policy",
        eyebrow = "ACCOUNT & DATA",
        body = "DoPalette keeps your coloring on this device. If you sign in, your profile and shared Community posts can stay connected to your account.",
        sections = listOf(
            "Saved on this device" to listOf("Draft artwork", "Finished artwork", "Small artwork previews", "Profile name and picture", "Achievements"),
            "Saved with your account" to listOf("Display name", "Email", "XP, level, badge, and border", "Profile picture", "Community posts you choose to share"),
            "Images" to listOf("Profile pictures are made smaller so they load faster.", "Community artwork uses a preview for the feed and a sharper image when opened."),
            "Your controls" to listOf("Sign Out keeps your artwork on this device.", "Erase App Data removes local drafts, finished artwork, profile data, and badges.", "You can report or hide Community posts that do not belong.")
        ),
        onBack = onBack
    )
}

@Composable
private fun TermsOfServiceScreen(onBack: () -> Unit) {
    ProfessionalInfoPage(
        title = "Terms of Service",
        eyebrow = "DOPALETTE",
        body = "DoPalette is for coloring, creativity, and safe artwork sharing. These terms are written into the app so the Play Store build has clear user-facing rules.",
        sections = listOf(
            "Using the app" to listOf("You may color, save, edit, export, and share your finished coloring pages.", "You are responsible for artwork and text you publish to Community."),
            "Accounts" to listOf("Google sign-in helps protect your account and profile.", "You may delete your account from the Profile tab."),
            "Community" to listOf("Do not upload illegal, hateful, sexual, violent, harassing, or spam content.", "DoPalette may remove reported artwork and restrict abusive accounts."),
            "Uploads" to listOf("Profile pictures and shared artwork are made smaller so they load faster.", "Do not upload content that you do not have the right to share.")
        ),
        onBack = onBack
    )
}

@Composable
private fun CommunityGuidelinesScreen(onBack: () -> Unit) {
    ProfessionalInfoPage(
        title = "Community Guidelines",
        eyebrow = "SAFETY",
        body = "DoPalette Community is for friendly coloring inspiration, not social media. Sharing is temporary, limited, and moderated.",
        sections = listOf(
            "Allowed" to listOf("Finished coloring artworks", "Friendly comments and positive feedback", "Creative inspiration suitable for general audiences"),
            "Not allowed" to listOf("Hate, harassment, bullying, threats, or spam", "Sexual, graphic, violent, illegal, or unsafe content", "Impersonation or uploading someone else's private content"),
            "Built-in protections" to listOf("Report artwork", "Block user", "Limited active shared artworks", "7-day expiration for shared artworks", "No followers and no permanent liked-artwork profile gallery"),
            "Moderation" to listOf("Reported artwork can be hidden or removed.", "Repeated abuse can lead to loss of Community access.")
        ),
        onBack = onBack
    )
}

@Composable
private fun BlockedUsersScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var blockedIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun loadBlocked() {
        scope.launch {
            loading = true
            blockedIds = withContext(Dispatchers.IO) { CommunityRepository.loadBlockedUserIds(context).toList().sorted() }
            loading = false
        }
    }

    LaunchedEffect(GoogleAuthController.accountStateTick.intValue) { loadBlocked() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("COMMUNITY SAFETY", color = DoseviaGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text("Blocked Users", color = DoseviaText, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("Unblock people you previously hid from your Community feed.", color = DoseviaMuted, fontSize = 13.sp)
            }
            BackPillButton(onClick = onBack)
        }

        if (GoogleAuthController.refreshCurrentAccount(context) == null) {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = DoseviaSoftPanel, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)) {
                Text("Sign in first to manage blocked users.", color = DoseviaMuted, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold)
            }
        } else if (loading) {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = DoseviaSoftPanel) {
                Text("Please wait…", color = DoseviaMuted, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold)
            }
        } else if (blockedIds.isEmpty()) {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = DoseviaSoftPanel, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)) {
                Text("No blocked users yet.", color = DoseviaMuted, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold)
            }
        } else {
            blockedIds.forEach { uid ->
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = DoseviaCard, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder), shadowElevation = 4.dp) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.size(42.dp).background(DoseviaAccentBrush, CircleShape), contentAlignment = Alignment.Center) {
                            Text(uid.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Blocked user", color = DoseviaText, fontWeight = FontWeight.Black)
                            Text("Hidden from your Community", color = DoseviaMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) { CommunityRepository.unblockUser(context, uid) }
                                    message = result.message
                                    loadBlocked()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary, contentColor = Color.White)
                        ) { Text("Unblock", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    message?.let { CommunityMessageDialog(message = it, onDismiss = { message = null }) }
}

@Composable
private fun ReportHistoryScreen(onBack: () -> Unit) {
    ProfessionalInfoPage(
        title = "Report History",
        eyebrow = "COMMUNITY SAFETY",
        body = "Reports help keep Community friendly and safe. Reports you send will appear here when available.",
        sections = listOf(
            "Report reasons" to listOf("Spam", "Inappropriate artwork", "Bullying", "Other safety concern"),
            "What happens" to listOf("A report is sent for review.", "Unsafe artwork can be hidden or removed.")
        ),
        onBack = onBack
    )
}

@Composable
private fun ProfessionalInfoPage(
    title: String,
    eyebrow: String,
    body: String,
    sections: List<Pair<String, List<String>>>,
    primaryText: String? = null,
    onPrimary: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(eyebrow, color = DoseviaGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text(title, color = DoseviaText, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
            BackPillButton(onClick = onBack)
        }
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), color = DoseviaCard, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder), shadowElevation = 8.dp) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(body, color = DoseviaMuted, fontSize = 14.sp)
                sections.forEach { (heading, items) -> InfoSection(heading, items) }
                if (primaryText != null && onPrimary != null) {
                    Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary)) {
                        Text(primaryText, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun InfoSection(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = DoseviaText, fontSize = 15.sp, fontWeight = FontWeight.Black)
        items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Text("•", color = DoseviaPinkPrimary, fontWeight = FontWeight.Black)
                Text(item, color = DoseviaMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
            }
        }
    }
}


private fun dedupeCommunityPosts(posts: List<CommunityRepository.CommunityPost>): List<CommunityRepository.CommunityPost> {
    fun duplicateKey(post: CommunityRepository.CommunityPost): String {
        val ownerKey = post.ownerId.ifBlank { post.ownerEmail.ifBlank { post.ownerName } }
            .trim()
            .lowercase()
        val templateKey = post.templateId.ifBlank { post.title }
            .trim()
            .lowercase()
        return listOf(
            ownerKey,
            post.title.trim().lowercase(),
            post.category.trim().lowercase(),
            templateKey,
            post.templateVersion.toString()
        ).joinToString("|")
    }
    return posts
        .filter { it.id.isNotBlank() && it.status == "active" }
        .sortedByDescending { it.createdAtMillis }
        .distinctBy { duplicateKey(it) }
}

@Composable
private fun CommunityTab(onComingSoon: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var newToday by remember { mutableStateOf(dedupeCommunityPosts(CommunityRepository.loadCachedNewToday(context))) }
    var featured by remember { mutableStateOf(dedupeCommunityPosts(CommunityRepository.loadCachedFeatured(context))) }
    var myPosts by remember { mutableStateOf(dedupeCommunityPosts(CommunityRepository.loadCachedMyPosts(context))) }
    var selectedPost by remember { mutableStateOf<CommunityRepository.CommunityPost?>(null) }
    var showReports by remember { mutableStateOf(false) }
    var selectedCommunityTab by remember { mutableStateOf("New") }
    val signedInAccount = GoogleAuthController.refreshCurrentAccount(context)
    val isSignedIn = signedInAccount != null
    val isAdmin = CommunityRepository.isAdmin(signedInAccount)
    val visibleNewToday = remember(newToday) { dedupeCommunityPosts(newToday) }
    val visibleFeatured = remember(featured, newToday, myPosts) {
        dedupeCommunityPosts(featured + newToday + myPosts)
            .sortedWith(
                compareByDescending<CommunityRepository.CommunityPost> { it.likesCount }
                    .thenByDescending { it.createdAtMillis }
            )
            .take(60)
    }
    val visibleMyPosts = remember(myPosts) { dedupeCommunityPosts(myPosts) }
    val selectedPosts = remember(selectedCommunityTab, visibleNewToday, visibleFeatured, visibleMyPosts) {
        when (selectedCommunityTab) {
            "Featured" -> visibleFeatured
            "My Art" -> visibleMyPosts
            else -> visibleNewToday
        }
    }

    fun refreshCommunity() {
        scope.launch {
            withContext(Dispatchers.IO) {
                CommunityRepository.cleanupLocalDuplicatePosts(context)
                CommunityRepository.refreshCachedOwnerProfiles(context)
            }
            val cachedMy = withContext(Dispatchers.IO) { CommunityRepository.loadCachedMyPosts(context) }
            val cachedNew = withContext(Dispatchers.IO) { CommunityRepository.loadCachedNewToday(context) }
            val cachedFeatured = withContext(Dispatchers.IO) { CommunityRepository.loadCachedFeatured(context) }
            if (cachedMy.isNotEmpty()) myPosts = dedupeCommunityPosts(cachedMy)
            if (cachedNew.isNotEmpty()) newToday = dedupeCommunityPosts(cachedNew)
            if (cachedFeatured.isNotEmpty()) featured = dedupeCommunityPosts(cachedFeatured)

            val hasAnyCachedPosts = cachedMy.isNotEmpty() || cachedNew.isNotEmpty() || cachedFeatured.isNotEmpty()
            loading = !hasAnyCachedPosts

            // Older devices need the Community screen to become usable first.
            // If we already have saved posts, give Compose a moment to draw those cached cards
            // before checking for new posts and thumbnail work in the background.
            if (hasAnyCachedPosts) delay(180)

            runCatching {
                val liveNew = withContext(Dispatchers.IO) { CommunityRepository.loadNewToday(context, 24) }
                if (liveNew.isNotEmpty() || newToday.isEmpty()) newToday = dedupeCommunityPosts(liveNew)

                val liveMy = withContext(Dispatchers.IO) { CommunityRepository.loadMyPosts(context) }
                if (liveMy.isNotEmpty() || myPosts.isEmpty()) myPosts = dedupeCommunityPosts(liveMy)

                // Trending is nice to have, but it should not block first paint on slower phones.
                if (hasAnyCachedPosts) delay(700)
                val liveFeatured = withContext(Dispatchers.IO) { CommunityRepository.loadFeatured(context, 24) }
                if (liveFeatured.isNotEmpty() || featured.isEmpty()) featured = dedupeCommunityPosts(liveFeatured)

                AchievementStore.recordCommunityStats(myPosts.size, myPosts.sumOf { it.likesCount })
            }.onFailure { error ->
                if (!hasAnyCachedPosts) message = error.message ?: "Community could not open. Please try again."
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        GoogleAuthController.refreshCurrentAccount(context)
        refreshCommunity()
    }
    LaunchedEffect(GoogleAuthController.accountStateTick.intValue) { refreshCommunity() }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Community", color = DoseviaText, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("Explore artwork shared by DoPalette artists.", color = DoseviaMuted, fontSize = 13.sp)
            }
            Button(onClick = { refreshCommunity() }, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary, contentColor = Color.White)) {
                Text(if (loading) "Loading" else "Refresh", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }
        if (!isSignedIn) {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = DoseviaSoftPanel, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)) {
                Text("Sign in to share and like artwork.", color = DoseviaMuted, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
            }
        }
        if (isAdmin) {
            Button(onClick = { showReports = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaSoftPanel, contentColor = DoseviaText)) {
                Text("Review Posts", fontWeight = FontWeight.ExtraBold)
            }
        }
        CommunityFilterTabs(
            selected = selectedCommunityTab,
            showMyArt = isSignedIn,
            onSelected = { selectedCommunityTab = it }
        )
        CommunityPostGrid(
            modifier = Modifier.weight(1f),
            posts = selectedPosts,
            emptyText = when (selectedCommunityTab) {
                "Featured" -> "Featured artwork will appear here."
                "My Art" -> "Your shared artwork will appear here."
                else -> "No artwork shared yet."
            },
            onPostClick = { selectedPost = it }
        )
    }

    selectedPost?.let { post ->
        CommunityPostModal(
            post = post,
            onDismiss = { selectedPost = null },
            onLike = {
                if (GoogleAuthController.refreshCurrentAccount(context) == null) {
                    message = "Please sign in to like artwork."
                } else {
                    scope.launch {
                        runCatching {
                            val result = withContext(Dispatchers.IO) { CommunityRepository.likePost(context, post.id) }
                            message = result.message
                            refreshCommunity()
                        }.onFailure {
                            message = "Could not like this artwork. Please try again."
                        }
                    }
                }
            },
            onReportArtwork = { reason, details ->
                if (GoogleAuthController.refreshCurrentAccount(context) == null) {
                    message = "Please sign in to report artwork."
                    selectedPost = null
                } else {
                    scope.launch {
                        runCatching {
                            message = withContext(Dispatchers.IO) { CommunityRepository.reportArtwork(context, post, reason, details).message }
                        }.onFailure {
                            message = "Could not send this. Please try again."
                        }
                        selectedPost = null
                    }
                }
            },
            onReportUser = { reason, details ->
                if (GoogleAuthController.refreshCurrentAccount(context) == null) {
                    message = "Please sign in to report this person."
                    selectedPost = null
                } else {
                    scope.launch {
                        runCatching {
                            message = withContext(Dispatchers.IO) { CommunityRepository.reportUser(context, post, reason, details).message }
                        }.onFailure {
                            message = "Could not send this. Please try again."
                        }
                        selectedPost = null
                    }
                }
            },
            onBlockUser = {
                if (GoogleAuthController.refreshCurrentAccount(context) == null) {
                    message = "Please sign in to hide this person."
                    selectedPost = null
                } else {
                    scope.launch {
                        runCatching {
                            message = withContext(Dispatchers.IO) { CommunityRepository.blockUser(context, post.ownerId).message }
                            refreshCommunity()
                        }.onFailure {
                            message = "Could not hide this person. Please try again."
                        }
                        selectedPost = null
                    }
                }
            },
            onDeletePost = {
                if (GoogleAuthController.refreshCurrentAccount(context) == null) {
                    message = "Please sign in to delete your Community post."
                    selectedPost = null
                } else {
                    scope.launch {
                        runCatching {
                            message = withContext(Dispatchers.IO) { CommunityRepository.deletePost(context, post.id).message }
                            refreshCommunity()
                        }.onFailure {
                            message = "Could not delete this post. Please try again."
                        }
                        selectedPost = null
                    }
                }
            }
        )
    }
    if (showReports) AdminReportsDialog(onDismiss = { showReports = false }, onMessage = { message = it })
    message?.let { text -> CommunityMessageDialog(message = text, onDismiss = { message = null }) }
}

@Composable
private fun CommunityMessageDialog(message: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                shape = RoundedCornerShape(30.dp),
                color = DoseviaCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
                shadowElevation = 18.dp
            ) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(42.dp).background(DoseviaAccentBrush, CircleShape), contentAlignment = Alignment.Center) {
                            Text("◎", color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Community", color = DoseviaText, fontWeight = FontWeight.Black, fontSize = 22.sp)
                            Text("Quick message", color = DoseviaMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Surface(shape = RoundedCornerShape(20.dp), color = DoseviaSoftPanel, modifier = Modifier.fillMaxWidth()) {
                        Text(message, modifier = Modifier.padding(16.dp), color = DoseviaText, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary, contentColor = Color.White)) {
                        Text("Got it", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}


@Composable
private fun CommunityGuideScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Community Help", color = DoseviaText, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("A quick guide for sharing safely.", color = DoseviaMuted, fontSize = 13.sp)
            }
            Button(onClick = onBack, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaSoftPanel, contentColor = DoseviaText)) {
                Text("Back", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }
        CommunityGuideCard("Share your finished art", listOf("Finish your coloring page.", "Tap Share to Community.", "Your post appears in My Art and New."))
        CommunityGuideCard("Be kind", listOf("Share friendly artwork only.", "Do not post mean, scary, or unsafe things.", "Report anything that does not belong."))
        CommunityGuideCard("Your posts", listOf("You can keep up to 10 public posts.", "You can share 2 artworks each week.", "Delete an old post when you want to make room."))
        CommunityGuideCard("Viewing art", listOf("Tap a post to open it.", "Tap the artwork again to see it bigger.", "Pinch to zoom and drag to move."))
    }
}

@Composable
private fun CommunityGuideCard(title: String, lines: List<String>) {
    Surface(shape = RoundedCornerShape(24.dp), color = DoseviaCard, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder), shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = DoseviaText, fontSize = 18.sp, fontWeight = FontWeight.Black)
            lines.forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Text("•", color = DoseviaPinkPrimary, fontWeight = FontWeight.Black)
                    Text(line, color = DoseviaMuted, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}



@Composable
private fun CommunityFilterTabs(selected: String, showMyArt: Boolean, onSelected: (String) -> Unit) {
    val tabs = if (showMyArt) listOf("New", "Featured", "My Art") else listOf("New", "Featured")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val active = selected == tab
            Button(
                onClick = { onSelected(tab) },
                modifier = Modifier.weight(1f).heightIn(min = 46.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (active) DoseviaPinkPrimary else DoseviaCard,
                    contentColor = if (active) Color.White else DoseviaText
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = if (active) 5.dp else 1.dp)
            ) {
                Text(tab, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun CommunityPostGrid(
    modifier: Modifier = Modifier,
    posts: List<CommunityRepository.CommunityPost>,
    emptyText: String,
    onPostClick: (CommunityRepository.CommunityPost) -> Unit
) {
    val displayPosts = remember(posts) { dedupeCommunityPosts(posts) }
    if (displayPosts.isEmpty()) {
        Surface(shape = RoundedCornerShape(24.dp), color = DoseviaCard, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder), modifier = Modifier.fillMaxWidth()) {
            Text(emptyText, color = DoseviaMuted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(18.dp))
        }
        return
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 720.dp) 4 else 2
        val gap = if (columns == 4) 12.dp else 10.dp
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalArrangement = Arrangement.spacedBy(gap),
            contentPadding = PaddingValues(bottom = 128.dp)
        ) {
            gridItems(displayPosts, key = { it.id.trim() }) { post ->
                CommunityArtworkCard(post, width = Dp.Unspecified) { onPostClick(post) }
            }
        }
    }
}

@Composable
private fun CommunityGalleryScreen(
    title: String,
    posts: List<CommunityRepository.CommunityPost>,
    onBack: () -> Unit,
    onPostClick: (CommunityRepository.CommunityPost) -> Unit
) {
    val displayPosts = remember(posts) { dedupeCommunityPosts(posts) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = DoseviaText, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("${displayPosts.size} artworks", color = DoseviaMuted, fontSize = 12.sp)
            }
            Button(onClick = onBack, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaSoftPanel, contentColor = DoseviaText)) {
                Text("Back", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val columns = if (maxWidth >= 720.dp) 4 else 2
            val gap = 12.dp
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(gap),
                horizontalArrangement = Arrangement.spacedBy(gap),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                gridItems(displayPosts, key = { it.id.trim() }) { post ->
                    CommunityArtworkCard(post, width = Dp.Unspecified) { onPostClick(post) }
                }
            }
        }
    }
}

@Composable
private fun CommunityPostSection(title: String, subtitle: String, posts: List<CommunityRepository.CommunityPost>, emptyText: String, onViewAll: () -> Unit, onPostClick: (CommunityRepository.CommunityPost) -> Unit) {
    val displayPosts = remember(posts) { dedupeCommunityPosts(posts) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = DoseviaText, fontSize = 20.sp, fontWeight = FontWeight.Black)
            if (displayPosts.isNotEmpty()) TextButton(onClick = onViewAll) { Text("View All", fontWeight = FontWeight.ExtraBold) }
        }
        if (subtitle.isNotBlank()) Text(subtitle, color = DoseviaMuted, fontSize = 12.sp, lineHeight = 18.sp)
        if (displayPosts.isEmpty()) {
            Surface(shape = RoundedCornerShape(22.dp), color = DoseviaCard, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder), modifier = Modifier.fillMaxWidth()) {
                Text(emptyText, color = DoseviaMuted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(16.dp))
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cardWidth = when {
                    maxWidth < 360.dp -> maxWidth * 0.82f
                    maxWidth < 480.dp -> 210.dp
                    maxWidth < 720.dp -> 230.dp
                    else -> 250.dp
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(displayPosts.take(24), key = { it.id.trim() }) { post ->
                        CommunityArtworkCard(post, width = cardWidth) { onPostClick(post) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityArtworkPreview(post: CommunityRepository.CommunityPost, modifier: Modifier = Modifier, targetWidth: Int = 720) {
    val context = LocalContext.current
    val cacheKey = remember(post.id, post.title, targetWidth) { "${post.id}_${post.title}_$targetWidth" }
    var rendered by remember(cacheKey) {
        mutableStateOf(CommunityPreviewMemoryCache.get(cacheKey) ?: CommunityPreviewMemoryCache.loadDisk(context, cacheKey))
    }
    var baseBitmap by remember(post.title) { mutableStateOf(HomePreviewCache.get(post.title, 0, true)) }
    var failed by remember(cacheKey) { mutableStateOf(false) }
    var isLoading by remember(cacheKey) { mutableStateOf(rendered == null) }

    LaunchedEffect(post.title) {
        if (baseBitmap == null) {
            baseBitmap = withContext(Dispatchers.IO) {
                HomePreviewCache.getOrCreate(
                    context = context.applicationContext,
                    title = post.title,
                    strokes = emptyList(),
                    signature = 0,
                    staticBase = true
                )
            }
        }
    }

    LaunchedEffect(cacheKey) {
        if (rendered != null) {
            isLoading = false
            return@LaunchedEffect
        }
        failed = false
        isLoading = true
        // Draw the card first with the same Home thumbnail loader, then build the Community image.
        delay(80)
        val bitmap = withContext(Dispatchers.IO) {
            CommunityPreviewMemoryCache.loadDisk(context, cacheKey)?.asAndroidBitmap() ?: run {
                val recipe = CommunityRepository.loadPostRecipe(context, post)
                if (recipe.isBlank()) return@withContext null
                val strokes = CommunityRepository.recipeToStrokes(recipe)
                val (baseW, baseH) = SelectionArtworkAssets.baseSize(context, post.title)
                val previewW = targetWidth.coerceIn(720, 2160)
                val previewH = (previewW * (baseH.toFloat() / baseW.toFloat())).roundToInt().coerceAtLeast(1)
                ArtworkRenderer.renderArtworkBitmap(context, strokes, width = previewW, height = previewH, title = post.title)?.also {
                    CommunityPreviewMemoryCache.saveDisk(context, cacheKey, it)
                }
            }
        }
        val image = bitmap?.asImageBitmap()
        rendered = image
        image?.let { CommunityPreviewMemoryCache.put(cacheKey, it) }
        failed = image == null
        isLoading = false
    }

    Box(modifier = modifier.background(DoseviaSoftPanel, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
        val image = rendered
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = post.title,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Fit,
            )
        } else {
            val base = baseBitmap
            if (base != null) {
                Image(
                    bitmap = base.asImageBitmap(),
                    contentDescription = post.title,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Fit,
                )
            } else {
                PlaceholderCategoryImage(genre = post.title, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)))
            }
        }

        if (isLoading && !failed) {
            ThumbnailLoadingOverlay(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)))
        } else if (failed && rendered == null) {
            Surface(
                color = Color.White.copy(alpha = 0.9f),
                shape = RoundedCornerShape(999.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)
            ) {
                Text(
                    "Open to refresh",
                    color = DoseviaMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun communityAspectRatio(title: String): Float {
    val context = LocalContext.current
    val (w, h) = remember(title) { SelectionArtworkAssets.baseSize(context, title) }
    return (w.toFloat() / h.toFloat()).takeIf { it.isFinite() && it > 0f } ?: 1f
}

private fun communityBorderColor(borderId: String): Color = when (borderId.lowercase(Locale.US)) {
    "gold", "golden", "showcase", "gold_master_border", "completionist_border" -> Color(0xFFE7C94F)
    "legendary", "legendary_border" -> Color(0xFF9C27B0)
    "blue", "ocean", "starter", "ocean_border", "space_border" -> Color(0xFF2E6BFF)
    "pink", "rose", "popular", "flower_border", "dessert_border" -> Color(0xFFF17878)
    "green", "emerald", "fruit_border", "vegetable_border" -> Color(0xFF24B47E)
    "purple", "violet", "fantasy_border", "diamond_border" -> Color(0xFF9C27B0)
    "dinosaur_border", "dragon_border", "vehicle_border", "sports_border", "animal_border" -> Color(0xFFE7C94F)
    else -> Color(0xFF8FB1FF)
}


private fun badgeAccent(badgeId: String): Color = when (badgeId.lowercase(Locale.US)) {
    "starter" -> Color(0xFF2E6BFF)
    "beginner" -> Color(0xFFB87333)
    "explorer" -> Color(0xFF00A7B5)
    "pro" -> Color(0xFF2E7D32)
    "champion" -> Color(0xFFE0A800)
    "master" -> Color(0xFF7B2CFF)
    "legend" -> Color(0xFFE84AAE)
    "jungle" -> Color(0xFF2E7D32)
    "bloom" -> Color(0xFFD84AA8)
    "road" -> Color(0xFF263238)
    "prehistoric" -> Color(0xFF7C5CFF)
    "garden" -> Color(0xFF4C9A2A)
    "fruit" -> Color(0xFFE36B2C)
    else -> DoseviaPinkPrimary
}

private fun profileBadgeIcon(badgeId: String): String = ""

private fun communityBadgeLabel(badgeId: String): String = profileBadgeTitle(badgeId.ifBlank { "starter" })

@Composable
private fun CommunityOwnerAvatar(post: CommunityRepository.CommunityPost, size: Dp) {
    val context = LocalContext.current
    var bitmap by remember(post.ownerId, post.ownerPhotoUrl, ProfileStore.avatarVersion.intValue) { mutableStateOf<ImageBitmap?>(null) }
    val isCurrentUser = GoogleAuthController.account.value?.uid == post.ownerId
    LaunchedEffect(post.ownerId, post.ownerPhotoUrl, isCurrentUser, ProfileStore.avatarVersion.intValue) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                if (isCurrentUser) {
                    ProfileStore.localAvatarFile()?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
                } else if (!post.ownerPhotoUrl.isNullOrBlank()) {
                    val avatarKey = "avatar_${post.ownerPhotoUrl.hashCode()}"
                    CommunityPreviewMemoryCache.loadDisk(context, avatarKey) ?:
                        URL(post.ownerPhotoUrl).openStream().use { stream ->
                            BitmapFactory.decodeStream(stream)?.also { CommunityPreviewMemoryCache.saveDisk(context, avatarKey, it) }?.asImageBitmap()
                        }
                } else null
            }.getOrNull()
        }
    }
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        val image = bitmap
        Box(modifier = Modifier.fillMaxSize(0.72f).background(DoseviaAccentBrush, CircleShape).clip(CircleShape), contentAlignment = Alignment.Center) {
            if (image != null) Image(bitmap = image, contentDescription = post.ownerName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Text(post.ownerName.firstOrNull()?.uppercaseChar()?.toString() ?: "D", color = Color.White, fontWeight = FontWeight.Black, fontSize = (size.value * 0.30f).sp)
        }
        ProfileBorderOverlay(borderId = post.ownerBorderId, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun CommunityProfilePanel(post: CommunityRepository.CommunityPost) {
    Surface(shape = RoundedCornerShape(22.dp), color = DoseviaSoftPanel, modifier = Modifier.fillMaxWidth(), border = androidx.compose.foundation.BorderStroke(1.dp, communityBorderColor(post.ownerBorderId).copy(alpha = 0.35f))) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CommunityOwnerAvatar(post = post, size = 58.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(post.ownerName, color = DoseviaText, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Level ${post.ownerLevel}", color = DoseviaMuted, fontSize = 12.sp)
                Text(communityBadgeLabel(post.ownerBadgeId), color = badgeAccent(post.ownerBadgeId), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${post.likesCount} likes", color = DoseviaRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CommunityArtworkCard(post: CommunityRepository.CommunityPost, width: Dp, onClick: () -> Unit) {
    val cardModifier = if (width.value.isNaN()) Modifier.fillMaxWidth() else Modifier.width(width)
    Surface(modifier = cardModifier.clip(RoundedCornerShape(26.dp)).clickable(onClick = onClick), shape = RoundedCornerShape(26.dp), color = DoseviaCard, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder), shadowElevation = 6.dp) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CommunityArtworkPreview(post, targetWidth = 1200, modifier = Modifier.fillMaxWidth().aspectRatio(communityAspectRatio(post.title)).clip(RoundedCornerShape(20.dp)))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CommunityOwnerAvatar(post = post, size = 36.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.title, color = DoseviaText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(post.ownerName, color = DoseviaMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Level ${post.ownerLevel}", color = communityBorderColor(post.ownerBorderId), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("❤️ ${post.likesCount}", color = DoseviaRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CommunityPostModal(post: CommunityRepository.CommunityPost, onDismiss: () -> Unit, onLike: () -> Unit, onReportArtwork: (String, String) -> Unit, onReportUser: (String, String) -> Unit, onBlockUser: () -> Unit, onDeletePost: () -> Unit) {
    var reportMode by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showFullArtwork by remember { mutableStateOf(false) }
    val account = GoogleAuthController.account.value
    val isOwner = account?.uid == post.ownerId
    val isAdmin = CommunityRepository.isAdmin(account)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
            val modalWidth = when {
                maxWidth >= 900.dp -> 720.dp
                maxWidth >= 600.dp -> 620.dp
                else -> maxWidth
            }
            val maxModalHeight = maxHeight * 0.90f
            Surface(modifier = Modifier.width(modalWidth).heightIn(max = maxModalHeight), shape = RoundedCornerShape(30.dp), color = DoseviaCard, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder), shadowElevation = 18.dp) {
                Column(modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(post.title, color = DoseviaText, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (isOwner) "Shared by you" else "Shared artwork", color = DoseviaMuted, fontSize = 12.sp)
                        }
                        TextButton(onClick = onDismiss) { Text("Close", fontWeight = FontWeight.Bold) }
                    }
                    CommunityArtworkPreview(post, targetWidth = 1400, modifier = Modifier.fillMaxWidth().aspectRatio(communityAspectRatio(post.title)).clip(RoundedCornerShape(22.dp)).clickable { showFullArtwork = true })
                    CommunityProfilePanel(post = post)
                    if (!isOwner) {
                        Button(onClick = onLike, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaRed, contentColor = Color.White)) { Text("Like", fontWeight = FontWeight.ExtraBold) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { reportMode = "artwork" }, modifier = Modifier.weight(1f).heightIn(min = 46.dp), shape = RoundedCornerShape(18.dp)) { Text("Report", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            Button(onClick = { reportMode = "user" }, modifier = Modifier.weight(1f).heightIn(min = 46.dp), shape = RoundedCornerShape(18.dp)) { Text("Report Person", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                    if (isOwner || isAdmin) {
                        Button(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaRed, contentColor = Color.White)) { Text(if (isOwner) "Delete Post" else "Remove Post", fontWeight = FontWeight.ExtraBold) }
                    } else {
                        Button(onClick = onBlockUser, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaSoftPanel, contentColor = DoseviaText)) { Text("Hide Person", fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }
        }
    }
    reportMode?.let { mode -> ReportReasonDialog(if (mode == "artwork") "Report Artwork" else "Report User", onDismiss = { reportMode = null }) { reason, details -> if (mode == "artwork") onReportArtwork(reason, details) else onReportUser(reason, details); reportMode = null } }
    if (showFullArtwork) {
        CommunityFullArtworkViewer(post = post, onDismiss = { showFullArtwork = false })
    }
    if (confirmDelete) {
        CommunityConfirmDialog(
            title = "Delete this post?",
            body = "This removes it from Community. Your saved artwork will stay in your app.",
            confirmText = "Delete Post",
            danger = true,
            onDismiss = { confirmDelete = false },
            onConfirm = { confirmDelete = false; onDeletePost() }
        )
    }
}


@Composable
private fun CommunityFullArtworkViewer(post: CommunityRepository.CommunityPost, onDismiss: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)).padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(post.id) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = if (scale <= 1.01f) Offset.Zero else offset + pan
                        }
                    }
                    .clickable { if (scale > 1f) { scale = 1f; offset = Offset.Zero } },
                contentAlignment = Alignment.Center
            ) {
                CommunityArtworkPreview(
                    post = post,
                    targetWidth = 2160,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(communityAspectRatio(post.title))
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )
            }
            Surface(modifier = Modifier.align(Alignment.TopStart), shape = RoundedCornerShape(999.dp), color = Color.White.copy(alpha = 0.14f)) {
                Text(post.title, color = Color.White, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
            }
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd), shape = RoundedCornerShape(999.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.16f), contentColor = Color.White)) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
            Text("Pinch to zoom • Drag to move • Tap to reset", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp))
        }
    }
}

@Composable
private fun CommunityConfirmDialog(title: String, body: String, confirmText: String, danger: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Surface(modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp), shape = RoundedCornerShape(30.dp), color = DoseviaCard, border = androidx.compose.foundation.BorderStroke(1.dp, if (danger) DoseviaRed.copy(alpha = 0.45f) else DoseviaBorder), shadowElevation = 18.dp) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(title, color = DoseviaText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(body, color = DoseviaMuted, fontSize = 14.sp, lineHeight = 21.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 50.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaSoftPanel, contentColor = DoseviaText)) { Text("Cancel", fontWeight = FontWeight.Bold) }
                        Button(onClick = onConfirm, modifier = Modifier.weight(1f).heightIn(min = 50.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = if (danger) DoseviaRed else DoseviaPinkPrimary, contentColor = Color.White)) { Text(confirmText, fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportReasonDialog(title: String, onDismiss: () -> Unit, onPick: (String, String) -> Unit) {
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var customReason by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Surface(modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp), shape = RoundedCornerShape(30.dp), color = DoseviaCard, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder), shadowElevation = 18.dp) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(title, color = DoseviaText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("Tell us what is wrong so we can keep DoPalette safe.", color = DoseviaMuted, fontSize = 13.sp)
                    listOf("Spam", "Mean or hurtful", "Bullying", "Bad profile", "Other").forEach { reason ->
                        Button(onClick = { if (reason == "Other") selectedReason = reason else onPick(reason, "") }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selectedReason == reason) DoseviaPinkPrimary else DoseviaSoftPanel, contentColor = if (selectedReason == reason) Color.White else DoseviaText)) { Text(reason, fontWeight = FontWeight.Bold) }
                    }
                    if (selectedReason == "Other") {
                        OutlinedTextField(
                            value = customReason,
                            onValueChange = { customReason = it.take(200) },
                            label = { Text("Tell us why") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DoseviaSoftPanel,
                                unfocusedContainerColor = DoseviaSoftPanel,
                                focusedTextColor = DoseviaText,
                                unfocusedTextColor = DoseviaText
                            )
                        )
                        Button(
                            onClick = { onPick("Other", customReason.trim()) },
                            enabled = customReason.trim().length >= 3,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary, contentColor = Color.White)
                        ) { Text("Send Report", fontWeight = FontWeight.ExtraBold) }
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancel", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun ReportPersonRow(label: String, name: String, email: String, photoUrl: String?) {
    var bitmap by remember(label, name, email, photoUrl) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(photoUrl) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                if (!photoUrl.isNullOrBlank()) URL(photoUrl).openStream().use { BitmapFactory.decodeStream(it)?.asImageBitmap() } else null
            }.getOrNull()
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.size(38.dp).background(DoseviaAccentBrush, CircleShape).clip(CircleShape), contentAlignment = Alignment.Center) {
            val image = bitmap
            if (image != null) {
                Image(bitmap = image, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
            } else {
                Text(name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = DoseviaGreen, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            Text(name.ifBlank { "Unknown person" }, color = DoseviaText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (email.isNotBlank()) Text(email, color = DoseviaMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AdminReportsDialog(onDismiss: () -> Unit, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reports by remember { mutableStateOf(emptyList<CommunityRepository.ReportItem>()) }
    var loading by remember { mutableStateOf(true) }
    fun loadReports() {
        scope.launch {
            loading = true
            runCatching {
                reports = withContext(Dispatchers.IO) { CommunityRepository.loadOpenReports(context) }
            }.onFailure {
                reports = emptyList()
                onMessage("Could not open this page. Please try again.")
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { loadReports() }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = DoseviaCard, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)) {
            Column(modifier = Modifier.padding(18.dp).heightIn(max = 620.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Posts to Review", color = DoseviaText, fontSize = 22.sp, fontWeight = FontWeight.Black); TextButton(onClick = onDismiss) { Text("Close") } }
                Text("Check reported posts and keep Community friendly.", color = DoseviaMuted, fontSize = 12.sp)
                if (loading) LoadingWaveIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(72.dp)) else if (reports.isEmpty()) Text("Nothing to review right now.", color = DoseviaMuted) else reports.forEach { report ->
                    val reportKind = if (report.targetType == "artwork") "Artwork report" else "Artist report"
                    Surface(shape = RoundedCornerShape(18.dp), color = DoseviaSoftPanel, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("$reportKind: ${report.reason}", color = DoseviaText, fontWeight = FontWeight.ExtraBold)
                            Text("Artwork: ${report.artworkTitle}${if (report.artworkCategory.isNotBlank()) " • ${report.artworkCategory}" else ""}", color = DoseviaText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            if (report.artworkRecipe.isNotBlank()) {
                                CommunityArtworkPreview(
                                    post = CommunityRepository.CommunityPost(
                                        id = report.targetId,
                                        ownerId = report.targetOwnerId,
                                        ownerName = report.reportedName,
                                        ownerEmail = report.reportedEmail,
                                        ownerPhotoUrl = report.reportedPhotoUrl,
                                        title = report.artworkTitle,
                                        category = report.artworkCategory,
                                        recipe = report.artworkRecipe
                                    ),
                                    targetWidth = 360,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(communityAspectRatio(report.artworkTitle)).clip(RoundedCornerShape(16.dp))
                                )
                            }
                            ReportPersonRow("Sent by", report.reporterName, report.reporterEmail, report.reporterPhotoUrl)
                            ReportPersonRow("Artist", report.reportedName, report.reportedEmail, report.reportedPhotoUrl)
                            if (report.details.isNotBlank()) {
                                Surface(shape = RoundedCornerShape(14.dp), color = DoseviaCard, modifier = Modifier.fillMaxWidth()) {
                                    Text("Message: ${report.details}", color = DoseviaText, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(10.dp))
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { scope.launch { runCatching { onMessage(withContext(Dispatchers.IO) { CommunityRepository.dismissReport(context, report.id).message }); loadReports() }.onFailure { onMessage("Could not update this. Please try again.") } } }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Keep Post", fontSize = 12.sp) }
                                if (report.targetType == "artwork") Button(onClick = { scope.launch { runCatching { onMessage(withContext(Dispatchers.IO) { CommunityRepository.hideArtworkFromReport(context, report).message }); loadReports() }.onFailure { onMessage("Could not hide this artwork. Please try again.") } } }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaRed, contentColor = Color.White)) { Text("Hide Artwork", fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderFullPage(title: String, body: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BackPillButton(onClick = onBack)
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), color = DoseviaCard, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = DoseviaText, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text(body, color = DoseviaMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
                Text("Coming Soon", color = DoseviaGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun ComingSoonDialog(title: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = DoseviaCard, border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)) {
            Column(modifier = Modifier.padding(22.dp).widthIn(max = 420.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, color = DoseviaText, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text("Coming soon", color = DoseviaGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text("This feature will be available in a future update.", color = DoseviaMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary)) {
                    Text("Okay", color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun BottomTabs(activeTab: String, onSelect: (String) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 430.dp
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 4.dp else 10.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
            shadowElevation = 16.dp
        ) {
            Row(
                modifier = Modifier.padding(if (compact) 6.dp else 8.dp),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (selected) DoseviaPinkPrimary else Color.Transparent)
                            .clickable { onSelect(tab) }
                            .padding(vertical = if (compact) 11.dp else 12.dp, horizontal = if (compact) 5.dp else 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (tab) { "Selections" -> "▦"; "Community" -> "◎"; else -> "♙" },
                                color = if (selected) Color.White else Color(0xFF7E8AA0),
                                fontSize = if (compact) 17.sp else 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            if (selected) {
                                Text(
                                    text = if (compact && tab == "Selections") "Select" else tab,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = if (compact) 11.sp else 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPanel(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DoseviaCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = DoseviaText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(body, color = DoseviaMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ArtworkChoiceDialog(
    item: CatalogItem,
    canStartNew: Boolean,
    onDismiss: () -> Unit,
    onContinueDraft: () -> Unit,
    onStartNew: () -> Unit,
    onShowFinished: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(34.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .background(DoseviaCard, RoundedCornerShape(34.dp))
                    .border(1.dp, DoseviaBorder, RoundedCornerShape(34.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .heightIn(max = 620.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        ArtworkThumbnail(
                            title = item.title,
                            modifier = Modifier
                                .width(76.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(18.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("WORKSPACE", color = DoseviaGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            Text(item.title, color = DoseviaText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            Text(statusText(item.title), color = DoseviaMuted, fontSize = 13.sp)
                        }
                    }
                    if (!canStartNew) {
                        val pulse = rememberInfiniteTransition(label = "finished-limit-pulse")
                        val scale by pulse.animateFloat(
                            initialValue = 0.96f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(animation = tween(720), repeatMode = RepeatMode.Reverse),
                            label = "finished-limit-scale"
                        )
                        Text(
                            text = "LIMIT REACHED",
                            color = DoseviaRed,
                            fontSize = 28.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                        )
                    }
                    Text(
                        if (canStartNew) {
                            "Continue your draft, display a finished version, or start a clean new draft."
                        } else {
                            "You already have 2 finished layers. Delete one finished layer first before saving this current edit."
                        },
                        color = if (canStartNew) DoseviaMuted else DoseviaRed,
                        fontSize = if (canStartNew) 14.sp else 17.sp,
                        lineHeight = if (canStartNew) 20.sp else 24.sp,
                        fontWeight = if (canStartNew) FontWeight.Normal else FontWeight.ExtraBold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onContinueDraft,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DoseviaGreen, contentColor = Color.White)
                        ) { Text("Continue Draft", fontWeight = FontWeight.ExtraBold) }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            if (ArtworkStore.finishedFor(item.title).isNotEmpty()) {
                                Button(
                                    onClick = onShowFinished,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                                    shape = RoundedCornerShape(17.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DoseviaSoftPanel, contentColor = DoseviaText)
                                ) { Text("Show Finished", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                            }
                            if (canStartNew) {
                                Button(
                                    onClick = onStartNew,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                                    shape = RoundedCornerShape(17.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DoseviaOrangeAccent, contentColor = Color.White)
                                ) { Text("Start New", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                            }
                        }
                        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Cancel", color = DoseviaMuted, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumChoiceModal(
    eyebrow: String,
    title: String,
    body: String,
    primaryText: String,
    secondaryText: String,
    danger: Boolean,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp),
            shape = RoundedCornerShape(32.dp),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .background(DoseviaCard, RoundedCornerShape(32.dp))
                    .border(1.dp, DoseviaBorder, RoundedCornerShape(32.dp))
                    .padding(22.dp)
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(eyebrow.uppercase(), color = if (danger) DoseviaRed else DoseviaGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text(title, color = DoseviaText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(body, color = DoseviaMuted, fontSize = 14.sp, lineHeight = 20.sp)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onPrimary,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (danger) DoseviaRed else DoseviaGreen,
                            contentColor = Color.White
                        )
                    ) { Text(primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) }
                    Button(
                        onClick = onSecondary,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DoseviaSoftPanel, contentColor = DoseviaText)
                    ) { Text(secondaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                }
            }
        }
    }
}

private fun statusText(title: String): String {
    val hasDraft = ArtworkStore.hasDraft(title)
    val finishedCount = ArtworkStore.finishedFor(title).size
    return when {
        hasDraft && finishedCount > 0 -> "Draft + $finishedCount finished"
        hasDraft -> "Draft active"
        finishedCount > 0 -> "$finishedCount finished"
        else -> "Ready"
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawThumbnailStroke(stroke: StrokeData) {
    val activeBlend = if (stroke.color == Color.Transparent) BlendMode.Clear else BlendMode.SrcOver

    if (stroke.path.isEmpty) {
        drawCircle(
            color = stroke.color,
            radius = stroke.width / 4f,
            center = center,
            blendMode = activeBlend
        )
        return
    }

    drawPath(
        path = stroke.path,
        color = when (stroke.style) {
            BrushStyle.WATERCOLOR -> stroke.color.copy(alpha = 0.35f)
            BrushStyle.AIRBRUSH -> stroke.color.copy(alpha = 0.55f)
            else -> stroke.color
        },
        style = Stroke(
            width = stroke.width,
            cap = if (stroke.style == BrushStyle.CHISEL) StrokeCap.Square else StrokeCap.Round,
            join = if (stroke.style == BrushStyle.CHISEL) StrokeJoin.Miter else StrokeJoin.Round
        ),
        blendMode = activeBlend
    )
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun PremiumGiftButton(isPremium: Boolean, onClick: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "premium-gift-pulse")
    val scale by infinite.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "premium-gift-scale"
    )
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "premium-gift-glow"
    )

    Box(
        modifier = Modifier
            .size(52.dp)
            .graphicsLayer {
                scaleX = if (isPremium) 1f else scale
                scaleY = if (isPremium) 1f else scale
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!isPremium) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFFFFD966).copy(alpha = glowAlpha),
                    radius = size.minDimension * 0.48f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.58f),
                    radius = size.minDimension * 0.37f,
                    center = center
                )
            }
            PremiumGiftIcon(Modifier.size(38.dp))
        } else {
            PremiumCrownIcon(Modifier.size(38.dp))
        }
    }
}

@Composable
private fun PremiumGiftIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val boxBrush = Brush.verticalGradient(listOf(Color(0xFFFFD15C), Color(0xFFFF9F1C)))
        val lidBrush = Brush.verticalGradient(listOf(Color(0xFFFFE08A), Color(0xFFFFB33C)))
        val ribbonBrush = Brush.verticalGradient(listOf(Color(0xFFFF466A), Color(0xFFE11D48)))
        drawRoundRect(
            brush = boxBrush,
            topLeft = Offset(w * 0.18f, h * 0.43f),
            size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.42f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.11f, w * 0.11f)
        )
        drawRoundRect(
            brush = lidBrush,
            topLeft = Offset(w * 0.14f, h * 0.34f),
            size = androidx.compose.ui.geometry.Size(w * 0.72f, h * 0.20f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.10f, w * 0.10f)
        )
        drawRoundRect(
            brush = ribbonBrush,
            topLeft = Offset(w * 0.44f, h * 0.34f),
            size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.51f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f, w * 0.03f)
        )
        drawRoundRect(
            color = Color(0xFFD7173F),
            topLeft = Offset(w * 0.14f, h * 0.45f),
            size = androidx.compose.ui.geometry.Size(w * 0.72f, h * 0.07f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f, w * 0.02f)
        )
        drawOval(
            brush = ribbonBrush,
            topLeft = Offset(w * 0.18f, h * 0.15f),
            size = androidx.compose.ui.geometry.Size(w * 0.34f, h * 0.24f)
        )
        drawOval(
            brush = ribbonBrush,
            topLeft = Offset(w * 0.48f, h * 0.15f),
            size = androidx.compose.ui.geometry.Size(w * 0.34f, h * 0.24f)
        )
        drawCircle(color = Color(0xFFFF2D55), radius = w * 0.10f, center = Offset(w * 0.50f, h * 0.28f))
        drawCircle(color = Color.White.copy(alpha = 0.45f), radius = w * 0.035f, center = Offset(w * 0.30f, h * 0.21f))
        drawCircle(color = Color.White.copy(alpha = 0.38f), radius = w * 0.030f, center = Offset(w * 0.67f, h * 0.21f))
    }
}

@Composable
private fun PremiumCrownIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val gold = Color(0xFFFFD447)
        val goldDark = Color(0xFFE5A82E)
        val baseTop = h * 0.58f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.12f, baseTop)
            lineTo(w * 0.22f, h * 0.28f)
            lineTo(w * 0.40f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.20f)
            lineTo(w * 0.60f, h * 0.50f)
            lineTo(w * 0.78f, h * 0.28f)
            lineTo(w * 0.88f, baseTop)
            close()
        }
        drawPath(path, gold)
        drawRoundRect(goldDark, Offset(w * 0.17f, h * 0.58f), androidx.compose.ui.geometry.Size(w * 0.66f, h * 0.18f), androidx.compose.ui.geometry.CornerRadius(w * 0.05f, w * 0.05f))
        drawCircle(Color(0xFF3B82F6), w * 0.035f, Offset(w * 0.50f, h * 0.64f))
        drawCircle(Color(0xFF10B981), w * 0.03f, Offset(w * 0.30f, h * 0.62f))
        drawCircle(Color(0xFFEF4444), w * 0.03f, Offset(w * 0.70f, h * 0.62f))
    }
}

@Composable
private fun AdBannerPlaceholder(modifier: Modifier = Modifier) {
    if (!MonetizationStore.shouldShowBanner()) return
    val adTick = AdMobManager.adTick.intValue
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdMobManager.BANNER_AD_UNIT_ID
                adListener = object : com.google.android.gms.ads.AdListener() {
                    override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                        // Banner should keep trying at its proper placements for free users.
                        postDelayed({
                            if (MonetizationStore.shouldShowBanner()) {
                                loadAd(AdRequest.Builder().build())
                            }
                        }, 12_000L)
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
private fun SpecialRewardUnlockModal(
    genre: String,
    adReady: Boolean,
    adLoading: Boolean,
    onDismiss: () -> Unit,
    onWatchPlaceholder: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0x99081225)).padding(20.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                shape = RoundedCornerShape(32.dp),
                color = DoseviaCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
                shadowElevation = 20.dp
            ) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(categoryIcon(genre), fontSize = 44.sp)
                    Text("Unlock $genre", color = DoseviaText, fontWeight = FontWeight.Black, fontSize = 25.sp, textAlign = TextAlign.Center)
                    Text("Watch a rewarded ad to open this Special collection for 24 hours.", color = DoseviaMuted, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
                    Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFFEAF1FF), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC7D2E4))) {
                        Text("Watch one short ad to open this Special collection for 24 hours.", modifier = Modifier.fillMaxWidth().padding(18.dp), color = DoseviaText, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, fontSize = 15.sp, lineHeight = 22.sp)
                    }
                    Button(
                        onClick = onWatchPlaceholder,
                        enabled = adReady,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary, contentColor = Color.White, disabledContainerColor = Color(0xFFC7D2E4), disabledContentColor = DoseviaMuted)
                    ) {
                        Text(if (adReady) "Watch Ad" else "Loading Ad...", fontWeight = FontWeight.ExtraBold)
                    }
                    TextButton(onClick = onDismiss) { Text("Not now", color = DoseviaMuted, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun DoPalettePremiumModal(
    onDismiss: () -> Unit,
    onUpgradePlaceholder: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xAA081225))
                .padding(horizontal = 16.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            val tablet = maxWidth > 680.dp
            val modalWidth = if (tablet) 620.dp else maxWidth
            val scrollState = rememberScrollState()

            Surface(
                modifier = Modifier
                    .widthIn(max = modalWidth)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * 0.92f),
                shape = RoundedCornerShape(if (tablet) 36.dp else 30.dp),
                color = DoseviaCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(if (tablet) 24.dp else 18.dp),
                    verticalArrangement = Arrangement.spacedBy(if (tablet) 16.dp else 12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PremiumGiftIcon(Modifier.size(if (tablet) 40.dp else 34.dp))
                                Text("DoPalette\nPremium", color = DoseviaText, fontWeight = FontWeight.Black, fontSize = if (tablet) 31.sp else 27.sp, lineHeight = if (tablet) 31.sp else 27.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("One upgrade. More coloring. No ads.", color = DoseviaMuted, fontWeight = FontWeight.SemiBold, fontSize = if (tablet) 15.sp else 13.sp, lineHeight = 20.sp)
                        }
                        TextButton(onClick = onDismiss) { Text("✕", color = DoseviaMuted, fontWeight = FontWeight.Black, fontSize = 24.sp) }
                    }

                    PremiumRainbowBorderPreview(compact = !tablet)

                    PremiumFeatureCard(
                        iconContent = { NoAdsIcon(Modifier.size(34.dp)) },
                        title = "NO ADS",
                        body = "No banner ads. No ads after Done. No ads to open Specials."
                    )
                    PremiumFeatureCard(
                        iconContent = { RainbowBorderMiniPreview(Modifier.size(44.dp)) },
                        title = "RAINBOW BORDER",
                        body = "Your profile gets the special rainbow frame."
                    )
                    PremiumFeatureCard(
                        iconContent = { Text("🔓", fontSize = 28.sp) },
                        title = "SPECIALS FOREVER",
                        body = "Open Dinosaurs, Dragons, Space, and all future Special collections anytime."
                    )
                    PremiumFeatureCard(
                        iconContent = { PremiumCrownIcon(Modifier.size(38.dp)) },
                        title = "PREMIUM ARTIST",
                        body = "Your profile and Community posts show Premium Artist."
                    )

                    Button(onClick = onUpgradePlaceholder, modifier = Modifier.fillMaxWidth().height(if (tablet) 60.dp else 56.dp), shape = RoundedCornerShape(19.dp), colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary, contentColor = Color.White)) {
                        Text("Upgrade to Premium", fontWeight = FontWeight.ExtraBold, fontSize = if (tablet) 18.sp else 16.sp)
                    }
                    Text("Premium is saved to your signed-in account.", color = DoseviaMuted, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun PremiumUnlockedModal(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xAA081225))
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            val tablet = maxWidth > 680.dp
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = if (tablet) 560.dp else maxWidth)
                    .heightIn(max = maxHeight * 0.90f),
                shape = RoundedCornerShape(if (tablet) 36.dp else 30.dp),
                color = DoseviaCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DoseviaBorder),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(if (tablet) 24.dp else 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("🎉", fontSize = if (tablet) 52.sp else 44.sp)
                    Text(
                        "Premium Unlocked!",
                        color = DoseviaText,
                        fontWeight = FontWeight.Black,
                        fontSize = if (tablet) 30.sp else 25.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "You got your Premium gifts.",
                        color = DoseviaMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    AppIconWithRainbowBorder(size = if (tablet) 122.dp else 104.dp)
                    PremiumRewardRow(icon = "🚫", title = "No Ads", body = "No banner ads, no pop-up ads, and no ads needed for Specials.")
                    PremiumRewardRow(icon = "🌈", title = "Rainbow Border", body = "Your profile now has the rainbow frame.")
                    PremiumRewardRow(icon = "🔓", title = "Specials Forever", body = "Open all Special collections anytime.")
                    PremiumRewardRow(icon = "👑", title = "Premium Artist", body = "Your profile and Community posts show your Premium title.")
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DoseviaPinkPrimary, contentColor = Color.White)
                    ) {
                        Text("Awesome!", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumRewardRow(icon: String, title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFEAF0FA),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC7D2E4))
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(Color.White, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) { Text(icon, fontSize = 25.sp) }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = DoseviaText, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(body, color = DoseviaMuted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun PremiumFeatureCard(iconContent: @Composable () -> Unit, title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFEAF0FA),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC7D2E4))
    ) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(54.dp).background(Color.White, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) { iconContent() }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = DoseviaText, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 0.3.sp)
                Text(body, color = DoseviaMuted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun PremiumRainbowBorderPreview(compact: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFFFFF8E8),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE7C94F))
    ) {
        if (compact) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppIconWithRainbowBorder(size = 108.dp)
                Text("RAINBOW BORDER", color = DoseviaText, fontWeight = FontWeight.Black, fontSize = 20.sp, textAlign = TextAlign.Center)
                Text("See how your Premium profile frame will look.", color = DoseviaMuted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
            }
        } else {
            Row(modifier = Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIconWithRainbowBorder(size = 116.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text("RAINBOW BORDER", color = DoseviaText, fontWeight = FontWeight.Black, fontSize = 23.sp)
                    Text("See how your Premium profile frame will look.", color = DoseviaMuted, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun AppIconWithRainbowBorder(size: Dp) {
    ProfileAvatarWithBorder(
        size = size,
        textSize = (size.value * 0.26f).dp,
        borderId = "rainbow_border",
        avatarScale = 0.58f
    )
}

@Composable
private fun RainbowBorderMiniPreview(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        ProfileBorderOverlay("rainbow_border", Modifier.fillMaxSize())
    }
}

@Composable
private fun NoAdsIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.10f
        drawCircle(color = Color(0xFFEF4444), radius = size.minDimension * 0.42f, center = center, style = Stroke(width = strokeWidth))
        drawLine(color = Color(0xFFEF4444), start = Offset(size.width * 0.24f, size.height * 0.76f), end = Offset(size.width * 0.76f, size.height * 0.24f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }
}
