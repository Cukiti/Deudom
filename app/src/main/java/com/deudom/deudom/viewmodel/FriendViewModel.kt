package com.deudom.deudom.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deudom.deudom.data.FirestoreService
import com.deudom.deudom.data.FriendRequest
import com.deudom.deudom.data.FriendshipStatus
import com.deudom.deudom.data.User
import com.deudom.deudom.repository.DeudomRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class FriendViewModel(
    private val repository: DeudomRepository
) : ViewModel() {
    private var friendsSenderListener: ListenerRegistration? = null
    private var friendsReceiverListener: ListenerRegistration? = null
    private var requestsListener: ListenerRegistration? = null
    private var sentRequestsListener: ListenerRegistration? = null
    
    // Map to keep track of individual friend profile listeners
    private val friendProfileListeners = mutableMapOf<String, ListenerRegistration>()

    var friends by mutableStateOf<List<FriendRequest>>(emptyList())
    // We use a map to store live user data for each friend to ensure reactive updates
    var friendsWithData by mutableStateOf<List<Pair<FriendRequest, User?>>>(emptyList())
    var pendingRequests by mutableStateOf<List<FriendRequest>>(emptyList())
    var sentPendingRequests by mutableStateOf<List<FriendRequest>>(emptyList())
    var isLoading by mutableStateOf(false)
    
    private var senderFriendsList = listOf<FriendRequest>()
    private var receiverFriendsList = listOf<FriendRequest>()
    private val friendUserData = mutableMapOf<String, User>()
    
    var discoveredUsers by mutableStateOf<List<User>>(emptyList())

    fun loadFriends(phone: String) {
        isLoading = true
        
        friendsSenderListener?.remove()
        friendsReceiverListener?.remove()
        requestsListener?.remove()
        sentRequestsListener?.remove()
        clearProfileListeners()

        friendsSenderListener = FirestoreService.listenToFriendsAsSender(phone) { list ->
            senderFriendsList = list
            updateFriendsListAndListen(phone)
        }
        
        friendsReceiverListener = FirestoreService.listenToFriendsAsReceiver(phone) { list ->
            receiverFriendsList = list
            updateFriendsListAndListen(phone)
        }

        requestsListener = FirestoreService.listenToPendingRequests(phone) { list ->
            pendingRequests = list
        }

        sentRequestsListener = FirestoreService.listenToSentPendingRequests(phone) { list ->
            sentPendingRequests = list
        }
    }
    
    private fun updateFriendsListAndListen(myPhone: String) {
        val mergedList = senderFriendsList + receiverFriendsList
        friends = mergedList
        
        // Setup listeners for each friend's profile data
        mergedList.forEach { req ->
            val friendPhone = if (req.senderPhone == myPhone) req.receiverPhone else req.senderPhone
            
            // If we are not already listening to this friend, start now
            if (!friendProfileListeners.containsKey(friendPhone)) {
                val listener = FirestoreService.listenToUser(friendPhone) { updatedUser ->
                    if (updatedUser != null) {
                        friendUserData[friendPhone] = updatedUser
                    } else {
                        friendUserData.remove(friendPhone)
                    }
                    // When any friend's data changes (score, counts, etc), refresh the UI list
                    refreshFriendsWithData(myPhone)
                }
                friendProfileListeners[friendPhone] = listener
            }
        }
        
        // Initial data load or cleanup of removed friends
        refreshFriendsWithData(myPhone)
        isLoading = false
    }

    private fun refreshFriendsWithData(myPhone: String) {
        // Build the list instantly from the reactive map
        friendsWithData = friends.map { req ->
            val friendPhone = if (req.senderPhone == myPhone) req.receiverPhone else req.senderPhone
            req to friendUserData[friendPhone]
        }
    }

    private fun clearProfileListeners() {
        friendProfileListeners.values.forEach { it.remove() }
        friendProfileListeners.clear()
    }
    
    fun checkRegisteredContacts(manualPhones: List<String>) {
        if (manualPhones.isEmpty()) return
        viewModelScope.launch {
            val users = FirestoreService.findRegisteredUsers(manualPhones)
            val alreadyConnected = friends.flatMap { listOf(it.senderPhone, it.receiverPhone) } +
                                  sentPendingRequests.flatMap { listOf(it.senderPhone, it.receiverPhone) } +
                                  pendingRequests.flatMap { listOf(it.senderPhone, it.receiverPhone) }
            
            discoveredUsers = users.filter { !alreadyConnected.contains(it.phone) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        friendsSenderListener?.remove()
        friendsReceiverListener?.remove()
        requestsListener?.remove()
        sentRequestsListener?.remove()
        clearProfileListeners()
    }

    fun respondRequest(requestId: String, status: FriendshipStatus, phone: String) {
        viewModelScope.launch {
            if (FirestoreService.respondFriendRequest(requestId, status)) {
                // Force a refresh of the local database so the new friend shows up in Room immediately
                repository.refreshFriends(phone)
                loadFriends(phone)
            }
        }
    }

    fun deleteFriend(phone: String, firestoreId: String? = null) {
        viewModelScope.launch {
            // Use the repository to handle both local and remote deletion
            repository.deleteFriend(phone, firestoreId)
        }
    }
}
