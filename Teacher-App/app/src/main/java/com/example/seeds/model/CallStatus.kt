package com.example.seeds.model

import com.example.seeds.network.StudentCallStatusDto

data class CallStatus (
    val participants: List<StudentCallStatus>,
    val leaderPhoneNumber: String,
    val audio: AudioStatus
        )