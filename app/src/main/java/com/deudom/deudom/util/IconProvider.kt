package com.deudom.deudom.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Context

object IconProvider {
    private var currentIconPack: String = "default"

    fun loadIconPack(context: Context) {
        currentIconPack = Prefs.getIconPack(context)
    }

    fun getIcon(name: String): ImageVector {
        return when (currentIconPack) {
            "icon_neon" -> neonIcons[name] ?: Icons.Default.HelpOutline
            "icon_retro" -> retroIcons[name] ?: Icons.Default.HelpOutline
            else -> defaultIcons[name] ?: Icons.Default.HelpOutline
        }
    }

    private val defaultIcons: Map<String, ImageVector> = mapOf(
        "add" to Icons.Default.Add,
        "arrow_back" to Icons.AutoMirrored.Filled.ArrowBack,
        "arrow_forward" to Icons.AutoMirrored.Filled.ArrowForwardIos,
        "attach_money" to Icons.Default.AttachMoney,
        "account_balance" to Icons.Default.AccountBalance,
        "account_balance_wallet" to Icons.Default.AccountBalanceWallet,
        "bolt" to Icons.Default.Bolt,
        "camera" to Icons.Default.CameraAlt,
        "check" to Icons.Default.Check,
        "check_circle" to Icons.Default.CheckCircle,
        "close" to Icons.Default.Close,
        "cloud_off" to Icons.Default.CloudOff,
        "dark_mode" to Icons.Default.DarkMode,
        "delete" to Icons.Default.Delete,
        "delete_forever" to Icons.Default.DeleteForever,
        "description" to Icons.Default.Description,
        "diamond" to Icons.Default.Diamond,
        "event_repeat" to Icons.Default.EventRepeat,
        "forest" to Icons.Default.Forest,
        "handyman" to Icons.Default.Handyman,
        "info" to Icons.Default.Info,
        "lock" to Icons.Default.Lock,
        "logout" to Icons.AutoMirrored.Filled.Logout,
        "mic" to Icons.Default.Mic,
        "priority_high" to Icons.Default.PriorityHigh,
        "savings" to Icons.Default.Savings,
        "settings" to Icons.Default.Settings,
        "shield" to Icons.Default.Shield,
        "star" to Icons.Default.Star,
        "videogame_asset" to Icons.Default.VideogameAsset,
        "visibility" to Icons.Default.Visibility,
        "visibility_off" to Icons.Default.VisibilityOff,
        "warning" to Icons.Default.Warning,
        "water" to Icons.Default.Water
    )

    // In a real scenario, these would be custom vector assets.
    // For this example, we'll just reuse default icons to simulate the change.
    private val neonIcons: Map<String, ImageVector> = defaultIcons.toMutableMap().apply {
        this["add"] = Icons.Default.AddCircleOutline
        this["diamond"] = Icons.Default.Star
        this["savings"] = Icons.Default.Savings
        this["settings"] = Icons.Default.SettingsBrightness
    }

    private val retroIcons: Map<String, ImageVector> = defaultIcons.toMutableMap().apply {
        this["add"] = Icons.Filled.AddBox
        this["diamond"] = Icons.Default.Favorite
        this["savings"] = Icons.Default.AccountBalance
        this["settings"] = Icons.Default.Build
    }
}
