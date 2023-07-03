package com.example.seeds.ui.call

import android.util.Log
import androidx.lifecycle.*
import com.example.seeds.model.Classroom
import com.example.seeds.model.Content
import com.example.seeds.model.Student
import com.example.seeds.repository.ClassroomRepository
import com.example.seeds.repository.ContentRepository
import com.example.seeds.repository.StudentRepository
import com.example.seeds.repository.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallSettingsViewModel @Inject constructor(savedStateHandle: SavedStateHandle,
                                                private val studentRepository: StudentRepository,
                                                private val classroomRepository: ClassroomRepository,
                                                private val contentRepository: ContentRepository): ViewModel(){

    val args = CallSettingsFragmentArgs.fromSavedStateHandle(savedStateHandle)

//    private val _students = MutableLiveData<List<Student>>()
//    val students: LiveData<List<Student>>
//        get() = _students

    private val _classroom = MutableLiveData<Classroom>(args.classroom)
    val classroom: LiveData<Classroom>
        get() = _classroom

    private val _studentsForCall = MutableLiveData<List<Student>>()
    val studentsForCall: LiveData<List<Student>>
        get() = _studentsForCall

//    private val _selectedContentList = MutableLiveData<List<Content>>()
//    val selectedContentList: LiveData<List<Content>>
//        get() = _selectedContentList

    fun updateStudentsForCall(students: List<Student>){
        _studentsForCall.postValue(students)
    }

    private val _goToHome = MutableLiveData(false)
    val goToHome: LiveData<Boolean>
        get() = _goToHome

    //private val _selectedStudents = MutableLiveData<List<Student>>()
    //var selectedStudents: LiveData<List<Student>> = studentRepository.getSelectedStudents()
       // get() = _selectedStudents
    //val selectedContent: List<Content> = args.content.toList()

//    fun setSelectedContentList(content: List<Content>){
//        _selectedContentList.value = content
//    }

    fun updateClassroomContent(classroom: Classroom) {
        viewModelScope.launch {
            classroomRepository.saveClassroom(classroom)
            refreshClassroom()
        }
    }

    fun refreshClassroom(){
        viewModelScope.launch {
            val classroomById = classroomRepository.getClassroomById(args.classroom._id!!)
            Log.d("ONSTARTCALLEDCALLSETTINGSCLASSROOMREFRESHED", classroomById.toString())
            if(classroomById.contentIds.isNotEmpty())
                classroomById.contents = contentRepository.getContentsById(classroomById.contentIds)
            else
                classroomById.contents = listOf()
            _classroom.postValue(classroomById)
            //_selectedContentList.postValue(classroomById.contents!!)
        }
    }

    fun deleteClassroom() {
        viewModelScope.launch {
            classroomRepository.deleteClassroom(classroom.value!!)
            _goToHome.postValue(true)
        }
    }
}