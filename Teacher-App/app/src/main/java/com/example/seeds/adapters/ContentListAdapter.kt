package com.example.seeds.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.seeds.R
import com.example.seeds.databinding.ContentItemRowBinding
import com.example.seeds.model.Content
import com.example.seeds.model.Student


class ContentListAdapter(private val onContentClickListener: OnClickListener? = null, val showCheckbox: Boolean = false, val showRemoveContent: Boolean = false, var usersInGroup: MutableSet<String> = mutableSetOf(),
                         val maximumSelections: Int = 5000) : androidx.recyclerview.widget.ListAdapter<Content, ContentListAdapter.ContentViewHolder>(DiffCallBack){

    class OnClickListener(val clickListener: (content: Content) -> Unit) {
        fun onClick(content: Content) = clickListener(content)
    }

    inner class ContentViewHolder(private var binding: ContentItemRowBinding): RecyclerView.ViewHolder(binding.root) {
        var contentCheckbox = binding.contentCheckbox

        init {
            contentCheckbox.setOnClickListener {
                if (!usersInGroup.contains(binding.content!!.id)) {
                    if(usersInGroup.size == maximumSelections) {
                        Toast.makeText(binding.root.context, "Maximum $maximumSelections user${if(maximumSelections != 1) 's' else ' '} allowed!", Toast.LENGTH_SHORT).show()
                        contentCheckbox.isChecked = false
                    } else {
                        contentCheckbox.isChecked = true
                        usersInGroup.add(binding.content!!.id)
                    }
                } else {
                    contentCheckbox.isChecked = false
                    usersInGroup.remove(binding.content!!.id)
                }
            }
        }

        fun bind (content: Content) {
            binding.content = content
            binding.showCheckbox = showCheckbox
            binding.showRemoveContent = showRemoveContent
            binding.contentCheckbox.isChecked = usersInGroup.contains(content.id)
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentViewHolder {
        //reference: https://stackoverflow.com/questions/48363704/constraintlayout-as-recyclerview-items
        return ContentViewHolder(ContentItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ContentViewHolder, position: Int) {
        val content = getItem(position)
        onContentClickListener?.let { OnClickListener ->
            holder.itemView.setOnClickListener {
                OnClickListener.onClick(content)
            }
        }

        onContentClickListener?.let{ onClickListener ->
            holder.itemView.findViewById<ImageView>(R.id.remove_content).setOnClickListener {
                onClickListener.onClick(content)
            }
        }

        holder.bind(content)
    }

    companion object DiffCallBack : DiffUtil.ItemCallback<Content>() {
        override fun areItemsTheSame(oldItem: Content, newItem: Content): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Content, newItem: Content): Boolean {
            return oldItem.id == newItem.id
        }
    }

    override fun submitList(usersList: List<Content>?) {
        val newUsersList = usersList?.toMutableList()
        newUsersList?.sortBy {
            it.title
        }
        super.submitList(newUsersList)
    }
}