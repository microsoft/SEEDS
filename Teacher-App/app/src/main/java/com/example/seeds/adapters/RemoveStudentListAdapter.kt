package com.example.seeds.adapters

import android.util.Log
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.seeds.R
import com.example.seeds.databinding.RemoveStudentItemRowBinding
import com.example.seeds.model.Student

class RemoveStudentListAdapter(private val onClickListener: OnClickListener? = null) : androidx.recyclerview.widget.ListAdapter<Student, RecyclerView.ViewHolder>(DiffCallBack){

    class OnClickListener(val clickListener: (contact: Student) -> Unit) {
        fun onClick(contact: Student) = clickListener(contact)
    }

    inner class ContactViewHolder(private var binding: RemoveStudentItemRowBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind (contact: Student) {
            binding.student = contact
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        //reference: https://stackoverflow.com/questions/48363704/constraintlayout-as-recyclerview-items
        return ContactViewHolder(
            RemoveStudentItemRowBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        holder.checkBox.isChecked = checkboxStateArray.get(position, false)
        val contact = getItem(position)
        val contactHolder = holder as ContactViewHolder
        onClickListener?.let{ onClickListener ->
            contactHolder.itemView.findViewById<ImageView>(R.id.remove_user_btn).setOnClickListener {
                onClickListener.onClick(contact)
            }
        }
        contactHolder.bind(contact)
    }

    companion object DiffCallBack : DiffUtil.ItemCallback<Student>() {

        override fun areItemsTheSame(oldItem: Student, newItem: Student): Boolean {
            return false //oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Student, newItem: Student): Boolean {
            return false //oldItem == newItem
        }
    }

    override fun submitList(usersList: List<Student>?) {
        val newUsersList = usersList?.toMutableList()
        newUsersList?.sortBy {
            it.name
        }
        //filter
        super.submitList(newUsersList)
    }
}