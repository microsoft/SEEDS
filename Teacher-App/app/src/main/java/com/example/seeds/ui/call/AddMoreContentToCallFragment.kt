package com.example.seeds.ui.call

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.seeds.R
import com.example.seeds.adapters.ContentListAdapter
import com.example.seeds.adapters.FilterContentAdapter
import com.example.seeds.databinding.FilterContentBinding
import com.example.seeds.databinding.FilterContentOnCallBinding
import com.example.seeds.databinding.FragmentAddContentToCallBinding
import com.example.seeds.databinding.FragmentAddMoreContentToCallBinding
import com.example.seeds.ui.BaseFragment
import com.example.seeds.ui.home.HomeFragmentDirections
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddMoreContentToCallFragment : BaseFragment() {
    private lateinit var binding: FragmentAddMoreContentToCallBinding
    private val viewModel: CallViewModel by navGraphViewModels(R.id.call_nav) { defaultViewModelProviderFactory }
    private lateinit var alertDialog: AlertDialog


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddMoreContentToCallBinding.inflate(inflater)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        // Inflate the layout for this fragment
        binding.contentList.adapter = ContentListAdapter(showCheckbox = true)

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

        binding.confirmContentBtn.setOnClickListener {
            val contentChosen = (binding.contentList.adapter as ContentListAdapter).usersInGroup
            val additionalContentChosen = viewModel.allContent.value?.filter{
                contentChosen.contains(it.id)
            }
            logMessage("Additonal content chosen during call: $additionalContentChosen - ${additionalContentChosen?.map{it.title}}")
            val totalContent = viewModel.selectedContentList.value!!.toMutableList()
            totalContent.addAll(additionalContentChosen!!)
            Log.d("ContentChosen", contentChosen.toString())
            Log.d("AdditionalContentChosen", additionalContentChosen.toString())
            Log.d("totalContent", totalContent.toString())
            viewModel.setSelectedContentList(totalContent.toList())
            if(totalContent.isNotEmpty()) {
                viewModel.setSelectedContent(totalContent[0])
            }
            requireActivity().onBackPressed()
        }

        binding.filterContentBtn.setOnClickListener {
            val dialogBinding: FilterContentOnCallBinding = DataBindingUtil.inflate(
                layoutInflater,
                R.layout.filter_content_on_call,
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