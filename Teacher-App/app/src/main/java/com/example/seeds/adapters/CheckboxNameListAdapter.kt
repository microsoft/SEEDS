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
import com.example.seeds.databinding.CheckboxNameItemRowBinding

import com.example.seeds.model.Student

class CheckboxNameListAdapter(private val onClickListener: OnClickListener? = null,
                              var leaders: MutableSet<String> = mutableSetOf(),
                              val showCrown: Boolean = false,
                              val showPhoneNumber: Boolean = false,
                              var usersInGroup: MutableSet<String> = mutableSetOf(),
                              val maximumSelections: Int = 5000
) : androidx.recyclerview.widget.ListAdapter<Student, RecyclerView.ViewHolder>(DiffCallBack){

    class OnClickListener(val clickListener: (student: Student) -> Unit) {
        fun onClick(student: Student) = clickListener(student)
    }

    inner class ContactViewHolder(private var binding: CheckboxNameItemRowBinding): RecyclerView.ViewHolder(binding.root) {
        var userCheckbox = binding.userGroupCheckbox

        init {
            userCheckbox.setOnClickListener {
                if (!usersInGroup.contains(binding.student!!.phoneNumber)) {
                    if(usersInGroup.size == maximumSelections) {
                        Toast.makeText(binding.root.context, "Maximum $maximumSelections leader${if(maximumSelections != 1) 's' else ' '} allowed!", Toast.LENGTH_SHORT).show()
                        userCheckbox.isChecked = false
                    } else {
                        userCheckbox.isChecked = true
                        usersInGroup.add(binding.student!!.phoneNumber)
                    }
                } else {
                    userCheckbox.isChecked = false
                    usersInGroup.remove(binding.student!!.phoneNumber)
                }
            }
        }
        fun bind (student: Student) {
            binding.student = student
            binding.showCrown = showCrown && leaders.contains(student.phoneNumber)
            binding.showPhoneNumber = showPhoneNumber
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        //reference: https://stackoverflow.com/questions/48363704/constraintlayout-as-recyclerview-items
        return ContactViewHolder(
            CheckboxNameItemRowBinding.inflate(
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
        contactHolder.userCheckbox.isChecked = usersInGroup.contains(contact.phoneNumber)
        contactHolder.bind(contact)
    }

    companion object DiffCallBack : DiffUtil.ItemCallback<Student>() {

        override fun areItemsTheSame(oldItem: Student, newItem: Student): Boolean {
            return false
        }

        override fun areContentsTheSame(oldItem: Student, newItem: Student): Boolean {
            return false
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

    fun setUsersInGroup(students: List<String>){
        usersInGroup = students.toMutableSet()
    }
}