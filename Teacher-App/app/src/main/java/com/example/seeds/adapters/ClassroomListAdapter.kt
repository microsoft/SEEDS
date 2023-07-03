package com.example.seeds.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.seeds.databinding.ClassroomItemRowBinding
import com.example.seeds.databinding.ContentItemRowBinding
import com.example.seeds.model.Classroom
import com.example.seeds.model.Content


class ClassroomListAdapter(private val onClickListener: OnClickListener) : androidx.recyclerview.widget.ListAdapter<Classroom, ClassroomListAdapter.ClassroomViewHolder>(DiffCallBack){

    class OnClickListener(val clickListener: (classroom: Classroom) -> Unit) {
        fun onClick(classroom: Classroom) = clickListener(classroom)
    }

    inner class ClassroomViewHolder(private var binding: ClassroomItemRowBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind (classroom: Classroom) {
            binding.classroom = classroom
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassroomViewHolder {
        //reference: https://stackoverflow.com/questions/48363704/constraintlayout-as-recyclerview-items
        return ClassroomViewHolder(ClassroomItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ClassroomViewHolder, position: Int) {
        val classroom = getItem(position)
        holder.itemView.setOnClickListener {
            onClickListener.onClick(classroom)
        }
        holder.bind(classroom)
    }

    companion object DiffCallBack : DiffUtil.ItemCallback<Classroom>() {
        override fun areItemsTheSame(oldItem: Classroom, newItem: Classroom): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Classroom, newItem: Classroom): Boolean {
            return oldItem._id == newItem._id
        }
    }
}