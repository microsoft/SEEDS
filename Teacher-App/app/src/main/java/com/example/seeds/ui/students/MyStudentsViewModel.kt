package com.example.seeds.ui.students

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seeds.model.Student
import com.example.seeds.repository.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyStudentsViewModel @Inject constructor(private val teacherRepository: TeacherRepository): ViewModel() {

    private val _students = MutableLiveData<List<Student>>(null)
    val students: MutableLiveData<List<Student>>
        get() = _students

    fun refreshStudents() {
        viewModelScope.launch {
            _students.value = teacherRepository.getMyStudents()
        }
    }

    fun setMyStudents(students: List<String>){
        viewModelScope.launch {
            teacherRepository.setMyStudents(students)
        }
    }
}