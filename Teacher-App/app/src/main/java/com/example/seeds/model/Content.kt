package com.example.seeds.model

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import se.ansman.kotshi.JsonSerializable

@Parcelize
@JsonSerializable
@JsonClass(generateAdapter = true)
data class Content(
    var title: String,
    var description: String? = null,
    var id: String,
    val type: String,
    val language: String): Parcelable