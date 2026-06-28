package com.dopalette.app.data

/**
 * UI-only community/account models prepared for future backend connection.
 * These are intentionally plain Kotlin data classes so Firebase, Supabase,
 * or a custom API can map to them later without redesigning the UI.
 */
data class UserProfile(
    val userId: String = "local_user",
    val displayName: String = "DoPalette Artist",
    val profilePictureUri: String? = null,
    val memberSinceMillis: Long = 0L,
    val totalLikesReceived: Int = 0,
    val sharedArtworkCount: Int = 0,
    val unlockedAchievementCount: Int = 0
)

data class CommunityArtwork(
    val id: String,
    val title: String,
    val category: String,
    val creatorUserId: String,
    val creatorName: String,
    val creatorProfilePictureUri: String? = null,
    val localImagePath: String? = null,
    val remoteImageUrl: String? = null,
    val likes: Int = 0,
    val uploadedAtMillis: Long = 0L,
    val visibility: ArtworkVisibility = ArtworkVisibility.Public
)

enum class ArtworkVisibility {
    Public,
    Private
}

data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val rarity: AchievementRarity
)

data class AchievementProgress(
    val achievementId: String,
    val unlocked: Boolean = false,
    val unlockedAtMillis: Long? = null,
    val progress: Int = 0,
    val target: Int = 1
)

enum class AchievementRarity {
    Common,
    Rare,
    Epic,
    Legendary
}
