package com.example.seeds.repository

import androidx.lifecycle.LiveData
import com.example.seeds.dao.StudentsDao
import com.example.seeds.model.Student
import javax.inject.Inject

class StudentRepository @Inject constructor(private val database: StudentsDao){

    fun getSelectedStudents(): LiveData<List<Student>> = database.getAllSelectedStudents()

    suspend fun addSelectedStudents(students: List<Student>){
        database.selectedStudents(students)
    }

    suspend fun deleteSelectedStudents(){
        database.deleteAllSelectedStudents()
    }
}