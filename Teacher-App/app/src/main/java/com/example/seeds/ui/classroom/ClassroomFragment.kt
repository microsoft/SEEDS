package com.example.seeds.ui.classroom

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.seeds.R
import com.example.seeds.adapters.ClassroomListAdapter
import com.example.seeds.databinding.FragmentClassroomBinding
import com.example.seeds.model.Classroom
import com.example.seeds.model.Content
import com.example.seeds.ui.BaseFragment
import com.example.seeds.ui.createclassroom.CreateClassroomFragmentArgs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ClassroomFragment : BaseFragment() {
    private val viewModel: ClassroomViewModel by viewModels()
    override var bottomNavigationViewVisibility = View.VISIBLE
    private lateinit var binding: FragmentClassroomBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentClassroomBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.myClassroomsList.adapter = ClassroomListAdapter(ClassroomListAdapter.OnClickListener{
            logMessage("Classroom clicked: ${it._id} - ${it.name}")
            findNavController().navigate(ClassroomFragmentDirections.actionClassroomFragmentToCallSettingsFragment(it))
        })

        binding.searchTextBox.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                val text = binding.searchTextBox.text.toString().lowercase()
                if(text.isNotEmpty()){
                    logMessage("Classroom search text: $text")
                     val filteredList = viewModel.classrooms.value?.toMutableList()?.filter {
                         it.name.lowercase().contains(text)
                     }
                    (binding.myClassroomsList.adapter as ClassroomListAdapter).submitList(filteredList)
                    if(filteredList != null){
                        if(filteredList.isNotEmpty()){ binding.noGroupsFoundText.visibility = View.INVISIBLE }
                        else{ binding.noGroupsFoundText.visibility = View.VISIBLE }
                    }
                } else {
                    binding.noGroupsFoundText.visibility = View.INVISIBLE
                    (binding.myClassroomsList.adapter as ClassroomListAdapter).submitList(viewModel.classrooms.value)
                }
            }
        })

        binding.createClassroomBtn.setOnClickListener {
            logMessage("Create classroom button clicked")
            val emptyClassroom = Classroom.getNewClassroom()
            findNavController().navigate(ClassroomFragmentDirections.actionClassroomFragmentToCreateClassroomFragment(emptyClassroom))
        }

        return binding.root
    }

    override fun onStart() {
        viewModel.refreshClassrooms()
        //scroll to the top
        logMessage("onStart")
        binding.searchTextBox.setText("")
        super.onStart()
    }

    override fun onStop() {
        logMessage("onStop")
        super.onStop()
    }
}