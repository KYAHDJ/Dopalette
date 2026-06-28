package com.dopalette.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AchievementStore {
    private const val PREFS_NAME = "dopalette_achievements"
    private const val KEY_XP = "xp_total"
    private const val KEY_LAST_OPEN_DAY = "last_open_day"
    private const val KEY_UNLOCKED_IDS = "unlocked_ids"
    private lateinit var prefs: SharedPreferences

    val updateTick = mutableIntStateOf(0)
    val lastReward = mutableStateOf<AchievementReward?>(null)

    fun initialize(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        repairInvalidState()
        removeDeprecatedAutoStartRewards()
        recordAppOpen()
        evaluateAllProgress(context.applicationContext, showReward = false)
    }

    fun isUnlocked(id: String): Boolean = ::prefs.isInitialized && runCatching { prefs.getBoolean("unlocked_$id", false) }.getOrDefault(false)

    fun unlock(id: String, title: String = id, xp: Int = 0, reward: String = "") {
        unlockInternal(id = id, title = title, xp = xp, reward = reward, showReward = true)
    }

    private fun unlockInternal(id: String, title: String = id, xp: Int = 0, reward: String = "", showReward: Boolean = true): Boolean {
        if (!::prefs.isInitialized || isUnlocked(id)) return false
        val newXp = totalXp() + xp.coerceAtLeast(0)
        val ids = unlockedIds().toMutableSet()
        ids.add(id)
        prefs.edit()
            .putBoolean("unlocked_$id", true)
            .putLong("unlocked_at_$id", System.currentTimeMillis())
            .putStringSet(KEY_UNLOCKED_IDS, ids)
            .putInt(KEY_XP, newXp)
            .apply()
        if (showReward) {
            lastReward.value = AchievementReward(title = title, xp = xp, reward = reward, totalXp = newXp)
        }
        updateTick.intValue += 1
        return true
    }

    fun unlockedAt(id: String): Long? {
        if (!::prefs.isInitialized) return null
        val value = prefs.getLong("unlocked_at_$id", 0L)
        return if (value == 0L) null else value
    }

    fun totalXp(): Int = if (::prefs.isInitialized) runCatching { prefs.getInt(KEY_XP, 0) }.getOrDefault(0).coerceAtLeast(0) else 0

    fun unlockedIds(): Set<String> {
        if (!::prefs.isInitialized) return emptySet()
        val explicit = runCatching { prefs.getStringSet(KEY_UNLOCKED_IDS, emptySet()).orEmpty() }.getOrDefault(emptySet())
            .mapNotNull { it?.trim()?.takeIf { value -> value.isNotBlank() } }
            .toSet()
        if (explicit.isNotEmpty()) return explicit
        return prefs.all.keys
            .filter { key -> key.startsWith("unlocked_") && runCatching { prefs.getBoolean(key, false) }.getOrDefault(false) }
            .map { it.removePrefix("unlocked_") }
            .filter { it.isNotBlank() }
            .toSet()
    }

    fun applySyncedUnlocks(ids: Collection<String>, xp: Int) {
        if (!::prefs.isInitialized) return
        val clean = ids.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        val editor = prefs.edit()
        clean.forEach { id ->
            editor.putBoolean("unlocked_$id", true)
            if (runCatching { prefs.getLong("unlocked_at_$id", 0L) }.getOrDefault(0L) == 0L) {
                editor.putLong("unlocked_at_$id", System.currentTimeMillis())
            }
        }
        editor.putStringSet(KEY_UNLOCKED_IDS, clean)
        editor.putInt(KEY_XP, xp.coerceAtLeast(0))
        editor.apply()
        lastReward.value = null
        updateTick.intValue += 1
    }

    fun hasAnyLocalProgress(): Boolean {
        if (!::prefs.isInitialized) return false
        return totalXp() > 0 || unlockedIds().isNotEmpty() || prefs.all.keys.any { it.startsWith("count_") && runCatching { prefs.getInt(it, 0) }.getOrDefault(0) > 0 }
    }

    fun setTotalXp(xp: Int) {
        if (!::prefs.isInitialized) return
        val safeXp = xp.coerceAtLeast(0)
        if (totalXp() == safeXp) return
        prefs.edit().putInt(KEY_XP, safeXp).apply()
        updateTick.intValue += 1
    }

    fun counter(key: String): Int = if (::prefs.isInitialized) runCatching { prefs.getInt("count_$key", 0) }.getOrDefault(0).coerceAtLeast(0) else 0

    fun addCounter(key: String, amount: Int = 1): Int {
        if (!::prefs.isInitialized) return 0
        val next = (counter(key) + amount).coerceAtLeast(0)
        prefs.edit().putInt("count_$key", next).apply()
        updateTick.intValue += 1
        return next
    }


    private fun cleanIdPart(value: String): String = value.trim().lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun artistLevelForXp(xp: Int): Int {
        var level = 1
        var next = 100
        var step = 150
        val safeXp = xp.coerceAtLeast(0)
        while (safeXp >= next && level < 1000) {
            level += 1
            step = (step + 50).coerceAtMost(2000)
            next += step
        }
        return level
    }

    private fun maybeUnlock(id: String, title: String, xp: Int, reward: String, condition: Boolean, showReward: Boolean = true) {
        if (condition) unlockInternal(id, title, xp, reward, showReward)
    }

    private fun evaluateLevelAchievements(showReward: Boolean = true) {
        val level = artistLevelForXp(totalXp())
        listOf(
            5 to Triple("level_5", "Reach Level 5", 250),
            10 to Triple("level_10", "Reach Level 10", 500),
            25 to Triple("level_25", "Reach Level 25", 1000),
            50 to Triple("level_50", "Reach Level 50", 2000),
            100 to Triple("level_100", "Reach Level 100", 5000)
        ).forEach { (target, info) ->
            maybeUnlock(info.first, info.second, info.third, "Level $target Border", level >= target, showReward)
        }
    }

    fun evaluateAllProgress(context: Context, showReward: Boolean = true) {
        if (!::prefs.isInitialized) return
        val catalog = SelectionArtworkAssets.loadCatalog(context)
        val finishedTitles = catalog.filter { ArtworkStore.hasFinished(it.title) }
        val uniqueFinished = maxOf(counter("unique_finished"), finishedTitles.map { "${it.category}/${it.title}" }.toSet().size)
        if (uniqueFinished > counter("unique_finished")) {
            prefs.edit().putInt("count_unique_finished", uniqueFinished).apply()
        }

        listOf(
            1 to Triple("first_finished", "First Finished Artwork", 100),
            10 to Triple("colored_10", "10 Artworks Colored", 250),
            25 to Triple("colored_25", "25 Artworks Colored", 600),
            50 to Triple("colored_50", "50 Artworks Colored", 1200),
            100 to Triple("colored_100", "100 Artworks Colored", 2500),
            250 to Triple("colored_250", "250 Artworks Colored", 6000)
        ).forEach { (target, info) ->
            maybeUnlock(info.first, info.second, info.third, "Coloring Achievement", uniqueFinished >= target, showReward)
        }

        catalog.groupBy { it.category }.forEach { (category, items) ->
            val finishedInCategory = items.count { ArtworkStore.hasFinished(it.title) }
            evaluateCategoryProgress(category, finishedInCategory, items.size, showReward)
        }

        val freeCategories = setOf("fruits", "vegetables", "animals", "objects", "flowers", "vehicles", "sports")
        val specialCategories = setOf("dinosaurs", "dragons", "space", "desserts", "fantasy", "sea_life", "sea life")
        val completed = catalog.groupBy { it.category }.filter { (_, items) -> items.isNotEmpty() && items.all { ArtworkStore.hasFinished(it.title) } }.keys.map { cleanIdPart(it) }.toSet()
        maybeUnlock("complete_free_categories", "Complete Free Categories", 3000, "Free Master Border", freeCategories.all { it in completed }, showReward)
        maybeUnlock("complete_special_categories", "Complete Special Categories", 3000, "Special Master Border", specialCategories.intersect(completed).size >= 1 && specialCategories.all { it in completed || catalog.none { entry -> cleanIdPart(entry.category) == it } }, showReward)
        val allDone = catalog.isNotEmpty() && catalog.all { ArtworkStore.hasFinished(it.title) }
        maybeUnlock("completionist", "Completionist", 10000, "Completionist Border", allDone, showReward)

        evaluateLevelAchievements(showReward)
    }

    private fun evaluateColoringMilestones(showReward: Boolean = true) {
        val uniqueFinished = counter("unique_finished")
        listOf(
            1 to Triple("first_finished", "First Finished Artwork", 100),
            10 to Triple("colored_10", "10 Artworks Colored", 250),
            25 to Triple("colored_25", "25 Artworks Colored", 600),
            50 to Triple("colored_50", "50 Artworks Colored", 1200),
            100 to Triple("colored_100", "100 Artworks Colored", 2500),
            250 to Triple("colored_250", "250 Artworks Colored", 6000)
        ).forEach { (target, info) ->
            maybeUnlock(info.first, info.second, info.third, "Coloring Achievement", uniqueFinished >= target, showReward)
        }
        evaluateLevelAchievements(showReward)
    }

    private fun evaluateCategoryProgress(category: String, finishedInCategory: Int, totalInCategory: Int, showReward: Boolean = true) {
        if (category.isBlank() || totalInCategory <= 0) return
        val clean = cleanIdPart(category)
        val label = category.removeSuffix("s")
        maybeUnlock("${clean}_first", "$label Apprentice", 75, "$label Badge", finishedInCategory >= 1, showReward)
        maybeUnlock("${clean}_five", "$label Explorer", 180, "$label Explorer Badge", finishedInCategory >= minOf(5, totalInCategory), showReward)
        maybeUnlock("${clean}_master", "$category Master", totalInCategory * 30, "$category Border", finishedInCategory >= totalInCategory, showReward)
        evaluateLevelAchievements(showReward)
    }

    fun recordBrushStroke(artworkTitle: String) {
        addCounter("brush_strokes")
        if (artworkTitle.isNotBlank()) addArtworkTouched(artworkTitle)
        unlock("first_stroke", "First Stroke", 25, "Starter Badge")
        evaluateLevelAchievements()
    }

    fun recordFill(artworkTitle: String) {
        addCounter("bucket_fills")
        if (artworkTitle.isNotBlank()) addArtworkTouched(artworkTitle)
        unlock("first_fill", "First Fill", 50, "Fill Badge")
        evaluateLevelAchievements()
    }

    fun recordFinishedArtwork(artworkTitle: String, category: String = "", categoryFinished: Int = 0, categoryTotal: Int = 0) {
        val newlyFinishedArtwork = addArtworkFinished(artworkTitle, category)
        if (newlyFinishedArtwork) {
            addCounter("finished_total")
        }
        evaluateColoringMilestones()
        if (category.isNotBlank() && categoryFinished > 0 && categoryTotal > 0) {
            evaluateCategoryProgress(category, categoryFinished, categoryTotal)
        }
    }

    fun recordDownload() {
        addCounter("downloads")
        unlock("first_download", "First Download", 75, "Exporter Badge")
        evaluateLevelAchievements()
    }

    fun recordCommunityShare() {
        val total = addCounter("community_shares")
        unlock("first_community_share", "First Community Share", 50, "Community Badge")
        if (total >= 10) unlock("community_ten_posts", "10 Community Shares", 300, "Showcase Badge")
        if (total >= 50) unlock("community_50_posts", "50 Community Shares", 900, "Community Showcase Border")
        evaluateLevelAchievements()
    }

    fun recordCommunityLikeGiven() {
        val total = addCounter("community_likes_given")
        unlock("first_community_like", "First Community Like", 25, "Supporter Badge")
        if (total >= 25) unlock("community_supporter_25", "Community Supporter", 180, "Supporter Frame")
        evaluateLevelAchievements()
    }

    fun recordCommunityStats(publicPosts: Int, totalLikesReceived: Int) {
        if (!::prefs.isInitialized) return
        val existingPosts = counter("community_public_posts_max")
        val existingLikes = counter("community_likes_received_max")
        val safePosts = maxOf(existingPosts, publicPosts.coerceAtLeast(0))
        val safeLikes = maxOf(existingLikes, totalLikesReceived.coerceAtLeast(0))
        prefs.edit()
            .putInt("count_community_public_posts_max", safePosts)
            .putInt("count_community_likes_received_max", safeLikes)
            .apply()
        if (safePosts >= 1) unlock("first_community_share", "First Community Share", 50, "Community Badge")
        if (safePosts >= 10) unlock("community_full_wall", "Full Community Wall", 300, "Showcase Frame")
        if (safeLikes >= 10) unlock("community_10_likes", "10 Community Likes", 120, "Liked Badge")
        if (safeLikes >= 50) unlock("community_50_likes", "50 Community Likes", 350, "Popular Artist Frame")
        if (safeLikes >= 100) unlock("community_100_likes", "Community Star", 700, "Community Star Border")
        evaluateLevelAchievements()
        updateTick.intValue += 1
    }

    fun recordClear() { addCounter("clear_canvas") }
    fun recordUndo() { addCounter("undo") }

    private fun addArtworkTouched(title: String) {
        if (!::prefs.isInitialized || title.isBlank()) return
        val key = "touched_${title.lowercase(Locale.US)}"
        if (!prefs.getBoolean(key, false)) {
            prefs.edit().putBoolean(key, true).apply()
            addCounter("artworks_touched")
        }
    }

    private fun addArtworkFinished(title: String, category: String = ""): Boolean {
        if (!::prefs.isInitialized || title.isBlank()) return false
        val identity = if (category.isBlank()) title else "$category/$title"
        val key = "finished_${cleanIdPart(identity)}"
        val legacyKey = "finished_${title.lowercase(Locale.US)}"
        if (prefs.getBoolean(key, false) || prefs.getBoolean(legacyKey, false)) return false
        prefs.edit().putBoolean(key, true).apply()
        addCounter("unique_finished")
        return true
    }

    private fun recordAppOpen() {
        if (!::prefs.isInitialized) return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val last = prefs.getString(KEY_LAST_OPEN_DAY, "")
        if (last != today) {
            // Opening the app should never grant XP or badges by itself.
            // It only tracks streak progress for later long-term achievements.
            prefs.edit()
                .putString(KEY_LAST_OPEN_DAY, today)
                .putInt("count_open_days", (counter("open_days") + 1).coerceAtLeast(0))
                .apply()
            updateTick.intValue += 1
        }
    }

    private fun removeDeprecatedAutoStartRewards() {
        if (!::prefs.isInitialized) return
        val oldAutoIds = setOf("welcome")
        val existing = unlockedIds()
        val removeIds = existing.intersect(oldAutoIds)
        if (removeIds.isEmpty()) return
        val editor = prefs.edit()
        removeIds.forEach { id ->
            editor.remove("unlocked_$id")
            editor.remove("unlocked_at_$id")
        }
        editor.putStringSet(KEY_UNLOCKED_IDS, existing - removeIds)
        val oldXp = prefs.getInt(KEY_XP, 0)
        // The old welcome achievement granted 10 XP automatically. Remove it once.
        editor.putInt(KEY_XP, (oldXp - 10).coerceAtLeast(0))
        editor.apply()
        lastReward.value = null
        updateTick.intValue += 1
    }

    fun dismissReward() { lastReward.value = null }

    fun resetAll() {
        if (::prefs.isInitialized) {
            prefs.edit().clear().commit()
        }
        lastReward.value = null
        updateTick.intValue += 1
    }

    fun resetToFreshInstall(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .clear()
            .putInt(KEY_XP, 0)
            .putStringSet(KEY_UNLOCKED_IDS, emptySet())
            .commit()
        lastReward.value = null
        updateTick.intValue += 1
    }

    fun repairInvalidState() {
        if (!::prefs.isInitialized) return
        val cleanIds = unlockedIds()
        val cleanXp = totalXp()
        prefs.edit()
            .putInt(KEY_XP, cleanXp)
            .putStringSet(KEY_UNLOCKED_IDS, cleanIds)
            .commit()
        lastReward.value = null
    }
}

data class AchievementReward(
    val title: String,
    val xp: Int,
    val reward: String,
    val totalXp: Int
)
