package com.example.seeds.network

import com.example.seeds.model.AudioStatus
import com.example.seeds.model.CallStatus
import com.example.seeds.utils.ContactUtils

data class CallStatusDto (
    val participants: List<StudentCallStatusDto>,
    val leaderPhoneNumber: String,
    val audio: AudioStatus
)

fun CallStatusDto.asDomainModel(contactUtils: ContactUtils): CallStatus {
    return CallStatus(
        participants = participants.asDomainModel(contactUtils),
        leaderPhoneNumber = leaderPhoneNumber,
        audio = audio
    )
}