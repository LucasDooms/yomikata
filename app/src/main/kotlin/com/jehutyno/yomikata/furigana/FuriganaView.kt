/*
 * FuriganaView widget
 * Copyright (C) 2013 sh0 <sh0@yutani.ee>
 * Licensed under Creative Commons BY-SA 3.0
 */
// Package
package com.jehutyno.yomikata.furigana

import android.content.Context
import android.graphics.Canvas
import android.preference.PreferenceManager
import android.text.TextPaint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import java.util.Vector
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

// Imports
// Text view with furigana display
class FuriganaView : AppCompatTextView {
    // Paints
    private var m_paint_f = TextPaint()
    private var m_paint_k_norm = TextPaint()
    private var m_paint_k_mark = TextPaint()

    // Sizes
    private var m_linesize = 0.0f
    private var m_height_n = 0.0f
    private var m_height_f = 0.0f
    private var m_linemax = 0.0f

    // Spans and lines
    private val m_span = Vector<Span>()
    private val m_line_n = Vector<LineNormal>()
    private val m_line_f = Vector<LineFurigana>()

    // Constructors
    constructor(context: Context?) : super(context!!) {
        this.textSize = PreferenceManager.getDefaultSharedPreferences(context)
            .getString("font_size", "23")!!.toFloat()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
        this.textSize = PreferenceManager.getDefaultSharedPreferences(context)
            .getString("font_size", "23")!!.toFloat()
    }

    constructor(context: Context?, attrs: AttributeSet?, style: Int) : super(
        context!!, attrs, style
    ) {
        this.textSize = PreferenceManager.getDefaultSharedPreferences(context)
            .getString("font_size", "23")!!.toFloat()
    }

    override fun setTextColor(color: Int) {
        super.setTextColor(color)
        invalidate()
    }

    fun updateText(text: String) {
        text_set(text, 0, 0, 0)
    }

    // Text functions
    fun text_set(text: String, mark_s: Int, mark_e: Int, color: Int) {
        var text = text
        var mark_s = mark_s
        var mark_e = mark_e
        text = text.replace("{は;わ}", "は")
        text = text.replace("{へ;え}", "へ")
        // Text
        m_paint_k_norm = TextPaint(paint)
        m_paint_k_mark = TextPaint(paint)
        m_paint_k_mark.isFakeBoldText = true
        m_paint_k_mark.color = color
        m_paint_f = TextPaint(paint)
        m_paint_f.textSize = paint.textSize / 2.0f

        // Linesize
        m_height_n = m_paint_k_norm.descent() - m_paint_k_norm.ascent()
        m_height_f = m_paint_f.descent() - m_paint_f.ascent()
        m_linesize = m_height_n + m_height_f

        // Clear spans
        m_span.clear()

        // Sizes
        m_linesize = (m_paint_f.fontSpacing + max(
            m_paint_k_norm.fontSpacing.toDouble(),
            m_paint_k_mark.fontSpacing.toDouble()
        )).toFloat()

        // Spannify text
        while (text.length > 0) {
            var idx = text.indexOf('{')
            if (idx >= 0) {
                // Prefix string
                if (idx > 0) {
                    // Spans
                    m_span.add(Span("", text.substring(0, idx), mark_s, mark_e))

                    // Remove text
                    text = text.substring(idx)
                    mark_s -= idx
                    mark_e -= idx
                }

                // End bracket
                idx = text.indexOf('}')
                if (idx < 1) {
                    // Error
                    text = ""
                    break
                } else if (idx == 1) {
                    // Empty bracket
                    text = text.substring(2)
                    continue
                }

                // Spans
                val split =
                    text.substring(1, idx).split(";".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                m_span.add(Span(if (split.size > 1) split[1] else "", split[0], mark_s, mark_e))

                // Remove text
                text = text.substring(idx + 1)
                mark_s -= split[0].length
                mark_e -= split[0].length
            } else {
                // Single span
                m_span.add(Span("", text, mark_s, mark_e))
                text = ""
            }
        }

        // Invalidate view
        this.invalidate()
        requestLayout()
    }

    // Size calculation
    override fun onMeasure(width_ms: Int, height_ms: Int) {
        // Modes
        val wmode = MeasureSpec.getMode(width_ms)
        val hmode = MeasureSpec.getMode(height_ms)

        // Dimensions
        val wold = MeasureSpec.getSize(width_ms)
        val hold = MeasureSpec.getSize(height_ms)

        // Draw mode
        if (wmode == MeasureSpec.EXACTLY || wmode == MeasureSpec.AT_MOST && wold > 0) {
            // Width limited
            text_calculate(wold.toFloat())
        } else {
            // Width unlimited
            text_calculate(-1.0f)
        }

        // New height
        var hnew = Math.round(ceil((m_linesize * m_line_n.size.toFloat()).toDouble())).toInt()
        var wnew = wold
        if (wmode != MeasureSpec.EXACTLY && m_line_n.size <= 1) wnew =
            Math.round(ceil(m_linemax.toDouble())).toInt()
        if (hmode != MeasureSpec.UNSPECIFIED && hnew > hold) hnew = hnew or MEASURED_STATE_TOO_SMALL

        // Set result
        setMeasuredDimension(wnew, hnew)
    }

    private fun text_calculate(line_max: Float) {
        // Clear lines
        m_line_n.clear()
        m_line_f.clear()

        // Sizes
        m_linemax = 0.0f

        // Check if no limits on width
        if (line_max < 0.0) {

            // Create single normal and furigana line
            val line_n = LineNormal()
            val line_f = LineFurigana()

            // Loop spans
            for (span in m_span) {
                // Text
                line_n.add(span.normal())
                line_f.add(span.furigana(m_linemax))

                // Widths update
                for (width in span.widths()) m_linemax += width
            }

            // Commit both lines
            m_line_n.add(line_n)
            m_line_f.add(line_f)
        } else {

            // Lines
            var line_x = 0.0f
            var line_n = LineNormal()
            var line_f = LineFurigana()
            if (!m_span.isEmpty()) {
                // Initial span
                var span_i = 0
                var span = m_span[span_i]

                // Iterate
                while (span != null) {
                    // Start offset
                    val line_s = line_x

                    // Calculate possible line size
                    val widths = span.widths()
                    var i = 0
                    i = 0
                    while (i < widths.size) {
                        line_x += if (line_x + widths[i] <= line_max) widths[i] else break
                        i++
                    }

                    // Add span to line
                    if (i >= 0 && i < widths.size) {

                        // Span does not fit entirely
                        if (i > 0) {
                            // Split half that fits
                            val normal_a = Vector<TextNormal>()
                            val normal_b = Vector<TextNormal>()
                            span.split(i, normal_a, normal_b)
                            line_n.add(normal_a)
                            span = Span(normal_b)
                        }

                        // Add new line with current spans
                        if (line_n.size() != 0) {
                            // Add
                            m_linemax = if (m_linemax > line_x) m_linemax else line_x
                            m_line_n.add(line_n)
                            m_line_f.add(line_f)

                            // Reset
                            line_n = LineNormal()
                            line_f = LineFurigana()
                            line_x = 0.0f

                            // Next span
                            continue
                        }
                    } else {

                        // Span fits entirely
                        line_n.add(span.normal())
                        line_f.add(span.furigana(line_s))
                    }

                    // Next span
                    span = null
                    span_i++
                    if (span_i < m_span.size) span = m_span[span_i]
                }

                // Last span
                if (line_n.size() != 0) {
                    // Add
                    m_linemax = if (m_linemax > line_x) m_linemax else line_x
                    m_line_n.add(line_n)
                    m_line_f.add(line_f)
                }
            }
        }

        // Calculate furigana
        for (line in m_line_f) line.calculate()
    }

    // Drawing
    public override fun onDraw(canvas: Canvas) {
        /*
        // Debug background
        Paint paint = new Paint();
        paint.setARGB(0x30, 0, 0, 0xff);
        Rect rect = new Rect();
        canvas.getClipBounds(rect);
        canvas.drawRect(rect, paint);
        */

        // Check
        assert(m_line_n.size == m_line_f.size)

        // Coordinates
        var y = m_linesize

        // Loop lines
        for (i in m_line_n.indices) {
            m_line_n[i].draw(canvas, y)
            m_line_f[i].draw(canvas, y - m_height_n)
            y += m_linesize
        }
    }

    private inner class TextFurigana(// Info
        private val m_text: String, private val is_colored: Boolean
    ) {
        // Coordinates
        var m_offset = 0.0f
        var m_width = 0.0f

        // Constructor
        init {
            // Info

            // Coordinates
            m_width = m_paint_f.measureText(m_text)
        }

        // Info
        //private String text() { return m_text; }
        // Coordinates
        fun offset_get(): Float {
            return m_offset
        }

        fun offset_set(value: Float) {
            m_offset = value
        }

        fun width(): Float {
            return m_width
        }

        // Draw
        fun draw(canvas: Canvas, x: Float, y: Float) {
            var x = x
            x -= m_width / 2.0f
            if (!is_colored) m_paint_f.color = m_paint_k_norm.color else m_paint_f.color =
                m_paint_k_mark.color
            canvas.drawText(m_text, 0, m_text.length, x, y, m_paint_f)
        }
    }

    private inner class TextNormal(// Info
        private val m_text: String, private val m_is_marked: Boolean
    ) {
        // Widths
        private var m_width_total: Float
        private val m_width_chars: FloatArray

        // Constructor
        init {
            // Info

            // Character widths
            m_width_chars = FloatArray(m_text.length)
            if (m_is_marked) {
                m_paint_k_mark.getTextWidths(m_text, m_width_chars)
            } else {
                m_paint_k_norm.getTextWidths(m_text, m_width_chars)
            }

            // Total width
            m_width_total = 0.0f
            for (v in m_width_chars) m_width_total += v
        }

        // Info
        fun length(): Int {
            return m_text.length
        }

        // Widths
        fun width_chars(): FloatArray {
            return m_width_chars
        }

        // Split
        fun split(offset: Int): Array<TextNormal> {
            return arrayOf(
                TextNormal(m_text.substring(0, offset), m_is_marked),
                TextNormal(m_text.substring(offset), m_is_marked)
            )
        }

        // Draw
        fun draw(canvas: Canvas, x: Float, y: Float): Float {
            if (m_is_marked) {
                canvas.drawText(m_text, 0, m_text.length, x, y, m_paint_k_mark)
            } else {
                m_paint_k_norm.color = currentTextColor
                canvas.drawText(m_text, 0, m_text.length, x, y, m_paint_k_norm)
            }
            return m_width_total
        }
    }

    private inner class LineFurigana {
        // Text
        private val m_text = Vector<TextFurigana>()
        private val m_offset = Vector<Float>()

        // Add
        fun add(text: TextFurigana?) {
            if (text != null) m_text.add(text)
        }

        // Calculate
        fun calculate() {
            // Check size
            if (m_text.size == 0) return

            /*
            // Debug
            String str = "";
            for (TextFurigana text : m_text)
                str += "'" + text.text() + "' ";
            */

            // r[] - ideal offsets
            val r = FloatArray(m_text.size)
            for (i in m_text.indices) r[i] = m_text[i].offset_get()

            // a[] - constraint matrix
            val a = Array(m_text.size + 1) { FloatArray(m_text.size) }
            for (i in a.indices) for (j in a[0].indices) a[i][j] = 0.0f
            a[0][0] = 1.0f
            for (i in 1 until a.size - 2) {
                a[i][i - 1] = -1.0f
                a[i][i] = 1.0f
            }
            a[a.size - 1][a[0].size - 1] = -1.0f

            // b[] - constraint vector
            val b = FloatArray(m_text.size + 1)
            b[0] = -r[0] + 0.5f * m_text[0].width()
            for (i in 1 until b.size - 2) b[i] =
                0.5f * (m_text[i].width() + m_text[i - 1].width()) + (r[i - 1] - r[i])
            b[b.size - 1] = -m_linemax + r[r.size - 1] + 0.5f * m_text[m_text.size - 1].width()

            // Calculate constraint optimization
            val x = FloatArray(m_text.size)
            for (i in x.indices) x[i] = 0.0f
            val co = QuadraticOptimizer(a, b)
            co.calculate(x)
            for (i in x.indices) m_offset.add(x[i] + r[i])
        }

        // Draw
        fun draw(canvas: Canvas, y: Float) {
            var y = y
            y -= m_paint_f.descent()
            if (m_offset.size == m_text.size) {
                // Render with fixed offsets
                for (i in m_offset.indices) m_text[i].draw(canvas, m_offset[i], y)
            } else {
                // Render with original offsets
                for (text in m_text) text.draw(canvas, text.offset_get(), y)
            }
        }
    }

    private inner class LineNormal {
        // Text
        private val m_text = Vector<TextNormal>()

        // Elements
        fun size(): Int {
            return m_text.size
        }

        fun add(text: Vector<TextNormal>?) {
            m_text.addAll(text!!)
        }

        // Draw
        fun draw(canvas: Canvas, y: Float) {
            var y = y
            y -= m_paint_k_norm.descent()
            var x = 0.0f
            for (text in m_text) x += text.draw(canvas, x, y)
        }
    }

    private inner class Span {
        // Text
        private var m_furigana: TextFurigana? = null
        private var m_normal = Vector<TextNormal>()

        // Widths
        private val m_width_chars = Vector<Float>()
        private var m_width_total = 0.0f

        // Constructors
        constructor(text_f: String, text_k: String, mark_s: Int, mark_e: Int) {
            // Normal text
            var mark_s = mark_s
            var mark_e = mark_e
            if (mark_s < text_k.length && mark_e > 0 && mark_s < mark_e) {

                // Fix marked bounds
                mark_s = max(0.0, mark_s.toDouble()).toInt()
                mark_e = min(text_k.length.toDouble(), mark_e.toDouble()).toInt()

                // Prefix
                if (mark_s > 0) {
                    m_normal.add(TextNormal(text_k.substring(0, mark_s), false))
                    if (text_f.length > 0) m_furigana = TextFurigana(text_f, false)
                }

                // Marked
                if (mark_e > mark_s) {
                    m_normal.add(TextNormal(text_k.substring(mark_s, mark_e), true))
                    if (text_f.length > 0) m_furigana = TextFurigana(text_f, true)
                }

                // Postfix
                if (mark_e < text_k.length) {
                    m_normal.add(TextNormal(text_k.substring(mark_e), false))
                    if (text_f.length > 0) m_furigana = TextFurigana(text_f, false)
                }
            } else {

                // Non marked
                m_normal.add(TextNormal(text_k, false))
                if (text_f.length > 0) m_furigana = TextFurigana(text_f, false)
            }

            // Widths
            widths_calculate()
        }

        constructor(normal: Vector<TextNormal>) {
            // Only normal text
            m_normal = normal

            // Widths
            widths_calculate()
        }

        // Text
        fun furigana(x: Float): TextFurigana? {
            if (m_furigana == null) return null
            m_furigana!!.offset_set(x + m_width_total / 2.0f)
            return m_furigana
        }

        fun normal(): Vector<TextNormal> {
            return m_normal
        }

        // Widths
        fun widths(): Vector<Float> {
            return m_width_chars
        }

        private fun widths_calculate() {
            // Chars
            if (m_furigana == null) {
                for (normal in m_normal) for (v in normal.width_chars()) m_width_chars.add(v)
            } else {
                var sum = 0.0f
                for (normal in m_normal) for (v in normal.width_chars()) sum += v
                m_width_chars.add(sum)
            }

            // Total
            m_width_total = 0.0f
            for (v in m_width_chars) m_width_total += v
        }

        // Split
        fun split(offset: Int, normal_a: Vector<TextNormal>, normal_b: Vector<TextNormal>) {
            // Check if no furigana
            var offset = offset
            assert(m_furigana == null)

            // Split normal list
            for (cur in m_normal) {
                if (offset <= 0) {
                    normal_b.add(cur)
                } else if (offset >= cur.length()) {
                    normal_a.add(cur)
                } else {
                    val split = cur.split(offset)
                    normal_a.add(split[0])
                    normal_b.add(split[1])
                }
                offset -= cur.length()
            }
        }
    }
}
