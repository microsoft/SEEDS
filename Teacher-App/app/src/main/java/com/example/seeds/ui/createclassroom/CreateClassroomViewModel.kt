package com.example.seeds.ui.createclassroom

import android.util.Log
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.lifecycle.*
import com.example.seeds.model.Classroom
import com.example.seeds.model.Student
import com.example.seeds.repository.ClassroomRepository
import com.example.seeds.utils.ContactUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateClassroomViewModel @Inject constructor(val savedStateHandle: SavedStateHandle,
                                                    val classroomRepository: ClassroomRepository) : ViewModel() {
    val classroom = CreateClassroomFragmentArgs.fromSavedStateHandle(savedStateHandle).classroom

    private val _navigateBack = MutableLiveData(false)
    val navigateBack: LiveData<Boolean>
        get() = _navigateBack

    private val _classroomStudents = MutableLiveData(classroom.students)
    val classroomStudents: LiveData<List<Student>>
        get() = _classroomStudents

    private val _classroomLeaders = MutableLiveData(classroom.leaders)
    val classroomLeaders: LiveData<List<Student>>
        get() = _classroomLeaders

    fun doneNavigating() {
        _navigateBack.value = false
    }

    fun updateClassroomStudents(students: List<Student>){
        _classroomStudents.postValue(students)
        classroom.students = students
    }

    fun updateClassroomLeaders(leaders: List<Student>){
        _classroomLeaders.postValue(leaders)
        classroom.leaders = leaders
    }

    fun saveClassroom(classroom: Classroom) {
        viewModelScope.launch {
            classroomRepository.saveClassroom(classroom)
            _navigateBack.value = true
        }
    }
}