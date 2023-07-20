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
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.okButton
import splitties.alertdialog.appcompat.title


/**
 * Word selector action mode callback
 *
 * Pops up when selecting words in a Quiz to make a user selection.
 */
class WordSelectorActionModeCallback (
    private val activityProvider: () -> Activity, private val adapter: WordsAdapter,
    private val selectionsPresenter: SelectionsInterface, private val selection: Quiz?,
    private val onClose: (() -> Unit)? = null
    ) : ActionMode.Callback {

    private val activity: Activity
        get() = activityProvider()

    /** The currently selected words. If you want to use this multiple times, you may want
     *  to assign it to a local variable for better performance and consistency */
    private val selectedWords get() = adapter.items.filter { item -> item.isSelected.toBool() }

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

    /**
     * Setup popup menu
     *
     * @param selections List of all user selection quizzes
     * @param item The MenuItem that was clicked (to which the popup will attach)
     * @param showNew True if it should show the "New Selection" option at the top, False otherwise
     * @return The created PopupMenu, which is not shown yet
     */
    private fun setupPopupMenu(selections: List<Quiz>, item: MenuItem, showNew: Boolean): PopupMenu {
        val popup = PopupMenu(activity, activity.findViewById(item.itemId))
        if (showNew) {
            popup.menuInflater.inflate(R.menu.popup_selections, popup.menu)
        }
        for ((i, selection) in selections.withIndex()) {
            if (selection == this.selection)
                continue    // skip if it is the selection category in which we are selecting words
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
                val popup = setupPopupMenu(selections, item, true)
                popup.setOnMenuItemClickListener { popItem ->
                    when (popItem.itemId) {
                        R.id.add_selection -> addSelection()
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
                if (selection != null) {
                    // since we are in a user selection:
                    // don't show options, simply delete from the current selection quiz
                    val currentlySelectedWords = selectedWords
                    if (currentlySelectedWords.size <= 1) {
                        deleteWords(currentlySelectedWords, selection.id)
                    } else {
                        // if more than one word is deleted -> ask for confirmation
                        activity.alertDialog {
                            title = activity.getString(R.string.remove_x_words, currentlySelectedWords.size)

                            okButton {
                                deleteWords(currentlySelectedWords, selection.id)
                            }
                            cancelButton()
                        }.show()
                    }
                } else {
                    val popup = setupPopupMenu(selections, item, false)
                    popup.setOnMenuItemClickListener { popItem ->
                        deleteWords(selectedWords, selections[popItem.itemId].id)
                        popItem.isChecked = !popItem.isChecked
                        true
                    }
                    popup.show()
                }
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

    /**
     * Add selection
     *
     * Open a dialog to allow the user to choose a name.
     * If the selection is successfully created, [selectedWords] will be added to that selection.
     */
    private fun addSelection() {
        // assign copy now (in case selections somehow changes later)
        val currentlySelectedWords = selectedWords
        activity.createNewSelectionDialog("", { selectionName ->
            callWithTimeOut {
                val selectionId = selectionsPresenter.createSelection(selectionName)
                currentlySelectedWords.forEach {
                    selectionsPresenter.addWordToSelection(it.id, selectionId)
                }
            }
        }, null)
    }

    /**
     * Delete words
     *
     * @param words List of words to delete
     * @param selectionId Id of the selection from which the words should be deleted
     */
    private fun deleteWords(words: List<Word>, selectionId: Long) {
        callWithTimeOut {
            selectionsPresenter.deleteWordsFromSelection(
                words.map{ it.id }.toLongArray(), selectionId
            )
        }
    }

    private fun callWithTimeOut(block: suspend () -> Unit) {
        // don't use lifecycle since creation might take a while
        // and we don't want the adding / deleting / creating to stop even if the activity stops
        MainScope().launch {
            // use time out to prevent unexpected problems
            withTimeout(1000L) {
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
        onClose?.invoke()
    }

}
