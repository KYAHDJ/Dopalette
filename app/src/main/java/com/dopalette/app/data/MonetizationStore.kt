package com.dopalette.app.data

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.compose.runtime.mutableStateOf

object MonetizationStore {
    private const val PREFS_NAME = "dopalette_monetization"
    private const val KEY_IS_PREMIUM = "is_premium_user"
    private const val SPECIAL_PREFIX = "special_unlock_until_"
    private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L

    private lateinit var context: Context

    val isPremiumUser = mutableStateOf(false)
    val updateTick = mutableIntStateOf(0)
    val premiumSignInRequestTick = mutableIntStateOf(0)
    private var pendingPremiumSignInRequest = false

    fun initialize(appContext: Context) {
        context = appContext.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isPremiumUser.value = prefs.getBoolean(KEY_IS_PREMIUM, false)
        if (isPremiumUser.value) {
            applyPremiumProfileRewards()
        }
        updateTick.intValue += 1
    }

    fun adsEnabled(): Boolean = !isPremiumUser.value

    fun setPremiumPlaceholder(enabled: Boolean) {
        // Kept for the current UI preview button. Real purchase flow can call activatePremiumForAccount().
        setPremiumLocal(enabled, applyRewards = enabled)
    }

    fun setPremiumLocal(enabled: Boolean, applyRewards: Boolean = enabled) {
        isPremiumUser.value = enabled
        if (::context.isInitialized) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_PREMIUM, enabled)
                .apply()
        }
        if (enabled && applyRewards) {
            applyPremiumProfileRewards()
        }
        updateTick.intValue += 1
    }

    suspend fun restorePremiumFromFirebase(account: GoogleAuthController.Account): Boolean {
        return try {
            val snapshot = FirebaseFirestore.getInstance().collection("users").document(account.uid).get().await()
            val premium = snapshot.getBoolean("isPremium") == true ||
                snapshot.getBoolean("adsRemoved") == true ||
                snapshot.getBoolean("specialsForever") == true
            if (premium) {
                setPremiumLocal(true, applyRewards = true)
                true
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun activatePremiumForAccount(account: GoogleAuthController.Account): Boolean {
        setPremiumLocal(true, applyRewards = true)
        return try {
            FirebaseFirestore.getInstance().collection("users").document(account.uid).set(
                mapOf(
                    "isPremium" to true,
                    "premiumArtist" to true,
                    "adsRemoved" to true,
                    "specialsForever" to true,
                    "premiumBorderId" to "rainbow_border",
                    "borderId" to "rainbow_border",
                    "badgeId" to "premium_artist",
                    "premiumSince" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            true
        } catch (_: Throwable) {
            // Keep local Premium so the app still behaves correctly; Firebase will be retried on next profile sync.
            false
        }
    }

    fun applyPremiumProfileRewards() {
        // Premium rewards must remain owned after sync/reload.
        ProfileStore.setBorderId("rainbow_border")
        ProfileStore.setBadgeId("premium_artist")
    }

    fun resetForGuestMode() {
        isPremiumUser.value = false
        if (::context.isInitialized) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        }
        updateTick.intValue += 1
    }

    fun isSpecialUnlocked(category: String): Boolean {
        if (isPremiumUser.value) return true
        if (!::context.isInitialized) return false
        val until = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(SPECIAL_PREFIX + categoryKey(category), 0L)
        return until > System.currentTimeMillis()
    }

    fun unlockSpecialFor24Hours(category: String) {
        if (!::context.isInitialized) return
        val until = System.currentTimeMillis() + ONE_DAY_MS
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(SPECIAL_PREFIX + categoryKey(category), until)
            .apply()
        updateTick.intValue += 1
    }

    fun specialUnlockRemainingText(category: String): String {
        if (isPremiumUser.value) return "Open forever"
        if (!::context.isInitialized) return "Watch ad to open"
        val until = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(SPECIAL_PREFIX + categoryKey(category), 0L)
        val remaining = until - System.currentTimeMillis()
        if (remaining <= 0L) return "Watch ad to open"
        val totalMinutes = (remaining / (60L * 1000L)).coerceAtLeast(1L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0L -> "${hours}h ${minutes}m left"
            else -> "${minutes}m left"
        }
    }

    fun requestPremiumSignIn() {
        pendingPremiumSignInRequest = true
        premiumSignInRequestTick.intValue += 1
    }

    fun consumePremiumSignInRequest(): Boolean {
        val pending = pendingPremiumSignInRequest
        pendingPremiumSignInRequest = false
        return pending
    }

    fun shouldShowInterstitialAfterDone(): Boolean = adsEnabled()
    fun shouldShowBanner(): Boolean = adsEnabled()
    fun shouldRequireRewardForSpecial(category: String): Boolean = adsEnabled() && !isSpecialUnlocked(category)

    private fun categoryKey(category: String): String {
        return category.trim().lowercase().replace(Regex("[^a-z0-9_]+"), "_")
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }
}
