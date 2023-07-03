package com.example.seeds.converters

import androidx.room.TypeConverter
import com.example.seeds.ApplicationJsonAdapterFactory
import com.example.seeds.model.Student
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.ParameterizedType

class ClassroomDataConverter {
    private val moshi =  Moshi.Builder()
        //.add(KotlinJsonAdapterFactory())
        .add(ApplicationJsonAdapterFactory)
        .build()
    private val listStudent : ParameterizedType = Types.newParameterizedType(List::class.java, Student::class.java)
    private val adapter: JsonAdapter<List<Student>> = moshi.adapter(listStudent)

    @TypeConverter
    fun listMyModelToJsonStr(listMyModel: List<Student>): String {
        return adapter.toJson(listMyModel)
    }

    @TypeConverter
    fun jsonStrToListMyModel(jsonStr: String): List<Student> {
        return jsonStr.let { adapter.fromJson(jsonStr)!! }
    }
}