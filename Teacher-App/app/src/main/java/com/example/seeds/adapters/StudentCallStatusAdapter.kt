package com.example.seeds.adapters

import android.media.Image
import com.example.seeds.model.StudentCallStatus
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.seeds.R
import com.example.seeds.databinding.StudentCallItemRowBinding

class StudentCallStatusAdapter(private val onClickListenerMute: OnClickListener, private val onClickListenerUnmute: OnClickListener, private val onClickListenerRemove: OnClickListener, private val onClickListenerRetry: OnClickListener, private val showCrown: Boolean = false, private val leader: String?=null) : androidx.recyclerview.widget.ListAdapter<StudentCallStatus, StudentCallStatusAdapter.StudentCallStatusViewHolder>(DiffCallBack){

    class OnClickListener(val clickListener: (student: StudentCallStatus) -> Unit) {
        fun onClick(student: StudentCallStatus) = clickListener(student)
    }

    inner class StudentCallStatusViewHolder(private var binding: StudentCallItemRowBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind (studentCallStatus: StudentCallStatus) {
            binding.studentCallStatus = studentCallStatus
            binding.showCrown = studentCallStatus.phoneNumber == leader
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentCallStatusViewHolder {
        //reference: https://stackoverflow.com/questions/48363704/constraintlayout-as-recyclerview-items
        return StudentCallStatusViewHolder(StudentCallItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: StudentCallStatusViewHolder, position: Int) {
        val studentCallStatus = getItem(position)

        holder.itemView.findViewById<ImageView>(R.id.mic).setOnClickListener {

            if(studentCallStatus.isMuted) {
                onClickListenerUnmute.onClick(studentCallStatus)
            }
            else {
                onClickListenerMute.onClick(studentCallStatus)
            }
        }

        holder.itemView.findViewById<ImageView>(R.id.remove_user_btn).setOnClickListener {
            onClickListenerRemove.onClick(studentCallStatus)
        }

        holder.itemView.findViewById<ImageView>(R.id.retry_student).setOnClickListener {
            onClickListenerRetry.onClick(studentCallStatus)
        }

        holder.bind(studentCallStatus)
    }

    companion object DiffCallBack : DiffUtil.ItemCallback<StudentCallStatus>() {
        override fun areItemsTheSame(oldItem: StudentCallStatus, newItem: StudentCallStatus): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: StudentCallStatus, newItem: StudentCallStatus): Boolean {
            return oldItem.phoneNumber == newItem.phoneNumber
        }
    }
}