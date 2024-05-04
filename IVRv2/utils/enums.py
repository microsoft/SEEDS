from enum import Enum


class ConversationRTCEventType(Enum):
    GENERAL = "leg:status:update"
    AUDIO_DTMF = "audio:dtmf"
    AUDIO_EARMUFF_OFF = "audio:earmuff:off"
    AUDIO_EARMUFF_ON = "audio:earmuff:on"
    AUDIO_MUTE_OFF = "audio:mute:off"
    AUDIO_MUTE_ON = "audio:mute:on"
    AUDIO_PLAY_STOP = "audio:play:stop"
    AUDIO_PLAY_DONE = "audio:play:done"
    AUDIO_PLAY = "audio:play"
    AUDIO_RECORD_STOP = "audio:record:stop"
    AUDIO_RECORD_DONE = "audio:record:done"
    AUDIO_RECORD = "audio:record"
    AUDIO_ASR_DONE = "audio:asr:done"
    AUDIO_ASR_RECORD_DONE = "audio:asr:record:done"
    AUDIO_SAY_STOP = "audio:say:stop"
    AUDIO_SAY_DONE = "audio:say:done"
    AUDIO_SAY = "audio:say"
    AUDIO_SPEAKING_ON = "audio:speaking:on"
    AUDIO_SPEAKING_OFF = "audio:speaking:off"
    MESSAGE = "message"
    MESSAGE_REJECTED = "message:rejected"
    MESSAGE_SUBMITTED = "message:submitted"
    MESSAGE_UNDELIVERABLE = "message:undeliverable"
    MESSAGE_DELIVERED = "message:delivered"
    MESSAGE_SEEN = "message:seen"
    CONVERSATION_CREATED = "conversation:created"
    CONVERSATION_UPDATED = "conversation:updated"
    MEMBER_INVITED = "member:invited"
    MEMBER_JOINED = "member:joined"
    MEMBER_LEFT = "member:left"
    MEMBER_MEDIA = "member:media"
    MEMBER_MESSAGE_STATUS = "member:message:status"
    RTC_STATUS = "rtc:status"
    RTC_TRANSFER = "rtc:transfer"
    RTC_HANGUP = "rtc:hangup"
    RTC_TERMINATE = "rtc:terminate"
    RTC_ANSWERED = "rtc:answered"
    RTC_RINGING = "rtc:ringing"
    RTC_ANSWER = "rtc:answer"
    SIP_STATUS = "sip:status"
    SIP_ANSWERED = "sip:answered"
    SIP_MACHINE = "sip:machine"
    SIP_HANGUP = "sip:hangup"
    SIP_RINGING = "sip:ringing"
    SIP_AMD_MACHINE = "sip:amd_machine"
    CUSTOM = "custom:"
    EPHEMERAL = "ephemeral"
    EVENT_DELETE = "event:delete"

class CallStatus(Enum):
    STARTED = "started"
    RINGING = "ringing"
    ANSWERED = "answered"
    BUSY = "busy"
    CANCELLED = "cancelled"
    UNANSWERED = "unanswered"
    DISCONNECTED = "disconnected"
    REJECTED = "rejected"
    FAILED = "failed"
    HUMAN = "human" 
    MACHINE = "machine"
    TIMEOUT = "timeout"
    COMPLETED = "completed"
    RECORD = "record"
    INPUT = "input"
    TRANSFER = "transfer"
    
    @staticmethod
    def get_end_call_enums():
        return [
            CallStatus.BUSY,
            CallStatus.CANCELLED,
            CallStatus.UNANSWERED,
            CallStatus.DISCONNECTED,
            CallStatus.REJECTED,
            CallStatus.FAILED,
            CallStatus.COMPLETED,
            CallStatus.TIMEOUT
        ]