package com.example.seeds.network

import com.example.seeds.model.StudentCallStatus
import com.example.seeds.model.CallerState
import com.example.seeds.utils.ContactUtils

data class StudentCallStatusDto (
    val status: String?,
    val isMuted: Boolean?,
    val onHold: Boolean?,
    val phoneNumber: String,
    val raiseHand: Boolean
)

fun List<StudentCallStatusDto>.asDomainModel(contactUtils: ContactUtils): List<StudentCallStatus> {

    return map {
        StudentCallStatus (
       when(it.status) {
                "started" -> CallerState.STARTED
                "ringing" -> CallerState.RINGING
                "answered" -> CallerState.ANSWERED
                "joined" -> CallerState.ANSWERED
                "unanswered" -> CallerState.UNANSWERED
                "busy" -> CallerState.BUSY
                "cancelled" -> CallerState.CANCELLED
                "completed" -> CallerState.COMPLETED
                "rejected" -> CallerState.REJECTED
                "failed" -> CallerState.FAILED
                else -> CallerState.UNDEFINED
            },
            it.isMuted?: true,
            it.onHold?: false,
            it.phoneNumber,
            contactUtils.getNameFromString(it.phoneNumber),
            it.raiseHand
        )
    }
}
