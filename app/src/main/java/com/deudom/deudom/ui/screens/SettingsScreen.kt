package com.deudom.deudom.ui.screens

import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.util.BiometricManager
import com.deudom.deudom.util.Prefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: Prefs.ThemeMode,
    onThemeChange: (Prefs.ThemeMode) -> Unit,
    onThemeIdChange: (String) -> Unit,
    onClose: () -> Unit,
    onShowDeleteDialog: () -> Unit
) {
    val context = LocalContext.current
    var appLockEnabled by remember { mutableStateOf(Prefs.isAppLockEnabled(context)) }
    val themeId = remember { Prefs.getThemeId(context) }
    val hasPremiumTheme = themeId != "default"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsGroup(title = "Apariencia") {
                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = "Modo de Pantalla",
                        subtitle = if (hasPremiumTheme) "Opción deshabilitada porque tienes un tema Premium activo." else "Elige entre claro, oscuro o el tema del sistema."
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("Auto", "Claro", "Oscuro").zip(
                                    listOf(Prefs.ThemeMode.SYSTEM, Prefs.ThemeMode.LIGHT, Prefs.ThemeMode.DARK)
                                ).forEach { (label, mode) ->
                                    OutlinedButton(
                                        onClick = { if (!hasPremiumTheme) onThemeChange(mode) },
                                        enabled = !hasPremiumTheme,
                                        colors = if (currentTheme == mode && !hasPremiumTheme) {
                                            ButtonDefaults.outlinedButtonColors(containerColor = GoldPremium, contentColor = Color.Black)
                                        } else {
                                            ButtonDefaults.outlinedButtonColors()
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(label, fontWeight = if (currentTheme == mode) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                            
                            if (hasPremiumTheme) {
                                Button(
                                    onClick = { 
                                        onThemeIdChange("default")
                                        onThemeChange(Prefs.ThemeMode.SYSTEM)
                                        onClose()
                                        Toast.makeText(context, "Tema restablecido", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("QUITAR TEMA PREMIUM", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = "Idioma",
                        subtitle = "Cambia el idioma de la aplicación."
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(
                                onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es")) },
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("Español") }
                            OutlinedButton(
                                onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) },
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("English") }
                        }
                    }
                }
            }

            item {
                SettingsGroup(title = "Seguridad") {
                    SettingsSwitchItem(
                        icon = Icons.Default.Shield,
                        title = "Bloqueo con Huella",
                        subtitle = "Asegura la app con tu huella dactilar.",
                        checked = appLockEnabled,
                        onCheckedChange = { isEnabled ->
                            if (isEnabled) {
                                val status = BiometricManager.checkSecurityStatus(context)
                                if (status == BiometricManager.SecurityStatus.READY) {
                                    Prefs.setAppLockEnabled(context, true)
                                    appLockEnabled = true
                                } else {
                                    Toast.makeText(context, "Configura un método de bloqueo en tu teléfono primero.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Prefs.setAppLockEnabled(context, false)
                                Prefs.removeCustomPin(context)
                                appLockEnabled = false
                            }
                        }
                    )
                }
            }
            
            item {
                SettingsGroup(title = "Cuenta") {
                     SettingsItem(
                        icon = Icons.Default.DeleteForever,
                        title = "Eliminar Cuenta",
                        subtitle = "Esta acción es permanente e irreversible.",
                        isDestructive = true,
                        onClick = {
                            onShowDeleteDialog()
                            onClose() // Close settings to show the dialog on the profile view
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = GoldPremium,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    val modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else GoldPremium,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (content != null) {
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = GoldPremium,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GoldPremium,
                checkedTrackColor = GoldPremium.copy(alpha = 0.5f)
            )
        )
    }
}
