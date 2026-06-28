package com.dopalette.app.data

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ListResult
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

object AccountProfileSync {
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

    private fun safeEquippedBadge(id: String, xp: Int, achievementIds: Set<String>): String {
        val clean = id.ifBlank { "starter" }
        if (clean == "premium_artist" && MonetizationStore.isPremiumUser.value) return clean
        if (clean == "starter" || clean == "beginner") return clean
        val level = levelForXp(xp)
        if (clean in setOf("explorer", "pro", "champion", "master", "legend")) {
            return if (level >= levelForBadgeId(clean)) clean else "starter"
        }
        val required = achievementIdForBadge(clean) ?: return "starter"
        return if (required in achievementIds) clean else "starter"
    }

    private fun safeEquippedBorder(id: String, achievementIds: Set<String>): String {
        val clean = id.ifBlank { "default" }
        if (clean == "rainbow_border" && MonetizationStore.isPremiumUser.value) return clean
        if (clean == "default") return clean
        val required = achievementIdForBorder(clean) ?: return "default"
        return if (required in achievementIds) clean else "default"
    }

    data class SyncResult(
        val success: Boolean,
        val message: String
    )

    data class CloudProfile(
        val uid: String,
        val displayName: String,
        val email: String,
        val photoUrl: String?,
        val avatarUrl: String?,
        val xp: Int,
        val level: Int,
        val badgeId: String,
        val borderId: String
    )

    suspend fun ensureUserProfile(account: GoogleAuthController.Account): SyncResult {
        if (AppResetManager.isResetting.value) return SyncResult(false, "App reset is in progress.")
        return try {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(account.uid)
            val snapshot = userRef.get().await()

            if (snapshot.exists()) {
                userRef.set(
                    mapOf(
                        "uid" to account.uid,
                        "email" to account.email,
                        "googleDisplayName" to account.displayName,
                        "googlePhotoUrl" to account.photoUrl,
                        "provider" to "google",
                        "profileVersion" to 10,
                        "lastLoginAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
                SyncResult(true, "Account profile loaded.")
            } else {
                userRef.set(
                    mapOf(
                        "uid" to account.uid,
                        "displayName" to account.displayName.ifBlank { "DoPalette Artist" },
                        "email" to account.email,
                        "googleDisplayName" to account.displayName,
                        "googlePhotoUrl" to account.photoUrl,
                        "photoUrl" to account.photoUrl,
                        "avatarUrl" to null,
                        "profileImageUrl" to null,
                        "avatarPath" to null,
                        "xp" to 0,
                        "level" to 1,
                        "badgeId" to "starter",
                        "borderId" to "default",
                        "achievementIds" to emptyList<String>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "lastLoginAt" to FieldValue.serverTimestamp(),
                        "provider" to "google",
                        "profileVersion" to 10
                    ),
                    SetOptions.merge()
                ).await()
                SyncResult(true, "Account profile created.")
            }
        } catch (error: Throwable) {
            SyncResult(false, friendlyProfileError(error))
        }
    }

    suspend fun syncLightweightProfile(account: GoogleAuthController.Account): SyncResult {
        if (AppResetManager.isResetting.value) return SyncResult(false, "App reset is in progress.")
        return try {
            val db = FirebaseFirestore.getInstance()
            val storage = FirebaseStorage.getInstance()
            val userRef = db.collection("users").document(account.uid)
            val snapshot = userRef.get().await()

            val localName = ProfileStore.displayName.value.trim().ifBlank { "DoPalette Artist" }
            val localXp = AchievementStore.totalXp().coerceAtLeast(0)
            val localBadgeId = ProfileStore.badgeId.value.ifBlank { "starter" }
            val localBorderId = ProfileStore.borderId.value.ifBlank { "default" }
            val localAvatarFile = ProfileStore.localAvatarFile()
            val localAchievementIds = AchievementStore.unlockedIds()
            val localProfileDirty = ProfileStore.isProfileDirty()
            val localAvatarNeedsUpload = localAvatarFile != null && ProfileStore.localAvatarNeedsCloudUpload()
            val localAvatarRemovePending = ProfileStore.isAvatarRemovePending()

            val cloudName = snapshot.getString("displayName")?.trim().orEmpty()
            val cloudXp = snapshot.getLong("xp")?.toInt()?.coerceAtLeast(0) ?: 0
            val cloudBadgeId = snapshot.getString("badgeId")?.trim().orEmpty()
            val cloudBorderId = snapshot.getString("borderId")?.trim().orEmpty()
            var cloudAvatarUrl = (snapshot.getString("avatarUrl") ?: snapshot.getString("profileImageUrl"))
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            var cloudAvatarPath = snapshot.getString("avatarPath")?.trim()?.takeIf { it.isNotBlank() }
            val cloudAchievementIds = (snapshot.get("achievementIds") as? List<*>)
                ?.mapNotNull { it?.toString()?.trim()?.takeIf { value -> value.isNotBlank() } }
                ?.toSet()
                .orEmpty()
            val cloudPremium = snapshot.getBoolean("isPremium") == true ||
                snapshot.getBoolean("adsRemoved") == true ||
                snapshot.getBoolean("specialsForever") == true
            if (cloudPremium && !MonetizationStore.isPremiumUser.value) {
                MonetizationStore.setPremiumLocal(true, applyRewards = true)
            }

            val isExistingCloudAccount = snapshot.exists() && (
                cloudName.isNotBlank() || cloudXp > 0 || cloudBadgeId.isNotBlank() ||
                    cloudBorderId.isNotBlank() || !cloudAvatarUrl.isNullOrBlank() || cloudAchievementIds.isNotEmpty()
                )

            val pushLocalProfile = localProfileDirty || localAvatarRemovePending || !isExistingCloudAccount
            val localHasNewProgress = localXp > cloudXp || localAchievementIds.any { it !in cloudAchievementIds }

            val finalName = if (pushLocalProfile) {
                localName.takeIf { it != "DoPalette Artist" } ?: account.displayName.ifBlank { "DoPalette Artist" }
            } else {
                cloudName.ifBlank { account.displayName.ifBlank { "DoPalette Artist" } }
            }.take(24)

            val finalXp = max(localXp, cloudXp)
            val mergedAchievementIds = if (localHasNewProgress || pushLocalProfile) {
                cloudAchievementIds + localAchievementIds
            } else {
                cloudAchievementIds
            }
            val premiumActive = MonetizationStore.isPremiumUser.value || cloudPremium
            val requestedBadgeId = if (premiumActive) "premium_artist" else if (pushLocalProfile) localBadgeId else cloudBadgeId.ifBlank { "starter" }
            val requestedBorderId = if (premiumActive) "rainbow_border" else if (pushLocalProfile) localBorderId else cloudBorderId.ifBlank { "default" }
            val finalBadgeId = safeEquippedBadge(requestedBadgeId, finalXp, mergedAchievementIds)
            val finalBorderId = safeEquippedBorder(requestedBorderId, mergedAchievementIds)
            val finalAchievementIds = mergedAchievementIds
            val finalLevel = levelForXp(finalXp)

            if (isExistingCloudAccount && !pushLocalProfile) {
                if (!cloudAvatarUrl.isNullOrBlank()) {
                    restoreProfileAvatarFromUrl(cloudAvatarUrl!!, force = ProfileStore.cloudAvatarUrl.value != cloudAvatarUrl)
                } else if (ProfileStore.cloudAvatarUrl.value != null) {
                    ProfileStore.removeAvatar()
                    ProfileStore.markAvatarRemoveSynced()
                }
            } else {
                if (localAvatarRemovePending) {
                    cloudAvatarPath?.let { runCatching { storage.reference.child(it).delete().await() } }
                    cloudAvatarUrl = null
                    cloudAvatarPath = null
                    ProfileStore.markAvatarRemoveSynced()
                }
            }

            if (localAvatarNeedsUpload && !localAvatarRemovePending) {
                val uploaded = localAvatarFile?.let { file ->
                    uploadAvatarFileInternal(storage, account.uid, file.absolutePath, cloudAvatarPath)
                }
                if (uploaded != null) {
                    cloudAvatarUrl = uploaded.first
                    cloudAvatarPath = uploaded.second
                    ProfileStore.markAvatarSynced(uploaded.first)
                }
            }

            val finalAvatarUrl = cloudAvatarUrl
            val finalPhotoUrl = finalAvatarUrl ?: account.photoUrl
            val data = linkedMapOf<String, Any?>(
                "uid" to account.uid,
                "displayName" to finalName,
                "email" to account.email,
                "googleDisplayName" to account.displayName,
                "googlePhotoUrl" to account.photoUrl,
                "photoUrl" to finalPhotoUrl,
                "avatarUrl" to finalAvatarUrl,
                "profileImageUrl" to finalAvatarUrl,
                "avatarPath" to cloudAvatarPath,
                "xp" to finalXp,
                "level" to finalLevel,
                "badgeId" to finalBadgeId,
                "borderId" to finalBorderId,
                "achievementIds" to finalAchievementIds.toList().sorted(),
                "isPremium" to MonetizationStore.isPremiumUser.value,
                "premiumArtist" to MonetizationStore.isPremiumUser.value,
                "adsRemoved" to MonetizationStore.isPremiumUser.value,
                "specialsForever" to MonetizationStore.isPremiumUser.value,
                "premiumBorderId" to if (MonetizationStore.isPremiumUser.value) "rainbow_border" else null,
                "provider" to "google",
                "profileVersion" to 10,
                "updatedAt" to FieldValue.serverTimestamp(),
                "lastLoginAt" to FieldValue.serverTimestamp()
            )
            if (!snapshot.exists()) data["createdAt"] = FieldValue.serverTimestamp()
            userRef.set(data, SetOptions.merge()).await()

            ProfileStore.applySyncedProfile(
                displayName = finalName,
                badgeId = finalBadgeId,
                borderId = finalBorderId,
                cloudAvatarUrl = finalAvatarUrl
            )
            updateCommunityOwnerStyle(db, account, finalBadgeId, finalBorderId, finalXp, finalLevel)
            ProfileStore.markProfileSynced()
            AchievementStore.applySyncedUnlocks(finalAchievementIds, finalXp)

            SyncResult(true, when {
                !isExistingCloudAccount -> "New account saved from this device."
                pushLocalProfile || localHasNewProgress -> "Profile changes saved to account."
                else -> "Profile loaded from account."
            })
        } catch (error: Throwable) {
            SyncResult(false, friendlyProfileError(error))
        }
    }

    private suspend fun updateCommunityOwnerStyle(db: FirebaseFirestore, account: GoogleAuthController.Account, badgeId: String, borderId: String, xp: Int, level: Int) {
        val styleFields = mapOf<String, Any?>(
            "ownerName" to ProfileStore.displayName.value.trim().ifBlank { account.displayName.ifBlank { "DoPalette Artist" } },
            "ownerPhotoUrl" to (ProfileStore.cloudAvatarUrl.value ?: account.photoUrl),
            "ownerXp" to xp,
            "ownerLevel" to level,
            "ownerBadgeId" to badgeId,
            "ownerBorderId" to borderId,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        listOf("communityPosts", "communityArtworks").forEach { collection ->
            val snapshot = db.collection(collection).whereEqualTo("ownerId", account.uid).get().await()
            snapshot.documents.forEach { doc ->
                doc.reference.set(styleFields, SetOptions.merge()).await()
            }
        }
    }

    suspend fun syncProfileStyleAndProgress(account: GoogleAuthController.Account): SyncResult {
        if (AppResetManager.isResetting.value) return SyncResult(false, "App reset is in progress.")
        return try {
            val xp = AchievementStore.totalXp().coerceAtLeast(0)
            val level = levelForXp(xp)
            val achievementIds = AchievementStore.unlockedIds()
            val badgeId = safeEquippedBadge(ProfileStore.badgeId.value.ifBlank { "starter" }, xp, achievementIds)
            val borderId = safeEquippedBorder(ProfileStore.borderId.value.ifBlank { "default" }, achievementIds)
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(account.uid).set(
                mapOf(
                    "uid" to account.uid,
                    "displayName" to ProfileStore.displayName.value.trim().ifBlank { account.displayName.ifBlank { "DoPalette Artist" } },
                    "email" to account.email,
                    "googleDisplayName" to account.displayName,
                    "googlePhotoUrl" to account.photoUrl,
                    "photoUrl" to (ProfileStore.cloudAvatarUrl.value ?: account.photoUrl),
                    "avatarUrl" to ProfileStore.cloudAvatarUrl.value,
                    "profileImageUrl" to ProfileStore.cloudAvatarUrl.value,
                    "xp" to xp,
                    "level" to level,
                    "badgeId" to badgeId,
                    "borderId" to borderId,
                    "achievementIds" to achievementIds.toList().sorted(),
                    "provider" to "google",
                    "profileVersion" to 10,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastLoginAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            updateCommunityOwnerStyle(db, account, badgeId, borderId, xp, level)
            ProfileStore.applySyncedProfile(
                displayName = ProfileStore.displayName.value,
                badgeId = badgeId,
                borderId = borderId,
                cloudAvatarUrl = ProfileStore.cloudAvatarUrl.value
            )
            ProfileStore.markProfileSynced()
            SyncResult(true, "Profile badge, border, XP, and level synced online.")
        } catch (error: Throwable) {
            SyncResult(false, friendlyProfileError(error))
        }
    }

    suspend fun saveDisplayName(account: GoogleAuthController.Account, name: String): SyncResult {
        if (AppResetManager.isResetting.value) return SyncResult(false, "App reset is in progress.")
        return try {
            val clean = name.trim().replace(Regex("\\s+"), " ").takeIf { it.length in 2..24 }
                ?: return SyncResult(false, "Name must be 2-24 characters.")
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(account.uid).set(
                mapOf(
                    "uid" to account.uid,
                    "displayName" to clean,
                    "email" to account.email,
                    "googleDisplayName" to account.displayName,
                    "googlePhotoUrl" to account.photoUrl,
                    "provider" to "google",
                    "profileVersion" to 10,
                    "lastLoginAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            ProfileStore.applySyncedProfile(
                displayName = clean,
                badgeId = ProfileStore.badgeId.value,
                borderId = ProfileStore.borderId.value,
                cloudAvatarUrl = ProfileStore.cloudAvatarUrl.value
            )
            updateCommunityOwnerStyle(db, account, ProfileStore.badgeId.value, ProfileStore.borderId.value, AchievementStore.totalXp().coerceAtLeast(0), levelForXp(AchievementStore.totalXp().coerceAtLeast(0)))
            ProfileStore.markProfileSynced()
            SyncResult(true, "Profile name saved to account.")
        } catch (error: Throwable) {
            SyncResult(false, friendlyProfileError(error))
        }
    }

    suspend fun uploadCurrentAvatar(account: GoogleAuthController.Account): SyncResult {
        if (AppResetManager.isResetting.value) return SyncResult(false, "App reset is in progress.")
        return try {
            val file = ProfileStore.localAvatarFile() ?: return SyncResult(false, "No profile image selected.")
            val storage = FirebaseStorage.getInstance()
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(account.uid)
            val snapshot = userRef.get().await()
            val oldPath = snapshot.getString("avatarPath")?.trim()?.takeIf { it.isNotBlank() }
            val uploaded = uploadAvatarFileInternal(storage, account.uid, file.absolutePath, oldPath)
                ?: return SyncResult(false, "Could not upload profile image.")
            val url = uploaded.first
            val avatarPath = uploaded.second
            userRef.set(
                mapOf(
                    "uid" to account.uid,
                    "displayName" to ProfileStore.displayName.value.trim().ifBlank { account.displayName.ifBlank { "DoPalette Artist" } },
                    "email" to account.email,
                    "avatarUrl" to url,
                    "profileImageUrl" to url,
                    "avatarPath" to avatarPath,
                    "photoUrl" to url,
                    "googleDisplayName" to account.displayName,
                    "googlePhotoUrl" to account.photoUrl,
                    "provider" to "google",
                    "profileVersion" to 10,
                    "lastLoginAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            ProfileStore.markAvatarSynced(url)
            ProfileStore.applySyncedProfile(
                displayName = ProfileStore.displayName.value,
                badgeId = ProfileStore.badgeId.value,
                borderId = ProfileStore.borderId.value,
                cloudAvatarUrl = url
            )
            updateCommunityOwnerStyle(db, account, ProfileStore.badgeId.value, ProfileStore.borderId.value, AchievementStore.totalXp().coerceAtLeast(0), levelForXp(AchievementStore.totalXp().coerceAtLeast(0)))
            ProfileStore.markProfileSynced()
            SyncResult(true, "Profile picture saved to account.")
        } catch (error: Throwable) {
            SyncResult(false, friendlyProfileError(error))
        }
    }

    suspend fun removeCloudAvatar(account: GoogleAuthController.Account): SyncResult {
        if (AppResetManager.isResetting.value) return SyncResult(false, "App reset is in progress.")
        return try {
            val db = FirebaseFirestore.getInstance()
            val storage = FirebaseStorage.getInstance()
            val snapshot = db.collection("users").document(account.uid).get().await()
            snapshot.getString("avatarPath")?.trim()?.takeIf { it.isNotBlank() }?.let {
                runCatching { storage.reference.child(it).delete().await() }
            }
            db.collection("users").document(account.uid).set(
                mapOf(
                    "avatarUrl" to null,
                    "profileImageUrl" to null,
                    "avatarPath" to null,
                    "photoUrl" to account.photoUrl,
                    "profileVersion" to 10,
                    "lastLoginAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            ProfileStore.markAvatarRemoveSynced()
            SyncResult(true, "Profile picture removed.")
        } catch (error: Throwable) {
            SyncResult(false, friendlyProfileError(error))
        }
    }

    suspend fun deleteSignedInAccountData(account: GoogleAuthController.Account): SyncResult {
        return try {
            val db = FirebaseFirestore.getInstance()
            val storage = FirebaseStorage.getInstance()
            val uid = account.uid.trim()
            if (uid.isBlank()) return SyncResult(false, "Missing account UID.")

            // Best-effort cleanup for related Storage/Firestore content first.
            // The primary users/{uid} document delete below is NOT best-effort; it must succeed.
            deleteStorageFolder(storage, "profileImages/$uid")
            deleteStorageFolder(storage, "sharedArtworks/$uid")
            deleteMatchingDocs(db, "communityPosts", "ownerId", uid)
            deleteMatchingDocs(db, "likes", "userId", uid)
            deleteMatchingDocs(db, "blocks", "ownerId", uid)
            deleteMatchingDocs(db, "reports", "reporterId", uid)

            val userRef = db.collection("users").document(uid)
            deleteUserSubcollection(userRef, "artworkDraftsV2")
            deleteUserSubcollection(userRef, "artworkFinishedV2")

            
            userRef.delete().await()

            // Verify Firestore really removed it before FirebaseAuth is deleted.
            // This prevents orphaned users/{oldUid} records from being left behind silently.
            val stillExists = userRef.get().await().exists()
            if (stillExists) {
                return SyncResult(false, "Firestore user document was not removed. Check Firestore rules for users/$uid delete permission.")
            }

            SyncResult(true, "Account data deleted.")
        } catch (error: Throwable) {
            SyncResult(false, friendlyProfileError(error))
        }
    }

    private suspend fun uploadAvatarFileInternal(
        storage: FirebaseStorage,
        uid: String,
        localFilePath: String,
        oldPath: String?
    ): Pair<String, String>? {
        val file = java.io.File(localFilePath)
        val bytes = ProfileStore.compressedAvatarBytes(file, maxSide = 320, quality = 82) ?: return null
        val avatarPath = "profileImages/$uid/avatar_current.jpg"

        // Keep exactly ONE profile picture per account.
        // Delete any older timestamp-named avatar files before uploading the new current avatar.
        deleteStorageFolder(storage, "profileImages/$uid")

        val avatarRef = storage.reference.child(avatarPath)
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .setCustomMetadata("ownerId", uid)
            .build()
        avatarRef.putBytes(bytes, metadata).awaitUpload()
        val url = avatarRef.downloadUrl.await().toString()
        return url to avatarPath
    }

    private suspend fun deleteMatchingDocs(
        db: FirebaseFirestore,
        collection: String,
        field: String,
        value: String
    ) {
        runCatching {
            val snapshot = db.collection(collection).whereEqualTo(field, value).get().await()
            snapshot.documents.forEach { doc -> runCatching { doc.reference.delete().await() } }
        }
    }

    private suspend fun deleteUserSubcollection(
        userRef: com.google.firebase.firestore.DocumentReference,
        collection: String
    ) {
        runCatching {
            val snapshot = userRef.collection(collection).get().await()
            snapshot.documents.forEach { doc -> runCatching { doc.reference.delete().await() } }
        }
    }

    private suspend fun deleteStorageFolder(storage: FirebaseStorage, folderPath: String) {
        runCatching {
            val result = storage.reference.child(folderPath).listAll().await()
            result.items.forEach { item -> runCatching { item.delete().await() } }
            result.prefixes.forEach { prefix -> deleteStorageFolder(storage, prefix.path) }
        }
    }

    private fun restoreProfileAvatarFromUrl(url: String, force: Boolean = false) {
        if (!force && ProfileStore.localAvatarFile() != null && ProfileStore.cloudAvatarUrl.value == url) return
        try {
            val bytes = URL(url).openStream().use { it.readBytes() }
            if (bytes.isNotEmpty()) ProfileStore.saveAvatarBytes(bytes, url, markSynced = true)
        } catch (_: Throwable) {
            // Keep profile usable even when image restore fails.
        }
    }

    private fun levelForXp(xp: Int): Int {
        val thresholds = listOf(0, 100, 250, 500, 900, 1400, 2100, 3000, 4200, 5600, 7200, 9000, 11000, 13200, 15600, 18200, 21000, 24000, 27200, 30600, 34200, 38000, 42000, 46200, 50600)
        return thresholds.indexOfLast { xp >= it }.coerceAtLeast(0) + 1
    }

    private fun friendlyProfileError(error: Throwable): String {
        val raw = error.message.orEmpty()
        return when {
            raw.contains("offline", ignoreCase = true) -> "Your profile could not be backed up right now. Your progress is safe on this device."
            raw.contains("permission", ignoreCase = true) -> "Your profile could not be backed up right now. Please try again later."
            raw.contains("storage", ignoreCase = true) -> "Your picture could not be backed up right now. Please try again later."
            raw.isNotBlank() -> raw.take(160)
            else -> "Profile backup is not ready yet."
        }
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { value -> cont.resume(value) }
        addOnFailureListener { error -> cont.resumeWithException(error) }
        addOnCanceledListener { cont.cancel() }
    }

    private suspend fun UploadTask.awaitUpload(): UploadTask.TaskSnapshot = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { value -> cont.resume(value) }
        addOnFailureListener { error -> cont.resumeWithException(error) }
        addOnCanceledListener { cont.cancel() }
    }
}
