package com.dopalette.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firebase sync layer for the lightweight ArtworkStore V2 JSON format.
 *
 * Important rules:
 * - Local save remains the source of instant editor behavior.
 * - Firebase only uploads/downloads the same tiny V2 JSON files.
 * - No thumbnails, previews, exports, undo stacks, or redo stacks are uploaded.
 */
object ArtworkCloudSync {
    enum class SyncState { LOCAL_ONLY, SYNCING, SYNCED, NOT_SYNCED }

    val syncState = mutableStateOf(SyncState.LOCAL_ONLY)
    val syncMessage = mutableStateOf("Saved on this device.")

    private fun markSyncing(message: String = "Syncing…") {
        syncState.value = SyncState.SYNCING
        syncMessage.value = message
    }

    private fun markSynced(message: String = "Synced") {
        syncState.value = SyncState.SYNCED
        syncMessage.value = message
    }

    private fun markNotSynced(message: String = "Not synced yet. We’ll keep trying.") {
        syncState.value = SyncState.NOT_SYNCED
        syncMessage.value = message
    }

    private fun markLocalOnly(message: String = "Saved on this device.") {
        syncState.value = SyncState.LOCAL_ONLY
        syncMessage.value = message
    }

    private const val DRAFTS_COLLECTION = "artworkDraftsV2"
    private const val FINISHED_COLLECTION = "artworkFinishedV2"

    private var initialized = false
    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
        initialized = true
    }

    fun uploadDraft(title: String, json: String, modifiedAtMs: Long = System.currentTimeMillis()) {
        val account = GoogleAuthController.account.value ?: run {
            markLocalOnly()
            return
        }
        if (!initialized || !GoogleAuthController.configured.value) {
            markNotSynced("Not synced yet. We’ll keep trying.")
            return
        }
        val uid = account.uid.trim()
        if (uid.isBlank() || json.isBlank()) return

        markSyncing()
        runCatching {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection(DRAFTS_COLLECTION)
                .document(title.sanitizeCloudId())
                .set(
                    mapOf(
                        "saveVersion" to 2,
                        "kind" to "draft",
                        "title" to title,
                        "json" to json,
                        "updatedAtMs" to modifiedAtMs,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .addOnSuccessListener { markSynced("Synced") }
                .addOnFailureListener { error -> markNotSynced(friendlyArtworkError(error)) }
        }.onFailure { error -> markNotSynced(friendlyArtworkError(error)) }
    }

    fun uploadFinished(id: String, title: String, json: String, modifiedAtMs: Long = System.currentTimeMillis()) {
        val account = GoogleAuthController.account.value ?: run {
            markLocalOnly()
            return
        }
        if (!initialized || !GoogleAuthController.configured.value) {
            markNotSynced("Not synced yet. We’ll keep trying.")
            return
        }
        val uid = account.uid.trim()
        if (uid.isBlank() || id.isBlank() || json.isBlank()) return

        markSyncing("Syncing…")
        runCatching {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection(FINISHED_COLLECTION)
                .document(id.sanitizeCloudId())
                .set(
                    mapOf(
                        "saveVersion" to 2,
                        "kind" to "finished",
                        "id" to id,
                        "title" to title,
                        "json" to json,
                        "updatedAtMs" to modifiedAtMs,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .addOnSuccessListener { markSynced("Synced") }
                .addOnFailureListener { error -> markNotSynced(friendlyArtworkError(error)) }
        }.onFailure { error -> markNotSynced(friendlyArtworkError(error)) }
    }

    fun deleteDraft(title: String) {
        val account = GoogleAuthController.account.value ?: return
        if (!initialized || !GoogleAuthController.configured.value) return
        val uid = account.uid.trim()
        if (uid.isBlank()) return

        runCatching {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection(DRAFTS_COLLECTION)
                .document(title.sanitizeCloudId())
                .delete()
        }
    }

    fun deleteFinished(id: String) {
        val account = GoogleAuthController.account.value ?: return
        if (!initialized || !GoogleAuthController.configured.value) return
        val uid = account.uid.trim()
        if (uid.isBlank()) return

        runCatching {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection(FINISHED_COLLECTION)
                .document(id.sanitizeCloudId())
                .delete()
        }
    }


    suspend fun syncDraftOnly(title: String): AccountProfileSync.SyncResult {
        val account = GoogleAuthController.account.value ?: run {
            markLocalOnly()
            return AccountProfileSync.SyncResult(true, "Saved on this device.")
        }
        if (!initialized || !GoogleAuthController.configured.value) {
            markNotSynced("Not synced yet. We’ll keep trying.")
            return AccountProfileSync.SyncResult(false, "Sync is not ready yet.")
        }
        val file = ArtworkStore.localDraftCloudFiles().firstOrNull { it.title == title || it.id == title.sanitizeCloudId() }
            ?: return AccountProfileSync.SyncResult(true, "Saved on this device.")
        markSyncing("Syncing…")
        return try {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(account.uid)
                .collection(DRAFTS_COLLECTION)
                .document(file.title.sanitizeCloudId())
                .set(
                    mapOf(
                        "saveVersion" to 2,
                        "kind" to "draft",
                        "title" to file.title,
                        "json" to file.json,
                        "updatedAtMs" to file.modifiedAtMs,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            markSynced("Synced")
            AccountProfileSync.SyncResult(true, "Synced")
        } catch (error: Throwable) {
            val msg = friendlyArtworkError(error)
            markNotSynced(msg)
            AccountProfileSync.SyncResult(false, msg)
        }
    }

    suspend fun syncFinishedOnly(id: String): AccountProfileSync.SyncResult {
        val account = GoogleAuthController.account.value ?: run {
            markLocalOnly()
            return AccountProfileSync.SyncResult(true, "Saved on this device.")
        }
        if (!initialized || !GoogleAuthController.configured.value) {
            markNotSynced("Not synced yet. We’ll keep trying.")
            return AccountProfileSync.SyncResult(false, "Sync is not ready yet.")
        }
        val file = ArtworkStore.localFinishedCloudFiles().firstOrNull { it.id == id }
            ?: return AccountProfileSync.SyncResult(true, "Saved on this device.")
        markSyncing("Syncing…")
        return try {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(account.uid)
                .collection(FINISHED_COLLECTION)
                .document(file.id.sanitizeCloudId())
                .set(
                    mapOf(
                        "saveVersion" to 2,
                        "kind" to "finished",
                        "id" to file.id,
                        "title" to file.title,
                        "json" to file.json,
                        "updatedAtMs" to file.modifiedAtMs,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            markSynced("Synced")
            AccountProfileSync.SyncResult(true, "Synced")
        } catch (error: Throwable) {
            val msg = friendlyArtworkError(error)
            markNotSynced(msg)
            AccountProfileSync.SyncResult(false, msg)
        }
    }

    suspend fun syncAfterSignIn(account: GoogleAuthController.Account): AccountProfileSync.SyncResult {
        if (!initialized || !GoogleAuthController.configured.value) {
            return AccountProfileSync.SyncResult(false, "Sync is not ready yet.")
        }

        return try {
            downloadCloudProgress(account)
            uploadLocalProgress(account)
            ArtworkStore.reloadAfterCloudRestore()
            AccountProfileSync.SyncResult(true, "Synced")
        } catch (error: Throwable) {
            AccountProfileSync.SyncResult(false, friendlyArtworkError(error))
        }
    }

    suspend fun syncLocalBeforeLeaving(): AccountProfileSync.SyncResult {
        val account = GoogleAuthController.account.value ?: run {
            markLocalOnly()
            return AccountProfileSync.SyncResult(true, "Saved on this device.")
        }
        return uploadLocalProgress(account)
    }

    suspend fun uploadLocalProgress(account: GoogleAuthController.Account): AccountProfileSync.SyncResult {
        if (!initialized || !GoogleAuthController.configured.value) {
            markNotSynced("Not synced yet. We’ll keep trying.")
            return AccountProfileSync.SyncResult(false, "Sync is not ready yet.")
        }

        markSyncing("Syncing…")
        return try {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(account.uid)

            ArtworkStore.localDraftCloudFiles().forEach { file ->
                userRef.collection(DRAFTS_COLLECTION)
                    .document(file.title.sanitizeCloudId())
                    .set(
                        mapOf(
                            "saveVersion" to 2,
                            "kind" to "draft",
                            "title" to file.title,
                            "json" to file.json,
                            "updatedAtMs" to file.modifiedAtMs,
                            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()
            }

            ArtworkStore.localFinishedCloudFiles().forEach { file ->
                userRef.collection(FINISHED_COLLECTION)
                    .document(file.id.sanitizeCloudId())
                    .set(
                        mapOf(
                            "saveVersion" to 2,
                            "kind" to "finished",
                            "id" to file.id,
                            "title" to file.title,
                            "json" to file.json,
                            "updatedAtMs" to file.modifiedAtMs,
                            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()
            }

            markSynced("Synced")
            AccountProfileSync.SyncResult(true, "Synced")
        } catch (error: Throwable) {
            val msg = friendlyArtworkError(error)
            markNotSynced(msg)
            AccountProfileSync.SyncResult(false, msg)
        }
    }

    private suspend fun downloadCloudProgress(account: GoogleAuthController.Account) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(account.uid)

        val draftDocs = userRef.collection(DRAFTS_COLLECTION).get().await()
        draftDocs.documents.forEach { doc ->
            val title = doc.getString("title") ?: doc.id
            val json = doc.getString("json").orEmpty()
            val updatedAtMs = doc.getLong("updatedAtMs") ?: 0L
            if (json.isNotBlank()) ArtworkStore.importCloudDraft(title, json, updatedAtMs)
        }

        val finishedDocs = userRef.collection(FINISHED_COLLECTION).get().await()
        finishedDocs.documents.forEach { doc ->
            val id = doc.getString("id") ?: doc.id
            val json = doc.getString("json").orEmpty()
            val updatedAtMs = doc.getLong("updatedAtMs") ?: 0L
            if (json.isNotBlank()) ArtworkStore.importCloudFinished(id, json, updatedAtMs)
        }
    }

    private fun friendlyArtworkError(error: Throwable): String {
        val raw = error.message.orEmpty()
        return when {
            raw.contains("offline", ignoreCase = true) -> "Not synced yet. Your artwork is safe on this device."
            raw.contains("permission", ignoreCase = true) -> "Not synced yet. Please try again later."
            raw.contains("1048487", ignoreCase = true) || raw.contains("document", ignoreCase = true) && raw.contains("large", ignoreCase = true) -> "Not synced yet. Your artwork is safe on this device."
            raw.isNotBlank() -> "Not synced yet. We’ll keep trying."
            else -> "Not synced yet. Your artwork is safe on this device."
        }
    }

    private fun String.sanitizeCloudId(): String {
        return replace(Regex("[/\\\\:*?\"<>|#\\[\\]]"), "_").take(120).ifBlank { "untitled" }
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { value -> cont.resume(value) }
        addOnFailureListener { error -> cont.resumeWithException(error) }
        addOnCanceledListener { cont.cancel() }
    }
}
