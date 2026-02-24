package com.bangreedy.splitsync

import android.app.Application
import com.bangreedy.splitsync.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SplitSyncApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SplitSyncApp)
            modules(appModule)
        }
    }
}
