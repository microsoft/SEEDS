package com.example.seeds.repository

import com.example.seeds.model.Student
import com.example.seeds.network.SeedsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ParticipantRepository @Inject constructor(
    private val network: SeedsService,
) {
    suspend fun getAllParticipants(): List<Student> {
        return withContext(Dispatchers.IO) {
            network.getParticipants().sortedByDescending {
                it.name
            }
        }
    }
}