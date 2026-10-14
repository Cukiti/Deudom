package com.deudom.deudom.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deudom.deudom.R
import com.deudom.deudom.data.FirestoreService
import com.deudom.deudom.data.FriendRequest
import com.deudom.deudom.data.User
import com.deudom.deudom.ui.components.PremiumTextField
import com.deudom.deudom.ui.components.ReliabilityInfo
import com.deudom.deudom.ui.components.UserAvatar
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.util.ContactManager
import com.deudom.deudom.util.formatPhone
import kotlinx.coroutines.launch

@Composable
fun AddFriendScreen(
    userPhone: String, 
    onDismiss: () -> Unit, 
    onSent: () -> Unit,
    onAddManual: (String, String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var foundUser by remember { mutableStateOf<User?>(null) }
    var userNotFound by remember { mutableStateOf(false) }
    var isSent by remember { mutableStateOf(false) }
    var suggestedUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var alreadyFriend by remember { mutableStateOf(false) }
    var requestAlreadySent by remember { mutableStateOf(false) }
    var manualName by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val loadSuggestedUsers: suspend () -> Unit = {
        try {
            val deviceContacts = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                ContactManager.getDeviceContacts(context)
            }
            val phones = deviceContacts.map { it.phone }
            if (phones.isNotEmpty()) {
                val registered = FirestoreService.findRegisteredUsers(phones).filter { it.phone != userPhone }
                suggestedUsers = registered
            }
        } catch (e: Exception) {
            android.util.Log.e("AddFriendScreen", "Error loading suggested users: ${e.message}")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            scope.launch { loadSuggestedUsers() }
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            loadSuggestedUsers()
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    LaunchedEffect(phone) {
        if (phone.length == 10) {
            if (phone == userPhone) { userNotFound = false; foundUser = null }
            else {
                isSearching = true
                val u = FirestoreService.getUser(phone)
                if (u != null) { 
                    foundUser = u
                    userNotFound = false
                    val friends = FirestoreService.getFriends(userPhone)
                    alreadyFriend = friends.any { it.senderPhone == phone || it.receiverPhone == phone }
                    val sentRequests = FirestoreService.getSentPendingRequests(userPhone)
                    requestAlreadySent = sentRequests.any { it.receiverPhone == phone }
                }
                else { 
                    foundUser = null
                    userNotFound = true 
                }
                isSearching = false
            }
        } else { foundUser = null; userNotFound = false; alreadyFriend = false; requestAlreadySent = false }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = GoldPremium) }
                Text(stringResource(R.string.add_friend), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(32.dp))
            PremiumTextField(phone, { if (it.length <= 10) phone = it }, stringResource(R.string.phone_number), Icons.Default.Search, keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone, prefix = "+1 ")
            
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isSearching) {
                    CircularProgressIndicator(color = GoldPremium)
                } else if (foundUser != null) {
                    UserFoundCard(foundUser!!, isSent || requestAlreadySent, alreadyFriend) {
                        scope.launch {
                            val s = FirestoreService.getUser(userPhone)
                            if (FirestoreService.sendFriendRequest(FriendRequest(
                                senderPhone = userPhone,
                                senderName = s?.name ?: "Usuario",
                                senderPhotoUrl = s?.photoUrl,
                                receiverPhone = phone,
                                receiverName = foundUser!!.name,
                                receiverPhotoUrl = foundUser!!.photoUrl
                            ))) isSent = true
                        }
                    }
                } else if (userNotFound) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.2f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PersonOff, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Este usuario no tiene la App", fontWeight = FontWeight.Bold)
                            Text("Puedes agregarlo como contacto manual para gestionar sus deudas tú mismo.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(24.dp))
                            PremiumTextField(value = manualName, onValueChange = { manualName = it }, label = "Nombre del contacto", icon = Icons.Default.Person)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { onAddManual(manualName, phone); onDismiss() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = manualName.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            ) {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("GUARDAR COMO MANUAL")
                            }
                        }
                    }
                } else {
                    if (suggestedUsers.isNotEmpty()) {
                        Text(stringResource(R.string.suggested_friends), fontWeight = FontWeight.Bold, color = GoldPremium, modifier = Modifier.align(Alignment.Start))
                        Spacer(Modifier.height(16.dp))
                        suggestedUsers.forEach { user ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                onClick = { phone = user.phone }
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    UserAvatar(name = user.name, photoUrl = user.photoUrl)
                                    Spacer(Modifier.width(16.dp))
                                    Column(Modifier.weight(1f)) { Text(user.name, fontWeight = FontWeight.Bold); Text(formatPhone(user.phone), style = MaterialTheme.typography.bodySmall) }
                                    Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    } else if (phone.isEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 48.dp).alpha(0.5f)) {
                            Icon(Icons.Default.GroupAdd, null, modifier = Modifier.size(120.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text(stringResource(R.string.search_prompt))
                        }
                    }
                }
                
                if (isSent) {
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onSent, 
                        modifier = Modifier.fillMaxWidth().height(56.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPremium),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.understood), color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun UserFoundCard(user: User, isSent: Boolean, alreadyFriend: Boolean, onSendRequest: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            UserAvatar(name = user.name, photoUrl = user.photoUrl, size = 100.dp)
            Spacer(modifier = Modifier.height(20.dp))
            Text(user.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(formatPhone(user.phone), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(24.dp))
            
            ReliabilityInfo(good = user.goodPayCount, bad = user.badPayCount)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                if (alreadyFriend) {
                    Surface(
                        color = GoldPremium.copy(0.1f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, GoldPremium)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = GoldPremium, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("YA SON AMIGOS", color = GoldPremium, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                } else if (isSent) {
                    Surface(
                        color = Color(0xFF4CAF50).copy(0.1f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color(0xFF4CAF50))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("SOLICITUD ENVIADA", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = onSendRequest,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPremium),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("AÑADIR AL CÍRCULO", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
