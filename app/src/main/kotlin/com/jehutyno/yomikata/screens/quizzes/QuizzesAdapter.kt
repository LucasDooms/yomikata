package com.jehutyno.yomikata.screens.quizzes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.jehutyno.yomikata.databinding.VhNewSelectionBinding
import com.jehutyno.yomikata.databinding.VhQuizBinding
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.util.Category


private val DIFF_CALLBACK = object: DiffUtil.ItemCallback<Quiz>() {
    override fun areItemsTheSame(oldQuiz: Quiz, newQuiz: Quiz): Boolean {
        return oldQuiz.id == newQuiz.id
    }
    override fun areContentsTheSame(oldQuiz: Quiz, newQuiz: Quiz): Boolean {
        return oldQuiz == newQuiz
    }
}
/**
 * Created by valentin on 04/10/2016.
 */
class QuizzesAdapter(val category: Category, private val callback: Callback, private var isSelections: Boolean)
    : ListAdapter<Quiz, QuizzesAdapter.ViewHolder>(DIFF_CALLBACK) {

    val items: MutableList<Quiz> get() = currentList

    companion object {
        const val TYPE_NEW_SELECTION = 0
        const val TYPE_QUIZ = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TYPE_NEW_SELECTION -> {
                val binding = VhNewSelectionBinding.inflate(layoutInflater, parent, false)
                ViewHolder.ViewHolderNewSelection(binding)
            }
            TYPE_QUIZ -> {
                val binding = VhQuizBinding.inflate(layoutInflater, parent, false)
                ViewHolder.ViewHolderQuiz(binding)
            }
            else -> throw IllegalArgumentException("Invalid ViewType Provided")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder.ViewHolderNewSelection -> {
                holder.itemView.setOnClickListener {
                    callback.addSelection()
                }
            }
            is ViewHolder.ViewHolderQuiz -> {
                val quiz = items[position]
                val quizNames = quiz.getName().split("%")
                holder.quizName.text = quizNames[0]
                holder.quizSubtitle.text = if (quizNames.size > 1) quizNames[1] else ""

                // set to null before changing isChecked to prevent weird behaviour
                holder.quizCheck.setOnCheckedChangeListener(null)
                holder.quizCheck.isChecked = quiz.isSelected
                holder.itemView.setOnClickListener {
                    callback.onItemClick(position)
                }
                holder.itemView.setOnLongClickListener {
                    callback.onItemLongClick(position)
                    true
                }
                holder.quizCheck.setOnCheckedChangeListener { _, isSelected ->
                    callback.onItemChecked(position, isSelected)
                    items[position].isSelected = isSelected
                }

            }
        }
    }

    override fun getItemCount(): Int {
        return if (isSelections)
            items.count() + 1
        else
            items.count()

    }

    override fun getItemViewType(position: Int): Int {
        return if (isSelections && position == items.count())
            TYPE_NEW_SELECTION
        else
            TYPE_QUIZ
    }

    fun replaceData(list: List<Quiz>, isSelections: Boolean) {
        this.isSelections = isSelections
        submitList(list)
    }

    sealed class ViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {

        class ViewHolderQuiz(binding: VhQuizBinding) : ViewHolder(binding) {
            val quizName = binding.quizName
            val quizSubtitle = binding.quizSubtitle
            val quizCheck = binding.quizCheck
        }

        class ViewHolderNewSelection(binding: VhNewSelectionBinding) : ViewHolder(binding) {
            val quizName = binding.quizName
        }

    }

    interface Callback {
        fun onItemClick(position: Int)
        fun onItemLongClick(position: Int)
        fun onItemChecked(position: Int, checked: Boolean)
        fun addSelection()
    }

    fun deleteItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

}
