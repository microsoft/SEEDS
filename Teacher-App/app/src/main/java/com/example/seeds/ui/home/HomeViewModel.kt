package com.example.seeds.ui.home

import androidx.lifecycle.*
import com.example.seeds.model.Classroom
import com.example.seeds.model.Content
import com.example.seeds.model.Student
import com.example.seeds.repository.ClassroomRepository
import com.example.seeds.repository.ContentRepository
import com.example.seeds.repository.TeacherRepository
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

    private val _languages = MutableLiveData<List<String>>(listOf())
    val languages: LiveData<List<String>>
        get() = _languages

    private val _experiences = MutableLiveData<List<String>>(listOf())
    val experiences: LiveData<List<String>>
        get() = _experiences

    private val _filtersChosen = MutableLiveData(FilterCriteria())
    val filtersChosen: LiveData<FilterCriteria>
        get() = _filtersChosen

    private val _filteredContent = MutableLiveData<List<Content>>()
    val filteredContent: LiveData<List<Content>>
        get() = _filteredContent

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
            _languages.value = content.map { it.language.lowercase() }.distinct().map { it.capitalize() }
            _experiences.value = content.map { it.type.lowercase() }.distinct().map { it.capitalize() }
            setFiltersChosen(FilterCriteria())
        }
    }

//     fun applyFilters(filters: FilterCriteria) {
//         _filteredContent.value = _allContent.value?.filter {
//             (filters.languages.isEmpty() || it.language.toLowerCase() in filters.languages) &&
//                     (filters.experiences.isEmpty() || it.type.toLowerCase() in filters.experiences)
//         }
//     }

    fun applyFilters(filters: FilterCriteria) {
        _filteredContent.value = _allContent.value?.filter { content ->
            val matchesLanguage = filters.languages.isEmpty() || filters.languages.map { it.lowercase() }.contains(content.language.lowercase())
            val matchesExperience = filters.experiences.isEmpty() || filters.experiences.map { it.lowercase() }.contains(content.type.lowercase())

            matchesLanguage && matchesExperience
        }
    }

    fun removeFilter(filter: String) {
        val currentFilters = _filtersChosen.value ?: FilterCriteria()
        val updatedLanguages = currentFilters.languages.toMutableSet().apply { remove(filter) }
        val updatedExperiences = currentFilters.experiences.toMutableSet().apply { remove(filter) }
        setFiltersChosen(FilterCriteria(updatedLanguages, updatedExperiences))
    }

    fun setFiltersChosen(newFilter: FilterCriteria) {
         _filtersChosen.value = newFilter
     }

    fun clearFilters() {
        //_filtersChosen.value = listOf()
        //_filteredContent.value = allContent.value
        setFiltersChosen(FilterCriteria())
    }

    suspend fun registerUser() {
        teacherRepository.register()
    }
}


//        filtersChosen.observeForever { filters ->
//            _filteredContent.value = applyFilters(_allContent.value ?: emptyList(), filters)
//        }

//
//    init {
//        viewModelScope.launch {
//            val content = contentRepository.getAllContent()
//            _allContent.value = content
//            //_filteredContent.value = content
//            _languages.value =
//                content.map { it.language.lowercase() }.distinct().map { it.capitalize() }
//            _experiences.value = content.map { it.type.lowercase() }.distinct().map {
//                it.capitalize()
//            }
//
////            _filteredContent.value = content
//            if(filtersChosen.value != null) {
//                val langs = _languages.value!!.filter { filtersChosen.value!!.contains(it) }.toMutableSet()
//                val exps = _experiences.value!!.filter { filtersChosen.value!!.contains(it) }.toMutableSet()
//                applyFilters(langs, exps)
//            } else{
//                _filteredContent.value = content
//            }
//        }
//    }

//    fun applyFilters(languages: MutableSet<String>, experiences: MutableSet<String>) {
//        var languagesChosen = languages.map{ it.lowercase()}.toMutableSet()
//        var experiencesChosen = experiences.map{ it.lowercase()}.toMutableSet()
//
//        if (experiences.isEmpty()) experiencesChosen = _experiences.value!!.toMutableSet().map{ it.lowercase()}.toMutableSet()
//        if (languages.isEmpty()) languagesChosen = _languages.value!!.toMutableSet().map{ it.lowercase()} .toMutableSet()
//
//
//        _filteredContent.value = allContent.value?.filter {
//            languagesChosen.contains(it.language.lowercase()) && experiencesChosen.contains(it.type.lowercase())
//        }
//        //INCORRECT -- FIX
//        _filteredContent.value = allContent.value?.filter {
//            languagesChosen.contains(it.language.lowercase()) && experiencesChosen.contains(it.type.lowercase())
//        } // if languagesChosen is empty, it will return all content, if experiencesChosen is empty, it will return all content
//
////        if (experiences.isEmpty()) experiencesChosen = _experiences.value?.toMutableSet()?.map{ it.lowercase()}
////            ?.toMutableSet() ?: mutableSetOf()
////        if (languages.isEmpty()) languagesChosen = _languages.value?.toMutableSet()?.map{ it.lowercase()}
////            ?.toMutableSet() ?: mutableSetOf()
////        _filteredContent.value = allContent.value?.filter {
////            languagesChosen.contains(it.language.lowercase()) && experiencesChosen.contains(it.type.lowercase())
////        }
//    }