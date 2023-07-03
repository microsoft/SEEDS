package com.example.seeds.network



import com.example.seeds.model.CallerState
import com.example.seeds.model.Classroom
import com.example.seeds.model.Student
import com.example.seeds.model.StudentCallStatus
import com.example.seeds.utils.ContactUtils



data class ClassroomDto (
    var _id: String?=null,
    var name: String,
    var students: List<String>,
    var leaders: List<String>,
    var contentIds: List<String>
)

fun ClassroomDto.asDomainModel(contactUtils: ContactUtils): Classroom {
    return Classroom(
        _id,
        name,
        contactUtils.getStudentsFromString(students),
        contactUtils.getStudentsFromString(leaders),
        contentIds
    )
}

fun List<ClassroomDto>.asDomainModel(contactUtils: ContactUtils): List<Classroom> {
    return map {
        it.asDomainModel(contactUtils)
    }
}