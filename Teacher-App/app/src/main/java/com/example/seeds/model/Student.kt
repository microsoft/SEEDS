package com.example.seeds.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import se.ansman.kotshi.JsonSerializable

@Parcelize
@Entity(tableName = "selected_students")
@JsonSerializable
@JsonClass(generateAdapter = true)
data class Student(
    @PrimaryKey
    var phoneNumber: String,
    var name: String,
    var isLeader: Boolean = false): Parcelable

@JsonSerializable
@JsonClass(generateAdapter = true)
data class StudentListContainer(
    var students: List<String>
    )