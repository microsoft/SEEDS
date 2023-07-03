package com.example.seeds.ui.call

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.seeds.R
import com.example.seeds.adapters.ContentListAdapter
import com.example.seeds.databinding.FragmentAddContentToCallBinding
import com.example.seeds.ui.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddContentToCallFragment : BaseFragment() {
    private lateinit var binding: FragmentAddContentToCallBinding
    private val viewModel: CallViewModel by navGraphViewModels(R.id.call_nav) { defaultViewModelProviderFactory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAddContentToCallBinding.inflate(inflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.contentList.adapter = ContentListAdapter(
            ContentListAdapter.OnClickListener{
                Log.d("Content Clicked", "Content Clicked: ${it.id} - ${it.title} - $it")
                logMessage("Content Clicked: ${it.id} - ${it.title} - $it")
                viewModel.setSelectedContent(it)
                viewModel.playAudio(it.id)
                viewModel.startedAudio = true
                requireActivity().onBackPressed()
            }
        )

//        binding.contentListAll.adapter = ContentListAdapter(showCheckbox = true)
        
        viewModel.selectedContentList.observe(viewLifecycleOwner) {
            if (it != null) {
                val selectedContentListIds = it.map {
                    it.id
                }

                val filteredListContent = viewModel.allContent.value?.filter {
                    !selectedContentListIds.contains(it.id)
                }
                viewModel.setAllContentList(filteredListContent!!)

                if(it.isNotEmpty()){
                    viewModel.setSelectedContent(it[0])
                }
            }
        }

        binding.addMoreContentBtn.setOnClickListener {
            findNavController().navigate(AddContentToCallFragmentDirections.actionAddContentToCallFragment2ToAddMoreContentToCallFragment())
        }

//        binding.contentSearchTextBox.addTextChangedListener(object: TextWatcher {
//            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//
//            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//
//            override fun afterTextChanged(p0: Editable?) {
//                val text = binding.contentSearchTextBox.text.toString().lowercase()
//                if(text.isNotEmpty()){
//                    logMessage("Content searched during call: $text")
//                    (binding.contentListAll.adapter as ContentListAdapter).submitList(viewModel.allContent.value?.toMutableList()?.filter {
//                        it.title.lowercase().contains(text)
//                    })
//                } else {
//                    (binding.contentListAll.adapter as ContentListAdapter).submitList(viewModel.allContent.value)
//                }
//            }
//        })

//        binding.addContentButton.setOnClickListener {
//            val contentChosen = (binding.contentListAll.adapter as ContentListAdapter).usersInGroup
//            val additionalContentChosen = viewModel.allContent.value?.filter{
//                contentChosen.contains(it.id)
//            }
//            logMessage("Additonal content chosen during call: $additionalContentChosen - ${additionalContentChosen?.map{it.title}}")
//            val totalContent = viewModel.selectedContentList.value!!.toMutableList()
//            totalContent.addAll(additionalContentChosen!!)
//            Log.d("ContentChosen", contentChosen.toString())
//            Log.d("AdditionalContentChosen", additionalContentChosen.toString())
//            Log.d("totalContent", totalContent.toString())
//            (binding.contentListAll.adapter as ContentListAdapter).usersInGroup = mutableSetOf()
//            viewModel.setSelectedContentList(totalContent.toList())
//            requireActivity().onBackPressed()
//        }

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