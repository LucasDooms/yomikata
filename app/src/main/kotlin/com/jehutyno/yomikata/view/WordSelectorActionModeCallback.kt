package com.jehutyno.yomikata.view

import android.app.Activity
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.widget.PopupMenu
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.presenters.SelectionsInterface
import com.jehutyno.yomikata.presenters.WordInQuizInterface
import com.jehutyno.yomikata.screens.content.WordsAdapter
import com.jehutyno.yomikata.util.DimensionHelper
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.okButton
import splitties.alertdialog.appcompat.titleResource


/**
 * Word selector action mode callback
 *
 * Pops up when selecting words in a Quiz to make a user selection.
 */
class WordSelectorActionModeCallback (
    private val activityProvider: () -> Activity, private val adapter: WordsAdapter,
    private val selectionsPresenterProvider: () -> SelectionsInterface,
    private val wordsPresenterProvider: () -> WordInQuizInterface
    ) : ActionMode.Callback {

    private val activity: Activity
        get() = activityProvider.invoke()
    private val selectionsPresenter: SelectionsInterface
        get() = selectionsPresenterProvider.invoke()
    private val wordsPresenter: WordInQuizInterface
        get() = wordsPresenterProvider.invoke()

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        adapter.checkMode = true
        adapter.notifyItemRangeChanged(0, adapter.items.size)
        return false
    }
    enum class SelectAction(val value: Int) {
        ADD_TO_SELECTIONS(1),
        REMOVE_FROM_SELECTIONS(2),
        SELECT_ALL(3),
        UNSELECT_ALL(4)
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val selections: List<Quiz>
        runBlocking {
            selections = selectionsPresenter.getSelections()
        }
        when (item.itemId) {
            SelectAction.ADD_TO_SELECTIONS.value -> {
                val popup = PopupMenu(activity, activity.findViewById(item.itemId))
                popup.menuInflater.inflate(R.menu.popup_selections, popup.menu)
                for ((i, selection) in selections.withIndex()) {
                    popup.menu.add(1, i, i, selection.getName()).isChecked = false
                }
                popup.setOnMenuItemClickListener {it -> runBlocking {
                    val selectedWords: ArrayList<Word> = arrayListOf()
                    adapter.items.forEach { item -> if (item.isSelected == 1) selectedWords.add(item) }
                    val selectionItemId = it.itemId
                    when (it.itemId) {
                        R.id.add_selection -> addSelection(selectedWords)
                        else -> {
                            selectedWords.forEach {
                                if (!wordsPresenter.isWordInQuiz(it.id, selections[selectionItemId].id))
                                    selectionsPresenter.addWordToSelection(it.id, selections[selectionItemId].id)
                            }
                            it.isChecked = !it.isChecked
                        }
                    }
                    true
                }
                }
                popup.show()
            }
            SelectAction.REMOVE_FROM_SELECTIONS.value -> {
                val popup = PopupMenu(activity, activity.findViewById(item.itemId))
                for ((i, selection) in selections.withIndex()) {
                    popup.menu.add(1, i, i, selection.getName()).isChecked = false
                }
                popup.setOnMenuItemClickListener {it -> runBlocking {
                    val selectedWords: ArrayList<Word> = arrayListOf()
                    adapter.items.forEach { item -> if (item.isSelected == 1) selectedWords.add(item) }
                    val selectionItemId = it.itemId
                    when (it.itemId) {
                        else -> {
                            selectedWords.forEach {
                                if (wordsPresenter.isWordInQuiz(it.id, selections[selectionItemId].id))
                                    selectionsPresenter.deleteWordFromSelection(it.id, selections[selectionItemId].id)
                            }
                            it.isChecked = !it.isChecked
                        }
                    }
                    true
                }
                }
                popup.show()

            }
            SelectAction.SELECT_ALL.value -> {
                runBlocking {
                    wordsPresenter.updateWordsCheck(adapter.items.map{it.id}.toLongArray(), true)
                }
                adapter.items.forEach {
                    it.isSelected = 1
                }
                adapter.notifyItemRangeChanged(0, adapter.items.size)
            }
            SelectAction.UNSELECT_ALL.value -> {
                runBlocking {
                    wordsPresenter.updateWordsCheck(adapter.items.map{it.id}.toLongArray(), false)
                }
                adapter.items.forEach {
                    it.isSelected = 0
                }
                adapter.notifyItemRangeChanged(0, adapter.items.size)
            }
        }
        return false
    }

    private fun addSelection(selectedWords: ArrayList<Word>) {
        val input = EditText(activity)
        input.setSingleLine()
        input.hint = activity.getString(R.string.selection_name)

        val container = FrameLayout(activity)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.leftMargin = DimensionHelper.getPixelFromDip(activity, 20)
        params.rightMargin = DimensionHelper.getPixelFromDip(activity, 20)
        input.layoutParams = params
        container.addView(input)

        activity.alertDialog {
            titleResource = R.string.new_selection
            setView(container)

            okButton {
                MainScope().launch {// don't use lifecycle since creation might
                    // take a while, and we don't want the quiz selection to stop even if the activity stops
                    // use time out to prevent unexpected problems
                    withTimeout(2000L) {
                        val selectionId = selectionsPresenter.createSelection(input.text.toString())
                        selectedWords.forEach {
                            selectionsPresenter.addWordToSelection(it.id, selectionId)
                        }
                    }
                }
            }
            cancelButton { }
        }.show()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.title = null
        menu.add(0, SelectAction.ADD_TO_SELECTIONS.value, 0,
            activity.getString(R.string.add_to_selections)).setIcon(R.drawable.ic_selections_selected)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, SelectAction.REMOVE_FROM_SELECTIONS.value, 0,
            activity.getString(R.string.remove_from_selection)).setIcon(R.drawable.ic_unselect)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, SelectAction.SELECT_ALL.value, 0,
            activity.getString(R.string.select_all)).setIcon(R.drawable.ic_select_multiple)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, SelectAction.UNSELECT_ALL.value, 0,
            activity.getString(R.string.unselect_all)).setIcon(R.drawable.ic_unselect_multiple)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        adapter.checkMode = false
        adapter.notifyItemRangeChanged(0, adapter.items.size)
    }

}
