package com.jehutyno.yomikata.screens.word

import android.content.Context
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.RecyclerView
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.VhWordShortBinding
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.model.getCategoryIcon
import com.jehutyno.yomikata.model.getWordColor
import com.jehutyno.yomikata.util.toBool
import com.jehutyno.yomikata.util.toInt


/**
 * Created by valentin on 04/10/2016.
 */
class WordsAdapter(private val context: Context, private val callback: Callback)
    : RecyclerView.Adapter<WordsAdapter.ViewHolder>() {

    var items: MutableList<Word> = arrayListOf()

    var checkMode = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = VhWordShortBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    class ChecksChanged(val selections: List<Boolean>)

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val checks = payloads.filterIsInstance<ChecksChanged>()
            // if there are multiple ChecksChanged payloads, only the last one matters
            if (checks.isNotEmpty()) {
                checks.last().selections.getOrNull(position)?.also {
                    holder.checkBox.isChecked = it
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val word = items[position]
        holder.wordName.text = word.japanese
        holder.wordName.setTextColor(getWordColor(context, word.points))
        holder.categoryIcon.setImageResource(getCategoryIcon(word.baseCategory))

        val color = ContextCompat.getColor(context, R.color.content_icon_color)
        holder.categoryIcon.drawable?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_ATOP)

        // unset listener before changing isChecked to avoid weird behaviour
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = word.isSelected.toBool()

        if (checkMode) {
            holder.categoryIcon.visibility = GONE
            holder.checkBox.visibility = VISIBLE
        } else {
            holder.categoryIcon.visibility = VISIBLE
            holder.checkBox.visibility = GONE
        }

        holder.itemView.setOnClickListener { callback.onItemClick(position) }

        holder.checkBox.setOnCheckedChangeListener { _, isSelected ->
            callback.onCheckChange(position, isSelected)
            items[position].isSelected = isSelected.toInt()
        }

        holder.categoryIcon.setOnClickListener {
            // make sure checkbox listener is set, so that this causes it to call!!
            holder.checkBox.isChecked = true
            callback.onCategoryIconClick(position)
            if (!checkMode) {
                checkMode = true
                // notify all items changed, since checkMode affects all items
                notifyItemRangeChanged(0, items.size)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    fun replaceData(list: List<Word>) {
        items.clear()
        items.addAll(list)
        @Suppress("notifyDataSetChanged")
        notifyDataSetChanged()
    }

    fun clearData() {
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size)
    }

    class ViewHolder(binding: VhWordShortBinding) : RecyclerView.ViewHolder(binding.root) {
        val wordName = binding.kanjiWord
        val categoryIcon = binding.categoryIcon
        val checkBox = binding.wordCheck
    }

    interface Callback {
        fun onItemClick(position: Int)
        fun onCategoryIconClick(position: Int)
        fun onCheckChange(position: Int, check: Boolean)
    }

}
