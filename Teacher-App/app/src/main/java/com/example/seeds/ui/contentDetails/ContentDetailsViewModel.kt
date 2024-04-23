package com.example.seeds.ui.contentDetails

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seeds.model.Classroom
import com.example.seeds.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContentDetailsViewModel @Inject constructor(val savedStateHandle: SavedStateHandle, val contentRepository: ContentRepository): ViewModel() {

    val content = ContentDetailsFragmentArgs.fromSavedStateHandle(savedStateHandle).content


    private val _contentUrl = MutableLiveData<String>(null)
    val contentUrl: MutableLiveData<String>
        get() = _contentUrl

    fun refreshContentUrl() {
        val src = "https://seedscontent.blob.core.windows.net/output-original/${content.id}.mp3"
        viewModelScope.launch {
            _contentUrl.value = contentRepository.getContentSas(
                src
            )
        }
    }
}