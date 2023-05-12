package com.jehutyno.yomikata.screens.quizzes

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.FragmentQuizzesBinding
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.screens.content.ContentActivity
import com.jehutyno.yomikata.screens.quiz.QuizActivity
import com.jehutyno.yomikata.util.Categories
import com.jehutyno.yomikata.util.DimensionHelper
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.util.Level
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.QuizStrategy
import com.jehutyno.yomikata.util.QuizType
import com.jehutyno.yomikata.util.SeekBarsManager
import com.jehutyno.yomikata.util.SpeechAvailability
import com.jehutyno.yomikata.util.TextValidator
import com.jehutyno.yomikata.util.animateSeekBar
import com.jehutyno.yomikata.util.checkSpeechAvailability
import com.jehutyno.yomikata.util.getCategoryLevel
import com.jehutyno.yomikata.util.getLevelDownloadSize
import com.jehutyno.yomikata.util.getLevelDownloadVersion
import com.jehutyno.yomikata.util.onTTSinit
import com.jehutyno.yomikata.util.speechNotSupportedAlert
import com.jehutyno.yomikata.util.spotlightTuto
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.instance
import org.kodein.di.newInstance
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.message
import splitties.alertdialog.appcompat.neutralButton
import splitties.alertdialog.appcompat.okButton
import splitties.alertdialog.appcompat.titleResource
import java.lang.Thread.sleep


/**
 * Created by valentin on 30/09/2016.
 */
class QuizzesFragment(di: DI) : Fragment(), QuizzesContract.View, QuizzesAdapter.Callback, TextToSpeech.OnInitListener {

    // kodein
    private val mpresenter: QuizzesContract.Presenter by di.newInstance {
        QuizzesPresenter(instance(), instance(), instance(),
            instance ( arg =
                instance<QuizRepository>().getQuiz(selectedCategory).map {
                    lst -> lst.map{ it.id }.toLongArray()
                }
            ), selectedCategory
        )
    }

    val REQUEST_TUTO: Int = 55
    private lateinit var adapter: QuizzesAdapter
    private var selectedCategory: Int = 0
    private var tts: TextToSpeech? = null
    private var ttsSupported: Int = TextToSpeech.LANG_NOT_SUPPORTED

    // seekBars
    private lateinit var seekBars : SeekBarsManager

    // View Binding
    private var _binding: FragmentQuizzesBinding? = null
    private val binding get() = _binding!!


    override fun onInit(status: Int) {
        ttsSupported = onTTSinit(context, status, tts)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // make sure selectedCategory is set before mpresenter is used to properly initialize with kodein
        selectedCategory = requireArguments().getInt(Extras.EXTRA_CATEGORY)
        adapter = QuizzesAdapter(requireActivity(), selectedCategory, this, selectedCategory == Categories.CATEGORY_SELECTIONS)
    }

    override fun onStart() {
        // use onStart so that viewPager2 can set everything up before the page becomes visible
        super.onStart()
        mpresenter.start()
        subscribeDisplayQuizzes()
        // setup seekBars observers
        seekBars.setTextViews(binding.textLow, binding.textMedium, binding.textHigh, binding.textMaster)
        seekBars.setPlay(binding.playLow, binding.playMedium, binding.playHigh, binding.playMaster)
        mpresenter.let {
            seekBars.setObservers(it.quizCount,
                it.lowCount, it.mediumCount, it.highCount, it.masterCount, viewLifecycleOwner)
        }
    }

    override fun onResume() {
        super.onResume()
        val position = (binding.recyclerview.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        mpresenter.start()
        seekBars.animateAll()    // call this after loadQuizzes, since seekBars variables are set there
        binding.recyclerview.scrollToPosition(position)
        tutos()

        // check if voices downloads have changed (e.g. voices files have been deleted in preferences)
        updateVoicesDownloadVisibility()
    }

    override fun onPause() {
        super.onPause()

        // cancel animation in case it is currently running
        // set all to zero to prepare for the next animation when the page resumes again
        seekBars.resetAll()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQuizzesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tts = TextToSpeech(activity, this)
        binding.recyclerview.let {
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(context)
        }

        mpresenter.initQuizTypes()
        mpresenter.selectedTypes.observe(viewLifecycleOwner) { types ->
            selectAuto(false)
            selectPronunciation(false)
            selectPronunciationQcm(false)
            selectAudio(false)
            selectEnJap(false)
            selectJapEn(false)
            types.forEach {
                when (it) {
                    QuizType.TYPE_AUTO -> selectAuto(true)
                    QuizType.TYPE_PRONUNCIATION -> selectPronunciation(true)
                    QuizType.TYPE_PRONUNCIATION_QCM -> selectPronunciationQcm(true)
                    QuizType.TYPE_AUDIO -> selectAudio(true)
                    QuizType.TYPE_EN_JAP -> selectEnJap(true)
                    QuizType.TYPE_JAP_EN -> selectJapEn(true)

                }
            }
        }

        binding.btnPronunciationQcmSwitch.setOnClickListener {
            mpresenter.pronunciationQcmSwitch()
            spotlightTuto(requireActivity(), binding.btnPronunciationQcmSwitch, getString(R.string.tutos_pronunciation_mcq), getString(R.string.tutos_pronunciation_mcq_message)
            ) { }
        }
        binding.btnPronunciationSwitch.setOnClickListener {
            mpresenter.pronunciationSwitch()
            spotlightTuto(requireActivity(), binding.btnPronunciationSwitch, getString(R.string.tutos_pronunciation_quiz), getString(R.string.tutos_pronunciation_quiz_message)
            ) { }
        }
        binding.btnAudioSwitch.setOnClickListener {
            spotlightTuto(requireActivity(), binding.btnAudioSwitch, getString(R.string.tutos_audio_quiz), getString(R.string.tutos_audio_quiz_message)
            ) { }
            when (checkSpeechAvailability(requireActivity(), ttsSupported, getCategoryLevel(selectedCategory))) {
                SpeechAvailability.NOT_AVAILABLE -> speechNotSupportedAlert(requireActivity(), getCategoryLevel(selectedCategory)) {
                    (activity as QuizzesActivity).quizzesAdapter.notifyDataSetChanged()
                }
                else -> mpresenter.audioSwitch()
            }
        }
        binding.btnEnJapSwitch.setOnClickListener {
            mpresenter.enJapSwitch()
            spotlightTuto(requireActivity(), binding.btnEnJapSwitch, getString(R.string.tutos_en_jp), getString(R.string.tutos_en_jp_message)
            ) { }
        }
        binding.btnJapEnSwitch.setOnClickListener {
            mpresenter.japEnSwitch()
            spotlightTuto(requireActivity(), binding.btnJapEnSwitch, getString(R.string.tutos_jp_en), getString(R.string.tutos_jp_en_message)
            ) { }
        }
        binding.btnAutoSwitch.setOnClickListener {
            mpresenter.autoSwitch()
            spotlightTuto(requireActivity(), binding.btnAutoSwitch, getString(R.string.tutos_auto_quiz), getString(R.string.tutos_auto_quiz_message)
            ) { }
        }

        binding.playLow.setOnClickListener {
            openContent(selectedCategory, Level.LOW)
        }
        binding.playMedium.setOnClickListener {
            openContent(selectedCategory, Level.MEDIUM)
        }
        binding.playHigh.setOnClickListener {
            openContent(selectedCategory, Level.HIGH)
        }
        binding.playMaster.setOnClickListener {
            openContent(selectedCategory, Level.MASTER)
        }

        updateVoicesDownloadVisibility()

        binding.download.setOnClickListener {
            requireContext().alertDialog {
                if (getLevelDownloadVersion(getCategoryLevel(selectedCategory)) > 0 && previousVoicesDownloaded(getLevelDownloadVersion(getCategoryLevel(selectedCategory)))) {
                    titleResource = R.string.update_voices_alert
                    message = getString(R.string.update_voices_alert_message, getLevelDownloadSize(getCategoryLevel(selectedCategory)))
                } else {
                    titleResource = R.string.download_voices_alert
                    message = getString(R.string.download_voices_alert_message, getLevelDownloadSize(getCategoryLevel(selectedCategory)))
                }
                okButton {
                    (activity as QuizzesActivity).voicesDownload(getCategoryLevel(selectedCategory)) {
                        binding.download.visibility = GONE
                    }
                }
                cancelButton { }
            }.show()
        }

        // initialize seekBarsManager
        seekBars = SeekBarsManager(binding.seekLow, binding.seekMedium, binding.seekHigh, binding.seekMaster)
    }

    private fun updateVoicesDownloadVisibility() {
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (selectedCategory == -1 || selectedCategory == 8
                || pref.getBoolean(Prefs.VOICE_DOWNLOADED_LEVEL_V.pref +
                        "${getLevelDownloadVersion(getCategoryLevel(selectedCategory))}_${getCategoryLevel(selectedCategory)}", false)) {
            binding.download.visibility = GONE
        } else {
            binding.download.visibility = VISIBLE
            if (getLevelDownloadVersion(getCategoryLevel(selectedCategory)) > 0 && previousVoicesDownloaded(getLevelDownloadVersion(getCategoryLevel(selectedCategory)))) {
                binding.download.text = getString(R.string.update_voices, getLevelDownloadSize(getCategoryLevel(selectedCategory)))
            } else {
                binding.download.text = getString(R.string.download_voices, getLevelDownloadSize(getCategoryLevel(selectedCategory)))
            }
        }
    }

    private fun previousVoicesDownloaded(downloadVersion: Int): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        return (0 until downloadVersion).any {
            pref.getBoolean("${Prefs.VOICE_DOWNLOADED_LEVEL_V.pref}${it}_${getCategoryLevel(selectedCategory)}", false)
        }
    }

    override fun selectPronunciationQcm(isSelected: Boolean) {
        binding.btnPronunciationQcmSwitch.isSelected = isSelected
    }

    override fun selectPronunciation(isSelected: Boolean) {
        binding.btnPronunciationSwitch.isSelected = isSelected
    }

    override fun selectAudio(isSelected: Boolean) {
        binding.btnAudioSwitch.isSelected = isSelected
    }

    override fun selectEnJap(isSelected: Boolean) {
        binding.btnEnJapSwitch.isSelected = isSelected
    }

    override fun selectJapEn(isSelected: Boolean) {
        binding.btnJapEnSwitch.isSelected = isSelected
    }

    override fun selectAuto(isSelected: Boolean) {
        binding.btnAutoSwitch.isSelected = isSelected
    }

    override fun launchQuiz(strategy: QuizStrategy, level: Level?, selectedTypes: ArrayList<QuizType>, title: String) {
        val ids = mutableListOf<Long>()
        adapter.items.forEach {
            if (it.isSelected)
                ids.add(it.id)
        }

        // If nothing selected or if inside of a specific level, use all quizzes
        if (ids.size == 0 || level != null) {   // TODO
            ids.clear()
            adapter.items.forEach {
                ids.add(it.id)
            }
        }

        // No quiz to launch
        if (ids.size == 0 || runBlocking {mpresenter.countQuiz(ids.toLongArray())} <= 0) {
            val toast = Toast.makeText(context, R.string.error_no_quiz_no_word, Toast.LENGTH_SHORT)
            toast.show()
            return
        }

        val intent = Intent(activity, QuizActivity::class.java).apply {
            putExtra(Extras.EXTRA_QUIZ_IDS, ids.toLongArray())
            putExtra(Extras.EXTRA_QUIZ_TITLE, title)
            putExtra(Extras.EXTRA_QUIZ_STRATEGY, strategy)
            putExtra(Extras.EXTRA_LEVEL, level)
            putExtra(Extras.EXTRA_QUIZ_TYPES, selectedTypes)
        }
        startActivity(intent)
    }

    fun launchQuizClick(strategy: QuizStrategy, level: Level?, title: String) {
        lifecycleScope.launch {
            mpresenter.onLaunchQuizClick(selectedCategory)
        }
        launchQuiz(strategy, level, mpresenter.getSelectedTypes(), title)
    }

    private fun subscribeDisplayQuizzes() {
        mpresenter.quizList.observe(viewLifecycleOwner) {
            displayQuizzes(it)
        }
    }

    override fun displayQuizzes(quizzes: List<Quiz>) {
        adapter.replaceData(quizzes, selectedCategory == Categories.CATEGORY_SELECTIONS)
    }

    private fun openContent(position: Int, level: Level?) {
        val intent = Intent(context, ContentActivity::class.java).apply {
            putExtra(Extras.EXTRA_CATEGORY, selectedCategory)
            putExtra(Extras.EXTRA_QUIZ_POSITION, position)
            putExtra(Extras.EXTRA_QUIZ_TYPES, mpresenter.getSelectedTypes())
            putExtra(Extras.EXTRA_LEVEL, level)
        }
        startActivity(intent)
    }

    override fun displayNoData() {
        adapter.noData(selectedCategory == Categories.CATEGORY_SELECTIONS)
        animateSeekBar(binding.seekLow, 0, 0, 0)
        binding.textLow.text = 0.toString()
        animateSeekBar(binding.seekMedium, 0, 0, 0)
        binding.textMedium.text = 0.toString()
        animateSeekBar(binding.seekHigh, 0, 0, 0)
        binding.textHigh.text = 0.toString()
        animateSeekBar(binding.seekMaster, 0, 0, 0)
        binding.textMaster.text = 0.toString()
    }

    override fun onItemClick(position: Int) {
        openContent(position, null)
    }

    override fun onItemChecked(position: Int, checked: Boolean) = runBlocking {
        mpresenter.updateQuizCheck(adapter.items[position].id, checked)

    }

    override fun onItemLongClick(position: Int) {
        if (selectedCategory != Categories.CATEGORY_SELECTIONS) {
            // TODO propose to add all words to selections
            return
        }
        val input = EditText(activity)
        input.setSingleLine()
        input.hint = getString(R.string.selection_name)
        input.setText(adapter.items[position].getName())

        val container = FrameLayout(requireActivity())
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.leftMargin = DimensionHelper.getPixelFromDip(activity, 20)
        params.rightMargin = DimensionHelper.getPixelFromDip(activity, 20)
        input.layoutParams = params

        input.addTextChangedListener(object : TextValidator(input) {
            override fun validate(textView: TextView, text: String) {
                if (text.isEmpty())
                    input.error = getString(R.string.selection_not_empty_name)
                else
                    input.error = null
            }
        })
        container.addView(input)

        requireContext().alertDialog {
            titleResource = R.string.selection_edit
            setView(container)

            neutralButton(R.string.action_delete) {
                requireContext().alertDialog {
                    titleResource = R.string.selection_delete_sure
                    okButton {
                        runBlocking {
                            mpresenter.deleteQuiz(adapter.items[position].id)
                            adapter.deleteItem(position)
                        }
                    }
                    cancelButton { }
                }.show()
            }
            okButton {
                if (adapter.items[position].getName() != input.text.toString() && input.error == null) {
                    runBlocking {
                        mpresenter.updateQuizName(adapter.items[position].id, input.text.toString())
                    }
                    adapter.items[position].nameFr = input.text.toString()
                    adapter.items[position].nameEn = input.text.toString()
                    adapter.notifyItemChanged(position)
                }
            }
            cancelButton { }
        }.show()
    }

    override fun addSelection() {
        val input = EditText(activity)
        input.setSingleLine()
        input.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        input.hint = getString(R.string.selection_name)

        val container = FrameLayout(requireActivity())
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.leftMargin = DimensionHelper.getPixelFromDip(activity, 20)
        params.rightMargin = DimensionHelper.getPixelFromDip(activity, 20)
        input.layoutParams = params

        input.addTextChangedListener(object : TextValidator(input) {
            override fun validate(textView: TextView, text: String) {
                if (text.isEmpty()) {
                    input.error = getString(R.string.selection_not_empty_name)
                } else {
                    input.error = null
                }
            }
        })
        input.error = getString(R.string.selection_not_empty_name)
        container.addView(input)

        requireContext().alertDialog {
            titleResource = R.string.new_selection
            setView(container)

            okButton {
                if (input.error == null) {
                    runBlocking {
                        mpresenter.createQuiz(input.text.toString())
                    }
                }
            }
            cancelButton { }
        }.show()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun tutos() = lifecycleScope.launch {
        withContext(IO) {
            sleep(500)
        }
        if (activity != null) {
            spotlightTuto(requireActivity(), binding.btnPronunciationQcmSwitch, getString(R.string.tuto_quiz_type), getString(R.string.tuto_quiz_type_message)
            ) {
                if (activity != null) {
                    spotlightTuto(requireActivity(),
                        binding.textLow,
                        getString(R.string.tuto_progress),
                        getString(R.string.tuto_progress_message)
                    ) {
                        if (activity != null) {
                            spotlightTuto(requireActivity(),
                                binding.recyclerview.findViewHolderForAdapterPosition(0)?.itemView?.findViewById(
                                    R.id.quiz_check
                                ),
                                getString(R.string.tuto_part_selection),
                                getString(R.string.tuto_part_selection_message)
                            ) { }
                        }
                    }
                }
            }
        }
    }
}
