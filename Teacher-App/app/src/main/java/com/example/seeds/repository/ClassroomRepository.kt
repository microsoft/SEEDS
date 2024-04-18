package com.example.seeds.repository

import android.content.Context
import com.example.seeds.model.Classroom
import com.example.seeds.model.asDto
import com.example.seeds.network.SeedsService
import com.example.seeds.network.asDomainModel
import com.example.seeds.utils.ContactUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ClassroomRepository @Inject constructor(
    private val network: SeedsService,
    val context: Context,
) {

    private val contactUtils = ContactUtils(context)
    suspend fun getAllClassrooms(): List<Classroom> {
        return withContext(Dispatchers.IO) {
            network.getAllClassrooms().asDomainModel(contactUtils).sortedByDescending {
                it._id
            }
        }
    }

    suspend fun getClassroomById(classId: String): Classroom{
        return withContext(Dispatchers.IO) {
            network.getClassroomById(classId).asDomainModel(contactUtils)
        }
    }

    suspend fun saveClassroom(classroom: Classroom): Classroom {
        return withContext(Dispatchers.IO) {
            network.saveClassroom(classroom.asDto()).asDomainModel(contactUtils)
        }
    }

    suspend fun deleteClassroom(classroom: Classroom) {
        withContext((Dispatchers.IO)) {
            network.deleteClassroom(classroom._id!!)
        }
    }
}