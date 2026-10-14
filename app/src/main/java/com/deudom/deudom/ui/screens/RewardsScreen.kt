package com.deudom.deudom.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deudom.deudom.data.FirestoreService
import com.deudom.deudom.data.User
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.util.IconProvider
import com.deudom.deudom.util.Prefs
import kotlinx.coroutines.launch

data class RewardItem(
    val id: String,
    val name: String,
    val cost: Int,
    val type: String, // "theme", "icon"
    val icon: ImageVector,
    val color: Color,
    val themeColors: List<Color> = emptyList()
)

val REWARDS = listOf(
    RewardItem("theme_dark_gold", "Tema Oro Oscuro", 500, "theme", Icons.Default.DarkMode, GoldPremium, themeColors = listOf(Color(0xFF121212), GoldPremium, Color(0xFF3B3B3B))),
    RewardItem("theme_forest", "Tema Bosque", 300, "theme", Icons.Default.Forest, Color(0xFF4CAF50), themeColors = listOf(Color(0xFF1B5E20), Color(0xFFA5D6A7), Color(0xFF388E3C))),
    RewardItem("theme_ocean", "Tema Océano", 300, "theme", Icons.Default.Water, Color(0xFF2196F3), themeColors = listOf(Color(0xFF0D47A1), Color(0xFF90CAF9), Color(0xFF1976D2))),
    RewardItem("icon_neon", "Iconos Neón", 200, "icon", Icons.Default.Bolt, Color(0xFFFFEB3B)),
    RewardItem("icon_retro", "Iconos Retro", 150, "icon", Icons.Default.VideogameAsset, Color(0xFFE91E63))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(
    userPhone: String,
    onDismiss: () -> Unit,
    onSettingsChange: (type: String, value: String) -> Unit
) {
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentIconPack by rememberUpdatedState(newValue = Prefs.getIconPack(context))


    LaunchedEffect(userPhone) {
        val listener = FirestoreService.listenToUser(userPhone) { u -> user = u }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mercado Cukitis", fontWeight = FontWeight.Bold, color = GoldPremium) },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = GoldPremium) } },
                actions = {
                    Box(modifier = Modifier.padding(end = 16.dp).clip(RoundedCornerShape(16.dp)).background(GoldPremium.copy(0.2f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(IconProvider.getIcon("diamond"), null, tint = GoldPremium, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${user?.cukitis ?: 0}", fontWeight = FontWeight.Bold, color = GoldPremium)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { p ->
        Column(modifier = Modifier.fillMaxSize().padding(p).padding(16.dp)) {
            Text("Personaliza tu Experiencia", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Usa tus Cukitis ganados para desbloquear temas e iconos exclusivos.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)

            Spacer(Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(REWARDS) { item ->
                    val isOwned = user?.unlockedThemes?.contains(item.id) == true
                    val canAfford = (user?.cukitis ?: 0) >= item.cost
                    val isApplied = (item.type == "theme" && Prefs.getThemeId(context) == item.id) || (item.type == "icon" && currentIconPack == item.id)

                    PremiumRewardCard(
                        item = item,
                        isOwned = isOwned,
                        canAfford = canAfford,
                        isApplied = isApplied,
                        onClick = {
                            if (isOwned) {
                                if (!isApplied) {
                                    onSettingsChange(item.type, item.id)
                                    val toastMessage = if (item.type == "theme") "Tema aplicado" else "Iconos aplicados"
                                    Toast.makeText(context, "$toastMessage: ${item.name}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                if (canAfford && !isLoading) {
                                    scope.launch {
                                        isLoading = true
                                        if (FirestoreService.purchaseReward(userPhone, item.cost, item.id)) {
                                            Toast.makeText(context, "¡Comprado!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Error al comprar", Toast.LENGTH_SHORT).show()
                                        }
                                        isLoading = false
                                    }
                                } else if (!canAfford) {
                                    Toast.makeText(context, "No tienes suficientes Cukitis", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumRewardCard(
    item: RewardItem,
    isOwned: Boolean,
    canAfford: Boolean,
    isApplied: Boolean,
    onClick: () -> Unit
) {
    val animatedBorderColor = animateColorAsState(
        targetValue = if (isApplied) GoldPremium else Color.Transparent,
        animationSpec = tween(500)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isOwned || canAfford, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(2.dp, animatedBorderColor.value),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
    ) {
        val gradient = Brush.radialGradient(
            colors = listOf(item.color.copy(alpha = 0.1f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            center = Offset(0f, 0f),
            radius = 300f
        )
        Column(
            modifier = Modifier.background(gradient).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (item.type == "theme" && item.themeColors.isNotEmpty()) {
                ThemePreview(colors = item.themeColors)
            } else {
                Box(
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(item.color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, null, tint = item.color, modifier = Modifier.size(36.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))

            when {
                isApplied -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = GoldPremium, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("APLICADO", color = GoldPremium, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                isOwned -> {
                    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)) {
                        Text("APLICAR", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
                else -> {
                    Button(
                        onClick = onClick,
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = item.color,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    ) {
                        Icon(IconProvider.getIcon("diamond"), null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${item.cost}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ThemePreview(colors: List<Color>) {
    Box(
        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(colors.getOrElse(0) { Color.Gray }),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            colors.drop(1).forEach { color ->
                Box(Modifier.size(15.dp).clip(CircleShape).background(color).padding(2.dp))
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}
