package com.example.seeds

import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.seeds.adapters.*
import com.example.seeds.model.*
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@BindingAdapter("contentData")
fun bindContentRecyclerView(recyclerView: RecyclerView, data: List<Content>?) {
    val adapter = recyclerView.adapter as ContentListAdapter
    Log.d("Content", data.toString())
    adapter.submitList(data)
}

@BindingAdapter("classroomData")
fun bindClassroomRecyclerView(recyclerView: RecyclerView, data: List<Classroom>?) {
    val adapter = recyclerView.adapter as ClassroomListAdapter
    Log.d("Classrooms", data.toString())
    adapter.submitList(data)
}

@BindingAdapter("studentData")
fun bindStudentRecyclerView(recyclerView: RecyclerView, data: List<Student>?) {
    if(recyclerView.adapter != null) {
        val adapter = recyclerView.adapter as RemoveStudentListAdapter
        Log.d("Student", data.toString())
        adapter.submitList(data)
    }

}

@BindingAdapter("checkboxNamesData")
fun bindCheckboxNamesRecyclerView(recyclerView: RecyclerView, data: List<Student>?) {
    if(recyclerView.adapter != null) {
        val adapter = recyclerView.adapter as CheckboxNameListAdapter
        Log.d("Student", data.toString())
        adapter.submitList(data)
    }

}

@BindingAdapter("filterContentData")
fun bindFilterContentRecyclerView(recyclerView: RecyclerView, data: List<String>?) {
    if(recyclerView.adapter != null) {
        val adapter = recyclerView.adapter as FilterContentAdapter
        Log.d("Data", data.toString())
        adapter.submitList(data)
    }
}

//@BindingAdapter("filters")
//fun bindFilterChips(chipsLayout: ChipGroup, filters: List<String>?){
//    if(filters != null) {
//        for (filter in filters) {
//            val chip = Chip(chipsLayout.context)
//            chip.text = filter
//            chip.isCloseIconVisible = true
//            chip.setChipBackgroundColorResource(R.color.seeds_yellow)
//            chip.setTextColor(chipsLayout.context.getColor(R.color.white))
//            chip.setTextAppearance(R.style.filterChips)
//
////            chip.isChipIconVisible = true
//            chip.setOnCloseIconClickListener {
//                chipsLayout.removeView(chip)
//            }
//            chipsLayout.addView(chip)
//        }
//    }
//}

@BindingAdapter("studentCallStatusData", "teacherPhoneNumber")
fun bindStudentCallStatusRecyclerView(recyclerView: RecyclerView, data: List<StudentCallStatus>?, teacherPhoneNumber: String) {
    val adapter = recyclerView.adapter as StudentCallStatusAdapter
//    val teacherPhoneNumber = Firebase.auth.currentUser!!.phoneNumber.toString().replace("+", "")
    Log.d("studentCallStatusData", data.toString())
    data?.let {
        adapter.submitList(it.toMutableList().filter { state ->
            state.callerState != CallerState.COMPLETED
        }.filter { state ->
            state.phoneNumber != teacherPhoneNumber
        })
    }
}

@BindingAdapter("imageDrawable")
fun bindDrawableImageView(imageView: ImageView, drawable: Drawable) {
    imageView.setImageDrawable(drawable)
}