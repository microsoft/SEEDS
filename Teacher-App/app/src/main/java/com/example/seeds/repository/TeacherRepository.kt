package com.example.seeds.repository

import android.content.Context
import com.example.seeds.model.Student
import com.example.seeds.model.StudentListContainer
import com.example.seeds.network.SeedsService
import com.example.seeds.utils.ContactUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TeacherRepository @Inject constructor(
    context: Context,
    private val network: SeedsService
    ) {

    private val contactUtils = ContactUtils(context)

    suspend fun getMyStudents(): List<Student>{
        return withContext(Dispatchers.IO) {
            contactUtils.getStudentsFromString(network.getStudents())
        }
    }

    suspend fun getMyStudentPhoneNumbers(): List<String>{
        return withContext(Dispatchers.IO) {
            network.getStudents()
        }
    }

    suspend fun setMyStudents(students: List<String>): List<Student> {
        return withContext(Dispatchers.IO) {
            contactUtils.getStudentsFromString(network.setStudents(StudentListContainer(students)))
        }
    }

    suspend fun register() {
        withContext(Dispatchers.IO) {
            network.registerTeacher()
        }
    }
}