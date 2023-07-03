package com.example.seeds.ui.call

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.example.seeds.model.*
import com.example.seeds.network.SeedsService
import com.example.seeds.network.asDomainModel
import com.example.seeds.repository.ClassroomRepository
import com.example.seeds.repository.ContentRepository
import com.example.seeds.repository.TeacherRepository
import com.example.seeds.utils.ContactUtils
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val network: SeedsService,
    context: Context,
    private val teacherRepository: TeacherRepository,
    private val contentRepository: ContentRepository,
    private val classroomRepository: ClassroomRepository
    ) : ViewModel(){

    private val contactUtils = ContactUtils(context)
    val args = CallFragmentArgs.fromSavedStateHandle(savedStateHandle)

    val leader = args.leader.toString()

    private var callStarted = false
    private var phoneNumbers: List<String> = args.phoneNumbers.toMutableList()
    private var allStudents = listOf<Student>()
    private lateinit var token: AccessToken
    private var cancelCallOnFailure: Job? = null

    val teacherPhoneNumber = Firebase.auth.currentUser!!.phoneNumber.toString().replace("+", "")
    var startedAudio = false

    var content: Content? = if (args.classroom.contents!!.isNotEmpty()) args.classroom.contents!![0] else null

    private val client =  OkHttpClient()
    private lateinit var socket: WebSocket

    private val _callToken = MutableLiveData<AccessToken>()
    val callToken: LiveData<AccessToken>
        get() = _callToken

    private val _callState = MutableLiveData<List<StudentCallStatus>>()
    val callState: LiveData<List<StudentCallStatus>>
        get() = _callState

    val _isMutedAll = MutableLiveData(false)
    val isMutedAll: LiveData<Boolean>
        get() = _isMutedAll

    private val _connectionLost = MutableLiveData<Boolean>(true)
    val connectionLost: LiveData<Boolean>
        get() = _connectionLost

    val _forwardStreamDone = MutableLiveData<Boolean>(true)
    val forwardStreamDone: LiveData<Boolean>
        get() = _forwardStreamDone

    val _backwardStreamDone = MutableLiveData<Boolean>(true)
    val backwardStreamDone: LiveData<Boolean>
        get() = _backwardStreamDone

    val _isMuteOrUnmuteAllDone = MutableLiveData<Boolean>(true)
    val isMuteOrUnmuteAllDone: LiveData<Boolean>
        get() = _isMuteOrUnmuteAllDone

    val _isAudioControlDone = MutableLiveData<Boolean>(true)
    val isAudioControlDone: LiveData<Boolean>
        get() = _isAudioControlDone

    private val _students = MutableLiveData<List<Student>>()
    val students: LiveData<List<Student>>
        get() = _students

    var studentsNotOnCall = listOf<Student>()

    private val _allContent = MutableLiveData<List<Content>>()
    val allContent: LiveData<List<Content>>
        get() = _allContent

    private val _selectedContent = MutableLiveData<Content>(content)
    val selectedContent: LiveData<Content>
        get() = _selectedContent

    private val _filteredContent = MutableLiveData<List<Content>>()
    val filteredContent: LiveData<List<Content>>
        get() = _filteredContent

    private val _languages = MutableLiveData<List<String>>()
    val languages: LiveData<List<String>>
        get() = _languages

    private val _experiences = MutableLiveData<List<String>>()
    val experiences: LiveData<List<String>>
        get() = _experiences

    private val _filtersChosen = MutableLiveData<List<String>>()
    val filtersChosen: LiveData<List<String>>
        get() = _filtersChosen

    private val _selectedContentList = MutableLiveData<List<Content>>(args.classroom.contents)
    val selectedContentList: LiveData<List<Content>>
        get() = _selectedContentList

    private val _teacherCallStatus = MutableLiveData<StudentCallStatus>()
    val teacherCallStatus: LiveData<StudentCallStatus>
        get() = _teacherCallStatus

    private val _audioPlaying = MutableLiveData(false)
    val audioPlaying: LiveData<Boolean>
        get() = _audioPlaying

    private val _navigateBack = MutableLiveData(false)
    val navigateBack: LiveData<Boolean>
        get() = _navigateBack

    init {
        getAccessToken()
        viewModelScope.launch {
            val selectedContentListIds = args.classroom.contentIds
            _allContent.value = contentRepository.getAllContent()

            val filteredListContent = _allContent.value?.filter {
                !selectedContentListIds.contains(it.id)
            }
            _allContent.value = filteredListContent!!

            _languages.value =
                filteredListContent.map { it.language.lowercase() }.distinct().map { it.capitalize() }
            _experiences.value = filteredListContent.map { it.type.lowercase() }.distinct().map {
                it.capitalize()
            }

//            _filteredContent.value = content
            if(filtersChosen.value != null) {
                val langs = _languages.value!!.filter { filtersChosen.value!!.contains(it) }.toMutableSet()
                val exps = _experiences.value!!.filter { filtersChosen.value!!.contains(it) }.toMutableSet()
                filterContent(langs, exps)
            } else{
                _filteredContent.value = filteredListContent!!
            }
        }

        Log.d("LEADERISHERE", leader)
        Log.d("CONTENTCALL", args.classroom.contents!!.map{
            content -> content.title
        }.toString())
    }

    fun updateClassroomContent(classroom: Classroom) {
        viewModelScope.launch {
            classroomRepository.saveClassroom(classroom)
            _navigateBack.value = true
        }
    }

    fun doneNavigating() {
        _navigateBack.value = false
    }

    private fun getAccessToken() {
        viewModelScope.launch {
            token = network.getAccessToken()
            allStudents = args.classroom.students //teacherRepository.getMyStudents()
            _callToken.postValue(token)
            connectWebSocket()
        }
    }

    fun setSelectedContent(content: Content){
        _selectedContent.value = content
    }

    fun setSelectedContentList(content: List<Content>){
        _selectedContentList.value = content
    }


    fun setAllContentList(content: List<Content>){
        _allContent.value = content
    }

    private fun startCall() {
        viewModelScope.launch {
            val names = mutableListOf<String>()
            names.add("Teacher")
            for(num in args.phoneNumbers.copyOfRange(1, args.phoneNumbers.size))
                names.add(args.classroom.students.filter { it.phoneNumber == num }[0].name)
            if(!callStarted) {
                network.startCall(CallDetails(_callToken.value!!.confId, phoneNumbers, names))
                callStarted = true
            }
        }
    }

    fun refreshCallState() {
        viewModelScope.launch {
            val callStatus = network.getCallStatus(_callToken.value!!.confId).asDomainModel(contactUtils)
            val networkCallState = callStatus.participants
            _audioPlaying.value = callStatus.audio.state == "play"
            Log.d("AUDIOCONTROLNETWORK", callStatus.audio.toString())
            _callState.postValue(networkCallState.sortedByDescending { it.raiseHand })
            Log.d("MOMENTISHERE", networkCallState.toString())
            _teacherCallStatus.postValue(networkCallState.find {
                it.phoneNumber == teacherPhoneNumber
            })
            Log.d("TEACHERCALLSTATUS", _teacherCallStatus.toString())
            studentsNotOnCall = allStudents.filter { stu ->
                networkCallState.find { stu.phoneNumber == it.phoneNumber } == null
                        || when(networkCallState.find { stu.phoneNumber == it.phoneNumber }?.callerState) {
                                CallerState.COMPLETED, CallerState.FAILED, CallerState.REJECTED, CallerState.CANCELLED, CallerState.UNANSWERED, CallerState.BUSY -> true
                                else -> false
                            }
            }
            _isMutedAll.value = networkCallState.filter { it.phoneNumber != teacherPhoneNumber }.all { it.isMuted }
        }
    }

    fun filterContent(languages: MutableSet<String>, experiences: MutableSet<String>) {
        var languagesChosen = languages.map{ it.lowercase()}.toMutableSet()
        var experiencesChosen = experiences.map{ it.lowercase()}.toMutableSet()
        if (experiences.isEmpty()) experiencesChosen = _experiences.value!!.toMutableSet().map{ it.lowercase()}.toMutableSet()
        if (languages.isEmpty()) languagesChosen = _languages.value!!.toMutableSet().map{ it.lowercase()}.toMutableSet()
        _filteredContent.value = allContent.value?.filter {
            languagesChosen.contains(it.language.lowercase()) && experiencesChosen.contains(it.type.lowercase())
        }
    }

    fun clearFilters() {
        //_filtersChosen.value = listOf()
        _filteredContent.value = allContent.value
    }

    fun setFiltersChosen(filters: List<String>) {
        _filtersChosen.value = filters
    }

    private fun message(message: String) {
        Log.d("MESSAGE", message)

        if (message.contains("refresh")) {
            refreshCallState()
        } else if (message.contains("forwardStreamDone")){
            _forwardStreamDone.postValue(true)
        } else if(message.contains("backwardStreamDone")){
            _backwardStreamDone.postValue(true)
        } else if(message.contains("muteAllDone") || message.contains("unMuteAllDone")) {
             refreshCallState()
             _isMuteOrUnmuteAllDone.postValue(true)
        } else if(message.contains("playDone") || message.contains("pauseDone") || message.contains("resumeDone")){
            Log.d("AUDIOCONTROLMESSAGE", message)
            _isAudioControlDone.postValue(true)
            refreshCallState()
//            Log.d("AUDIOCONTROL CURRENT", audioPlaying.value!!.toString())
        } else if(message.contains("muteDone:") || message.contains("unmuteDone")){
            val phoneNumber = message.split(":")[1]
            Log.d("MUTEUNMUTEDONE", phoneNumber)
            val tempCallState = callState.value!!.toMutableList()
            val index = tempCallState?.indexOfFirst { it.phoneNumber == phoneNumber }
            Log.d("MUTEUNMUTEDONEIN1", index.toString())
            index?.let {
                tempCallState[it].isMuteUnmuteDone = true
            }
            Log.d("MUTEUNMUTEDONEIN2", tempCallState[index!!].toString())
            _callState.postValue(tempCallState)
            Log.d("MUTEUNMUTEDONEIN3", tempCallState[index!!].toString())
            refreshCallState()
            Log.d("MUTEUNMUTEDONEIN4",  _callState.value!![index!!].toString())
        }
        else if (message.contains("vonageWebsocket:disconnected") || message.contains("vonageWebsocket:failed")) {
            _connectionLost.postValue(true)
        } else if (message.contains("vonageWebsocket:connected")) {
            Log.d("Message", message)
            Log.d("AHHHHHHH", callState.value.toString())
            //TODO: Put leader code here.
            if(!args.leader.isNullOrEmpty()){
                socket.send("lead:${args.leader}")
            }
            _connectionLost.postValue(false)
        }
    }

    fun muteParticipant(phoneNumber: String) {
        val tempCallState = callState.value!!.toMutableList()
        val index = tempCallState?.indexOfFirst { it.phoneNumber == phoneNumber }
        index?.let {
            tempCallState[it].isMuteUnmuteDone = false
        }
        Log.d("MUTEPARTICIPANT", "MUTE TRIGGERED $phoneNumber")
        _callState.postValue(tempCallState)
        socket.send("mute:$phoneNumber")
    }

    fun unmuteParticipant(phoneNumber: String) {
        val tempCallState = callState.value!!.toMutableList()
        val index = tempCallState?.indexOfFirst { it.phoneNumber == phoneNumber }
        index?.let {
            tempCallState[it].isMuteUnmuteDone = false
        }
        _callState.postValue(tempCallState)
        Log.d("UNMUTEPARTICIPANT", "UNMUTE TRIGGERED")
        socket.send("unmute:$phoneNumber")
    }

    fun unmuteAll() {
        socket.send("unMuteAll")
    }

    fun muteAll() {
        socket.send("muteAll")
    }

    fun connectParticipant(name: String, phoneNumber: String) {
        socket.send("add:$phoneNumber:$name") // put namw  // add:{phoneNumber}:{name}
    }

    fun disconnectParticipant(phoneNumber: String) {
        socket.send("remove:$phoneNumber")
    }

    fun playAudio(audioId: String) {
        socket.send("play:$audioId")
        _audioPlaying.postValue(true)
    }

    fun resumeAudio(audioId: String) {
        if(startedAudio) {
            socket.send("resume:$audioId")
        } else {
            playAudio(audioId)
            startedAudio = true
            return
        }
        _audioPlaying.postValue(true)
    }

    fun pauseAudio() {
        socket.send("pause")
        _audioPlaying.postValue(false)
    }

    fun forwardAudio() {
        socket.send("forwardStream")
    }

    fun backwardAudio() {
        socket.send("backwardStream")
    }

    fun endCall() {
        socket.send("end")
    }

    /*

    Commands Server can send:
    refresh
    vonageWebSocket:connected
    vonageWebSocket:disconnected
    vonageWebSocket:failed

    *****************************************

    Commands the client can send:
    mute:phNo
    unmute:phNo
    play:audioId
    pause
    resume:audioId
    connect:phNo
    disconnect:phNo
    endcall

    After every command is complete, refresh event is sent to the client.
    */

    //event on disconnect to Android from Azure PubSub

    inner class SeedsWebSocketListener: WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            Log.d("socket", "Socket Created!!")
            //need to start the call here
            startCall()
            cancelCallOnFailure?.cancel()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            Log.d("socket", text)
            message(text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            super.onMessage(webSocket, bytes)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosing(webSocket, code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("SOCKETCLOSED", "")
            super.onClosed(webSocket, code, reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.d("SOCKETFAILURE", t.message.toString())

            //reference: https://stackoverflow.com/questions/54088030/reconnect-okhttp-websocket-when-internet-disconnects
            socket.close(1000, null)
            Thread.sleep(4000)
            connectWebSocket()
            cancelCallOnFailure = viewModelScope.launch {
                delay(180000L)
                _navigateBack.postValue(true)
            }
        }
    }

    override fun onCleared() {
        endCall()
        socket.close(1000, "close")
    }

    fun connectWebSocket(){
        val request = Request.Builder()
            .url(token.accessToken)
            .build()
        socket = client.newWebSocket(request, SeedsWebSocketListener())
        client.dispatcher().executorService()
    }
}


