package com.example.seeds.ui.call

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seeds.repository.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(private val teacherRepository: TeacherRepository): ViewModel(){

    private val _navigateBack = MutableLiveData<Boolean>(false)
    val navigateBack: LiveData<Boolean>
        get() = _navigateBack

    fun setMyStudents(students: List<String>){
        viewModelScope.launch {
            teacherRepository.setMyStudents(students)
            _navigateBack.value = true
        }
    }

}