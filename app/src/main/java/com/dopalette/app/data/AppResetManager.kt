package com.dopalette.app.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import com.dopalette.app.ui.home.HomePreviewCache
import java.io.File

/**
 * Single owner for destructive local resets.
 *
 * Delete Account must not wipe stores from random screens while Compose/editor/preview jobs are still alive.
 * This manager puts the app in reset mode, invalidates queued artwork writes, clears volatile bitmap caches,
 * recreates the same empty folders a fresh install would have, then restarts the task so no stale references survive.
 */
object AppResetManager {
    val isResetting = mutableStateOf(false)

    fun beginReset() {
        isResetting.value = true
    }

    fun endResetWithoutRestart() {
        isResetting.value = false
    }

    fun freshInstallLocalReset(context: Context) {
        val appContext = context.applicationContext

        beginReset()

        // Clear all volatile renderer/preview/selection caches first so old bitmaps cannot be reused.
        SelectionArtworkAssets.clearCaches()
        AutoRegionDetector.clearCaches()
        HomePreviewCache.clear()

        // Delete user-owned progress/state only. Built-in APK assets such as Fruits base/mask are untouched.
        ArtworkStore.eraseAllLocalArtwork()
        AchievementStore.resetToFreshInstall(appContext)
        ProfileStore.resetToFreshInstall(appContext)
        MonetizationStore.resetForGuestMode()

        clearDirectoryContents(appContext.cacheDir)
        clearDirectoryContents(appContext.codeCacheDir)
        clearDirectoryContents(File(appContext.filesDir, "tmp"))

        // Rebuild clean local state exactly like first launch after install.
        ArtworkStore.recreateLocalArtworkFolders()
        ProfileStore.initialize(appContext)
        AchievementStore.initialize(appContext)
        ArtworkStore.initialize(appContext)
        ProfileStore.repairInvalidState()
        AchievementStore.repairInvalidState()
        endResetWithoutRestart()

        System.gc()
    }


    /**
     * Sign Out now uses the same full fresh local reset as Delete Account.
     * Built-in APK assets remain untouched, but local drafts, finished artworks,
     * previews, XP, achievements, profile, badge, border, and caches are reset.
     */
    fun guestModeLocalReset(context: Context) {
        freshInstallLocalReset(context)
    }

    fun restartApp(context: Context) {
        val appContext = context.applicationContext
        val restartIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
        if (restartIntent != null) {
            restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            appContext.startActivity(restartIntent)
        }
        if (context is Activity) {
            context.finishAffinity()
        }
        Runtime.getRuntime().exit(0)
    }

    private fun clearDirectoryContents(dir: File?) {
        runCatching {
            dir?.listFiles()?.forEach { child -> child.deleteRecursively() }
            dir?.mkdirs()
        }
    }
}
