package com.example.seeds.model

enum class CallerState {
    STARTED, RINGING, ANSWERED, UNANSWERED, BUSY, CANCELLED, COMPLETED, REJECTED, FAILED, UNDEFINED, TIMEOUT
}

data class StudentCallStatus (
    val callerState: CallerState,
    val isMuted: Boolean,
    val onHold: Boolean,
    val phoneNumber: String,
    val name: String,
    val raiseHand: Boolean,
    var isMuteUnmuteDone: Boolean = true
    )
