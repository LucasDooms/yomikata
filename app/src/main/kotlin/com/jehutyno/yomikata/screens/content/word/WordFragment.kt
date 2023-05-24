package com.jehutyno.yomikata.screens.content.word

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.VhKanjiSoloBinding
import com.jehutyno.yomikata.databinding.VhWordDetailBinding
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.model.getWordColor
import com.jehutyno.yomikata.util.QuizType
import com.jehutyno.yomikata.util.getWordPositionInFuriSentence
import com.jehutyno.yomikata.util.sentenceNoAnswerFuri
import com.jehutyno.yomikata.util.sentenceNoFuri


class WordFragment (
    private val wordKanjiSentence: Triple<LiveData<Word>, List<KanjiSoloRadical?>, Sentence>,
    private val quizType: QuizType?,
    private val callback: WordPagerAdapter.InternalCallback
) : Fragment() {

    // View Binding
    private var _binding: VhWordDetailBinding? = null
    private val binding get() = _binding!!

    private var displaySeparator = false


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = VhWordDetailBinding.inflate(inflater, container, false)

        wordKanjiSentence.second.forEach {
            if (it != null) {
                val radicalLayoutBinding = VhKanjiSoloBinding.inflate(inflater, container, false)
                radicalLayoutBinding.separator.visibility = if (displaySeparator) View.VISIBLE else View.GONE
                radicalLayoutBinding.kanjiSolo.text = it.kanji
                radicalLayoutBinding.ksTrad.text = it.getTrad()
                radicalLayoutBinding.ksStrokes.text = it.strokes.toString()
                radicalLayoutBinding.kunyomiTitle.visibility = if (it.kunyomi.isEmpty() || quizType != null) View.GONE else View.VISIBLE
                radicalLayoutBinding.ksKunyomi.visibility = if (it.kunyomi.isEmpty() || quizType != null) View.GONE else View.VISIBLE
                radicalLayoutBinding.ksKunyomi.text = it.kunyomi
                radicalLayoutBinding.onyomiTitle.visibility = if (it.onyomi.isEmpty() || quizType != null) View.GONE else View.VISIBLE
                radicalLayoutBinding.ksOnyomi.visibility = if (it.onyomi.isEmpty() || quizType != null) View.GONE else View.VISIBLE
                radicalLayoutBinding.ksOnyomi.text = it.onyomi
                radicalLayoutBinding.radical.text = it.radical
                radicalLayoutBinding.radicalStroke.text = it.radStroke.toString()
                radicalLayoutBinding.radicalTrad.text = it.getRadTrad()
                // assign _binding first!!
                binding.containerInfo.addView(radicalLayoutBinding.root)
                displaySeparator = true
            }
        }
        return binding.root
    }

    private fun subscribeWord() {
        wordKanjiSentence.first.observe(viewLifecycleOwner) { word ->
            val wordDisplay =
                if (quizType != null)
                    word.japanese
                else
                    "    {${word.japanese};${word.reading}}    "
            wordDisplay.let { binding.furiWord.text_set(wordDisplay, 0, it.length,
                getWordColor(requireContext(), word.points)
            ) }

            binding.textTraduction.text = word.getTrad()

            val wordTruePosition = wordKanjiSentence.third.jap.let { getWordPositionInFuriSentence(it, word) }
            wordTruePosition.let {
                binding.furiSentence.text_set (
                    if (quizType == null)
                        wordKanjiSentence.third.jap
                    else
                        sentenceNoAnswerFuri(wordKanjiSentence.third, word),
                    it,
                    wordTruePosition + word.japanese.length,
                    getWordColor(requireContext(), word.points)
                )
            }

            binding.textTraduction.visibility =
                if (quizType == QuizType.TYPE_JAP_EN || word.isKana == 2)
                    View.INVISIBLE else View.VISIBLE

            binding.btnCopy.setOnClickListener(getCopyListener(word))
            binding.btnSelection.setOnClickListener { callback.onSelectionClick(binding.btnSelection, word) }
            binding.btnReport.setOnClickListener { callback.onReportClick(
                Triple(word, wordKanjiSentence.second, wordKanjiSentence.third)
            ) }
            binding.btnTts.setOnClickListener { callback.onWordTTSClick(word) }

            binding.levelDown.setOnClickListener { callback.onLevelDown(word) }
            binding.levelUp.setOnClickListener { callback.onLevelUp(word) }
        }
    }

    private fun getCopyListener(word: Word): (View?) -> Unit {
        return {
            val popup = PopupMenu(requireActivity(), binding.btnCopy)
            popup.menuInflater.inflate(R.menu.popup_copy, popup.menu)
            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.copy_word -> {
                        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText(
                            requireContext().getString(R.string.copy_word),
                            word.japanese)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(requireActivity(), requireContext().getString(R.string.word_copied), Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText(
                            requireContext().getString(R.string.copy_sentence),
                            sentenceNoFuri(wordKanjiSentence.third)
                        )
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(requireActivity(), requireContext().getString(R.string.sentence_copied), Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
            popup.show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.levelDown.visibility = if (quizType != null) View.GONE else View.VISIBLE
        binding.levelUp.visibility = if (quizType != null) View.GONE else View.VISIBLE


        binding.textSentenceEn.text = wordKanjiSentence.third.getTrad()

        binding.btnSentenceTts.setOnClickListener { callback.onSentenceTTSClick(wordKanjiSentence.third) }
        binding.close.setOnClickListener { callback.onCloseClick() }

        binding.containerInfo.visibility =
            if (wordKanjiSentence.second.isEmpty() || wordKanjiSentence.second[0] == null)
                View.GONE else View.VISIBLE

    }

    override fun onStart() {
        super.onStart()

        subscribeWord()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
