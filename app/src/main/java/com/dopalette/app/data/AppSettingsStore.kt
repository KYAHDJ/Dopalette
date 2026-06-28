package com.dopalette.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf

object AppSettingsStore {
    private const val PREFS_NAME = "dopalette_app_settings"
    private const val KEY_AUTO_SAVE = "auto_save_drafts"
    private const val KEY_CONFIRM_CLEAR = "confirm_before_clear"
    private const val KEY_HELP_TIPS = "show_help_tips"
    private const val KEY_HIGH_QUALITY_EXPORT = "high_quality_export"
    private const val KEY_KEEP_SCREEN_AWAKE = "keep_screen_awake"
    private const val KEY_SHOW_STATUS_DETAILS = "show_status_details"

    private lateinit var context: Context

    val autoSaveDrafts = mutableStateOf(true)
    val confirmBeforeClear = mutableStateOf(true)
    val showHelpTips = mutableStateOf(true)
    val highQualityExport = mutableStateOf(true)
    val keepScreenAwake = mutableStateOf(false)
    val showStatusDetails = mutableStateOf(true)

    fun initialize(appContext: Context) {
        context = appContext.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        autoSaveDrafts.value = prefs.getBoolean(KEY_AUTO_SAVE, true)
        confirmBeforeClear.value = prefs.getBoolean(KEY_CONFIRM_CLEAR, true)
        showHelpTips.value = prefs.getBoolean(KEY_HELP_TIPS, true)
        highQualityExport.value = prefs.getBoolean(KEY_HIGH_QUALITY_EXPORT, true)
        keepScreenAwake.value = prefs.getBoolean(KEY_KEEP_SCREEN_AWAKE, false)
        showStatusDetails.value = prefs.getBoolean(KEY_SHOW_STATUS_DETAILS, true)
    }

    fun setAutoSaveDrafts(value: Boolean) {
        autoSaveDrafts.value = value
        save(KEY_AUTO_SAVE, value)
    }

    fun setConfirmBeforeClear(value: Boolean) {
        confirmBeforeClear.value = value
        save(KEY_CONFIRM_CLEAR, value)
    }

    fun setShowHelpTips(value: Boolean) {
        showHelpTips.value = value
        save(KEY_HELP_TIPS, value)
    }

    fun setHighQualityExport(value: Boolean) {
        highQualityExport.value = value
        save(KEY_HIGH_QUALITY_EXPORT, value)
    }

    fun setKeepScreenAwake(value: Boolean) {
        keepScreenAwake.value = value
        save(KEY_KEEP_SCREEN_AWAKE, value)
    }

    fun setShowStatusDetails(value: Boolean) {
        showStatusDetails.value = value
        save(KEY_SHOW_STATUS_DETAILS, value)
    }

    private fun save(key: String, value: Boolean) {
        if (!::context.isInitialized) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key, value)
            .apply()
    }
}
