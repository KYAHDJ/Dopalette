package com.dopalette.app.data

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object CommunityRepository {
    const val ADMIN_EMAIL = "kyaiko.dev@gmail.com"
    const val MAX_PUBLIC_POSTS_PER_USER = 10
    const val MAX_WEEKLY_UPLOADS_PER_USER = 2
    private const val COMMUNITY_COLLECTION = "communityArtworks"
    private const val REPORTS_COLLECTION = "reports"
    private const val USERS_COLLECTION = "users"
    private const val BLOCKED_COLLECTION = "blockedUsers"

    data class Result(val success: Boolean, val message: String)

    data class CommunityPost(
        val id: String = "",
        val ownerId: String = "",
        val ownerName: String = "DoPalette Artist",
        val ownerEmail: String = "",
        val ownerPhotoUrl: String? = null,
        val title: String = "Artwork",
        val category: String = "Fruits",
        val templateId: String = "",
        val templateVersion: Int = 1,
        val recipe: String = "",
        val likesCount: Int = 0,
        val createdAtMillis: Long = 0L,
        val status: String = "active",
        val ownerXp: Int = 0,
        val ownerLevel: Int = 1,
        val ownerBadgeId: String = "starter",
        val ownerBorderId: String = "default"
    )

    data class ReportItem(
        val id: String = "",
        val targetType: String = "artwork",
        val targetId: String = "",
        val targetOwnerId: String = "",
        val reporterId: String = "",
        val reason: String = "Other",
        val details: String = "",
        val status: String = "open",
        val createdAtMillis: Long = 0L,
        val reporterName: String = "Unknown reporter",
        val reporterEmail: String = "",
        val reporterPhotoUrl: String? = null,
        val reportedName: String = "Unknown user",
        val reportedEmail: String = "",
        val reportedPhotoUrl: String? = null,
        val artworkTitle: String = "Artwork",
        val artworkCategory: String = "",
        val artworkRecipe: String = ""
    )


    private const val CACHE_PREFS = "dopalette_community_cache"
    private const val CACHE_NEW_TODAY = "new_today"
    private const val CACHE_FEATURED = "featured"
    private const val CACHE_MY_POSTS_PREFIX = "my_posts_"
    private const val CACHE_LAST_SYNC_MILLIS = "last_sync_millis"
    private const val RECIPE_DIR = "community_recipe_cache"

    fun loadCachedNewToday(context: Context): List<CommunityPost> = loadCachedPosts(context, CACHE_NEW_TODAY)
    fun loadCachedFeatured(context: Context): List<CommunityPost> = loadCachedPosts(context, CACHE_FEATURED)
        .sortedWith(compareByDescending<CommunityPost> { it.likesCount }.thenByDescending { it.createdAtMillis })
    fun loadCachedMyPosts(context: Context): List<CommunityPost> {
        val uid = GoogleAuthController.refreshCurrentAccount(context)?.uid ?: GoogleAuthController.account.value?.uid ?: return emptyList()
        return loadCachedPosts(context, CACHE_MY_POSTS_PREFIX + uid)
    }

    fun loadCachedCommunityHome(context: Context): List<CommunityPost> {
        return mergePosts(loadCachedMyPosts(context) + loadCachedNewToday(context) + loadCachedFeatured(context))
    }

    private fun saveCachedMyPosts(context: Context, ownerId: String, posts: List<CommunityPost>) {
        if (ownerId.isNotBlank()) saveCachedPosts(context, CACHE_MY_POSTS_PREFIX + ownerId, posts)
    }

    private fun mergePosts(posts: List<CommunityPost>): List<CommunityPost> {
        // Strict local cleanup: keep only the first visible copy.
        // Duplicates can arrive with different Firebase ids if the user shared the
        // same finished artwork before the stable-post-id fix. We therefore check
        // the cloud id, a strong owner/template key, and a safe same-name fallback.
        val seenIds = mutableSetOf<String>()
        val seenStrongKeys = mutableSetOf<String>()
        val seenNameKeys = mutableSetOf<String>()
        return posts
            .filter { it.id.isNotBlank() && it.status == "active" }
            .sortedByDescending { it.createdAtMillis }
            .filter { post ->
                val idKey = post.id.trim().lowercase()
                val strongKey = duplicateKeyFor(post)
                val nameKey = duplicateNameKeyFor(post)
                when {
                    idKey in seenIds -> false
                    strongKey in seenStrongKeys -> false
                    nameKey in seenNameKeys -> false
                    else -> {
                        seenIds += idKey
                        seenStrongKeys += strongKey
                        seenNameKeys += nameKey
                        true
                    }
                }
            }
    }

    private fun cleanPart(value: String): String = value.trim().lowercase()

    private fun duplicateKeyFor(post: CommunityPost): String {
        val ownerKey = post.ownerId.ifBlank { post.ownerEmail.ifBlank { post.ownerName } }
        val templateKey = post.templateId.ifBlank { post.title }
        return listOf(
            cleanPart(ownerKey),
            cleanPart(post.title),
            cleanPart(post.category),
            cleanPart(templateKey),
            post.templateVersion.toString()
        ).joinToString("|")
    }

    private fun duplicateNameKeyFor(post: CommunityPost): String {
        // Fallback for older saved posts where one duplicate may be missing ownerId
        // but still has the same shown artist name and same artwork name.
        return listOf(
            cleanPart(post.ownerName.ifBlank { post.ownerEmail.ifBlank { post.ownerId } }),
            cleanPart(post.title),
            cleanPart(post.category),
            cleanPart(post.templateId.ifBlank { post.title })
        ).joinToString("|")
    }

    private fun stableCommunityPostId(ownerId: String, artworkId: String, category: String?, title: String): String {
        val raw = listOf(ownerId, category ?: "Fruits", artworkId.ifBlank { title }).joinToString("_")
        val safe = raw.replace(Regex("[^A-Za-z0-9_-]"), "_").trim('_')
        return if (safe.isBlank()) "post_${System.currentTimeMillis()}" else safe.take(120)
    }

    fun cleanupLocalDuplicatePosts(context: Context) {
        runCatching {
            saveCachedPosts(context, CACHE_NEW_TODAY, loadCachedNewToday(context))
            saveCachedPosts(context, CACHE_FEATURED, loadCachedFeatured(context))
            GoogleAuthController.refreshCurrentAccount(context)?.uid?.let { uid ->
                saveCachedPosts(context, CACHE_MY_POSTS_PREFIX + uid, loadCachedPosts(context, CACHE_MY_POSTS_PREFIX + uid))
            }
        }
    }

    private fun upsertCachedPost(context: Context, post: CommunityPost) {
        if (post.id.isBlank()) return
        cacheRecipeIfPresent(context, post)
        val lightPost = post.copy(recipe = "")
        val newToday = mergePosts(listOf(lightPost) + loadCachedNewToday(context)).take(60)
        saveCachedPosts(context, CACHE_NEW_TODAY, newToday)
        val myPosts = mergePosts(listOf(lightPost) + loadCachedPosts(context, CACHE_MY_POSTS_PREFIX + post.ownerId)).take(60)
        saveCachedMyPosts(context, post.ownerId, myPosts)
    }

    private fun removeCachedPost(context: Context, postId: String) {
        if (postId.isBlank()) return
        saveCachedPosts(context, CACHE_NEW_TODAY, loadCachedNewToday(context).filterNot { it.id == postId })
        saveCachedPosts(context, CACHE_FEATURED, loadCachedFeatured(context).filterNot { it.id == postId })
        GoogleAuthController.refreshCurrentAccount(context)?.uid?.let { uid ->
            saveCachedPosts(context, CACHE_MY_POSTS_PREFIX + uid, loadCachedPosts(context, CACHE_MY_POSTS_PREFIX + uid).filterNot { it.id == postId })
        }
    }

    fun patchCachedOwnerStyle(context: Context) {
        val account = GoogleAuthController.refreshCurrentAccount(context) ?: GoogleAuthController.account.value ?: return
        val xp = AchievementStore.totalXp().coerceAtLeast(0)
        val level = levelForXp(xp)
        fun patch(list: List<CommunityPost>): List<CommunityPost> = list.map { post ->
            if (post.ownerId == account.uid) {
                post.copy(
                    ownerName = ProfileStore.displayName.value.ifBlank { account.displayName.ifBlank { "DoPalette Artist" } },
                    ownerPhotoUrl = ProfileStore.cloudAvatarUrl.value ?: account.photoUrl,
                    ownerXp = xp,
                    ownerLevel = level,
                    ownerBadgeId = ProfileStore.badgeId.value,
                    ownerBorderId = ProfileStore.borderId.value
                )
            } else post
        }
        saveCachedPosts(context, CACHE_NEW_TODAY, patch(loadCachedNewToday(context)))
        saveCachedPosts(context, CACHE_FEATURED, patch(loadCachedFeatured(context)))
        saveCachedMyPosts(context, account.uid, patch(loadCachedPosts(context, CACHE_MY_POSTS_PREFIX + account.uid)))
    }

    suspend fun refreshCachedOwnerProfiles(context: Context) {
        if (!isReady(context)) return
        runCatching {
            val account = GoogleAuthController.refreshCurrentAccount(context) ?: GoogleAuthController.account.value
            saveCachedPosts(context, CACHE_NEW_TODAY, enrichOwnerProfiles(loadCachedNewToday(context)).map { it.copy(recipe = "") })
            saveCachedPosts(context, CACHE_FEATURED, enrichOwnerProfiles(loadCachedFeatured(context)).map { it.copy(recipe = "") })
            account?.uid?.let { uid ->
                saveCachedMyPosts(context, uid, enrichOwnerProfiles(loadCachedPosts(context, CACHE_MY_POSTS_PREFIX + uid)).map { it.copy(recipe = "") })
            }
        }
    }

    private fun saveCachedPosts(context: Context, key: String, posts: List<CommunityPost>) {
        runCatching {
            val arr = JSONArray()
            mergePosts(posts).take(60).forEach { post ->
                // Keep the feed cache lightweight. Large stroke recipes are saved separately
                // so reopening Community does not parse huge JSON on older phones.
                cacheRecipeIfPresent(context, post)
                arr.put(JSONObject().apply {
                    put("id", post.id)
                    put("ownerId", post.ownerId)
                    put("ownerName", post.ownerName)
                    put("ownerEmail", post.ownerEmail)
                    put("ownerPhotoUrl", post.ownerPhotoUrl)
                    put("title", post.title)
                    put("category", post.category)
                    put("templateId", post.templateId)
                    put("templateVersion", post.templateVersion)
                    put("likesCount", post.likesCount)
                    put("createdAtMillis", post.createdAtMillis)
                    put("status", post.status)
                    put("ownerXp", post.ownerXp)
                    put("ownerLevel", post.ownerLevel)
                    put("ownerBadgeId", post.ownerBadgeId)
                    put("ownerBorderId", post.ownerBorderId)
                })
            }
            val newest = posts.maxOfOrNull { it.createdAtMillis } ?: 0L
            context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE).edit()
                .putString(key, arr.toString())
                .putLong(CACHE_LAST_SYNC_MILLIS, maxOf(newest, context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE).getLong(CACHE_LAST_SYNC_MILLIS, 0L)))
                .apply()
        }
    }

    private fun recipeFile(context: Context, postId: String): File = File(File(context.filesDir, RECIPE_DIR), "$postId.recipe")

    private fun cacheRecipeIfPresent(context: Context, post: CommunityPost) {
        if (post.id.isBlank() || post.recipe.isBlank()) return
        runCatching {
            val file = recipeFile(context, post.id)
            file.parentFile?.mkdirs()
            file.writeText(post.recipe)
        }
    }

    private fun loadCachedRecipe(context: Context, postId: String): String {
        if (postId.isBlank()) return ""
        return runCatching { recipeFile(context, postId).takeIf { it.exists() }?.readText().orEmpty() }.getOrDefault("")
    }

    suspend fun loadPostRecipe(context: Context, post: CommunityPost): String {
        if (post.recipe.isNotBlank()) {
            cacheRecipeIfPresent(context, post)
            return post.recipe
        }
        val cached = loadCachedRecipe(context, post.id)
        if (cached.isNotBlank()) return cached
        if (!isReady(context) || post.id.isBlank()) return ""
        return runCatching {
            val snap = db().collection(COMMUNITY_COLLECTION).document(post.id).get().await()
            val recipe = snap.getString("recipe").orEmpty()
            if (recipe.isNotBlank()) cacheRecipeIfPresent(context, post.copy(recipe = recipe))
            recipe
        }.getOrDefault("")
    }

    private fun loadCachedPosts(context: Context, key: String): List<CommunityPost> {
        return runCatching {
            val raw = context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE).getString(key, null) ?: return emptyList()
            val arr = JSONArray(raw)
            List(arr.length()) { index ->
                val obj = arr.getJSONObject(index)
                CommunityPost(
                    id = obj.optString("id"),
                    ownerId = obj.optString("ownerId"),
                    ownerName = obj.optString("ownerName", "DoPalette Artist"),
                    ownerEmail = obj.optString("ownerEmail"),
                    ownerPhotoUrl = obj.optString("ownerPhotoUrl").takeIf { it.isNotBlank() && it != "null" },
                    title = obj.optString("title", "Artwork"),
                    category = obj.optString("category", "Fruits"),
                    templateId = obj.optString("templateId"),
                    templateVersion = obj.optInt("templateVersion", 1),
                    recipe = "",
                    likesCount = obj.optInt("likesCount", 0),
                    createdAtMillis = obj.optLong("createdAtMillis", 0L),
                    status = obj.optString("status", "active"),
                    ownerXp = obj.optInt("ownerXp", 0),
                    ownerLevel = obj.optInt("ownerLevel", 1),
                    ownerBadgeId = obj.optString("ownerBadgeId", "starter"),
                    ownerBorderId = obj.optString("ownerBorderId", "default")
                )
            }.filter { it.status == "active" }
        }.getOrElse { emptyList() }
    }

    fun isAdmin(account: GoogleAuthController.Account?): Boolean =
        account?.email?.equals(ADMIN_EMAIL, ignoreCase = true) == true

    suspend fun loadNewToday(context: Context, limit: Long = 20): List<CommunityPost> {
        val cached = loadCachedNewToday(context)
        if (!isReady(context)) return cached
        return runCatching {
            val blocked = loadBlockedUserIds(context)
            // Always refresh the newest visible posts, not only posts newer than cache.
            // Profile badge/border changes update existing post documents, so a createdAt-only
            // delta query can miss style changes and guests/other users would keep seeing stale borders.
            val live = db().collection(COMMUNITY_COLLECTION)
                .orderBy("createdAtMillis", Query.Direction.DESCENDING)
                .limit(limit * 3)
                .get()
                .await()
                .documents
                .mapNotNull { doc -> doc.toCommunityPost() }
                .filter { it.status == "active" }
                .filterNot { it.ownerId in blocked }
            live.forEach { cacheRecipeIfPresent(context, it) }
            val enrichedLive = enrichOwnerProfiles(live).map { it.copy(recipe = "") }
            val enrichedCached = enrichOwnerProfiles(cached).map { it.copy(recipe = "") }
            val merged = mergePosts(enrichedLive + enrichedCached).take(limit.toInt().coerceAtLeast(24))
            if (merged.isNotEmpty()) saveCachedPosts(context, CACHE_NEW_TODAY, merged)
            if (merged.isNotEmpty()) merged else cached
        }.getOrElse { cached }
    }

    suspend fun loadFeatured(context: Context, limit: Long = 20): List<CommunityPost> {
        val cached = loadCachedFeatured(context)
        if (!isReady(context)) return cached
        return runCatching {
            val blocked = loadBlockedUserIds(context)
            // Trending changes more slowly, so keep it lightweight and cache-first.
            val snap = db().collection(COMMUNITY_COLLECTION)
                .orderBy("likesCount", Query.Direction.DESCENDING)
                .limit(limit * 2)
                .get()
                .await()
            val live = snap.documents.mapNotNull { doc -> doc.toCommunityPost() }
                .filter { it.status == "active" }
                .filterNot { it.ownerId in blocked }
                .take(limit.toInt())
            live.forEach { cacheRecipeIfPresent(context, it) }
            val enriched = enrichOwnerProfiles(live).map { it.copy(recipe = "") }
                .sortedWith(compareByDescending<CommunityPost> { it.likesCount }.thenByDescending { it.createdAtMillis })
            if (enriched.isNotEmpty()) saveCachedPosts(context, CACHE_FEATURED, enriched)
            if (enriched.isNotEmpty()) enriched else cached.sortedWith(compareByDescending<CommunityPost> { it.likesCount }.thenByDescending { it.createdAtMillis })
        }.getOrElse { cached }
    }

    suspend fun loadMyPosts(context: Context): List<CommunityPost> {
        val account = GoogleAuthController.refreshCurrentAccount(context) ?: GoogleAuthController.account.value ?: return loadCachedMyPosts(context)
        if (!isReady(context)) return loadCachedMyPosts(context)
        return runCatching {
            val snap = db().collection(COMMUNITY_COLLECTION)
                .whereEqualTo("ownerId", account.uid)
                .get()
                .await()
            val posts = snap.documents.mapNotNull { doc -> doc.toCommunityPost() }
                .filter { it.status == "active" }
                .sortedByDescending { it.createdAtMillis }
            posts.forEach { cacheRecipeIfPresent(context, it) }
            val enriched = enrichOwnerProfiles(posts).map { it.copy(recipe = "") }
            val cached = loadCachedMyPosts(context)
            if (enriched.isNotEmpty() || cached.isEmpty()) saveCachedMyPosts(context, account.uid, enriched)
            if (enriched.isNotEmpty() || cached.isEmpty()) enriched else cached
        }.getOrElse { loadCachedMyPosts(context) }
    }


    private suspend fun enrichOwnerProfiles(posts: List<CommunityPost>): List<CommunityPost> {
        if (posts.isEmpty()) return posts

        // Online profile style is intentionally synced by ID only.
        // Always refresh the small user profile fields so Community shows the latest
        // equipped badge/border/level even on older posts, while artwork recipes/images stay cached locally.
        val profileMap = mutableMapOf<String, com.google.firebase.firestore.DocumentSnapshot?>()
        posts.map { it.ownerId }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { ownerId ->
                profileMap[ownerId] = runCatching {
                    db().collection(USERS_COLLECTION).document(ownerId).get().await()
                }.getOrNull()
            }

        return posts.map { post ->
            val user = profileMap[post.ownerId]
            if (user == null || !user.exists()) {
                post
            } else {
                val xp = user.getLong("xp")?.toInt()?.coerceAtLeast(0) ?: post.ownerXp
                post.copy(
                    ownerName = user.getString("displayName")?.takeIf { it.isNotBlank() } ?: post.ownerName,
                    ownerEmail = user.getString("email") ?: post.ownerEmail,
                    ownerPhotoUrl = (user.getString("avatarUrl") ?: user.getString("profileImageUrl") ?: user.getString("photoUrl")) ?: post.ownerPhotoUrl,
                    ownerXp = xp,
                    ownerLevel = user.getLong("level")?.toInt() ?: levelForXp(xp),
                    ownerBadgeId = user.getString("badgeId")?.takeIf { it.isNotBlank() } ?: post.ownerBadgeId,
                    ownerBorderId = user.getString("borderId")?.takeIf { it.isNotBlank() } ?: post.ownerBorderId
                )
            }
        }
    }

    suspend fun deletePost(context: Context, postId: String): Result {
        val account = GoogleAuthController.refreshCurrentAccount(context) ?: return Result(false, "Please sign in to delete your Community post.")
        if (!isReady(context)) return Result(false, "Community is getting ready. Please try again in a moment.")
        val ref = db().collection(COMMUNITY_COLLECTION).document(postId)
        val snap = ref.get().await()
        if (!snap.exists()) {
            removeCachedPost(context, postId)
            return Result(true, "Post already removed.")
        }
        val ownerId = snap.getString("ownerId") ?: ""
        if (ownerId != account.uid && !isAdmin(account)) return Result(false, "You can only delete your own Community posts.")

        // Hard delete the post and everything attached to it. This prevents orphaned
        // reports, likes, and Storage images from piling up in Firebase.
        deleteCommunityPostEverywhere(context, postId, snap)
        return Result(true, "Post deleted. You can share another artwork now.")
    }

    suspend fun shareFinishedArtwork(context: Context, artwork: FinishedArtwork, category: String?): Result {
        val account = GoogleAuthController.refreshCurrentAccount(context)
            ?: return Result(false, "Please sign in before sharing to Community.")
        if (!isReady(context)) return Result(false, "Community is getting ready. Please try again in a moment.")

        return runCatching {
            // The Home/Me list can hold lightweight finished metadata with empty strokes.
            // Community must upload the REAL final recipe, so reload the full artwork from disk
            // before building the recipe. Without this, Community shows only the blank base template.
            val fullArtwork = ArtworkStore.getFinishedArtworkById(artwork.id)?.takeIf { it.strokes.isNotEmpty() } ?: artwork
            val safeCategory = category ?: "Fruits"
            val templateId = "$safeCategory/${fullArtwork.title}"
            val stablePostId = stableCommunityPostId(account.uid, fullArtwork.id, safeCategory, fullArtwork.title)
            val stablePostRef = db().collection(COMMUNITY_COLLECTION).document(stablePostId)
            val existingStablePost = stablePostRef.get().await()
            val isUpdatingExistingPost = existingStablePost.exists() &&
                ((existingStablePost.getString("status") ?: "active") == "active")

            // Keep these checks on a simple ownerId-only query so Firestore does not need
            // extra composite indexes. Each user can only have 10 posts, so local filtering
            // is cheap and avoids FAILED_PRECONDITION index errors during upload.
            val userPosts = db().collection(COMMUNITY_COLLECTION)
                .whereEqualTo("ownerId", account.uid)
                .get()
                .await()
            val activePosts = userPosts.documents.filter { (it.getString("status") ?: "active") == "active" }

            // Sharing the same finished artwork again should update its existing community post.
            // It should not consume a new public-post slot or a new weekly upload slot.
            if (!isUpdatingExistingPost) {
                val duplicateLegacyPosts = activePosts.filter { doc ->
                    val docTitle = doc.getString("title").orEmpty()
                    val docCategory = doc.getString("category") ?: "Fruits"
                    val docTemplate = doc.getString("templateId").orEmpty()
                    doc.id != stablePostId &&
                        docTitle.equals(fullArtwork.title, ignoreCase = true) &&
                        docCategory.equals(safeCategory, ignoreCase = true) &&
                        (docTemplate.isBlank() || docTemplate.equals(templateId, ignoreCase = true))
                }
                val nonDuplicatePostCount = activePosts.count { doc -> doc.id !in duplicateLegacyPosts.map { it.id } }
                if (nonDuplicatePostCount >= MAX_PUBLIC_POSTS_PER_USER) {
                    return Result(false, "You already shared 10 artworks. Please delete one old post before sharing a new one.")
                }

                val weekStart = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L
                val weeklyUploads = activePosts
                    .filterNot { doc -> doc.id in duplicateLegacyPosts.map { it.id } }
                    .count { (it.getLong("createdAtMillis") ?: 0L) >= weekStart }
                if (weeklyUploads >= MAX_WEEKLY_UPLOADS_PER_USER) {
                    return Result(false, "You can share 2 artworks each week. Please wait a little before sharing again.")
                }
            }

            val recipe = buildRecipe(fullArtwork)
            if (fullArtwork.strokes.isEmpty() || recipe.isBlank()) {
                return Result(false, "This artwork is not ready yet. Open it, save it, then share again.")
            }
            val now = System.currentTimeMillis()
            val ownerXp = AchievementStore.totalXp()
            val ownerLevel = levelForXp(ownerXp)
            val createdAtMillis = existingStablePost.getLong("createdAtMillis") ?: now
            val likesCount = existingStablePost.getLong("likesCount")?.toInt() ?: 0
            val post = hashMapOf<String, Any?>(
                "ownerId" to account.uid,
                "ownerName" to ProfileStore.displayName.value.ifBlank { account.displayName.ifBlank { "DoPalette Artist" } },
                "ownerEmail" to account.email,
                "ownerPhotoUrl" to (ProfileStore.cloudAvatarUrl.value ?: account.photoUrl),
                "ownerXp" to ownerXp,
                "ownerLevel" to ownerLevel,
                "ownerBadgeId" to ProfileStore.badgeId.value,
                "ownerBorderId" to ProfileStore.borderId.value,
                "title" to fullArtwork.title,
                "category" to safeCategory,
                "templateId" to templateId,
                "templateVersion" to 1,
                "recipe" to recipe,
                "likesCount" to likesCount,
                "createdAt" to (existingStablePost.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
                "createdAtMillis" to createdAtMillis,
                "updatedAt" to FieldValue.serverTimestamp(),
                "updatedAtMillis" to now,
                "status" to "active"
            )

            // Stable document id makes duplicate uploads impossible for the same finished layer.
            stablePostRef.set(post).await()

            // Clean old legacy duplicates that were created with random Firebase ids before this fix.
            // These are fully removed so Community does not leave stale documents/reports behind.
            activePosts
                .filter { doc ->
                    doc.id != stablePostId &&
                        (doc.getString("title") ?: "").equals(fullArtwork.title, ignoreCase = true) &&
                        ((doc.getString("category") ?: "Fruits").equals(safeCategory, ignoreCase = true)) &&
                        ((doc.getString("templateId") ?: templateId).equals(templateId, ignoreCase = true))
                }
                .forEach { duplicateDoc ->
                    runCatching { deleteCommunityPostEverywhere(context, duplicateDoc.id, duplicateDoc) }
                }

            val localPost = CommunityPost(
                id = stablePostId,
                ownerId = account.uid,
                ownerName = ProfileStore.displayName.value.ifBlank { account.displayName.ifBlank { "DoPalette Artist" } },
                ownerEmail = account.email,
                ownerPhotoUrl = ProfileStore.cloudAvatarUrl.value ?: account.photoUrl,
                title = fullArtwork.title,
                category = safeCategory,
                templateId = templateId,
                templateVersion = 1,
                recipe = recipe,
                likesCount = likesCount,
                createdAtMillis = createdAtMillis,
                status = "active",
                ownerXp = ownerXp,
                ownerLevel = ownerLevel,
                ownerBadgeId = ProfileStore.badgeId.value,
                ownerBorderId = ProfileStore.borderId.value
            )
            upsertCachedPost(context, localPost)
            cleanupLocalDuplicatePosts(context)
            if (!isUpdatingExistingPost) AchievementStore.recordCommunityShare()
            Result(true, if (isUpdatingExistingPost) "Community post updated." else "Shared to Community.")
        }.getOrElse { error ->
            Result(false, communityUploadErrorMessage(error))
        }
    }

    suspend fun likePost(context: Context, postId: String): Result {
        val account = GoogleAuthController.refreshCurrentAccount(context) ?: return Result(false, "Please sign in to like artwork.")
        if (!isReady(context)) return Result(false, "Community is getting ready. Please try again in a moment.")
        return runCatching {
            val postRef = db().collection(COMMUNITY_COLLECTION).document(postId)
            val postSnap = postRef.get().await()
            if ((postSnap.getString("ownerId") ?: "") == account.uid) {
                return Result(false, "You cannot like your own Community post.")
            }
            val likeRef = postRef.collection("likes").document(account.uid)
            val existing = likeRef.get().await()
            if (existing.exists()) {
                likeRef.delete().await()
                runCatching { postRef.update("likesCount", FieldValue.increment(-1)).await() }
                Result(true, "Like removed.")
            } else {
                likeRef.set(mapOf("uid" to account.uid, "createdAt" to FieldValue.serverTimestamp())).await()
                runCatching { postRef.update("likesCount", FieldValue.increment(1)).await() }
                AchievementStore.recordCommunityLikeGiven()
                Result(true, "Liked.")
            }
        }.getOrElse { error ->
            Result(false, communityLikeErrorMessage(error))
        }
    }

    suspend fun reportArtwork(context: Context, post: CommunityPost, reason: String, details: String = ""): Result {
        val account = GoogleAuthController.refreshCurrentAccount(context) ?: return Result(false, "Please sign in to report.")
        if (!isReady(context)) return Result(false, "Community is getting ready. Please try again in a moment.")
        val reportId = "artwork_${post.id}_${account.uid}"
        db().collection(REPORTS_COLLECTION).document(reportId).set(
            reportPayload("artwork", post.id, post.ownerId, account, post.copy(recipe = loadPostRecipe(context, post)), reason, details)
        ).await()
        return Result(true, "Report sent. Thank you for helping keep DoPalette safe.")
    }

    suspend fun reportUser(context: Context, post: CommunityPost, reason: String, details: String = ""): Result {
        val account = GoogleAuthController.refreshCurrentAccount(context) ?: return Result(false, "Please sign in to report.")
        if (!isReady(context)) return Result(false, "Community is getting ready. Please try again in a moment.")
        val reportId = "user_${post.ownerId}_${account.uid}"
        db().collection(REPORTS_COLLECTION).document(reportId).set(
            reportPayload("user", post.ownerId, post.ownerId, account, post.copy(recipe = loadPostRecipe(context, post)), reason, details)
        ).await()
        return Result(true, "Report sent. Thank you for helping keep DoPalette safe.")
    }

    private fun reportPayload(
        targetType: String,
        targetId: String,
        targetOwnerId: String,
        reporter: GoogleAuthController.Account,
        post: CommunityPost,
        reason: String,
        details: String
    ): Map<String, Any?> = mapOf(
        "targetType" to targetType,
        "targetId" to targetId,
        "targetOwnerId" to targetOwnerId,
        "reporterId" to reporter.uid,
        "reporterName" to reporter.displayName,
        "reporterEmail" to reporter.email,
        "reporterPhotoUrl" to reporter.photoUrl,
        "reportedId" to post.ownerId,
        "reportedName" to post.ownerName,
        "reportedEmail" to post.ownerEmail,
        "reportedPhotoUrl" to post.ownerPhotoUrl,
        "artworkId" to post.id,
        "artworkTitle" to post.title,
        "artworkCategory" to post.category,
        "artworkOwnerId" to post.ownerId,
        "artworkOwnerName" to post.ownerName,
        "artworkOwnerEmail" to post.ownerEmail,
        "artworkRecipe" to post.recipe,
        "reason" to reason,
        "details" to details,
        "status" to "open",
        "createdAt" to FieldValue.serverTimestamp(),
        "createdAtMillis" to System.currentTimeMillis()
    )

    suspend fun blockUser(context: Context, targetUserId: String): Result {
        val account = GoogleAuthController.refreshCurrentAccount(context) ?: return Result(false, "Please sign in to block users.")
        if (targetUserId == account.uid) return Result(false, "You cannot block yourself.")
        if (!isReady(context)) return Result(false, "Community is getting ready. Please try again in a moment.")
        db().collection(USERS_COLLECTION).document(account.uid)
            .collection(BLOCKED_COLLECTION).document(targetUserId)
            .set(mapOf("blockedUserId" to targetUserId, "createdAt" to FieldValue.serverTimestamp()))
            .await()
        return Result(true, "Artist hidden. Their posts will not show in your Community.")
    }

    suspend fun unblockUser(context: Context, targetUserId: String): Result {
        val account = GoogleAuthController.account.value ?: return Result(false, "Please sign in first.")
        if (!isReady(context)) return Result(false, "Community is getting ready. Please try again in a moment.")
        db().collection(USERS_COLLECTION).document(account.uid)
            .collection(BLOCKED_COLLECTION).document(targetUserId).delete().await()
        return Result(true, "Artist unhidden.")
    }

    suspend fun loadBlockedUserIds(context: Context): Set<String> {
        val account = GoogleAuthController.refreshCurrentAccount(context) ?: return emptySet()
        if (!isReady(context)) return emptySet()
        return runCatching {
            val snap = db().collection(USERS_COLLECTION).document(account.uid)
                .collection(BLOCKED_COLLECTION).get().await()
            snap.documents.map { it.id }.toSet()
        }.getOrElse { emptySet() }
    }

    suspend fun loadOpenReports(context: Context): List<ReportItem> {
        if (!isReady(context) || !isAdmin(GoogleAuthController.refreshCurrentAccount(context))) return emptyList()
        return runCatching {
            val snap = db().collection(REPORTS_COLLECTION)
                .whereEqualTo("status", "open")
                .limit(100)
                .get()
                .await()
            snap.documents.map { doc ->
                ReportItem(
                    id = doc.id,
                    targetType = doc.getString("targetType") ?: "artwork",
                    targetId = doc.getString("targetId") ?: "",
                    targetOwnerId = doc.getString("targetOwnerId") ?: "",
                    reporterId = doc.getString("reporterId") ?: "",
                    reason = doc.getString("reason") ?: "Other",
                    details = doc.getString("details") ?: "",
                    status = doc.getString("status") ?: "open",
                    createdAtMillis = doc.getLong("createdAtMillis") ?: 0L,
                    reporterName = doc.getString("reporterName") ?: "Unknown reporter",
                    reporterEmail = doc.getString("reporterEmail") ?: "",
                    reporterPhotoUrl = doc.getString("reporterPhotoUrl"),
                    reportedName = doc.getString("reportedName") ?: doc.getString("artworkOwnerName") ?: "Unknown user",
                    reportedEmail = doc.getString("reportedEmail") ?: doc.getString("artworkOwnerEmail") ?: "",
                    reportedPhotoUrl = doc.getString("reportedPhotoUrl"),
                    artworkTitle = doc.getString("artworkTitle") ?: "Artwork",
                    artworkCategory = doc.getString("artworkCategory") ?: "",
                    artworkRecipe = doc.getString("artworkRecipe") ?: ""
                )
            }.sortedByDescending { it.createdAtMillis }.take(50)
        }.getOrElse { emptyList() }
    }

    suspend fun dismissReport(context: Context, reportId: String): Result {
        if (!isReady(context) || !isAdmin(GoogleAuthController.refreshCurrentAccount(context))) return Result(false, "Admin only.")
        db().collection(REPORTS_COLLECTION).document(reportId).delete().await()
        return Result(true, "Report deleted.")
    }

    suspend fun hideArtworkFromReport(context: Context, report: ReportItem): Result {
        if (!isReady(context) || !isAdmin(GoogleAuthController.refreshCurrentAccount(context))) return Result(false, "Admin only.")
        if (report.targetType == "artwork" && report.targetId.isNotBlank()) {
            val ref = db().collection(COMMUNITY_COLLECTION).document(report.targetId)
            val snap = ref.get().await()
            if (snap.exists()) deleteCommunityPostEverywhere(context, report.targetId, snap)
        }
        if (report.id.isNotBlank()) {
            runCatching { db().collection(REPORTS_COLLECTION).document(report.id).delete().await() }
        }
        return Result(true, "Artwork and report deleted.")
    }

    private suspend fun deleteCommunityPostEverywhere(context: Context, postId: String, snap: DocumentSnapshot? = null) {
        if (postId.isBlank()) return
        val postSnap = snap ?: runCatching { db().collection(COMMUNITY_COLLECTION).document(postId).get().await() }.getOrNull()
        val ownerId = postSnap?.getString("ownerId").orEmpty()

        deleteStorageForCommunityPost(ownerId, postId, postSnap)
        deleteLikesForPost(postId)
        deleteReportsForPost(postId)
        runCatching { db().collection(COMMUNITY_COLLECTION).document(postId).delete().await() }
        removeCachedPost(context, postId)
    }

    private suspend fun deleteLikesForPost(postId: String) {
        val likes = db().collection(COMMUNITY_COLLECTION).document(postId).collection("likes").get().await()
        likes.documents.forEach { doc -> runCatching { doc.reference.delete().await() } }
    }

    private suspend fun deleteReportsForPost(postId: String) {
        val reports = mutableMapOf<String, com.google.firebase.firestore.DocumentReference>()
        listOf("targetId", "artworkId").forEach { field ->
            runCatching {
                db().collection(REPORTS_COLLECTION).whereEqualTo(field, postId).get().await().documents.forEach { doc ->
                    reports[doc.id] = doc.reference
                }
            }
        }
        reports.values.forEach { ref -> runCatching { ref.delete().await() } }
    }

    private suspend fun deleteStorageForCommunityPost(ownerId: String, postId: String, snap: DocumentSnapshot?) {
        val storage = FirebaseStorage.getInstance().reference
        val explicitPaths = listOf(
            snap?.getString("imagePath"),
            snap?.getString("storagePath"),
            snap?.getString("artworkImagePath"),
            snap?.getString("remoteImagePath")
        ).filterNotNull().filter { it.isNotBlank() && !it.startsWith("http", ignoreCase = true) }

        explicitPaths.forEach { path ->
            runCatching { storage.child(path.trimStart('/')).delete().await() }
        }

        // Current/future shared artwork storage locations. listAll handles folders;
        // direct deletes handle older single-file uploads if they exist.
        val folderCandidates = listOfNotNull(
            ownerId.takeIf { it.isNotBlank() }?.let { "sharedArtworks/$it/$postId" },
            "communityArtworks/$postId"
        )
        folderCandidates.forEach { folder ->
            runCatching {
                val result = storage.child(folder).listAll().await()
                result.items.forEach { item -> runCatching { item.delete().await() } }
                result.prefixes.forEach { prefix ->
                    runCatching {
                        val nested = prefix.listAll().await()
                        nested.items.forEach { item -> runCatching { item.delete().await() } }
                    }
                }
            }
        }

        val fileCandidates = listOfNotNull(
            ownerId.takeIf { it.isNotBlank() }?.let { "sharedArtworks/$it/$postId/artwork.png" },
            ownerId.takeIf { it.isNotBlank() }?.let { "sharedArtworks/$it/$postId/image.png" },
            "communityArtworks/$postId/artwork.png",
            "communityArtworks/$postId/image.png"
        )
        fileCandidates.forEach { path -> runCatching { storage.child(path).delete().await() } }
    }

    private fun communityLikeErrorMessage(error: Throwable): String {
        val raw = error.message ?: error.javaClass.simpleName
        val lower = raw.lowercase()
        return when {
            "permission_denied" in lower || "missing or insufficient permissions" in lower ->
                "Could not like this artwork right now. Please try again later."
            else -> "Could not like this artwork right now. Please try again."
        }
    }

    private fun communityUploadErrorMessage(error: Throwable): String {
        val raw = error.message ?: error.javaClass.simpleName
        val lower = raw.lowercase()
        return when {
            "permission_denied" in lower || "missing or insufficient permissions" in lower ->
                "Could not share right now. Please try again later."
            "failed_precondition" in lower || "index" in lower ->
                "Community is getting ready. Please try again later."
            "document" in lower && ("too large" in lower || "maximum" in lower) ->
                "This artwork is too big to share right now. Try sharing a simpler one."
            else ->
                "Could not share right now. Please try again."
        }
    }

    fun recipeToStrokes(recipe: String): List<StrokeData> {
        if (recipe.isBlank()) return emptyList()
        val strokesPart = recipe.substringAfter("strokes=", missingDelimiterValue = "")
        if (strokesPart.isBlank()) return emptyList()
        return strokesPart.split(";")
            .filter { it.isNotBlank() }
            .mapNotNull { encodedStroke ->
                runCatching {
                    val parts = encodedStroke.split("~", limit = 8)
                    if (parts.size < 8) return@runCatching null
                    val colorValue = parts[0].toULongOrNull() ?: return@runCatching null
                    val width = parts[1].toFloatOrNull() ?: 12f
                    val style = runCatching { BrushStyle.valueOf(parts[2]) }.getOrDefault(BrushStyle.MARKER)
                    val canvasWidth = parts[3].toFloatOrNull() ?: 0f
                    val canvasHeight = parts[4].toFloatOrNull() ?: 0f
                    val seedX = parts[5].toFloatOrNull() ?: -1f
                    val seedY = parts[6].toFloatOrNull() ?: -1f
                    val actions = parts[7].split("|")
                        .filter { it.isNotBlank() }
                        .mapNotNull { rawAction ->
                            val actionParts = rawAction.split(",")
                            if (actionParts.size < 5) return@mapNotNull null
                            val type = runCatching { PathActionType.valueOf(actionParts[0]) }.getOrNull() ?: return@mapNotNull null
                            SerializablePathAction(
                                type = type,
                                x = actionParts[1].toFloatOrNull() ?: 0f,
                                y = actionParts[2].toFloatOrNull() ?: 0f,
                                x2 = actionParts[3].toFloatOrNull() ?: 0f,
                                y2 = actionParts[4].toFloatOrNull() ?: 0f
                            )
                        }
                        .toMutableList()
                    val path = Path()
                    actions.forEach { action ->
                        when (action.type) {
                            PathActionType.MOVE_TO -> path.moveTo(action.x, action.y)
                            PathActionType.LINE_TO -> path.lineTo(action.x, action.y)
                            PathActionType.QUAD_TO -> path.quadraticBezierTo(action.x, action.y, action.x2, action.y2)
                        }
                    }
                    StrokeData(
                        path = path,
                        color = Color(colorValue),
                        width = width,
                        style = style,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        regionSeedX = seedX,
                        regionSeedY = seedY,
                        serializableActions = actions
                    )
                }.getOrNull()
            }
    }

    private fun buildRecipe(artwork: FinishedArtwork): String {
        val strokes = artwork.strokes.filter { it.serializableActions.isNotEmpty() }.joinToString(separator = ";") { stroke ->
            val points = stroke.serializableActions.joinToString(separator = "|") { action ->
                "${action.type.name},${action.x},${action.y},${action.x2},${action.y2}"
            }
            listOf(stroke.color.value.toString(), stroke.width.toString(), stroke.style.name, stroke.canvasWidth.toString(), stroke.canvasHeight.toString(), stroke.regionSeedX.toString(), stroke.regionSeedY.toString(), points).joinToString(separator = "~")
        }
        return if (strokes.isBlank()) "" else "v=1&title=${artwork.title}&strokes=$strokes"
    }

    private fun levelForXp(xp: Int): Int {
        val thresholds = listOf(0, 100, 250, 500, 900, 1400, 2100, 3000, 4200, 5600)
        return thresholds.indexOfLast { xp >= it }.coerceAtLeast(0) + 1
    }

    private fun isReady(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()
    private fun db(): FirebaseFirestore = FirebaseFirestore.getInstance()

    private fun com.google.firebase.firestore.DocumentSnapshot.toCommunityPost(): CommunityPost? {
        return CommunityPost(
            id = id,
            ownerId = getString("ownerId") ?: return null,
            ownerName = getString("ownerName") ?: "DoPalette Artist",
            ownerEmail = getString("ownerEmail") ?: "",
            ownerPhotoUrl = getString("ownerPhotoUrl"),
            title = getString("title") ?: "Artwork",
            category = getString("category") ?: "Fruits",
            templateId = getString("templateId") ?: "",
            templateVersion = getLong("templateVersion")?.toInt() ?: 1,
            recipe = getString("recipe") ?: "",
            likesCount = getLong("likesCount")?.toInt() ?: 0,
            createdAtMillis = getLong("createdAtMillis") ?: (getTimestamp("createdAt") ?: Timestamp.now()).toDate().time,
            status = getString("status") ?: "active",
            ownerXp = getLong("ownerXp")?.toInt() ?: 0,
            ownerLevel = getLong("ownerLevel")?.toInt() ?: 1,
            ownerBadgeId = getString("ownerBadgeId") ?: "starter",
            ownerBorderId = getString("ownerBorderId") ?: "default"
        )
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }
}
