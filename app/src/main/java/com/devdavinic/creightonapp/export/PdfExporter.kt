package com.devdavinic.creightonapp.export

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.devdavinic.creightonapp.model.DailyRecord
import com.devdavinic.creightonapp.model.StampType
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// =============================================================================
// PDF EXPORTER v2
// Layout: A4 portrait, 4 cycles per page, max 35 days per cycle row.
// Each cycle = 3 rows: dates | colored stamps | official codes
// =============================================================================

object PdfExporter {

    // A4 portrait in points
    private const val PAGE_W = 595
    private const val PAGE_H = 842

    private const val MARGIN_H = 24f
    private const val MARGIN_TOP = 20f

    // Per cycle block
    private const val CYCLES_PER_PAGE = 4
    private const val MAX_DAYS = 35

    // Row heights inside each cycle block
    private const val ROW_CYCLE_LABEL = 14f
    private const val ROW_DATE        = 12f
    private const val ROW_STAMP       = 26f
    private const val ROW_CODE        = 13f
    private const val ROW_GAP         = 8f   // gap between cycle blocks

    // Header height
    private const val HEADER_H = 50f
    private const val LEGEND_H = 22f

    // Computed cell width: fit 35 cells in page width minus margins
    private val CELL_W: Float get() {
        val available = PAGE_W - MARGIN_H * 2
        return available / MAX_DAYS
    }

    // Colors
    private val C_RED          = Color.rgb(0xEF, 0x44, 0x44)
    private val C_RED_BORDER   = Color.rgb(0xB9, 0x1C, 0x1C)
    private val C_GREEN        = Color.rgb(0x34, 0xD3, 0x99)
    private val C_GREEN_BORDER = Color.rgb(0x05, 0x96, 0x69)
    private val C_WHITE        = Color.WHITE
    private val C_WHITE_BORDER = Color.rgb(0x34, 0xD3, 0x99)
    private val C_LGREEN       = Color.rgb(0xA7, 0xF3, 0xD0)
    private val C_LGREEN_BORDER= Color.rgb(0x34, 0xD3, 0x99)
    private val C_EMPTY        = Color.rgb(0xF3, 0xF4, 0xF6)
    private val C_EMPTY_BORDER = Color.rgb(0xD1, 0xD5, 0xDB)

    private val C_TEXT         = Color.rgb(0x11, 0x18, 0x27)
    private val C_TEXT_MID     = Color.rgb(0x4B, 0x55, 0x63)
    private val C_TEXT_LIGHT   = Color.rgb(0x9C, 0xA3, 0xAF)
    private val C_HEADER_BG    = Color.rgb(0x05, 0x96, 0x69)
    private val C_ACCENT       = Color.rgb(0x05, 0x96, 0x69)
    private val C_PEAK_MARK    = Color.rgb(0x7C, 0x3A, 0xED)
    private val C_DOT_I        = Color.rgb(0x60, 0x6E, 0xFF)
    private val C_DOT_AM       = Color.rgb(0xEC, 0x48, 0x99)
    private val C_STRIPE       = Color.rgb(0xF0, 0xFD, 0xF4)
    private val C_LINE         = Color.rgb(0xE5, 0xE7, 0xEB)

    fun generate(
        cycles: List<List<DailyRecord>>,
        userName: String,
        context: Context
    ): File {
        val doc    = PdfDocument()
        val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("es"))
        val dayFmt  = SimpleDateFormat("d/M", Locale("es"))
        val now     = dateFmt.format(Date())

        val pages = cycles.chunked(CYCLES_PER_PAGE)

        pages.forEachIndexed { pageIdx, pageCycles ->
            val info   = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageIdx + 1).create()
            val page   = doc.startPage(info)
            val canvas = page.canvas

            // White background
            canvas.drawColor(Color.WHITE)

            // Draw header on every page
            var y = drawHeader(canvas, userName, now, cycles.size, pageIdx + 1, pages.size)

            // Draw 4 cycle blocks
            pageCycles.forEachIndexed { cycleIdxInPage, cycleRecords ->
                val globalCycleIdx = pageIdx * CYCLES_PER_PAGE + cycleIdxInPage + 1

                // Alternating stripe
                if (cycleIdxInPage % 2 == 1) {
                    val blockH = ROW_CYCLE_LABEL + ROW_DATE + ROW_STAMP + ROW_CODE + 4f
                    val paint  = Paint()
                    paint.color = C_STRIPE
                    canvas.drawRect(0f, y - 2f, PAGE_W.toFloat(), y + blockH, paint)
                }

                y = drawCycleBlock(canvas, globalCycleIdx, cycleRecords, y, dayFmt)
                y += ROW_GAP
            }

            // If fewer than 4 cycles on this page, draw empty placeholders
            val remaining = CYCLES_PER_PAGE - pageCycles.size
            repeat(remaining) { i ->
                val num = pageIdx * CYCLES_PER_PAGE + pageCycles.size + i + 1
                if (num <= cycles.size) return@repeat
                y = drawEmptyCycleBlock(canvas, num, y)
                y += ROW_GAP
            }

            doc.finishPage(page)
        }

        val fileName = "CreightonApp_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.pdf"
        val file     = File(context.getExternalFilesDir(null), fileName)
        doc.writeTo(file.outputStream())
        doc.close()
        return file
    }

    // =========================================================================
    // HEADER
    // =========================================================================

    private fun drawHeader(
        canvas: Canvas, userName: String, date: String,
        totalCycles: Int, pageNum: Int, totalPages: Int
    ): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Green header bar
        paint.color = C_HEADER_BG
        canvas.drawRect(0f, 0f, PAGE_W.toFloat(), HEADER_H, paint)

        // App name
        paint.color = Color.WHITE
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CreightonApp  —  Planilla NaProTRACKING", MARGIN_H, 18f, paint)

        // Sub info
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(
            "Usuaria: $userName    Exportado: $date    Ciclos: $totalCycles    Pag. $pageNum / $totalPages",
            MARGIN_H, 32f, paint
        )

        // Day numbers header (1..35)
        paint.color = Color.WHITE.also { paint.alpha = 180 }
        paint.textSize = 7f
        paint.textAlign = Paint.Align.CENTER
        for (d in 1..MAX_DAYS) {
            val x = MARGIN_H + (d - 1) * CELL_W + CELL_W / 2
            canvas.drawText(d.toString(), x, 44f, paint)
        }
        paint.textAlign = Paint.Align.LEFT
        paint.alpha = 255

        // Legend
        var lx = MARGIN_H
        val ly = HEADER_H + 14f
        paint.textSize = 8f
        paint.color = C_TEXT_MID
        canvas.drawText("Referencias:", lx, ly, paint)
        lx += 62f

        listOf(
            C_RED    to C_RED_BORDER    to "Menstrual",
            C_GREEN  to C_GREEN_BORDER  to "Infertil seco",
            C_WHITE  to C_WHITE_BORDER  to "Fertil/Moco",
            C_LGREEN to C_LGREEN_BORDER to "Post-Pico seco"
        ).forEach { (colors, label) ->
            val (fill, border) = colors
            val rp = Paint(Paint.ANTI_ALIAS_FLAG)
            rp.color = fill; rp.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(lx, ly - 8f, lx + 10f, ly + 1f), 2f, 2f, rp)
            rp.color = border; rp.style = Paint.Style.STROKE; rp.strokeWidth = 1f
            canvas.drawRoundRect(RectF(lx, ly - 8f, lx + 10f, ly + 1f), 2f, 2f, rp)
            paint.color = C_TEXT_MID
            canvas.drawText(label, lx + 13f, ly, paint)
            lx += label.length * 4.8f + 18f
        }

        // P = Pico marker in legend
        paint.color = C_PEAK_MARK
        canvas.drawText("P = Dia Pico", lx, ly, paint)
        lx += 58f
        paint.color = C_DOT_I
        canvas.drawText("● I = Intercurso", lx, ly, paint)
        lx += 75f
        paint.color = C_DOT_AM
        canvas.drawText("● AM = Autoexamen", lx, ly, paint)

        // Separator line
        val lp = Paint(); lp.color = C_LINE; lp.strokeWidth = 0.5f
        canvas.drawLine(MARGIN_H, HEADER_H + LEGEND_H, PAGE_W - MARGIN_H, HEADER_H + LEGEND_H, lp)

        return HEADER_H + LEGEND_H + 6f
    }

    // =========================================================================
    // CYCLE BLOCK
    // =========================================================================

    private fun drawCycleBlock(
        canvas: Canvas, cycleNum: Int,
        records: List<DailyRecord>, startY: Float,
        dayFmt: SimpleDateFormat
    ): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val days  = records.take(MAX_DAYS)
        var y     = startY

        // Cycle label
        paint.color = C_ACCENT
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val firstDate = days.firstOrNull()?.date?.let {
            SimpleDateFormat("d MMM yyyy", Locale("es")).format(Date(it))
        } ?: ""
        canvas.drawText("Ciclo $cycleNum   —   $firstDate   (${days.size} dias)", MARGIN_H, y + 10f, paint)
        y += ROW_CYCLE_LABEL

        // ── Row 1: Dates ──────────────────────────────────────────────────────
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 6.5f
        paint.textAlign = Paint.Align.CENTER

        for (i in 0 until MAX_DAYS) {
            val x = MARGIN_H + i * CELL_W + CELL_W / 2
            if (i < days.size) {
                paint.color = C_TEXT_MID
                canvas.drawText(dayFmt.format(Date(days[i].date)), x, y + 9f, paint)
            } else {
                // Empty cell marker
                paint.color = C_TEXT_LIGHT
                canvas.drawText("·", x, y + 9f, paint)
            }
        }
        y += ROW_DATE

        // ── Row 2: Stamps ─────────────────────────────────────────────────────
        for (i in 0 until MAX_DAYS) {
            val x    = MARGIN_H + i * CELL_W
            val rect = RectF(x + 0.5f, y + 0.5f, x + CELL_W - 0.5f, y + ROW_STAMP - 0.5f)

            if (i < days.size) {
                drawStampCell(canvas, rect, days[i])
            } else {
                // Empty placeholder
                paint.style = Paint.Style.FILL
                paint.color = C_EMPTY
                canvas.drawRoundRect(rect, 3f, 3f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.5f
                paint.color = C_EMPTY_BORDER
                canvas.drawRoundRect(rect, 3f, 3f, paint)
                paint.style = Paint.Style.FILL
            }
        }
        y += ROW_STAMP

        // ── Row 3: Official codes ─────────────────────────────────────────────
        paint.textSize = 5.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.CENTER

        for (i in 0 until MAX_DAYS) {
            val x = MARGIN_H + i * CELL_W + CELL_W / 2
            if (i < days.size) {
                val record = days[i]
                paint.color = when (record.stampType) {
                    StampType.RED.name -> C_RED_BORDER
                    else               -> C_TEXT
                }
                // Fit long codes: use first 7 chars
                val code = record.officialCode.take(7)
                canvas.drawText(code, x, y + 9f, paint)
            }
        }
        y += ROW_CODE

        // Bottom separator
        val sp = Paint(); sp.color = C_LINE; sp.strokeWidth = 0.5f
        canvas.drawLine(MARGIN_H, y + 1f, PAGE_W - MARGIN_H, y + 1f, sp)

        paint.textAlign = Paint.Align.LEFT
        return y + 2f
    }

    private fun drawEmptyCycleBlock(canvas: Canvas, cycleNum: Int, startY: Float): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var y = startY

        paint.color = C_TEXT_LIGHT
        paint.textSize = 8.5f
        canvas.drawText("Ciclo $cycleNum   —   (sin registros)", MARGIN_H, y + 10f, paint)
        y += ROW_CYCLE_LABEL

        // Draw 35 empty cells
        for (i in 0 until MAX_DAYS) {
            val x    = MARGIN_H + i * CELL_W
            val rect = RectF(x + 0.5f, y + ROW_DATE + 0.5f, x + CELL_W - 0.5f,
                y + ROW_DATE + ROW_STAMP - 0.5f)
            paint.style = Paint.Style.FILL; paint.color = C_EMPTY
            canvas.drawRoundRect(rect, 3f, 3f, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.5f; paint.color = C_EMPTY_BORDER
            canvas.drawRoundRect(rect, 3f, 3f, paint)
            paint.style = Paint.Style.FILL
        }

        val sp = Paint(); sp.color = C_LINE; sp.strokeWidth = 0.5f
        val endY = y + ROW_DATE + ROW_STAMP + ROW_CODE + 2f
        canvas.drawLine(MARGIN_H, endY, PAGE_W - MARGIN_H, endY, sp)
        return endY
    }

    // =========================================================================
    // STAMP CELL
    // =========================================================================

    private fun drawStampCell(canvas: Canvas, rect: RectF, record: DailyRecord) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cw    = rect.width()
        val ch    = rect.height()
        val cx    = rect.left + cw / 2
        val cy    = rect.top + ch / 2

        // Fill
        val (fill, border) = stampColors(record.stampType)
        paint.style = Paint.Style.FILL
        paint.color = fill
        canvas.drawRoundRect(rect, 3f, 3f, paint)

        // Border — thicker for peak day
        paint.style       = Paint.Style.STROKE
        paint.strokeWidth = if (record.isPeakDay) 2f else 1f
        paint.color       = if (record.isPeakDay) C_PEAK_MARK else border
        canvas.drawRoundRect(rect, 3f, 3f, paint)
        paint.style = Paint.Style.FILL

        // ── Center content ────────────────────────────────────────────────────

        val textColor = if (record.stampType == StampType.RED.name) Color.WHITE else C_TEXT

        // Peak "P" — large, centered
        if (record.isPeakDay) {
            paint.color    = C_PEAK_MARK
            paint.textSize = ch * 0.45f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("P", cx, cy + ch * 0.16f, paint)
        }
        // Post-peak number
        else if (record.postPeakCount in 1..3) {
            paint.color    = C_GREEN_BORDER
            paint.textSize = ch * 0.42f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(record.postPeakCount.toString(), cx, cy + ch * 0.16f, paint)
        }
        // Baby symbol for fertile
        else if (record.stampType == StampType.WHITE_BABY.name ||
            record.stampType == StampType.GREEN_BABY.name) {
            paint.color    = C_GREEN_BORDER
            paint.textSize = ch * 0.38f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("b", cx, cy + ch * 0.18f, paint)
        }

        // ── Corner markers ────────────────────────────────────────────────────

        // Intercourse dot — bottom right
        if (record.hasIntercourse) {
            paint.color  = C_DOT_I
            paint.textAlign = Paint.Align.LEFT
            canvas.drawCircle(rect.right - 3f, rect.bottom - 3f, 2.2f, paint)
        }

        // AM dot — top right (pink)
        if (record.breastSelfExam) {
            paint.color = C_DOT_AM
            canvas.drawCircle(rect.right - 3f, rect.top + 3f, 2.2f, paint)
        }

        paint.textAlign = Paint.Align.LEFT
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private fun stampColors(stampType: String): Pair<Int, Int> = when (stampType) {
        StampType.RED.name         -> C_RED    to C_RED_BORDER
        StampType.GREEN_SOLID.name -> C_GREEN  to C_GREEN_BORDER
        StampType.WHITE_BABY.name  -> C_WHITE  to C_WHITE_BORDER
        StampType.GREEN_BABY.name  -> C_LGREEN to C_LGREEN_BORDER
        else                       -> C_EMPTY  to C_EMPTY_BORDER
    }

    fun groupIntoCycles(records: List<DailyRecord>): List<List<DailyRecord>> {
        if (records.isEmpty()) return emptyList()
        val sorted  = records.filter { !it.isPartial }.sortedBy { it.date }
        val cycles  = mutableListOf<MutableList<DailyRecord>>()
        var current = mutableListOf<DailyRecord>()
        for (i in sorted.indices) {
            val record   = sorted[i]
            val isMenst  = record.bleedingLevel == "H" || record.bleedingLevel == "M"
            if (isMenst && current.isNotEmpty()) {
                val prev      = sorted[i - 1]
                val prevMenst = prev.bleedingLevel == "H" || prev.bleedingLevel == "M"
                val gap       = (record.date - prev.date) / 86_400_000L
                if (!prevMenst || gap > 2) { cycles.add(current); current = mutableListOf() }
            }
            current.add(record)
        }
        if (current.isNotEmpty()) cycles.add(current)
        return cycles
    }
}