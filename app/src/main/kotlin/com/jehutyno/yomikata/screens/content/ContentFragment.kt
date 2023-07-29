package com.jehutyno.yomikata.screens.content

import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.FragmentContentGraphBinding
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.screens.quiz.QuizActivity
import com.jehutyno.yomikata.screens.word.WordDetailDialogFragment
import com.jehutyno.yomikata.screens.word.WordsAdapter
import com.jehutyno.yomikata.util.Categories
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.util.Level
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.QuizStrategy
import com.jehutyno.yomikata.util.QuizType
import com.jehutyno.yomikata.util.SeekBarsManager
import com.jehutyno.yomikata.util.getParcelableArrayListHelper
import com.jehutyno.yomikata.util.getSerializableHelper
import com.jehutyno.yomikata.util.toBool
import com.jehutyno.yomikata.view.WordSelectorActionModeCallback
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider


/**
 * Created by valentin on 30/09/2016.
 */
abstract class ContentFragment(private val di: DI) : Fragment(), ContentContract.View, WordsAdapter.Callback {

    private lateinit var adapter: WordsAdapter
    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: ActionMode.Callback
    private lateinit var quizIds: LongArray
    private var category: Int = -1
    private lateinit var selectedTypes: ArrayList<QuizType>
    protected abstract val level: Level?
    private var lastPosition = -1
    private var dialog: WordDetailDialogFragment? = null
    private var selection: Quiz? = null // only set if this corresponds to a specific user selection

    // kodein
    private val subDI by DI.lazy {
        extend(di)
        bind<ContentContract.Presenter>() with provider {
            ContentPresenter (
                instance(), instance(),
                instance(arg = lifecycleScope), instance(), instance(arg = quizIds), instance(),
                quizIds, level
            )
        }
    }

    private val mpresenter: ContentContract.Presenter by subDI.instance()

    // seekBars
    private lateinit var seekBars : SeekBarsManager

    // View Binding
    private var _binding: FragmentContentGraphBinding? = null
    protected val binding get() = _binding!!

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(
            "position",
            (binding.recyclerviewContent.layoutManager as LinearLayoutManager)
                .findFirstVisibleItemPosition()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireArguments().also { args ->
            quizIds = args.getLongArray(Extras.EXTRA_QUIZ_IDS)!!
            category = args.getInt(Extras.EXTRA_CATEGORY)
            if (category == Categories.CATEGORY_SELECTIONS) {
                selection = args.getSerializableHelper(Extras.EXTRA_SELECTION, Quiz::class.java)
            }
            selectedTypes = args.getParcelableArrayListHelper(Extras.EXTRA_QUIZ_TYPES, QuizType::class.java)!!
        }

        if (savedInstanceState != null) {
            lastPosition = savedInstanceState.getInt("position")
        }

        adapter = WordsAdapter(requireActivity(), this)
        actionModeCallback = WordSelectorActionModeCallback (
            ::requireActivity, adapter, mpresenter, mpresenter, selection
        ) { actionMode = null }
        setHasOptionsMenu(true)
    }


    private val wordsObserver = Observer<List<Word>> { words ->
        displayWords(words)
    }

    override fun onStart() {
        super.onStart()
        mpresenter.start()
        mpresenter.words.observe(viewLifecycleOwner, wordsObserver)

        displayStats()
    }

    override fun onResume() {
        super.onResume()
        val position =
            if (lastPosition != -1) lastPosition
            else (binding.recyclerviewContent.layoutManager as GridLayoutManager).findFirstVisibleItemPosition()
        lastPosition = -1
        mpresenter.start()
        binding.recyclerviewContent.scrollToPosition(position)

        seekBars.animateAll()
    }

    override fun onPause() {
        super.onPause()

        // cancel animation in case it is currently running
        // set all to zero to prepare for the next animation when the page resumes again
        seekBars.resetAll()
        // stop action mode
        actionMode?.finish()
    }

    override fun displayStats() {
        seekBars.setTextViews(binding.textLow, binding.textMedium, binding.textHigh, binding.textMaster)
        mpresenter.let {
            seekBars.setObservers(it.quizCount,
                it.lowCount, it.mediumCount, it.highCount, it.masterCount, viewLifecycleOwner)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContentGraphBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerviewContent.let {
            it.adapter = adapter
            it.layoutManager = GridLayoutManager(context, 2)
        }

        // initialize seekBarsManager
        seekBars = SeekBarsManager(binding.seekLow, binding.seekMedium, binding.seekHigh, binding.seekMaster)
    }

    override fun displayWords(words: List<Word>) {
        adapter.replaceData(words)
    }


    override fun onItemClick(position: Int) {
        val bundle = Bundle()
        bundle.putLongArray(Extras.EXTRA_QUIZ_IDS, quizIds)
        bundle.putString(Extras.EXTRA_QUIZ_TITLE, getQuizTitle())
        bundle.putSerializable(Extras.EXTRA_LEVEL, level)
        bundle.putInt(Extras.EXTRA_WORD_POSITION, position)
        bundle.putString(Extras.EXTRA_SEARCH_STRING, "")

        // unbind observer to prevent word from disappearing while viewing in detail dialog
        mpresenter.words.removeObserver(wordsObserver)

        dialog = WordDetailDialogFragment(di)
        dialog!!.arguments = bundle
        dialog!!.show(childFragmentManager, "")
        dialog!!.isCancelable = true

        dialog!!.lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                // continue observing
                mpresenter.words.observe(viewLifecycleOwner, wordsObserver)
            }
        })
    }

    override fun onCategoryIconClick(position: Int) {
        actionMode = requireActivity().startActionMode(actionModeCallback)
    }

    override fun onCheckChange(position: Int, check: Boolean) {
        //
    }

    fun launchQuiz(strategy: QuizStrategy) {
        lifecycleScope.launch {
            mpresenter.launchQuizStat(category)
        }
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val cat1 = pref.getInt(Prefs.LATEST_CATEGORY_1.pref, -1)

        if (category != cat1) {
            pref.edit().putInt(Prefs.LATEST_CATEGORY_2.pref, cat1).apply()
            pref.edit().putInt(Prefs.LATEST_CATEGORY_1.pref, category).apply()
        }

        val intent = Intent(requireActivity(), QuizActivity::class.java).apply {
            val allWordIds = mpresenter.words.value!!.map{ it.id }.toLongArray()
            putExtra(Extras.EXTRA_WORD_IDS,
                if (actionMode == null)
                    allWordIds
                else {
                    // if actionMode -> use currently selected words to start quiz
                    val currentlySelectedIds = adapter.items
                        .filter{ it.isSelected.toBool() }.map{ it.id }.toLongArray()
                    if (currentlySelectedIds.isEmpty())
                        allWordIds // but if no words are selected, use all words in quiz anyway
                    else
                        currentlySelectedIds
                }
            )
            putExtra(Extras.EXTRA_QUIZ_TITLE, getQuizTitle())
            putExtra(Extras.EXTRA_QUIZ_STRATEGY, strategy)
            putExtra(Extras.EXTRA_LEVEL, level)
            putExtra(Extras.EXTRA_QUIZ_TYPES, selectedTypes)
        }
        startActivity(intent)
    }

    protected abstract fun getQuizTitle(): String

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (activity != null) {
            inflater.inflate(R.menu.menu_content, menu)
            super.onCreateOptionsMenu(menu, inflater)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.select_mode -> {
                actionMode = requireActivity().startActionMode(actionModeCallback)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
