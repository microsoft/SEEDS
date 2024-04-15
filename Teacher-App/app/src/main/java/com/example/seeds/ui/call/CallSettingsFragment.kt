package com.example.seeds.ui.call

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.seeds.R
import com.example.seeds.adapters.CheckboxNameListAdapter
import com.example.seeds.adapters.ContentListAdapter
import com.example.seeds.databinding.AssignLeaderBinding
import com.example.seeds.databinding.FragmentCallSettingsBinding
import com.example.seeds.model.Content
import com.example.seeds.ui.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CallSettingsFragment : BaseFragment() {
    override var bottomNavigationViewVisibility = View.GONE
    private lateinit var binding: FragmentCallSettingsBinding
    private val viewModel: CallSettingsViewModel by viewModels()
    private val args: CallSettingsFragmentArgs by navArgs()
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var leaderForCall: String? = null
    private var teachList: ArrayList<String> = arrayListOf()
    private lateinit var alertDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCallSettingsBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.editClassroomButton.setOnClickListener {
            logMessage("Edit classroom button clicked - ${viewModel.classroom}")
            findNavController().navigate(CallSettingsFragmentDirections.actionCallSettingsFragmentToCreateClassroomFragment(viewModel.classroom.value!!))
        }

        binding.deleteClassroomButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage("Are you sure you want to delete the classroom?")
                .setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    logMessage("Classroom deleted ${viewModel.classroom.value}" )
                    viewModel.deleteClassroom()
                }
                .setNegativeButton("No", null)
                .show()
        }

        viewModel.goToHome.observe(viewLifecycleOwner, Observer {
            if(it) findNavController().popBackStack(R.id.classroomFragment, false)
        })

        binding.myStudentsList.adapter = CheckboxNameListAdapter(showCrown = true, leaders = viewModel.classroom.value?.leaders?.map{
            it.phoneNumber
        }!!.toMutableSet())

        binding.selectedContentList.adapter = ContentListAdapter(showRemoveContent = true,
            onContentClickListener = ContentListAdapter.OnClickListener {
                removeContent(it)
            })

        if(args.selectedStudents == null){
            (binding.myStudentsList.adapter as CheckboxNameListAdapter).usersInGroup = args.classroom.students.map{
                it.phoneNumber
            }.toMutableSet()
        }else{
            (binding.myStudentsList.adapter as CheckboxNameListAdapter).usersInGroup.addAll(args.selectedStudents!!)
        }

        viewModel.classroom.observe(viewLifecycleOwner, Observer {
           if (it!=null){
               binding.addContentCs.isEnabled = true
               binding.startCallBtn.isEnabled = true
               binding.editClassroomButton.isEnabled = true
               binding.deleteClassroomButton.isEnabled = true

           }
        })

        binding.addContentCs.setOnClickListener {
            val phoneNumbers = (binding.myStudentsList.adapter as CheckboxNameListAdapter).usersInGroup
            logMessage("Call Settings add content - to classroom: ${viewModel.classroom.value}")
            findNavController().navigate(CallSettingsFragmentDirections.actionCallSettingsFragmentToHomeFragment().setSelectedStudents(phoneNumbers.toTypedArray()).setClassroom(args.classroom).setSelectedContent(
                viewModel.classroom.value?.contents?.map{it.id}?.toTypedArray() //TODO:Null Error fix
            ))
        }

        binding.studentsSearchTextBox.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                val text = binding.studentsSearchTextBox.text.toString().lowercase()
                if(text.isNotEmpty()){
                    logMessage("Call Settings student search text: $text")
                    (binding.myStudentsList.adapter as CheckboxNameListAdapter).submitList(viewModel.classroom.value?.students?.toMutableList()?.filter {
                        it.name.lowercase().contains(text)
                    })
                } else {
                    (binding.myStudentsList.adapter as CheckboxNameListAdapter).submitList(viewModel.classroom.value?.students)
                }
            }
        })

        binding.startCallBtn.setOnClickListener {
            logMessage("Call Settings - start call button clicked")
            val phoneNumbersForCall = (binding.myStudentsList.adapter as CheckboxNameListAdapter).usersInGroup
            if(phoneNumbersForCall.isEmpty()){
                logMessage("Call Settings - no students selected for call")
                AlertDialog.Builder(requireContext())
                    .setMessage("Please select at least one student")
                    .setCancelable(true)
                    .setPositiveButton("OK", null)
                    .show()
            }
            else{
                //val teacherPhoneNumber = Firebase.auth.currentUser!!.phoneNumber.toString().replace("+", "")
                var teacherPhoneNumber = requireActivity().getSharedPreferences("sharedPref", AppCompatActivity.MODE_PRIVATE).getString("phone", null).toString().replace("+", "")
                teacherPhoneNumber = "91$teacherPhoneNumber"
                val teachList = arrayListOf(teacherPhoneNumber)
                teachList.addAll(phoneNumbersForCall)
                var leaderForCall = getLeader()
                if(leaderForCall.isNullOrEmpty()){
                    val selectedStudentsForCall = args.classroom.students.filter {
                        phoneNumbersForCall.contains(it.phoneNumber)
                    }
                    viewModel.updateStudentsForCall(selectedStudentsForCall)
                    val dialogBinding: AssignLeaderBinding = DataBindingUtil.inflate(
                        LayoutInflater.from(context),
                        R.layout.assign_leader,
                        null,
                        false
                    )

                    dialogBinding.viewModel = viewModel
                    dialogBinding.callMyPotentialLeadersList.adapter = CheckboxNameListAdapter(maximumSelections = 2)

                    val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
                    dialogBuilder.setOnDismissListener { }
                    dialogBuilder.setView(dialogBinding.root)
                    alertDialog = dialogBuilder.create()
                    val window = alertDialog.window
                    window?.setBackgroundDrawableResource(R.drawable.rounded_assign_leader)
                    window?.setGravity(Gravity.CENTER)

                    dialogBinding.assignLeadersBtn.setOnClickListener {
                        //check if this is empty
                        val leadersListChosen = (dialogBinding.callMyPotentialLeadersList.adapter as CheckboxNameListAdapter).usersInGroup
                        if(leadersListChosen.isNotEmpty()){
                            leaderForCall = leadersListChosen.first()
                            logMessage("Call Settings - leader selected for call: $leaderForCall") //add name
                        }
                        alertDialog.dismiss()
                        logMessage("Call Settings - people selected for call (teacher + students): $teachList")
                        if(leaderForCall.isNullOrEmpty()){
                            logMessage("Call Settings - no leader selected on assign button click")
                        }
                        findNavController().navigate(
                            CallSettingsFragmentDirections.actionCallSettingsFragmentToCallNav(
                                teachList.toTypedArray(), viewModel.classroom.value!!
                            ).setLeader(leaderForCall)
                        )
                    }

                    dialogBinding.cancelLeadersBtn.setOnClickListener {
                        logMessage("Call Settings - no leader selected on cancel button click")
                        logMessage("Call Settings - people selected for call (teacher + students): $teachList")
                        alertDialog.dismiss()
                        findNavController().navigate(
                            CallSettingsFragmentDirections.actionCallSettingsFragmentToCallNav(
                                teachList.toTypedArray(), viewModel.classroom.value!!
                            ).setLeader(leaderForCall)
                        )
                    }
                    alertDialog.show()
                } else {
                    logMessage("Call Settings - leader selected for call (no popup): $leaderForCall")
                    logMessage("Call Settings - people selected for call (teacher + students): $teachList")
                    findNavController().navigate(
                        CallSettingsFragmentDirections.actionCallSettingsFragmentToCallNav(
                            teachList.toTypedArray(), viewModel.classroom.value!!
                        ).setLeader(leaderForCall)
                    )
                }
                //requirePermissionsAndNavigate()
            }
        }

//        requestPermissionLauncher =
//            registerForActivityResult(
//                ActivityResultContracts.RequestPermission()
//            ) { isGranted: Boolean ->
//                if (isGranted) {
//                    val phoneNumbers = (binding.myStudentsList.adapter as CheckboxNameListAdapter).usersInGroup
//                    val teacherPhoneNumber = Firebase.auth.currentUser!!.phoneNumber.toString().replace("+", "")
//                    teachList = arrayListOf(teacherPhoneNumber)
//                    teachList.addAll(phoneNumbers)
//                    val selectedStudents = args.classroom.students.filter {
//                        phoneNumbers.contains(it.phoneNumber)
//                    }
//
//                    viewModel.updateStudentsForCall(selectedStudents)
//                    val leadersOfGroups = args.classroom.leaders.map{
//                        it.phoneNumber
//                    }
//
//                    val leadersSelectedForCall = phoneNumbers.filter {
//                        leadersOfGroups.contains(it)
//                    }
//
//                    if(leadersSelectedForCall.isEmpty()){
//                        showAssignLeaderDialog()
//                    }
//                    else {
//                        leaderForCall = leadersSelectedForCall[0]
//                        findNavController().navigate(
//                            CallSettingsFragmentDirections.actionCallSettingsFragmentToCallNav(
//                                teachList.toTypedArray(),
//                                viewModel.selectedContentList.value!!.toTypedArray(), args.classroom
//                            ).setLeader(leaderForCall)
//                        )
//                    }
//                    //viewModel.deleteAllSelectedStudents()
//                } else {
//                    Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
//                }
//            }

//        binding.assignLeadersBtn.setOnClickListener {
//            leaderForCall = (binding.callMyPotentialLeadersList.adapter as ContactsListAdapter).usersInGroup.toMutableList().first()
//            binding.addLeadersCardview.visibility = View.INVISIBLE
//
//            //nav to call
//            findNavController().navigate(
//                CallSettingsFragmentDirections.actionCallSettingsFragmentToCallNav(
//                    teachList.toTypedArray(), args.classroom!!
//                ).setContent(args.content).setLeader(leaderForCall)
//            )
//        }

//        binding.cancelLeadersBtn.setOnClickListener {
//            binding.addLeadersCardview.visibility = View.INVISIBLE
//
//            //nav to call
//            findNavController().navigate(
//                CallSettingsFragmentDirections.actionCallSettingsFragmentToCallNav(
//                    teachList.toTypedArray(), args.classroom!!
//                ).setContent(args.content).setLeader(leaderForCall)
//            )
//
//        }

        return binding.root
    }

    private fun removeContent(content: Content) {
        AlertDialog.Builder(requireContext())
            .setMessage("Are you sure you want to remove ${content.title}?")
            .setCancelable(true)
            .setPositiveButton("Yes") { _, _ ->
                val classroom = viewModel.classroom.value!!
                classroom.contentIds = classroom.contentIds.filter {
                    it != content.id
                }
                viewModel.updateClassroomContent(classroom)
                logMessage("Content removed from call settings: ${content.id} (${content.title})")

//                val filteredContent = viewModel.selectedContentList.value!!.filter {
//                    it.id != content.id
//                }
//                //should we make a network call?
//
//                viewModel.setSelectedContentList(filteredContent)
            }
            .setNegativeButton("No", null)
            .show()
    }

//    fun showAssignLeaderDialog(){
//        val dialogBinding: AssignLeaderBinding = DataBindingUtil.inflate(
//            LayoutInflater.from(context),
//            R.layout.assign_leader,
//            null,
//            false
//        )
//        dialogBinding.viewModel = viewModel
//        dialogBinding.callMyPotentialLeadersList.adapter = CheckboxNameListAdapter(maximumSelections = 2)
//
//        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
//        dialogBuilder.setOnDismissListener { }
//        dialogBuilder.setView(dialogBinding.root)
//        alertDialog = dialogBuilder.create()
//        val window = alertDialog.window
//        window?.setBackgroundDrawableResource(R.drawable.rounded_assign_leader)
//        window?.setGravity(Gravity.CENTER)
//
//        dialogBinding.assignLeadersBtn.setOnClickListener {
//            //validate that leaders are assigned
//
//            leaderForCall = (dialogBinding.callMyPotentialLeadersList.adapter as CheckboxNameListAdapter).usersInGroup.toMutableList().first()
//            alertDialog.dismiss()
//
//            //nav to call
//            findNavController().navigate(
//                CallSettingsFragmentDirections.actionCallSettingsFragmentToCallNav(
//                    teachList.toTypedArray(), viewModel.selectedContentList.value!!.toTypedArray(), args.classroom
//                ).setLeader(leaderForCall)
//            )
//        }
//
//        dialogBinding.cancelLeadersBtn.setOnClickListener {
//            alertDialog.dismiss()
//            //nav to call
//            findNavController().navigate(
//                CallSettingsFragmentDirections.actionCallSettingsFragmentToCallNav(
//                    teachList.toTypedArray(), viewModel.selectedContentList.value!!.toTypedArray(), args.classroom
//                ).setLeader(leaderForCall)
//            )
//        }
//        alertDialog.show()
//    }
//    fun showAssignLeaderDialog(){
//        val dialogBinding: AssignLeaderBinding = DataBindingUtil.inflate(
//            LayoutInflater.from(context),
//            R.layout.assign_leader,
//            null,
//            false
//        )
//        dialogBinding.viewModel = viewModel
//        dialogBinding.callMyPotentialLeadersList.adapter = CheckboxNameListAdapter(maximumSelections = 2)
//
//        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
//        dialogBuilder.setOnDismissListener { }
//        dialogBuilder.setView(dialogBinding.root)
//        alertDialog = dialogBuilder.create()
//        val window = alertDialog.window
//        window?.setBackgroundDrawableResource(R.drawable.rounded_assign_leader)
//        window?.setGravity(Gravity.CENTER)
//
//        dialogBinding.assignLeadersBtn.setOnClickListener {
//            //validate that leaders are assigned
//
//            leaderForCall = (dialogBinding.callMyPotentialLeadersList.adapter as CheckboxNameListAdapter).usersInGroup.toMutableList().first()
//            alertDialog.dismiss()
//
//            //nav to call
//            findNavController().navigate(
//                CallSettingsFragmentDirections.actionCallSettingsFragmentToCallNav(
//                    teachList.toTypedArray(), viewModel.selectedContentList.value!!.toTypedArray(), args.classroom
//                ).setLeader(leaderForCall)
//            )
//        }
//
//        dialogBinding.cancelLeadersBtn.setOnClickListener {
//            alertDialog.dismiss()
//            //nav to call
//            findNavController().navigate(
//                CallSettingsFragmentDirections.actionCallSettingsFragmentToCallNav(
//                    teachList.toTypedArray(), viewModel.selectedContentList.value!!.toTypedArray(), args.classroom
//                ).setLeader(leaderForCall)
//            )
//        }
//        alertDialog.show()
//    }
//    private fun requirePermissionsAndNavigate() {
//        when {
//            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
//                    == PackageManager.PERMISSION_GRANTED -> {
//                val phoneNumbers = (binding.myStudentsList.adapter as CheckboxNameListAdapter).usersInGroup
//                val teacherPhoneNumber = Firebase.auth.currentUser!!.phoneNumber.toString().replace("+", "")
//                teachList = arrayListOf(teacherPhoneNumber)
//                teachList.addAll(phoneNumbers)
//                val selectedStudents = args.classroom.students.filter {
//                    phoneNumbers.contains(it.phoneNumber)
//                }
//
//                viewModel.updateStudentsForCall(selectedStudents)
//                val leadersOfGroups = args.classroom.leaders.map{
//                    it.phoneNumber
//                }
//
//                val leadersSelectedForCall = phoneNumbers.filter {
//                    leadersOfGroups.contains(it)
//                }
//
//                if(leadersSelectedForCall.isEmpty()){
//                    //binding.addLeadersCardview.visibility = View.VISIBLE
//                    showAssignLeaderDialog()
//                }
//                else {
//                    leaderForCall = leadersSelectedForCall[0]
//                    findNavController().navigate(
//                        CallSettingsFragmentDirections.actionCallSettingsFragmentToCallNav(
//                            teachList.toTypedArray(), viewModel.selectedContentList.value!!.toTypedArray(), args.classroom
//                        ).setLeader(leaderForCall)
//                    )
//                }
//            }
//            else -> {
//                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
//            }
//        }
//    }

    private fun getLeader(): String? {
        val phoneNumbersForCall = (binding.myStudentsList.adapter as CheckboxNameListAdapter).usersInGroup
        val leadersOfGroups = args.classroom.leaders.map{
            it.phoneNumber
        }
        val leadersSelectedForCall = phoneNumbersForCall.filter {
            leadersOfGroups.contains(it)
        }

        return if(leadersSelectedForCall.isNotEmpty()) leadersSelectedForCall[0] else null
    }

    override fun onStart() {
        //viewModel.refreshStudents()
        lifecycleScope.launch {
            logMessage("onStart")
            viewModel.refreshClassroom()
//            (binding.myStudentsList.adapter as CheckboxNameListAdapter).leaders = viewModel.classroom.value?.leaders?.map{
//                it.phoneNumber
//            }!!.toMutableSet()
//            val classroomStudentsCopy = viewModel.classroom.value?.students?.toMutableList()
//            (binding.myStudentsList.adapter as CheckboxNameListAdapter).submitList(classroomStudentsCopy)
//            (binding.myStudentsList.adapter as CheckboxNameListAdapter).notifyDataSetChanged()
//            binding.myStudentsList.adapter = CheckboxNameListAdapter(showCrown = true, leaders = viewModel.classroom.value?.leaders?.map{
//                it.phoneNumber
//            }!!.toMutableSet())
//
//            if(args.selectedStudents == null){
//                (binding.myStudentsList.adapter as CheckboxNameListAdapter).usersInGroup = args.classroom.students.map{
//                    it.phoneNumber
//                }.toMutableSet()
//            }else{
//                (binding.myStudentsList.adapter as CheckboxNameListAdapter).usersInGroup.addAll(args.selectedStudents!!)
//            }

            binding.studentsSearchTextBox.setText("")
        }
        super.onStart()
    }

    override fun onStop() {
        logMessage("onStop")
        super.onStop()
    }

//    override fun onResume() {
//        viewModel.addSelectedStudents()
//        viewModel.selectedStudents.observe(viewLifecycleOwner, Observer {
//            if(it!=null && it.isNotEmpty()){
//                val adapter = binding.myStudentsList.adapter
//                val layoutManager = binding.myStudentsList.layoutManager
//                binding.myStudentsList.adapter = null
//                binding.myStudentsList.layoutManager = null
//                (adapter as ContactsListAdapter).usersInGroup = it.map{it.phoneNumber}.toMutableSet()
//                binding.myStudentsList.adapter = adapter
//                binding.myStudentsList.layoutManager = layoutManager
//                adapter!!.notifyDataSetChanged()
//            }
//        })
//        super.onResume()
//    }



//    override fun onPause() {
//        runBlocking {
//            val phoneNumbers = (binding.myStudentsList.adapter as ContactsListAdapter).usersInGroup
//            viewModel.setPersistentSelectedStudents(phoneNumbers.toList())
//            super.onPause()
//        }
//    }
}