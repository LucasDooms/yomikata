package com.jehutyno.yomikata.view

import android.app.Activity
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.PopupMenu
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.presenters.SelectionsInterface
import com.jehutyno.yomikata.screens.word.WordsAdapter
import com.jehutyno.yomikata.util.createNewSelectionDialog
import com.jehutyno.yomikata.util.toBool
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout


/**
 * Word selector action mode callback
 *
 * Pops up when selecting words in a Quiz to make a user selection.
 */
class WordSelectorActionModeCallback (
    private val activityProvider: () -> Activity, private val adapter: WordsAdapter,
    private val selectionsPresenter: SelectionsInterface
    ) : ActionMode.Callback {

    private val activity: Activity
        get() = activityProvider.invoke()

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        adapter.checkMode = true
        adapter.notifyItemRangeChanged(0, adapter.items.size)
        return false
    }

    companion object {
        const val ADD_TO_SELECTIONS = 1
        const val REMOVE_FROM_SELECTIONS = 2
        const val SELECT_ALL = 3
        const val UNSELECT_ALL = 4
    }

    private fun setupPopupMenu(selections: List<Quiz>, item: MenuItem): PopupMenu {
        val popup = PopupMenu(activity, activity.findViewById(item.itemId))
        popup.menuInflater.inflate(R.menu.popup_selections, popup.menu)
        for ((i, selection) in selections.withIndex()) {
            popup.menu.add(1, i, i, selection.getName()).isChecked = false
        }
        return popup
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val selections =
            runBlocking {
                selectionsPresenter.getSelections()
            }
        when (item.itemId) {
            ADD_TO_SELECTIONS -> {
                val popup = setupPopupMenu(selections, item)
                popup.setOnMenuItemClickListener { popItem ->
                    val selectedWords = adapter.items.filter { item -> item.isSelected.toBool() }
                    when (popItem.itemId) {
                        R.id.add_selection -> addSelection(selectedWords)
                        else -> {
                            callWithTimeOut {
                                val selectionId = selections[popItem.itemId].id
                                selectionsPresenter.addWordsToSelection(
                                    selectedWords.map{ it.id }.toLongArray(), selectionId
                                )
                            }
                            popItem.isChecked = !popItem.isChecked
                        }
                    }
                    true
                }
                popup.show()
            }
            REMOVE_FROM_SELECTIONS -> {
                val popup = setupPopupMenu(selections, item)
                popup.setOnMenuItemClickListener { popItem ->
                    val selectedWords = adapter.items.filter { item -> item.isSelected.toBool() }
                    callWithTimeOut {
                        val selectionId = selections[popItem.itemId].id
                        selectionsPresenter.deleteWordsFromSelection(
                            selectedWords.map{ it.id }.toLongArray(), selectionId
                        )
                    }
                    popItem.isChecked = !popItem.isChecked
                    true
                }
                popup.show()
            }
            SELECT_ALL -> {
                adapter.items.forEach {
                    it.isSelected = 1
                }
                adapter.notifyItemRangeChanged(0, adapter.items.size,
                    WordsAdapter.ChecksChanged(adapter.items.map{ it.isSelected.toBool() }))
            }
            UNSELECT_ALL -> {
                adapter.items.forEach {
                    it.isSelected = 0
                }
                adapter.notifyItemRangeChanged(0, adapter.items.size,
                    WordsAdapter.ChecksChanged(adapter.items.map{ it.isSelected.toBool() }))
            }
        }
        return false
    }

    private fun addSelection(selectedWords: List<Word>) {
        activity.createNewSelectionDialog("", { selectionName ->
            callWithTimeOut {
                val selectionId = selectionsPresenter.createSelection(selectionName)
                selectedWords.forEach {
                    selectionsPresenter.addWordToSelection(it.id, selectionId)
                }
            }
        }, null)
    }

    private fun callWithTimeOut(block: suspend () -> Unit) {
        // don't use lifecycle since creation might
        // take a while, and we don't want the quiz selection to stop even if the activity stops
        // use time out to prevent unexpected problems
        MainScope().launch {
            withTimeout(750L) {
                block()
            }
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.title = null
        menu.add(0, ADD_TO_SELECTIONS, 0,
            activity.getString(R.string.add_to_selections)).setIcon(R.drawable.ic_selections_selected)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, REMOVE_FROM_SELECTIONS, 0,
            activity.getString(R.string.remove_from_selection)).setIcon(R.drawable.ic_unselect)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, SELECT_ALL, 0,
            activity.getString(R.string.select_all)).setIcon(R.drawable.ic_select_multiple)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, UNSELECT_ALL, 0,
            activity.getString(R.string.unselect_all)).setIcon(R.drawable.ic_unselect_multiple)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        adapter.checkMode = false
        adapter.notifyItemRangeChanged(0, adapter.items.size)
    }

}
