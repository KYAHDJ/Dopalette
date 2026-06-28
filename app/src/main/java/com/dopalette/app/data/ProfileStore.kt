package com.dopalette.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import java.io.ByteArrayOutputStream
import java.io.File

object ProfileStore {
    private const val PREFS_NAME = "dopalette_profile"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_MEMBER_SINCE = "member_since"
    private const val KEY_BADGE_ID = "badge_id"
    private const val KEY_BORDER_ID = "border_id"
    private const val KEY_CLOUD_AVATAR_URL = "cloud_avatar_url"
    private const val KEY_AVATAR_SYNCED_LAST_MODIFIED = "avatar_synced_last_modified"
    private const val KEY_AVATAR_REMOVE_PENDING = "avatar_remove_pending"
    private const val KEY_PROFILE_DIRTY = "profile_dirty"
    private lateinit var context: Context

    val displayName = mutableStateOf("DoPalette Artist")
    val avatarFilePath = mutableStateOf<String?>(null)
    val cloudAvatarUrl = mutableStateOf<String?>(null)
    val avatarVersion = mutableIntStateOf(0)
    val memberSince = mutableStateOf("2026")
    val badgeId = mutableStateOf("starter")
    val borderId = mutableStateOf("default")

    fun initialize(appContext: Context) {
        context = appContext.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        displayName.value = runCatching { prefs.getString(KEY_DISPLAY_NAME, "DoPalette Artist") }.getOrNull()?.sanitizeDisplayName() ?: "DoPalette Artist"
        memberSince.value = runCatching { prefs.getString(KEY_MEMBER_SINCE, "2026") }.getOrNull()?.takeIf { it.isNotBlank() } ?: "2026"
        badgeId.value = runCatching { prefs.getString(KEY_BADGE_ID, "starter") }.getOrNull().safeBadgeId()
        borderId.value = runCatching { prefs.getString(KEY_BORDER_ID, "default") }.getOrNull().safeBorderId()
        cloudAvatarUrl.value = runCatching { prefs.getString(KEY_CLOUD_AVATAR_URL, null) }.getOrNull()?.trim()?.takeIf { it.isNotBlank() }
        val avatar = avatarFile()
        avatarFilePath.value = avatar.takeIf { it.exists() && it.length() > 0L }?.absolutePath
        repairInvalidState()
    }

    fun isProfileDirty(): Boolean {
        if (!::context.isInitialized) return false
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PROFILE_DIRTY, false)
    }

    private fun markDirty() {
        if (::context.isInitialized) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_PROFILE_DIRTY, true)
                .apply()
        }
    }

    fun markProfileSynced() {
        if (::context.isInitialized) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_PROFILE_DIRTY, false)
                .apply()
        }
    }

    fun setDisplayName(name: String): Boolean {
        val clean = name.trim().replace(Regex("\\s+"), " ")
        if (clean.length !in 2..24) return false
        displayName.value = clean
        if (::context.isInitialized) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_DISPLAY_NAME, clean)
                .putBoolean(KEY_PROFILE_DIRTY, true)
                .apply()
        }
        return true
    }

    fun setBadgeId(id: String) {
        val clean = id.safeBadgeId()
        badgeId.value = clean
        if (::context.isInitialized) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BADGE_ID, clean)
                .putBoolean(KEY_PROFILE_DIRTY, true)
                .apply()
        }
    }

    fun setBorderId(id: String) {
        val clean = id.safeBorderId()
        borderId.value = clean
        if (::context.isInitialized) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BORDER_ID, clean)
                .putBoolean(KEY_PROFILE_DIRTY, true)
                .apply()
        }
    }

    fun applySyncedProfile(
        displayName: String,
        badgeId: String,
        borderId: String,
        cloudAvatarUrl: String? = this.cloudAvatarUrl.value
    ) {
        val cleanName = displayName.sanitizeDisplayName()
        this.displayName.value = cleanName
        this.badgeId.value = badgeId.safeBadgeId()
        this.borderId.value = borderId.safeBorderId()
        this.cloudAvatarUrl.value = cloudAvatarUrl?.trim()?.takeIf { it.isNotBlank() }
        if (::context.isInitialized) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_DISPLAY_NAME, this.displayName.value)
                .putString(KEY_BADGE_ID, this.badgeId.value)
                .putString(KEY_BORDER_ID, this.borderId.value)
                .putString(KEY_CLOUD_AVATAR_URL, this.cloudAvatarUrl.value)
                .putBoolean(KEY_PROFILE_DIRTY, false)
                .apply()
        }
    }

    /**
     * Saves a tiny local avatar copy before cloud sync. This intentionally keeps profile pictures small:
     * 320px max side, JPEG quality 82. Typical result is roughly 25 KB - 120 KB, clear enough for profile display while still small.
     */
    fun saveAvatarFromUri(uri: Uri): Boolean {
        if (!::context.isInitialized) return false
        return try {
            val bytes = compressedAvatarBytesFromUri(uri, maxSide = 320, quality = 82) ?: return false
            val target = avatarFile()
            target.parentFile?.mkdirs()
            target.writeBytes(bytes)
            avatarFilePath.value = target.absolutePath
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_AVATAR_SYNCED_LAST_MODIFIED)
                .putBoolean(KEY_PROFILE_DIRTY, true)
                .apply()
            avatarVersion.intValue += 1
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun saveAvatarBytes(bytes: ByteArray, cloudUrl: String? = null, markSynced: Boolean = false): Boolean {
        if (!::context.isInitialized || bytes.isEmpty()) return false
        return try {
            val target = avatarFile()
            target.parentFile?.mkdirs()
            target.writeBytes(bytes)
            avatarFilePath.value = target.absolutePath
            if (!cloudUrl.isNullOrBlank()) cloudAvatarUrl.value = cloudUrl
            val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_CLOUD_AVATAR_URL, cloudAvatarUrl.value)
            if (markSynced) {
                editor.putLong(KEY_AVATAR_SYNCED_LAST_MODIFIED, target.lastModified())
                    .putBoolean(KEY_AVATAR_REMOVE_PENDING, false)
                    .putBoolean(KEY_PROFILE_DIRTY, false)
            }
            editor.apply()
            avatarVersion.intValue += 1
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun localAvatarFile(): File? {
        if (!::context.isInitialized) return null
        return avatarFile().takeIf { it.exists() && it.length() > 0L }
    }

    fun localAvatarNeedsCloudUpload(): Boolean {
        val file = localAvatarFile() ?: return false
        val synced = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_AVATAR_SYNCED_LAST_MODIFIED, -1L)
        return synced != file.lastModified()
    }

    fun isAvatarRemovePending(): Boolean {
        if (!::context.isInitialized) return false
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AVATAR_REMOVE_PENDING, false)
    }

    fun markAvatarRemoveSynced() {
        if (!::context.isInitialized) return
        cloudAvatarUrl.value = null
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_CLOUD_AVATAR_URL)
            .remove(KEY_AVATAR_SYNCED_LAST_MODIFIED)
            .putBoolean(KEY_AVATAR_REMOVE_PENDING, false)
            .putBoolean(KEY_PROFILE_DIRTY, false)
            .apply()
    }

    fun markAvatarSynced(cloudUrl: String) {
        if (!::context.isInitialized) return
        val file = localAvatarFile()
        cloudAvatarUrl.value = cloudUrl
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CLOUD_AVATAR_URL, cloudUrl)
            .putLong(KEY_AVATAR_SYNCED_LAST_MODIFIED, file?.lastModified() ?: System.currentTimeMillis())
            .putBoolean(KEY_AVATAR_REMOVE_PENDING, false)
            .apply()
    }

    fun removeAvatar() {
        if (::context.isInitialized) avatarFile().delete()
        avatarFilePath.value = null
        cloudAvatarUrl.value = null
        avatarVersion.intValue += 1
        if (::context.isInitialized) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_CLOUD_AVATAR_URL)
                .remove(KEY_AVATAR_SYNCED_LAST_MODIFIED)
                .putBoolean(KEY_AVATAR_REMOVE_PENDING, true)
                .putBoolean(KEY_PROFILE_DIRTY, true)
                .apply()
        }
    }

    fun hasLocalProfileData(): Boolean {
        return displayName.value != "DoPalette Artist" ||
            badgeId.value != "starter" ||
            borderId.value != "default" ||
            avatarFilePath.value != null ||
            cloudAvatarUrl.value != null
    }

    fun clearAccountSessionCache() {
        cloudAvatarUrl.value = null
        avatarVersion.intValue += 1
        if (::context.isInitialized) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_CLOUD_AVATAR_URL)
                .remove(KEY_AVATAR_SYNCED_LAST_MODIFIED)
                .putBoolean(KEY_AVATAR_REMOVE_PENDING, false)
                .putBoolean(KEY_PROFILE_DIRTY, false)
                .apply()
        }
    }

    fun reset() {
        displayName.value = "DoPalette Artist"
        memberSince.value = "2026"
        badgeId.value = "starter"
        borderId.value = "default"
        avatarFilePath.value = null
        cloudAvatarUrl.value = null
        avatarVersion.intValue += 1
        if (::context.isInitialized) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
            File(context.filesDir, "dopalette_profile").deleteRecursively()
        }
    }

    fun resetToFreshInstall(appContext: Context) {
        context = appContext.applicationContext
        reset()
        repairInvalidState()
    }

    fun repairInvalidState() {
        if (!::context.isInitialized) return
        displayName.value = displayName.value.sanitizeDisplayName()
        memberSince.value = memberSince.value.takeIf { it.isNotBlank() } ?: "2026"
        badgeId.value = badgeId.value.safeBadgeId()
        borderId.value = borderId.value.safeBorderId()
        val avatar = avatarFile()
        avatarFilePath.value = avatar.takeIf { it.exists() && it.length() > 0L }?.absolutePath
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_DISPLAY_NAME, displayName.value)
            .putString(KEY_MEMBER_SINCE, memberSince.value)
            .putString(KEY_BADGE_ID, badgeId.value)
            .putString(KEY_BORDER_ID, borderId.value)
            .commit()
    }

    private fun avatarFile(): File {
        return File(context.filesDir, "dopalette_profile/avatar.jpg")
    }

    private fun String?.sanitizeDisplayName(): String {
        return this?.trim()?.replace(Regex("\\s+"), " ")?.takeIf { it.length in 2..24 } ?: "DoPalette Artist"
    }

    private fun String?.safeBadgeId(): String {
        val clean = this?.trim()?.lowercase()?.replace(Regex("[^a-z0-9_-]"), "")
        return clean?.takeIf { it.isNotBlank() } ?: "starter"
    }

    private fun String?.safeBorderId(): String {
        val clean = this?.trim()?.lowercase()?.replace(Regex("[^a-z0-9_-]"), "")
        return clean?.takeIf { it.isNotBlank() } ?: "default"
    }

    private fun compressedAvatarBytesFromUri(uri: Uri, maxSide: Int, quality: Int): ByteArray? {
        val original = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
        return compressBitmap(original, maxSide, quality)
    }

    fun compressedAvatarBytes(file: File, maxSide: Int = 128, quality: Int = 58): ByteArray? {
        val original = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        return compressBitmap(original, maxSide, quality)
    }

    private fun compressBitmap(original: Bitmap, maxSide: Int, quality: Int): ByteArray? {
        val scale = minOf(1f, maxSide.toFloat() / maxOf(original.width, original.height).toFloat())
        val resized = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt().coerceAtLeast(1),
                (original.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            original
        }
        return ByteArrayOutputStream().use { output ->
            resized.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(40, 85), output)
            output.toByteArray()
        }
    }
}
