package com.example.seeds.adapters

import com.example.seeds.databinding.FilterItemRowBinding
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
import com.example.seeds.databinding.CheckboxNameItemRowBinding

class FilterContentAdapter(var usersInGroup: MutableSet<String> = mutableSetOf(), val maximumSelections: Int = 5000) : androidx.recyclerview.widget.ListAdapter<String, RecyclerView.ViewHolder>(DiffCallBack){

    inner class ContactViewHolder(private var binding: FilterItemRowBinding): RecyclerView.ViewHolder(binding.root) {
        var userCheckbox = binding.userGroupCheckbox

        init {
            userCheckbox.setOnClickListener {
                if (!usersInGroup.contains(binding.name!!)) {
                    userCheckbox.isChecked = true
                    usersInGroup.add(binding.name!!)
                } else {
                    userCheckbox.isChecked = false
                    usersInGroup.remove(binding.name!!)
                }
            }
        }
        fun bind (name: String) {
            binding.name = name
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        //reference: https://stackoverflow.com/questions/48363704/constraintlayout-as-recyclerview-items
        return ContactViewHolder(
            FilterItemRowBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val name = getItem(position)
        val contactHolder = holder as ContactViewHolder
        contactHolder.userCheckbox.isChecked = usersInGroup.contains(name)
        contactHolder.bind(name)
    }

    companion object DiffCallBack : DiffUtil.ItemCallback<String>() {

        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }

    override fun submitList(usersList: List<String>?) {
        val newUsersList = usersList?.toMutableList()
        newUsersList?.sortBy { it }
        super.submitList(newUsersList)
    }

    fun setUsersInGroup(students: List<String>){
        usersInGroup = students.toMutableSet()
    }
}