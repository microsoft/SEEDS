package com.example.seeds.ui.call

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import com.example.seeds.R
import com.example.seeds.databinding.FragmentContactsBinding
import com.example.seeds.model.Student
import com.example.seeds.ui.BaseFragment
import java.util.*
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.seeds.adapters.CheckboxNameListAdapter
import com.example.seeds.utils.ContactUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContactsFragment : BaseFragment() {
    private lateinit var binding : FragmentContactsBinding
    private var students: ArrayList<Student> = arrayListOf()
    private  var tempStudents: ArrayList<Student> = arrayListOf()
    private val args: ContactsFragmentArgs by navArgs()
    private val viewModel: ContactsViewModel by viewModels()
    private lateinit var contactUtils: ContactUtils

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentContactsBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val studentsPhoneNumbers = args.classroom.students.map{
            it.phoneNumber
        }

        setHasOptionsMenu(true)

        contactUtils = ContactUtils(requireContext())
        val allStudentsHash = contactUtils.contactsMap
        students = ArrayList(allStudentsHash.values)

        val usersInGroup = hashMapOf<String, Boolean>() //probably useless
        students.map {
            usersInGroup[it.phoneNumber] = false
        } //probably useless

        tempStudents.addAll(students)

        binding.contactsList.adapter = CheckboxNameListAdapter(
            usersInGroup = studentsPhoneNumbers.toMutableSet(), showPhoneNumber = true
        )

        (binding.contactsList.adapter as CheckboxNameListAdapter).submitList(tempStudents.toList())

        binding.addPeopleConfirmBtn.setOnClickListener {
            val users = (binding.contactsList.adapter as CheckboxNameListAdapter).usersInGroup.toList()
            val classroom = args.classroom
            classroom.students = students.toList().filter {
                users.contains(it.phoneNumber)
            }
            logMessage("Final Students in Classroom: ${classroom.students}")
            findNavController().navigate(ContactsFragmentDirections.actionContactsFragmentToCreateClassroomFragment(classroom))
            //viewModel.setMyStudents(users)
        }

//        viewModel.navigateBack.observe(viewLifecycleOwner, Observer {
//            if(it) {
//                requireActivity().onBackPressed()
//            }
//        })
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.contact_search, menu)
        val item = menu.findItem(R.id.contacts_search_icon)
        val searchView = item?.actionView as SearchView
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            //REFERENCE FOR BUG: https://stackoverflow.com/questions/62707924/indexoutofboundsexception-inconsistency-detected-error-while-scrolling
            override fun onQueryTextChange(newText: String?): Boolean {
                tempStudents.clear()

                val searchText = newText!!.lowercase()
                if(searchText.isNotEmpty()){
                    logMessage("Add Students search text: $searchText")
                    students.forEach {
                        if(it.name.lowercase().contains(searchText)){
                            tempStudents.add(it)
                        }
                    }
                    (binding.contactsList.adapter as CheckboxNameListAdapter).submitList(tempStudents.toList())
                }else{
                    tempStudents.clear()
                    tempStudents.addAll(students)
                    (binding.contactsList.adapter as CheckboxNameListAdapter).submitList(tempStudents.toList())
                }
                return false
            }
        })
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
