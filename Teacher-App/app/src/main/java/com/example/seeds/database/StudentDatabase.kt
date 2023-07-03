package com.example.seeds.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.seeds.dao.LogDao
import com.example.seeds.dao.StudentsDao
import com.example.seeds.model.Student


@Database(entities = [Student::class, LogEntity::class], version = 1, exportSchema = false)
abstract class StudentDatabase : RoomDatabase() {

    abstract val studentDao : StudentsDao
    abstract val logDao: LogDao

    companion object {
        @Volatile
        private var INSTANCE : StudentDatabase? = null

        fun create(context: Context): StudentDatabase {
            synchronized(this){
                var instance = INSTANCE

                if(instance == null){
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        StudentDatabase::class.java,
                        "quiz-db"
                    ).fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}