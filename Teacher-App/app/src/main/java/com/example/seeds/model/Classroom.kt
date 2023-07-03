package com.example.seeds.model

import android.os.Parcelable
import com.example.seeds.network.ClassroomDto
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import se.ansman.kotshi.JsonSerializable

@Parcelize
@JsonSerializable
@JsonClass(generateAdapter = true)
data class Classroom(
    var _id: String? = null,
    var name: String,
    var students: List<Student>,
    var leaders: List<Student>,
    var contentIds: List<String>,
    var contents: List<Content>? = null
    ): Parcelable{
    companion object {
        fun getNewClassroom(_id:String? = null,
                        name: String ="",
                        students: List<Student> = listOf(),
                        leaders: List<Student> = listOf(),
                        contentIds: List<String> = listOf()): Classroom {
            return Classroom(_id = _id, name=name, students = students, leaders = leaders, contentIds = contentIds)
        }
    }
}

fun Classroom.asDto(): ClassroomDto {
    return ClassroomDto(
        _id,
        name,
        students.map{
            it.phoneNumber
        },
        leaders.map{
            it.phoneNumber
        },
        contentIds
    )
}

