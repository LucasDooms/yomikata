package com.jehutyno.yomikata.view

import android.app.ActionBar.LayoutParams
import android.app.Activity
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.ActionMode
import android.view.Gravity.CENTER
import android.view.Gravity.CENTER_HORIZONTAL
import android.view.Gravity.END
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.presenters.LevelInterface
import com.jehutyno.yomikata.presenters.SelectionsInterface
import com.jehutyno.yomikata.screens.word.WordsAdapter
import com.jehutyno.yomikata.util.Level
import com.jehutyno.yomikata.util.createNewSelectionDialog
import com.jehutyno.yomikata.util.getNextLevel
import com.jehutyno.yomikata.util.getPreviousLevel
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
    private val selectionsPresenter: SelectionsInterface, private val levelPresenter: LevelInterface,
    private val selection: Quiz?,
    private val onClose: (() -> Unit)? = null
    ) : ActionMode.Callback {

    private val activity: Activity
        get() = activityProvider()

    /** The currently selected words. If you want to use this multiple times, you may want
     *  to assign it to a local variable for better performance and consistency */
    private val selectedWords get() = adapter.currentList.filter { item -> item.isSelected.toBool() }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        adapter.setCheckMode(true)
        return false
    }

    companion object {
        const val ADD_TO_SELECTIONS = 1
        const val REMOVE_FROM_SELECTIONS = 2
        const val SELECT_ALL = 3
        const val UNSELECT_ALL = 4
        const val LEVEL_UP = 5
        const val LEVEL_DOWN = 6
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
                adapter.currentList.forEach {
                    it.isSelected = 1
                }
                adapter.notifyItemRangeChanged(0, adapter.currentList.size,
                    WordsAdapter.ChecksChanged(adapter.currentList.map{ it.isSelected.toBool() }))
            }
            UNSELECT_ALL -> {
                adapter.currentList.forEach {
                    it.isSelected = 0
                }
                adapter.notifyItemRangeChanged(0, adapter.currentList.size,
                    WordsAdapter.ChecksChanged(adapter.currentList.map{ it.isSelected.toBool() }))
            }
            LEVEL_UP -> {
                handleLevelUpOrDown(selectedWords, true)
            }
            LEVEL_DOWN -> {
                handleLevelUpOrDown(selectedWords, false)
            }
        }
        return false
    }

    private fun applyLevelUpOrDown(words: List<Word>, levelUp: Boolean) {
        val ids = words.map { it.id }.toLongArray()
        val points = words.map { it.points }.toIntArray()

        if (levelUp)
            callWithTimeOut {
                levelPresenter.levelUp(ids, points)
            }
        else
            callWithTimeOut {
                levelPresenter.levelDown(ids, points)
            }
    }

    /**
     * Handle level up or down
     *
     * If words of only one level are leveled up/down, does that immediately.
     * Otherwise, will show a dialog if words of different levels are leveled up/down.
     *
     * @param selected List of selected words
     * @param levelUp True if level up, false if level down
     */
    private fun handleLevelUpOrDown(selected: List<Word>, levelUp: Boolean) {
        // if levelUp -> remove MAX
        // if levelDown -> remove points = 0
        val filteredWords = selected.filter { word ->
            if (levelUp)
                word.level != Level.MAX
            else
                word.points != 0
        }
        if (filteredWords.isEmpty())
            return
        // if all selected words are the same level, simply level up
        // if they are different levels, show a dialog for confirmation / customization
        if (filteredWords.map { it.level }.distinct().count() == 1) {
            applyLevelUpOrDown(filteredWords, levelUp)
        } else {
            showLevelingDialog(filteredWords, levelUp)
        }
    }

    /**
     * Show leveling dialog
     *
     * Dialog shows words organized per level, so that the user may select only those
     * words of (a) specific level(s) to level up/down.
     *
     * @param selected List of selected words
     * @param levelUp True if leveling up, false if leveling down
     */
    private fun showLevelingDialog(selected: List<Word>, levelUp: Boolean) {
        val groupByLevel = selected.groupBy { word -> word.level }

        val layout = RelativeLayout(activity)
        layout.gravity = CENTER_HORIZONTAL

        val levelToColor = mapOf(
            Pair(Level.LOW, R.color.level_low_1),
            Pair(Level.MEDIUM, R.color.level_medium_1),
            Pair(Level.HIGH, R.color.level_high_1),
            Pair(Level.MASTER, R.color.level_master_1),
            Pair(Level.MAX, R.color.level_master_4)
        )
        fun getColorSpanned(text: String, level: Level, otherLevel: Level): Spanned {
            fun getSpanned(spanned: SpannableString, l: Level) {
                val pos = text.indexOf(l.toString())
                spanned.setSpan(
                    ForegroundColorSpan(
                        ResourcesCompat.getColor(activity.resources, levelToColor[l]!!, null)
                    ),
                    pos, pos + l.toString().length, SpannableString.SPAN_INCLUSIVE_INCLUSIVE
                )
            }
            val span = SpannableString(text)
            getSpanned(span, level)
            getSpanned(span, otherLevel)
            return span
        }

        val checkBoxes: MutableMap<Level, CheckBox> = mutableMapOf()
        var prevId: Int? = null
        val levelsOrdered =
            if (levelUp) Level.values()
            else Level.values().reversed().toTypedArray()
        levelsOrdered.forEach { level ->
            if (level !in groupByLevel) {
                return@forEach
            }
            val innerLayout = RelativeLayout(activity)
            innerLayout.gravity = CENTER_HORIZONTAL

            val text = TextView(activity)
            val otherLevel = if (levelUp) getNextLevel(level) else getPreviousLevel(level)
            val rawText = activity.getString(
                R.string.words_from_to,
                groupByLevel[level]!!.size,
                level.toString(),
                otherLevel.toString()
            )
            text.text = getColorSpanned(rawText, level, otherLevel)

            val pd = 20
            text.setPadding(pd, pd, pd, pd)
            text.gravity = CENTER

            val checkBox = CheckBox(activity)
            checkBox.isChecked = true
            checkBox.setPadding(pd, pd, pd, pd)
            checkBox.gravity = END
            checkBoxes[level] = checkBox

            innerLayout.addView(text,
                RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            )
            val innerParams = RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            innerParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            innerLayout.addView(checkBox, innerParams)

            val outerParams = RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            if (prevId == null) {
                outerParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            } else {
                outerParams.addRule(RelativeLayout.BELOW, prevId!!)
            }

            innerLayout.id = View.generateViewId()
            layout.addView(innerLayout, outerParams)

            prevId = innerLayout.id
        }

        activity.alertDialog {
            title = if (levelUp)
                context.getString(R.string.level_up_words, selected.size)
            else
                context.getString(R.string.level_down_words, selected.size)

            setView(layout)

            okButton {
                // get only selected and checked words
                val checkedLevels = checkBoxes.filterValues { it.isChecked }.keys
                val checkedSelected = groupByLevel.filterKeys { it in checkedLevels }.values.flatten()
                applyLevelUpOrDown(checkedSelected, levelUp)
            }
            cancelButton()
        }.show()
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
        menu.add(0, LEVEL_UP, 0,
            activity.getString(R.string.level_up)).setIcon(R.drawable.arrow_up_thick)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, LEVEL_DOWN, 1,
            activity.getString(R.string.level_down)).setIcon(R.drawable.arrow_down_thick)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, ADD_TO_SELECTIONS, 2,
            activity.getString(R.string.add_to_selections)).setIcon(R.drawable.ic_selections_selected)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, REMOVE_FROM_SELECTIONS, 3,
            activity.getString(R.string.remove_from_selection)).setIcon(R.drawable.ic_unselect)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, SELECT_ALL, 4,
            activity.getString(R.string.select_all)).setIcon(R.drawable.ic_select_multiple)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, UNSELECT_ALL, 5,
            activity.getString(R.string.unselect_all)).setIcon(R.drawable.ic_unselect_multiple)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        adapter.setCheckMode(false)
        onClose?.invoke()
    }

}
