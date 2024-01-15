package com.example.seeds

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import androidx.work.*
import com.example.seeds.dao.LogDao
import com.example.seeds.utils.TimberRemoteTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class SeedsApplication: Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    @Inject
    lateinit var logDatabase: LogDao
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        val sharedPreferences = getSharedPreferences("YourSharedPreferencesName", Context.MODE_PRIVATE)
        val teacherPhoneNumber = sharedPreferences.getString("phone", "") ?: ""
            .replace("+", "")
        val remoteTree = TimberRemoteTree(logDatabase, teacherPhoneNumber)
        Timber.plant(remoteTree)
        //delayedInit()
    }

    //private fun delayedInit() = applicationScope.launch { setupWorker() }
}
