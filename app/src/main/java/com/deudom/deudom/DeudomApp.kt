package com.deudom.deudom

import android.app.Application
import com.deudom.deudom.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class DeudomApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@DeudomApp)
            modules(appModule)
        }
    }
}
