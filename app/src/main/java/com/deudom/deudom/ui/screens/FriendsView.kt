package com.deudom.deudom.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import com.deudom.deudom.data.FirestoreService
import com.deudom.deudom.data.FriendRequest
import com.deudom.deudom.data.FriendshipStatus
import com.deudom.deudom.data.User
import com.deudom.deudom.data.Loan
import com.deudom.deudom.data.LoanStatus
import com.deudom.deudom.data.Group
import com.deudom.deudom.data.GroupMember
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.ui.components.ReliabilityInfo
import com.deudom.deudom.util.formatPhone
import com.deudom.deudom.ui.components.UserAvatar
import com.deudom.deudom.ui.components.QRScanner
import com.deudom.deudom.viewmodel.FriendViewModel
import com.deudom.deudom.util.ScoreCalculator
import kotlinx.coroutines.launch

import androidx.compose.ui.res.stringResource
import com.deudom.deudom.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.deudom.deudom.ui.components.PremiumPullToRefresh
import kotlinx.coroutines.delay

data class DisplayFriend(val name: String, val phone: String, val photoUrl: String?, val isOnline: Boolean, val user: User?, val id: String, val canConnect: Boolean = false)

@Composable
fun FriendsView(
    userPhone: String, 
    loans: List<Loan>, 
    localFriends: List<com.deudom.deudom.data.local.FriendEntity>,
    onNewLoanClick: (String, String) -> Unit,
    onAddManualFriend: (String, String) -> Unit
) {
    val viewModel: FriendViewModel = koinViewModel()
    var showAdd by remember { mutableStateOf(false) }
    var showScan by remember { mutableStateOf(false) }
    var reliabilityFriendData by remember { mutableStateOf<Pair<String, User>?>(null) }
    var friendToDelete by remember { mutableStateOf<Triple<String, String, String?>?>(null) }
    var errorFriendName by remember { mutableStateOf<String?>(null) }
    var friendsSearchQuery by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    
    var selectedViewTab by remember { mutableIntStateOf(0) } 
    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var selectedGroupDetail by remember { mutableStateOf<Group?>(null) }
    var showCreateGroup by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var selectedGroupMembers by remember { mutableStateOf(setOf<Pair<String, String>>()) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) showScan = true }

    LaunchedEffect(Unit) { viewModel.loadFriends(userPhone) }
    
    DisposableEffect(userPhone) {
        val listener = FirestoreService.listenToGroups(userPhone) { groups = it }
        onDispose { listener.remove() }
    }
    
    LaunchedEffect(localFriends) { viewModel.checkRegisteredContacts(localFriends.map { it.phone }) }
    
    if (showAdd) {
        AddFriendScreen(
            userPhone = userPhone, 
            onDismiss = { showAdd = false }, 
            onSent = { showAdd = false },
            onAddManual = { name, phone -> 
                onAddManualFriend(name, phone)
                showAdd = false
            }
        )
    } else if (selectedGroupDetail != null) {
        GroupDetailScreen(selectedGroupDetail!!, userPhone) { selectedGroupDetail = null }
    } else if (showScan) {
        QRScanner(
            onCodeScanned = { p -> 
                scope.launch {
                    val u = FirestoreService.getUser(p)
                    if (u != null) {
                        val s = FirestoreService.getUser(userPhone)
                        FirestoreService.sendFriendRequest(
                            FriendRequest(
                                senderPhone = userPhone,
                                senderName = s?.name ?: "Usuario",
                                senderPhotoUrl = s?.photoUrl,
                                receiverPhone = p,
                                receiverName = u.name,
                                receiverPhotoUrl = u.photoUrl
                            )
                        )
                    }
                    showScan = false
                    viewModel.loadFriends(userPhone)
                }
            },
            onClose = { showScan = false }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            PremiumPullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        viewModel.loadFriends(userPhone)
                        delay(1000)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(stringResource(R.string.trust_circle), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { 
                                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) showScan = true 
                                else permissionLauncher.launch(android.Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Escanear", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
    
                        Button(
                            onClick = { showAdd = true },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Añadir", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Incoming Pending Requests Section
                    AnimatedVisibility(visible = viewModel.pendingRequests.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                            Text("SOLICITUDES PENDIENTES", style = MaterialTheme.typography.labelLarge, color = GoldPremium, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Spacer(Modifier.height(12.dp))
                            viewModel.pendingRequests.forEach { request ->
                                PendingRequestItem(
                                    request = request,
                                    onAccept = { viewModel.respondRequest(request.id, FriendshipStatus.ACCEPTED, userPhone) },
                                    onDecline = { viewModel.respondRequest(request.id, FriendshipStatus.REJECTED, userPhone) }
                                )
                            }
                        }
                    }

                    TabRow(
                        selectedTabIndex = selectedViewTab,
                        containerColor = Color.Transparent,
                        contentColor = GoldPremium,
                        indicator = { tabPositions -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedViewTab]), color = GoldPremium) },
                        divider = {}
                    ) {
                        Tab(selected = selectedViewTab == 0, onClick = { selectedViewTab = 0 }, text = { Text("Individual") })
                        Tab(selected = selectedViewTab == 1, onClick = { selectedViewTab = 1 }, text = { Text("Grupos") })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (viewModel.isLoading && viewModel.friends.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GoldPremium) }
                    } else {
                        if (selectedViewTab == 0) {
                            val onlineFriends = viewModel.friendsWithData.map { (f, u) ->
                                val fp = if (f.senderPhone == userPhone) f.receiverPhone else f.senderPhone
                                val fn = if (f.senderPhone == userPhone) f.receiverName else f.senderName
                                val fu = if (f.senderPhone == userPhone) f.receiverPhotoUrl else f.senderPhotoUrl
                                DisplayFriend(fn, fp, fu, true, u, f.id)
                            }
                            val onlinePhones = onlineFriends.map { it.phone }.toSet()
                            
                            // Map local manual friends and ensure we get their firestoreId if they were registered but now disconnected
                            val manualFriends = localFriends.filter { !onlinePhones.contains(it.phone) }.map { f ->
                                DisplayFriend(f.name, f.phone, f.photoUrl, false, null, f.firestoreId ?: f.phone, canConnect = viewModel.discoveredUsers.any { it.phone == f.phone })
                            }
                            val allFriends = (onlineFriends + manualFriends).sortedBy { it.name }.filter { it.name.contains(friendsSearchQuery, true) || it.phone.contains(friendsSearchQuery) }

                            OutlinedTextField(
                                value = friendsSearchQuery,
                                onValueChange = { friendsSearchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Buscar amigo...") },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(allFriends) { f ->
                                    val hasPending = loans.any { l -> (l.lenderPhone == f.phone || l.borrowerPhone == f.phone) && l.status != LoanStatus.PAID }
                                    FriendItem(
                                        friend = f, 
                                        loans = loans,
                                        onDelete = { 
                                            if (hasPending) {
                                                errorFriendName = f.name 
                                            } else {
                                                // Triple: phone, name, firestoreId (if online f.id is requestId, if manual f.id is firestoreId or phone)
                                                val fsId = if (f.isOnline) f.id else (localFriends.find { it.phone == f.phone }?.firestoreId)
                                                friendToDelete = Triple(f.phone, f.name, fsId)
                                            }
                                        },
                                        onConnect = {
                                            scope.launch {
                                                val s = FirestoreService.getUser(userPhone)
                                                val targetUser = viewModel.discoveredUsers.find { it.phone == f.phone }
                                                if (targetUser != null) {
                                                    FirestoreService.sendFriendRequest(FriendRequest(senderPhone = userPhone, senderName = s?.name ?: "Usuario", senderPhotoUrl = s?.photoUrl, receiverPhone = f.phone, receiverName = targetUser.name, receiverPhotoUrl = targetUser.photoUrl))
                                                    viewModel.loadFriends(userPhone)
                                                }
                                            }
                                        },
                                        onNewLoan = { onNewLoanClick(f.phone, f.name) }
                                    )
                                }
                                item { Spacer(Modifier.height(80.dp)) }
                            }
                        } else {
                            GroupsListContent(groups, { showCreateGroup = true }) { selectedGroupDetail = it }
                        }
                    }
                }
            }
        }
    }

    if (errorFriendName != null) {
        AlertDialog(onDismissRequest = { errorFriendName = null }, title = { Text("No se puede eliminar", color = Color.Red, fontWeight = FontWeight.Bold) }, text = { Text("Tienes deudas pendientes con $errorFriendName.") }, confirmButton = { TextButton(onClick = { errorFriendName = null }) { Text("Entendido") } })
    }

    if (friendToDelete != null) {
        AlertDialog(
            onDismissRequest = { friendToDelete = null }, 
            title = { Text("Eliminar amigo") }, 
            text = { Text("¿Eliminar a ${friendToDelete?.second}?") }, 
            confirmButton = { 
                TextButton(onClick = { 
                    viewModel.deleteFriend(friendToDelete!!.first, friendToDelete!!.third)
                    friendToDelete = null 
                }) { Text("Eliminar", color = Color.Red) } 
            }, 
            dismissButton = { TextButton(onClick = { friendToDelete = null }) { Text("Cancelar") } }
        )
    }

    if (reliabilityFriendData != null) {
        com.deudom.deudom.ui.components.ReliabilityDetailDialog(
            friendName = reliabilityFriendData!!.first,
            user = reliabilityFriendData!!.second,
            onDismiss = { reliabilityFriendData = null }
        )
    }
}

@Composable
fun GroupsListContent(groups: List<Group>, onCreateClick: () -> Unit, onGroupClick: (Group) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onCreateClick, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = GoldPremium), shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.GroupAdd, null, tint = Color.Black)
            Spacer(Modifier.width(8.dp))
            @Suppress("SpellCheckingInspection")
            Text("CREAR NUEVO GRUPO", color = Color.Black, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(24.dp))
        if (groups.isEmpty()) {
            Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Groups, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary.copy(0.5f))
                    Text("No tienes grupos aún.", color = MaterialTheme.colorScheme.secondary)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(groups) { group ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onGroupClick(group) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.2f))) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = GoldPremium.copy(0.1f)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Groups, null, tint = GoldPremium) }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(group.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("${group.members.size} miembros", style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) } // Added space for bottom nav
            }
        }
    }
}

@Composable
fun FriendItem(
    friend: DisplayFriend, 
    loans: List<Loan>, 
    onDelete: () -> Unit, 
    onConnect: () -> Unit = {}, 
    onNewLoan: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (friend.isOnline) GoldPremium.copy(0.3f) else MaterialTheme.colorScheme.outline.copy(0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Top Row: Avatar, Name and Delete Button
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(name = friend.name, photoUrl = friend.photoUrl, size = 56.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = friend.name, 
                            fontWeight = FontWeight.Black, 
                            fontSize = 18.sp, 
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (friend.isOnline) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.Verified, null, tint = Color(0xFF2196F3), modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = formatPhone(friend.phone), 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.secondary.copy(0.7f)
                    )
                }
                
                // Premium Delete Button (Top Right)
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.DeleteOutline, "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (friend.user != null) {
                // Bottom Section: Stickers and Quick Action
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Good Pay Sticker
                        PaymentSticker(
                            count = friend.user.goodPayCount, 
                            icon = Icons.Default.ThumbUp, 
                            color = Color(0xFF4CAF50)
                        )
                        // Bad Pay Sticker
                        PaymentSticker(
                            count = friend.user.badPayCount, 
                            icon = Icons.Default.ThumbDown, 
                            color = Color(0xFFF44336)
                        )
                    }

                    // Main Action: New Loan
                    Button(
                        onClick = onNewLoan,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = Color.Black),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("PRESTAR", fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            } else {
                // Manual contact placeholder
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Surface(
                        color = MaterialTheme.colorScheme.outline.copy(0.05f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "CONTACTO MANUAL", 
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (friend.canConnect) {
                            TextButton(onClick = onConnect) {
                                Text("CONECTAR", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = onNewLoan, modifier = Modifier.background(GoldPremium.copy(0.1f), CircleShape)) {
                            Icon(Icons.Default.Add, null, tint = GoldPremium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentSticker(count: Int, icon: ImageVector, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Text(count.toString(), fontWeight = FontWeight.Black, color = color, fontSize = 12.sp)
        }
    }
}

@Composable
fun PendingRequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GoldPremium.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(name = request.senderName, photoUrl = request.senderPhotoUrl, size = 48.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(request.senderName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Quiere ser tu amigo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onDecline,
                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.errorContainer.copy(0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier.size(40.dp).background(Color(0xFF4CAF50).copy(0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
