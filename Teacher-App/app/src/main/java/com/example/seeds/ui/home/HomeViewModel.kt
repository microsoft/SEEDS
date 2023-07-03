package com.example.seeds.ui.home

import androidx.lifecycle.*
import com.example.seeds.model.Classroom
import com.example.seeds.model.Content
import com.example.seeds.model.Student
import com.example.seeds.repository.ClassroomRepository
import com.example.seeds.repository.ContentRepository
import com.example.seeds.repository.TeacherRepository
import com.example.seeds.ui.call.CallSettingsFragmentArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val teacherRepository: TeacherRepository, private val contentRepository: ContentRepository, private val classroomRepository: ClassroomRepository): ViewModel() {

    val args = HomeFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val showConfirmButton = args.classroom

    private val _students = MutableLiveData<List<Student>>()
    val students: LiveData<List<Student>>
        get() = _students

    private val _allContent = MutableLiveData<List<Content>>()
    val allContent: LiveData<List<Content>>
        get() = _allContent

    private val _filteredContent = MutableLiveData<List<Content>>()
    val filteredContent: LiveData<List<Content>>
        get() = _filteredContent

    private val _languages = MutableLiveData<List<String>>()
    val languages: LiveData<List<String>>
        get() = _languages

    private val _experiences = MutableLiveData<List<String>>()
    val experiences: LiveData<List<String>>
        get() = _experiences

    private val _filtersChosen = MutableLiveData<List<String>>()
    val filtersChosen: LiveData<List<String>>
        get() = _filtersChosen

    private val _navigateBack = MutableLiveData(false)
    val navigateBack: LiveData<Boolean>
        get() = _navigateBack

    fun updateClassroomContent(classroom: Classroom) {
        viewModelScope.launch {
            classroomRepository.saveClassroom(classroom)
            _navigateBack.value = true
        }
    }

    fun doneNavigating() {
        _navigateBack.value = false
    }

    init {
        viewModelScope.launch {
            val content = contentRepository.getAllContent()
            _allContent.value = content
            //_filteredContent.value = content
            _languages.value =
                content.map { it.language.lowercase() }.distinct().map { it.capitalize() }
            _experiences.value = content.map { it.type.lowercase() }.distinct().map {
                it.capitalize()
            }

//            _filteredContent.value = content
            if(filtersChosen.value != null) {
                val langs = _languages.value!!.filter { filtersChosen.value!!.contains(it) }.toMutableSet()
                val exps = _experiences.value!!.filter { filtersChosen.value!!.contains(it) }.toMutableSet()
                filterContent(langs, exps)
            } else{
                _filteredContent.value = content
            }
        }
    }

    fun filterContent(languages: MutableSet<String>, experiences: MutableSet<String>) {
        var languagesChosen = languages.map{ it.lowercase()}.toMutableSet()
        var experiencesChosen = experiences.map{ it.lowercase()}.toMutableSet()
        if (experiences.isEmpty()) experiencesChosen = _experiences.value!!.toMutableSet().map{ it.lowercase()}.toMutableSet()
        if (languages.isEmpty()) languagesChosen = _languages.value!!.toMutableSet().map{ it.lowercase()}.toMutableSet()
        _filteredContent.value = allContent.value?.filter {
            languagesChosen.contains(it.language.lowercase()) && experiencesChosen.contains(it.type.lowercase())
        }
    }

    fun clearFilters() {
        //_filtersChosen.value = listOf()
        _filteredContent.value = allContent.value
    }

    fun setFiltersChosen(filters: List<String>) {
        _filtersChosen.value = filters
    }

    suspend fun registerUser() {
        teacherRepository.register()
    }

    fun refreshStudents() {
        viewModelScope.launch {
            _students.value = teacherRepository.getMyStudents()
        }
    }

}