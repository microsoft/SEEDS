package com.example.seeds.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.seeds.R
import com.example.seeds.adapters.CheckboxNameListAdapter
import com.example.seeds.adapters.ContentListAdapter
import com.example.seeds.adapters.FilterContentAdapter
import com.example.seeds.databinding.FilterContentBinding
import com.example.seeds.databinding.FragmentHomeBinding
import com.example.seeds.ui.BaseFragment
import com.example.seeds.ui.classroom.ClassroomFragmentDirections
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment() {
    private lateinit var binding : FragmentHomeBinding
    private val viewModel: HomeViewModel by viewModels()
    private val args: HomeFragmentArgs by navArgs()
    override var bottomNavigationViewVisibility = View.VISIBLE
    private lateinit var alertDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        bottomNavigationViewVisibility = if(args.classroom != null) View.GONE else View.VISIBLE
        val selectedContentIds = if(args.selectedContent != null) args.selectedContent!!.toMutableSet() else mutableSetOf<String>()

        binding.contentList.adapter = ContentListAdapter(ContentListAdapter.OnClickListener {
            if(args.classroom == null) {
                logMessage("Content clicked: ${it.title} - ${it.id}")
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeFragmentToContentDetailsFragment2(it)
                )
            }
        }, showCheckbox = args.classroom != null, usersInGroup = selectedContentIds)

        binding.contentSearchTextBox.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                val text = binding.contentSearchTextBox.text.toString().lowercase()
                if(text.isNotEmpty()){
                     logMessage("Content search text: $text")
                    (binding.contentList.adapter as ContentListAdapter).submitList(viewModel.filteredContent.value?.toMutableList()?.filter {
                        it.title.lowercase().contains(text)
                    })
                } else {
                    (binding.contentList.adapter as ContentListAdapter).submitList(viewModel.filteredContent.value)
                }
            }
        })

        viewModel.navigateBack.observe(viewLifecycleOwner, Observer {
            if(it){
                val classroom = args.classroom
                classroom!!.contentIds = (binding.contentList.adapter as ContentListAdapter).usersInGroup.toList()
                val contentChosen = viewModel.allContent.value?.filter { classroom.contentIds.contains(it.id) }
                logMessage("Navigating back to call settings with content: ${classroom.contentIds} ${contentChosen?.map { it.title }}")
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeFragmentToCallSettingsFragment(classroom).setSelectedStudents(args.selectedStudents)
                )
                viewModel.doneNavigating()
            }
        })

        binding.confirmCotentBtn.setOnClickListener {
            val contents = (binding.contentList.adapter as ContentListAdapter).usersInGroup
            val classroom = args.classroom
            classroom!!.contentIds = contents.toList()
            viewModel.updateClassroomContent(classroom)
        }

        binding.filterContentBtn.setOnClickListener {
            val dialogBinding: FilterContentBinding = DataBindingUtil.inflate(
                layoutInflater,
                R.layout.filter_content,
                null,
                false
            )
            dialogBinding.viewModel = viewModel
            dialogBinding.lifecycleOwner = viewLifecycleOwner

            if(viewModel.filtersChosen.value != null){
                dialogBinding.languagesList.adapter = FilterContentAdapter(usersInGroup = viewModel.languages.value!!.filter { viewModel.filtersChosen.value!!.contains(it) }.toMutableSet())
                dialogBinding.experiencesList.adapter = FilterContentAdapter(usersInGroup = viewModel.experiences.value!!.filter { viewModel.filtersChosen.value!!.contains(it) }.toMutableSet())
            } else {
                dialogBinding.languagesList.adapter = FilterContentAdapter()
                dialogBinding.experiencesList.adapter = FilterContentAdapter()
            }

            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
            dialogBuilder.setOnDismissListener { }
            dialogBuilder.setView(dialogBinding.root)
            alertDialog = dialogBuilder.create()
            val window = alertDialog.window
            window?.setBackgroundDrawableResource(R.drawable.rounded_assign_leader)
            window?.setGravity(Gravity.CENTER)

            dialogBinding.applyFiltersBtn.setOnClickListener {
                val languages = (dialogBinding.languagesList.adapter as FilterContentAdapter).usersInGroup
                val experiences = (dialogBinding.experiencesList.adapter as FilterContentAdapter).usersInGroup
                viewModel.setFiltersChosen(languages.union(experiences).toList())
                setChips(languages.union(experiences).toList())
                logMessage("Applied filters: $languages - $experiences")
                viewModel.filterContent(languages, experiences)
                alertDialog.dismiss()
            }

            dialogBinding.clearFiltersBtn.setOnClickListener {
                viewModel.setFiltersChosen(listOf())
                logMessage("Cleared filters")
                setChips(listOf())
                viewModel.clearFilters()
                alertDialog.dismiss()
            }
            alertDialog.show()
        }

        return binding.root
    }

    fun setChips(filters: List<String>) {
        binding.filterChips.removeAllViews()
        for (filter in filters) {
            val chip = Chip(binding.filterChips.context)
            chip.text = filter
            chip.isClickable = false
            chip.isCloseIconVisible = true
            chip.closeIconContentDescription = "Remove $filter filter"
            chip.setChipBackgroundColorResource(R.color.seeds_yellow)
            chip.setTextColor(binding.filterChips.context.getColor(R.color.white))
            chip.setTextAppearance(R.style.filterChips)
            chip.setOnCloseIconClickListener {
                val filtersChosen = viewModel.filtersChosen.value!!.toMutableList()
                filtersChosen.remove(filter)
                viewModel.setFiltersChosen(filtersChosen)
                viewModel.filterContent(viewModel.languages.value!!.filter { filtersChosen.contains(it) }.toMutableSet(), viewModel.experiences.value!!.filter { filtersChosen.contains(it) }.toMutableSet())
                binding.filterChips.removeView(chip)
            }
            binding.filterChips.addView(chip)
        }
    }

    override fun onStart() {
        lifecycleScope.launch {
            logMessage("onStart")
            viewModel.registerUser()
            viewModel.refreshStudents()
            binding.contentSearchTextBox.setText("")
            if(viewModel.filtersChosen.value != null) {
                val langs = viewModel.languages.value!!.filter { viewModel.filtersChosen.value!!.contains(it) }.toMutableSet()
                val exps = viewModel.experiences.value!!.filter { viewModel.filtersChosen.value!!.contains(it) }.toMutableSet()
                viewModel.filterContent(langs, exps)
                setChips(viewModel.filtersChosen.value!!)
            } else{
                viewModel.clearFilters()
            }
        }
        super.onStart()
    }

    override fun onStop() {
        logMessage("onStop")
        super.onStop()
    }
}