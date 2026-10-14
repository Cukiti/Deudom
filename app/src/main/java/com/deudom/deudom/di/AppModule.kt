package com.deudom.deudom.di

import com.deudom.deudom.data.local.DeudomDatabase
import com.deudom.deudom.repository.DeudomRepository
import com.deudom.deudom.viewmodel.AuthViewModel
import com.deudom.deudom.viewmodel.FriendViewModel
import com.deudom.deudom.viewmodel.LoanViewModel
import com.deudom.deudom.viewmodel.AIAssistantViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database
    single { DeudomDatabase.getDatabase(androidContext()) }
    single { get<DeudomDatabase>().loanDao() }
    single { get<DeudomDatabase>().friendDao() }

    // Repository
    single { DeudomRepository(get(), get()) }

    // ViewModels
    viewModel { LoanViewModel(androidApplication(), get()) }
    viewModel { FriendViewModel(get()) } // Injected repository
    viewModel { AuthViewModel() }
    viewModel { AIAssistantViewModel() }
}
