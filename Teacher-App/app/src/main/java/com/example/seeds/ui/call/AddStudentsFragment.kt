package com.example.seeds.ui.call

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navGraphViewModels
import com.example.seeds.R
import com.example.seeds.adapters.CheckboxNameListAdapter
import com.example.seeds.databinding.FragmentAddStudentsBinding
import com.example.seeds.ui.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddStudentsFragment : BaseFragment() {
    private lateinit var binding: FragmentAddStudentsBinding
    private val viewModel: CallViewModel by navGraphViewModels(R.id.call_nav) { defaultViewModelProviderFactory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAddStudentsBinding.inflate(inflater)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.myStudentsList.adapter = CheckboxNameListAdapter()
        (binding.myStudentsList.adapter as CheckboxNameListAdapter).submitList(viewModel.studentsNotOnCall)
        binding.viewModel = viewModel

        binding.startCallBtn.setOnClickListener {
            logMessage("Students added to call: ${(binding.myStudentsList.adapter as CheckboxNameListAdapter).usersInGroup}")

            (binding.myStudentsList.adapter as CheckboxNameListAdapter).usersInGroup.map { phoneNumber ->
                val name = viewModel.args.classroom.students.filter {
                    it.phoneNumber == phoneNumber
                }[0].name
                Log.d("ADDSTUDENTSFRAGMENT", "Name: $name, Phone: $phoneNumber")
                viewModel.connectParticipant(name, phoneNumber)
            }
            requireActivity().onBackPressed()
        }

        lifecycleScope.launch {
            delay(300)
            viewModel.refreshCallState()
        }
        return binding.root
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