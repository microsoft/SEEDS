package com.example.seeds.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.seeds.adapters.ContentListAdapter
import com.example.seeds.adapters.FilterContentAdapter
import com.example.seeds.databinding.FilterContentBinding
import com.example.seeds.databinding.FragmentHomeBinding
import com.example.seeds.ui.BaseFragment
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

        viewModel.filtersChosen.observe(viewLifecycleOwner, Observer { filterCriteria ->
            binding.filterContentBtn.isClickable = true
            viewModel.applyFilters(filterCriteria)
            setChips(filterCriteria)
        })

        binding.filterContentBtn.setOnClickListener {
            val dialogBinding: FilterContentBinding = DataBindingUtil.inflate(
                layoutInflater,
                R.layout.filter_content,
                null,
                false
            )
            dialogBinding.viewModel = viewModel
            dialogBinding.lifecycleOwner = viewLifecycleOwner

            val currentFilters = viewModel.filtersChosen.value ?: FilterCriteria()
            dialogBinding.languagesList.adapter = FilterContentAdapter(usersInGroup = currentFilters.languages.toMutableSet())
            dialogBinding.experiencesList.adapter = FilterContentAdapter(usersInGroup = currentFilters.experiences.toMutableSet())

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
                viewModel.setFiltersChosen(FilterCriteria(languages, experiences))
                logMessage("Applied filters: $languages - $experiences")
                alertDialog.dismiss()
            }

            dialogBinding.clearFiltersBtn.setOnClickListener {
                logMessage("Cleared filters")
                viewModel.clearFilters()
                alertDialog.dismiss()
            }
            alertDialog.show()
        }

        return binding.root
    }

    fun setChips(filterCriteria: FilterCriteria) {
        binding.filterChips.removeAllViews()
        filterCriteria.languages.forEach { language ->
            addChip(language)
        }
        filterCriteria.experiences.forEach { experience ->
            addChip(experience)
        }
    }

    private fun addChip(filter: String) {
        val chip = Chip(binding.filterChips.context).apply {
            text = filter
            isClickable = false
            isCloseIconVisible = true
            closeIconContentDescription = "Remove $filter filter"
            setChipBackgroundColorResource(R.color.seeds_yellow)
            setTextColor(binding.filterChips.context.getColor(R.color.white))
            setTextAppearance(R.style.filterChips)
            setOnCloseIconClickListener {
                viewModel.removeFilter(filter)
                binding.filterChips.removeView(this)
            }
        }
        binding.filterChips.addView(chip)
    }

    override fun onStart() {
        lifecycleScope.launch {
            logMessage("onStart")
            viewModel.registerUser()
            binding.contentSearchTextBox.setText("")

            viewModel.filtersChosen.value?.let { filterCriteria ->
                setChips(filterCriteria)
                viewModel.applyFilters(filterCriteria)
            } ?: viewModel.applyFilters(FilterCriteria())
        }
        super.onStart()
    }

    override fun onStop() {
        logMessage("onStop")
        super.onStop()
    }
}


//    fun setChips(filters: List<String>) {
//        binding.filterChips.removeAllViews()
//        for (filter in filters) {
//            val chip = Chip(binding.filterChips.context)
//            chip.text = filter
//            chip.isClickable = false
//            chip.isCloseIconVisible = true
//            chip.closeIconContentDescription = "Remove $filter filter"
//            chip.setChipBackgroundColorResource(R.color.seeds_yellow)
//            chip.setTextColor(binding.filterChips.context.getColor(R.color.white))
//            chip.setTextAppearance(R.style.filterChips)
//            chip.setOnCloseIconClickListener {
//                val filtersChosen = viewModel.filtersChosen.value!!.toMutableList()
//                filtersChosen.remove(filter)
//                viewModel.setFiltersChosen(filtersChosen)
//                viewModel.applyFilters(viewModel.languages.value!!.filter { filtersChosen.contains(it) }.toMutableSet(), viewModel.experiences.value!!.filter { filtersChosen.contains(it) }.toMutableSet())
//                binding.filterChips.removeView(chip)
//            }
//            binding.filterChips.addView(chip)
//        }
//    }

//
//            if(viewModel.filtersChosen.value != null) {
//                val langs = viewModel.languages.value!!.filter { viewModel.filtersChosen.value!!.contains(it) }.toMutableSet()
//                val exps = viewModel.experiences.value!!.filter { viewModel.filtersChosen.value!!.contains(it) }.toMutableSet()
//                viewModel.applyFilters(langs, exps)
//                setChips(viewModel.filtersChosen.value!!)
//            } else{
//                viewModel.clearFilters()
//            }

//            viewModel.filtersChosen.value?.let { filtersChosen ->
//                if (filtersChosen.isNotEmpty()) {
//                    viewModel.languages.value?.let { languages ->
//                        val filteredLanguages = languages.filter { filtersChosen.contains(it) }
//                        dialogBinding.languagesList.adapter = FilterContentAdapter(usersInGroup = filteredLanguages.toMutableSet())
//                    }
//                    viewModel.experiences.value?.let { experiences ->
//                        val filteredExperiences = experiences.filter { filtersChosen.contains(it) }
//                        dialogBinding.experiencesList.adapter = FilterContentAdapter(usersInGroup = filteredExperiences.toMutableSet())
//                    }
//                } else {
//                    dialogBinding.languagesList.adapter = FilterContentAdapter()
//                    dialogBinding.experiencesList.adapter = FilterContentAdapter()
//                }
//            }