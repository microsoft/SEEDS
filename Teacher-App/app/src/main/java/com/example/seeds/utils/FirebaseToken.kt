package com.example.seeds.utils

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import java.util.*

class FirebaseToken {
    companion object {
        @Volatile
        private var IDTOKEN: String? = null

        fun getIdToken(): String {
            synchronized(this) {
                var idtoken = IDTOKEN

                if (idtoken == null) {
                    //ref: https://stackoverflow.com/questions/46215461/firebase-task-is-not-yet-complete
                    val task = FirebaseAuth.getInstance().currentUser!!.getIdToken(true)
                    Tasks.await(task)
                    val tokenResult: GetTokenResult = Objects.requireNonNull(task.result)
                    idtoken = tokenResult.token
                    IDTOKEN = idtoken
                }
                return idtoken!!
            }
        }

        fun resetIdToken() {
            IDTOKEN = null
        }

    }
}