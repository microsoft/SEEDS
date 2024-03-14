package com.example.seeds.ui.call

import NetworkConnectivityLiveData
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.example.seeds.R
import com.example.seeds.adapters.ContentListAdapter
import com.example.seeds.adapters.StudentCallStatusAdapter
import com.example.seeds.databinding.FragmentCallBinding
import com.example.seeds.model.CallerState
import com.example.seeds.ui.BaseFragment
import com.example.seeds.ui.createclassroom.CreateClassroomFragmentArgs
import com.example.seeds.utils.ContactUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.log

@AndroidEntryPoint
class CallFragment : BaseFragment() {
    private lateinit var binding: FragmentCallBinding
    private val viewModel: CallViewModel by navGraphViewModels(R.id.call_nav) { defaultViewModelProviderFactory }
    private val args : CallFragmentArgs by navArgs()
    private lateinit var networkConnectivityLiveData: NetworkConnectivityLiveData


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCallBinding.inflate(inflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        networkConnectivityLiveData = NetworkConnectivityLiveData(requireActivity().applicationContext)

        networkConnectivityLiveData.observe(viewLifecycleOwner, Observer { isConnected ->
            if(isConnected) {
                Log.d("CallFragment", "Network is connected")
            } else {
                findNavController().navigate(CallFragmentDirections.actionCallFragmentToCallNoInternetFragment())
                Log.d("CallFragment", "Network is not connected")
            }
        })

        binding.myStudentsList.adapter = StudentCallStatusAdapter(StudentCallStatusAdapter.OnClickListener{
            logMessage("Muted: ${it.name} - ${it.phoneNumber}")
            viewModel.muteParticipant(it.phoneNumber)
        }, StudentCallStatusAdapter.OnClickListener{
            logMessage("Unmuted: ${it.name} - ${it.phoneNumber}")
            viewModel.unmuteParticipant(it.phoneNumber)
        }, StudentCallStatusAdapter.OnClickListener{
            removeUser(it.phoneNumber)
        }, StudentCallStatusAdapter.OnClickListener{
            logMessage("Retry calling: ${it.name} - ${it.phoneNumber}")
            viewModel.connectParticipant(it.name, it.phoneNumber)
        }, leader = viewModel.leader)

        viewModel.callToken.observe(viewLifecycleOwner, Observer {
            if(it != null) {
                Log.d("CallFragment", "Call token: $it")
                logMessage("Call token: $it")
            }
        })

//        viewModel.networkConnectivityLiveData.observe(viewLifecycleOwner, Observer { isConnected ->
//            if(isConnected) {
//                Log.d("CallFragment", "Network is connected")
//            } else {
//                Log.d("CallFragment", "Network is not connected")
//            }
//        })

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if(viewModel.navigateBack.value == true) {
                if (!findNavController().navigateUp()) {
                    if (isEnabled) {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        viewModel.doneNavigating()
                    }
                }
            }
            else {
                AlertDialog.Builder(requireContext())
                    .setMessage("Do you wish to end the call?")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { _, _ ->
//                        else {
                        lifecycleScope.launch {
                            val classroom = args.classroom
                            //check if no content was selected
                            classroom.contentIds = viewModel.selectedContentList.value!!.map {
                                it.id
                            }
                            logMessage("Call Ended on Back with final contents - id: ${classroom._id} - name: ${classroom.name} - contentIds: ${classroom.contentIds}")
                            logMessage("Call ended with Final Call Status: ${viewModel.callState.value}")
                            viewModel.updateClassroomContent(classroom)

//                        }
                    }
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }

//        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
////            AlertDialog.Builder(requireContext())
////                .setMessage("Do you wish to exit the game?")
////                .setCancelable(false)
////                .setPositiveButton("Yes") { _, _ ->
////                    lifecycleScope.launch {
////                        val increment = viewModel.incrementCoins()
////                        (requireActivity() as MainActivity).displayCoins(increment)
////                        if (viewModel.groupId != null) {
////                            logMessage("ExitToGroupDetails")
////                            binding.root.findNavController()
////                                .popBackStack(R.id.groupDetailsFragment, false)
////                        } else if (viewModel.goHome) {
////                            logMessage("ExitToPlayQuizdetails")
////                            binding.root.findNavController()
////                                .popBackStack(R.id.playQuizDetailsFragment, false)
////                        } else
////                            throw NotImplementedError("Quiz Details removed!")
////                    }
////                }
////                .setNegativeButton("No", null)
////                .show()
//        }
//        viewModel.teacherCallStatus.observe(viewLifecycleOwner, Observer {
//            if(it != null){
//                if(it.callerState == CallerState.COMPLETED || it.callerState == CallerState.UNANSWERED || it.callerState == CallerState.REJECTED || it.callerState == CallerState.BUSY || it.callerState == CallerState.REJECTED || it.callerState == CallerState.CANCELLED || it.callerState == CallerState.FAILED || it.callerState == CallerState.TIMEOUT){
//                    logMessage("Call ended because teacher disconnected - Reason: ${it.callerState}")
//                    Log.d("CALLCUT", it.callerState.toString())
//                    binding.endCallBtn.performClick()
//                }
//            }
//        })

        viewModel.navigateBack.observe(viewLifecycleOwner, Observer {
            if(it){
                requireActivity().onBackPressed()
            }
        })

//        viewModel.networkConnected.observe(viewLifecycleOwner) { isConnected ->
//            if (isConnected) {
//                // Handle reconnection logic, possibly re-establish WebSocket connection
//                // and refresh UI state to reflect current call state
//            } else {
//                // Navigate to "No Internet" screen
//                findNavController().navigate(CallFragmentDirections.actionCallFragmentToCallNoInternetFragment())
//            }
//        }

        binding.retryTeacher.setOnClickListener {
            viewModel.connectParticipant("Teacher", viewModel.teacherPhoneNumber)
            lifecycleScope.launch {
                delay(120000) // 120000 milliseconds = 2 minutes

                // Check if the teacher's status is still not ANSWERED
                if (viewModel.teacherCallStatus.value?.callerState != CallerState.ANSWERED) {
                    // Implement logic to end the conference
                    // Example: viewModel.endCall()
                    val classroom = args.classroom
                    classroom.contentIds = viewModel.selectedContentList.value!!.map{
                        it.id
                    }
                    viewModel.updateClassroomContent(classroom)

                    logMessage("Call ended because teacher didn't rejoin within 2 minutes - Reason: ${viewModel.teacherCallStatus.value?.callerState}")

                }
            }

        }

        binding.endCallBtn.setOnClickListener {
            val classroom = args.classroom
            classroom.contentIds = viewModel.selectedContentList.value!!.map{
                it.id
            }
            logMessage("Call Ended on end call button with final contents - id: ${classroom._id} - name: ${classroom.name} - contentIds: ${classroom.contentIds}")
            viewModel.updateClassroomContent(classroom)
        }

        binding.muteAllBtn.setOnClickListener {
            if(viewModel._isMutedAll.value!!){
                viewModel.unmuteAll()
                logMessage("Unmuted all")
                viewModel._isMutedAll.postValue(false)
            }
            else {
                viewModel.muteAll()
                logMessage("Muted all")
                viewModel._isMutedAll.postValue(true)
            }
            viewModel._isMuteOrUnmuteAllDone.postValue(false)
        }

        viewModel.callState.observe(viewLifecycleOwner, Observer {
            if(it != null) {
                logMessage("Call state changed to $it")
                if(it.none { state -> state.callerState != CallerState.COMPLETED }) {
                    requireActivity().onBackPressed()
                }
            }
        })

        binding.addStudentsButton.setOnClickListener {
            logMessage("Add students button clicked")
            findNavController().navigate(CallFragmentDirections.actionCallFragmentToAddStudentsFragment())
        }

        binding.teacherMic.setOnClickListener {
            if(viewModel.teacherCallStatus.value!!.isMuted) {
                viewModel.unmuteParticipant(viewModel.teacherPhoneNumber)
                logMessage("Teacher unmuted")
            }
            else {
                viewModel.muteParticipant(viewModel.teacherPhoneNumber)
                logMessage("Teacher muted")
            }
        }

//        viewModel.selectedContentList.observe(viewLifecycleOwner) {
//            if (it != null && it.isNotEmpty()) {
//
//            }
//        }

        //Here for add content we'll move to Home Fragment instead of AddContentToCallFragment

        binding.addContentButton.setOnClickListener {
            findNavController().navigate(CallFragmentDirections.actionCallFragmentToAddMoreContentToCallFragment())
        }

        binding.changeContent.setOnClickListener {
            findNavController().navigate(CallFragmentDirections.actionCallFragmentToAddContentToCallFragment2())
        }

        binding.pausePlayButton.setOnClickListener {
            viewModel._isAudioControlDone.postValue(false)
            if (viewModel.audioPlaying.value!!) {
                viewModel.pauseAudio()
                logMessage("Audio paused ${viewModel.selectedContent.value!!.id} ${viewModel.selectedContent.value!!.title}}")
                Log.d("AUDIOCONTROLPAUSEINI", viewModel.selectedContent.value!!.id)
            }
            else {
                viewModel.resumeAudio(viewModel.selectedContent.value!!.id)
                logMessage("Audio resumed ${viewModel.selectedContent.value!!.id} ${viewModel.selectedContent.value!!.title}}")
                Log.d("AUDIOCONTROLPLAYINI", viewModel.selectedContent.value!!.id)
            }
        }

        binding.forwardButton.setOnClickListener {
            if (viewModel.audioPlaying.value!!) {
                viewModel._forwardStreamDone.postValue(false)
                logMessage("Audio forward clicked ${viewModel.selectedContent.value!!.id} ${viewModel.selectedContent.value!!.title}}")
                viewModel.forwardAudio()
                showFeedback(binding.forwardFeedback, "+10s")
            }
        }

        binding.backwardButton.setOnClickListener {
            if (viewModel.audioPlaying.value!!) {
                viewModel._backwardStreamDone.postValue(false)
                logMessage("Audio backward clicked ${viewModel.selectedContent.value!!.id} ${viewModel.selectedContent.value!!.title}}")
                viewModel.backwardAudio()
                showFeedback(binding.backwardFeedback, "-10s")
            }
        }
        return binding.root
    }

    private fun showFeedback(textView: TextView, message: String) {
        textView.text = message
        textView.visibility = View.VISIBLE

        lifecycleScope.launch {
            delay(1500) // Delay for 1.5 seconds
            textView.visibility = View.INVISIBLE
        }
    }

    private fun removeUser(phoneNumber: String) {
        AlertDialog.Builder(requireContext())
            .setMessage("Are you sure?")
            .setCancelable(true)
            .setPositiveButton("Yes") { _, _ ->
                logMessage("Teacher removed student - $phoneNumber")
                viewModel.disconnectParticipant(phoneNumber)
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onStart() {
        logMessage("onStart")
        super.onStart()
    }

    override fun onStop() {
        logMessage("onStop")
        super.onStop()
    }
}