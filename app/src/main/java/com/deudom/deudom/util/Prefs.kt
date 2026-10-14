package com.deudom.deudom.util

import android.content.Context

object Prefs {
    private const val PREFS_NAME = "deudom_prefs"
    private const val KEY_TUTORIAL_DONE = "tutorial_done"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_THEME_ID = "theme_id" // New
    private const val KEY_ICON_PACK = "icon_pack"
    private const val KEY_APP_LOCK = "app_lock_enabled"
    private const val KEY_CACHED_BALANCE = "cached_balance"
    private const val KEY_CACHED_CURRENCY = "cached_currency"
    private const val KEY_CUSTOM_PIN = "custom_pin"

    enum class ThemeMode { SYSTEM, LIGHT, DARK }

    fun getIconPack(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ICON_PACK, "default") ?: "default"
    }

    fun setIconPack(context: Context, packId: String) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(KEY_ICON_PACK, packId)
            apply()
        }
    }

    fun getThemeId(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_ID, "default") ?: "default"
    }

    fun setThemeId(context: Context, id: String) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(KEY_THEME_ID, id)
            apply()
        }
    }

    fun hasCustomPin(context: Context): Boolean {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(KEY_CUSTOM_PIN, null) != null
    }

    fun setCustomPin(context: Context, pin: String) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(KEY_CUSTOM_PIN, hashPin(pin))
            apply()
        }
    }

    fun validatePin(context: Context, input: String): Boolean {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = sharedPref.getString(KEY_CUSTOM_PIN, "") ?: ""
        
        // Legacy Migration: If stored PIN is 4 digits (plain text)
        if (stored.length == 4 && stored.all { it.isDigit() }) {
            if (stored == input) {
                // Success! Migrate to hash immediately for security
                setCustomPin(context, input)
                return true
            }
            return false
        }
        
        // Standard Secure Check
        return stored == hashPin(input)
    }

    private fun hashPin(pin: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(pin.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            pin // Fallback to plain text if hashing fails (unlikely)
        }
    }

    fun removeCustomPin(context: Context) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove(KEY_CUSTOM_PIN)
            apply()
        }
    }

    fun getCachedBalance(context: Context): Double {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_CACHED_BALANCE, 0f).toDouble()
    }

    fun getCachedCurrency(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CACHED_CURRENCY, "RD$") ?: "RD$"
    }

    fun updateWidgetData(context: Context, balance: Double, currency: String) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putFloat(KEY_CACHED_BALANCE, balance.toFloat())
            putString(KEY_CACHED_CURRENCY, currency)
            apply()
        }
    }

    fun isAppLockEnabled(context: Context): Boolean {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(KEY_APP_LOCK, false)
    }

    fun setAppLockEnabled(context: Context, enabled: Boolean) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean(KEY_APP_LOCK, enabled)
            apply()
        }
    }

    fun getThemeMode(context: Context): ThemeMode {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = sharedPref.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try { ThemeMode.valueOf(name) } catch (e: Exception) { ThemeMode.SYSTEM }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(KEY_THEME_MODE, mode.name)
            apply()
        }
    }

    fun isTutorialDone(context: Context): Boolean {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(KEY_TUTORIAL_DONE, false)
    }

    fun setTutorialDone(context: Context, done: Boolean) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean(KEY_TUTORIAL_DONE, done)
            apply()
        }
    }

    fun clearUserData(context: Context) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove(KEY_TUTORIAL_DONE)
            remove(KEY_CUSTOM_PIN)
            remove(KEY_CACHED_BALANCE)
            remove(KEY_CACHED_CURRENCY)
            remove(KEY_APP_LOCK)
            apply()
        }
    }
}
