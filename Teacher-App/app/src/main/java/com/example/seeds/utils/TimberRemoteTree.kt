package com.example.seeds.utils

import com.example.seeds.dao.LogDao
import com.example.seeds.database.LogEntity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class TimberRemoteTree(val database: LogDao,
                       private val teacherPhoneNumber: String): Timber.DebugTree() {
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS a zzz", Locale.getDefault())
    private val coroutineScope = CoroutineScope(Job() + Dispatchers.IO )

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        //format: ScreenTag EventTag Message(parameters)
        //eg: QuizDetailsFragment Entered/OnCreate quizId
        val timestamp = System.currentTimeMillis()
        val time = timeFormat.format(Date(timestamp))
        try {
            val remoteLog = LogEntity(logText = "$tag $message", time = time, user = teacherPhoneNumber, priority = priority)
            coroutineScope.launch {
                database.insert(remoteLog)
            }
        } catch (e: Exception) {
            return
        }
    }
}