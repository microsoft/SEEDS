package com.example.seeds.ui.createclassroom

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.seeds.R
import com.example.seeds.adapters.CheckboxNameListAdapter
import com.example.seeds.adapters.RemoveStudentListAdapter
import com.example.seeds.databinding.AddLeadersBinding
import com.example.seeds.databinding.AssignLeaderBinding
import com.example.seeds.databinding.FragmentCreateClassroomBinding
import com.example.seeds.model.Student
import com.example.seeds.ui.BaseFragment
import com.example.seeds.ui.call.CallSettingsFragmentDirections
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateClassroomFragment : BaseFragment() {
    private lateinit var binding: FragmentCreateClassroomBinding
    private val viewModel: CreateClassroomViewModel by viewModels()
    private val args : CreateClassroomFragmentArgs by navArgs()
    private lateinit var alertDialog: android.app.AlertDialog
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCreateClassroomBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.classroomMyStudentsList.adapter = RemoveStudentListAdapter(RemoveStudentListAdapter.OnClickListener{
            removeUser(it)
        })

        binding.leadersList.adapter  = RemoveStudentListAdapter(RemoveStudentListAdapter.OnClickListener{
            removeLeader(it)
        })

        binding.addStudentsBtn.setOnClickListener {
            logMessage("Add students button clicked - ${viewModel.classroom}")
            val classroom = viewModel.classroom
            classroom.name = binding.classroomNameEdit.text.toString()
            findNavController().navigate(CreateClassroomFragmentDirections.actionCreateClassroomFragmentToContactsFragment(classroom))
        }
        // Inflate the layout for this fragment
        binding.saveClassroomBtn.setOnClickListener {
            val classroom = viewModel.classroom
            classroom.name = binding.classroomNameEdit.text.toString()
            if(classroom.name.isEmpty()){
                logMessage("Save classroom without title")
                AlertDialog.Builder(requireContext())
                    .setMessage("Title cannot be empty")
                    .setCancelable(true)
                    .setPositiveButton("OK", null)
                    .show()
            }
            else {
                logMessage("Save classroom: ${classroom._id} - ${classroom.name} - $classroom")
                viewModel.saveClassroom(classroom)
            }
        }

        binding.addLeadersBtn.setOnClickListener {
            logMessage("Add leaders button clicked")
            if(args.classroom.students.isNotEmpty()){
                showAddLeaderDialog()
            }
        }

        viewModel.navigateBack.observe(viewLifecycleOwner, Observer {
            if(it){
                requireActivity().onBackPressed()
                viewModel.doneNavigating()
            }
        })

        return binding.root
    }

    private fun removeUser(student: Student) {
        AlertDialog.Builder(requireContext())
            .setMessage("Are you sure you want to remove ${student.name}?")
            .setCancelable(true)
            .setPositiveButton("Yes") { _, _ ->
                val newStudents = viewModel.classroomStudents.value!!.filter {
                    it.phoneNumber != student.phoneNumber
                }

                val newLeaders = viewModel.classroomLeaders.value!!.filter {
                    it.phoneNumber != student.phoneNumber
                }

//                (binding.classroomMyPotentialLeadersList.adapter as RemoveStudentListAdapter).usersInGroup = newLeaders.map{
//                    it.phoneNumber
//                }.toMutableSet()

                viewModel.updateClassroomStudents(viewModel.classroomStudents.value!!.toMutableList().toList())
                viewModel.updateClassroomLeaders(newLeaders)
//                viewModel.classroom.students = newStudents
//                (binding.classroomMyStudentsList.adapter as ContactsListAdapter).submitList(newStudents)
                viewModel.updateClassroomStudents(newStudents)
                logMessage("Remove student from classroom ${student.phoneNumber} ${student.name}")
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun removeLeader(student: Student) {
        AlertDialog.Builder(requireContext())
            .setMessage("Are you sure you want to remove ${student.name} as a leader?")
            .setCancelable(true)
            .setPositiveButton("Yes") { _, _ ->
                val newLeaders = viewModel.classroomLeaders.value!!.filter {
                    it.phoneNumber != student.phoneNumber
                }

//                (binding.classroomMyPotentialLeadersList.adapter as RemoveStudentListAdapter).usersInGroup = newLeaders.map{
//                    it.phoneNumber
//                }.toMutableSet()

                viewModel.updateClassroomStudents(viewModel.classroomStudents.value!!.toMutableList().toList())
                viewModel.updateClassroomLeaders(newLeaders)
                logMessage("Remove leader from classroom ${student.phoneNumber} ${student.name}")
            }
            .setNegativeButton("No", null)
            .show()
    }



    fun showAddLeaderDialog(){
        val dialogBinding: AddLeadersBinding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.add_leaders,
            null,
            false
        )
        dialogBinding.viewModel = viewModel

        //        binding.classroomMyPotentialLeadersList.adapter = RemoveStudentListAdapter(showCheckBox = true, )
        dialogBinding.classroomMyPotentialLeadersList.adapter = CheckboxNameListAdapter(usersInGroup = viewModel.classroom.leaders.map{
           it.phoneNumber
        }.toMutableSet(), maximumSelections = 2)

        val dialogBuilder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(requireContext())
        dialogBuilder.setOnDismissListener { }
        dialogBuilder.setView(dialogBinding.root)
        alertDialog = dialogBuilder.create()
        val window = alertDialog.window
        window?.setBackgroundDrawableResource(R.drawable.rounded_assign_leader)
        window?.setGravity(Gravity.CENTER)

        dialogBinding.addLeadersBtn.setOnClickListener {
            val leadersPhoneNumbers = (dialogBinding.classroomMyPotentialLeadersList.adapter as CheckboxNameListAdapter).usersInGroup.toList()
            val leaders = args.classroom.students.filter {
                leadersPhoneNumbers.contains(it.phoneNumber)
            }
            logMessage("Assign leaders: $leaders - ${leaders.map{it.name}} - ${leaders.map{it.phoneNumber}}}")
            viewModel.updateClassroomLeaders(leaders)
            alertDialog.dismiss()
        }

        dialogBinding.cancelLeadersBtn.setOnClickListener {
            logMessage("Assign leaders cancelled")
            alertDialog.dismiss()
        }
        alertDialog.show()
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


