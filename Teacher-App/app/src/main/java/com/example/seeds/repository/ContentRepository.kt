package com.example.seeds.repository

import android.content.Context
import com.example.seeds.model.Content
import com.example.seeds.model.Student
import com.example.seeds.model.StudentListContainer
import com.example.seeds.network.SeedsService
import com.example.seeds.utils.ContactUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ContentRepository @Inject constructor(
    private val network: SeedsService
) {
    suspend fun getAllContent(): List<Content> {
        return withContext(Dispatchers.IO) {
            network.getAllContent()
        }
    }

    suspend fun getContentsById(contentIds: List<String>): List<Content> {
        return withContext(Dispatchers.IO) {
            network.getContentsById(contentIds)
        }
    }
}