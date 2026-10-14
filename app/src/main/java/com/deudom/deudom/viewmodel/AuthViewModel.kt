package com.deudom.deudom.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.deudom.deudom.data.User
import com.google.firebase.auth.FirebaseAuth

class AuthViewModel : ViewModel() {
    var currentUser by mutableStateOf<User?>(null)
        private set

    var userPhone by mutableStateOf("")
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    fun updatePhone(phone: String) {
        userPhone = phone
    }

    fun logout() {
        auth.signOut()
        currentUser = null
        userPhone = ""
    }
}
