package com.example.seeds.ui.classroom

import androidx.lifecycle.*
import com.example.seeds.model.Classroom
import com.example.seeds.model.Content
import com.example.seeds.model.Student
import com.example.seeds.repository.ClassroomRepository
import com.example.seeds.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClassroomViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository, private val contentRepository: ContentRepository
): ViewModel() {

    private val _classrooms = MutableLiveData<List<Classroom>>(null)
    val classrooms: MutableLiveData<List<Classroom>>
        get() = _classrooms

    fun refreshClassrooms() {
        viewModelScope.launch {
            _classrooms.value = classroomRepository.getAllClassrooms()
        }
    }
}