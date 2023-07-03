package com.example.seeds.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seeds.database.LogEntity

@Dao
interface LogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(log: LogEntity)

    @Query("select * from log")
    suspend fun getAll(): List<LogEntity>

    @Query("delete from log where id in (:logs)")
    suspend fun delete(logs: List<Int>)
}