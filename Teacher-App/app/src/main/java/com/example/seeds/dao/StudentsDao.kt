package com.example.seeds.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seeds.model.Student

@Dao
interface StudentsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun selectedStudents(students: List<Student>)

    @Query("SELECT * FROM selected_students")
    fun getAllSelectedStudents(): LiveData<List<Student>>

    @Query("DELETE FROM selected_students")
    suspend fun deleteAllSelectedStudents()
}