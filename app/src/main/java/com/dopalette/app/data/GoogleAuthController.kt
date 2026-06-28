package com.dopalette.app.data

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.dopalette.app.ui.home.HomePreviewCache
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object GoogleAuthController {
    data class Account(
        val uid: String,
        val displayName: String,
        val email: String,
        val photoUrl: String?
    )

    val account = mutableStateOf<Account?>(null)
    val configured = mutableStateOf(false)
    val accountStateTick = mutableIntStateOf(0)

    private var initialized = false
    private var auth: FirebaseAuth? = null

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true

        configured.value = FirebaseApp.getApps(context).isNotEmpty()
        if (!configured.value) return

        auth = FirebaseAuth.getInstance()
        account.value = auth?.currentUser?.let {
            Account(
                uid = it.uid,
                displayName = it.displayName ?: "DoPalette User",
                email = it.email ?: "",
                photoUrl = it.photoUrl?.toString()
            )
        }
    }

    fun refreshCurrentAccount(context: Context): Account? {
        initialize(context)
        val firebaseUser = auth?.currentUser
        val refreshed = firebaseUser?.let {
            Account(
                uid = it.uid,
                displayName = it.displayName ?: "DoPalette User",
                email = it.email ?: "",
                photoUrl = it.photoUrl?.toString()
            )
        }
        if (account.value?.uid != refreshed?.uid) accountStateTick.intValue += 1
        account.value = refreshed
        return refreshed
    }

    private fun googleOptions(context: Context): GoogleSignInOptions? {
        val webClientId = context.defaultWebClientIdOrNull() ?: return null
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()
    }

    fun getSignInIntent(context: Context): Intent? {
        initialize(context)
        val options = googleOptions(context) ?: return null
        return GoogleSignIn.getClient(context, options).signInIntent
    }

    suspend fun getAccountChooserSignInIntent(context: Context): Intent? {
        initialize(context)
        val options = googleOptions(context) ?: return null
        val client = GoogleSignIn.getClient(context, options)
        // Clear the cached GoogleSignIn account before launching the picker.
        // This prevents Android/Google Play Services from silently reusing an old Gmail
        // on tablets or shared devices and lets the user pick the exact account.
        runCatching { client.signOut().await() }
        return client.signInIntent
    }

    suspend fun handleSignInResult(context: Context, data: Intent?): String {
        initialize(context)
        if (!configured.value) {
            return "Firebase is not connected yet. Add google-services.json first."
        }

        val webClientId = context.defaultWebClientIdOrNull()
        if (webClientId.isNullOrBlank()) {
            return "Google Sign-In is missing default_web_client_id. Add the Firebase google-services.json file."
        }

        return try {
            val googleAccount = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            val token = googleAccount.idToken
                ?: return "Google did not return an ID token. Check Firebase SHA-1 and Google Sign-In setup."
            val credential = GoogleAuthProvider.getCredential(token, null)
            val result = auth!!.signInWithCredential(credential).await()
            val user = result.user ?: return "Sign-in failed. No Firebase user was returned."
            val signedInAccount = Account(
                uid = user.uid,
                displayName = user.displayName ?: googleAccount.displayName ?: "DoPalette User",
                email = user.email ?: googleAccount.email ?: "",
                photoUrl = user.photoUrl?.toString() ?: googleAccount.photoUrl?.toString()
            )
            account.value = signedInAccount
            accountStateTick.intValue += 1

            ArtworkCloudSync.initialize(context)
            val profileResult = AccountProfileSync.syncLightweightProfile(signedInAccount)
            val artworkResult = ArtworkCloudSync.syncAfterSignIn(signedInAccount)
            when {
                profileResult.success && artworkResult.success -> "Signed in and synced profile + artwork progress."
                profileResult.success -> "Signed in. ${artworkResult.message}"
                artworkResult.success -> "Signed in. ${profileResult.message}"
                else -> "Signed in. ${profileResult.message} ${artworkResult.message}"
            }
        } catch (e: ApiException) {
            "Google sign-in failed: ${e.statusCode}"
        } catch (e: Throwable) {
            e.message?.takeIf { it.isNotBlank() } ?: "Google sign-in failed."
        }
    }


    suspend fun deleteCurrentAccountDataAndAuth(context: Context): String {
        initialize(context)
        val currentUser = auth?.currentUser ?: return "No signed-in Firebase user found. Sign in again first."
        val deleteAccount = Account(
            uid = currentUser.uid,
            displayName = currentUser.displayName ?: account.value?.displayName ?: "DoPalette User",
            email = currentUser.email ?: account.value?.email ?: "",
            photoUrl = currentUser.photoUrl?.toString() ?: account.value?.photoUrl
        )

        return try {
            AppResetManager.beginReset()

            // Delete cloud data using the live FirebaseAuth UID BEFORE deleting Auth.
            // Never rely on a cached account UID here because stale account state can leave
            // old users/{uid} Firestore documents behind.
            val cloudDelete = AccountProfileSync.deleteSignedInAccountData(deleteAccount)
            if (!cloudDelete.success) {
                AppResetManager.endResetWithoutRestart()
                return "Could not delete account data: ${cloudDelete.message}"
            }

            currentUser.delete().await()
            auth?.signOut()
            account.value = null
            accountStateTick.intValue += 1

            val webClientId = context.defaultWebClientIdOrNull()
            if (!webClientId.isNullOrBlank()) {
                val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .requestProfile()
                    .build()
                runCatching { GoogleSignIn.getClient(context, options).signOut().await() }
            }

            AppResetManager.freshInstallLocalReset(context)
            AppResetManager.restartApp(context)
            "Account deleted. Restarting DoPalette."
        } catch (error: Throwable) {
            AppResetManager.endResetWithoutRestart()
            val raw = error.message.orEmpty()
            when {
                raw.contains("recent", ignoreCase = true) || raw.contains("credential", ignoreCase = true) ->
                    "For safety, Firebase needs a fresh sign-in before deleting this account. Sign out, sign in again, then delete account."
                raw.isNotBlank() -> raw.take(180)
                else -> "Delete Account failed. Please sign in again and try once more."
            }
        }
    }

    fun signOut(context: Context, onDone: () -> Unit = {}) {
        initialize(context)

        // User requested Sign Out to behave like a full fresh reset too.
        // Sign Out deletes the session and local user-created app data, then restarts cleanly in guest mode.
        fun finishSignOutFreshReset() {
            account.value = null
            accountStateTick.intValue += 1
            AppResetManager.freshInstallLocalReset(context)
            onDone()
            AppResetManager.restartApp(context)
        }

        auth?.signOut()
        val webClientId = context.defaultWebClientIdOrNull()
        if (!webClientId.isNullOrBlank()) {
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .requestProfile()
                .build()
            GoogleSignIn.getClient(context, options).signOut().addOnCompleteListener {
                finishSignOutFreshReset()
            }
        } else {
            finishSignOutFreshReset()
        }
    }

    private fun performFreshInstallReset(context: Context) {
        val appContext = context.applicationContext

        // Delete Account must behave like uninstall + reinstall without leaving stale in-memory state.
        ProfileStore.reset()
        AchievementStore.resetAll()
        ArtworkStore.eraseAllLocalArtwork()
        SelectionArtworkAssets.clearCaches()
        AutoRegionDetector.clearCaches()
        HomePreviewCache.clear()

        clearDirectoryContents(appContext.cacheDir)
        appContext.codeCacheDir?.let { clearDirectoryContents(it) }

        ProfileStore.initialize(appContext)
        AchievementStore.initialize(appContext)
        ArtworkStore.initialize(appContext)
        ArtworkStore.recreateLocalArtworkFolders()
        System.gc()
    }

    private fun clearSessionOnly(context: Context) {
        // Kept for compatibility if older screens call it, but Sign Out now uses full fresh reset.
        AppResetManager.freshInstallLocalReset(context)
    }

    private fun clearDirectoryContents(dir: java.io.File?) {
        runCatching {
            dir?.listFiles()?.forEach { child -> child.deleteRecursively() }
            dir?.mkdirs()
        }
    }

    private fun Context.defaultWebClientIdOrNull(): String? {
        val id = resources.getIdentifier("default_web_client_id", "string", packageName)
        return if (id == 0) null else runCatching { getString(id) }.getOrNull()
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { value -> cont.resume(value) }
        addOnFailureListener { error -> cont.resumeWithException(error) }
        addOnCanceledListener { cont.cancel() }
    }
}
