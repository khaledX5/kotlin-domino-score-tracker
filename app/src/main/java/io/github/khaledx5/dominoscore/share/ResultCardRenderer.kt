package io.github.khaledx5.dominoscore.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import io.github.khaledx5.dominoscore.ui.theme.MidnightTeal

/**
 * Draws the result image (1080 x 1350, the 4:5 size chat apps and stories show uncropped) in the
 * Midnight Teal colors: brand, trophy, winner and score, then the final ranking with KING and KOOZ.
 * Plain Canvas drawing, so it needs no window and runs on a background thread.
 */
class ResultCardRenderer(private val regular: Typeface, private val bold: Typeface) {

    fun render(card: ResultCard): Bitmap {
        val bitmap = createBitmap(WIDTH, HEIGHT)
        val canvas = Canvas(bitmap)
        canvas.drawColor(NAVY)
        drawHeader(canvas, card)
        drawRanking(canvas, card)
        drawCentered(canvas, card.footer, y = HEIGHT - MARGIN, paint = text(regular, FOOTER_SIZE, MUTED))
        return bitmap
    }

    private fun drawHeader(canvas: Canvas, card: ResultCard) {
        drawCentered(canvas, card.brand, y = 120f, paint = text(bold, BRAND_SIZE, MINT).apply { letterSpacing = 0.08f })
        drawCentered(canvas, card.trophy, y = 300f, paint = text(regular, TROPHY_SIZE, WHITE))
        drawCentered(canvas, card.winnerLine, y = 420f, paint = text(bold, WINNER_SIZE, WHITE))
        drawCentered(canvas, card.scoreLine, y = 495f, paint = text(bold, SCORE_LINE_SIZE, GOLD))
    }

    private fun drawRanking(canvas: Canvas, card: ResultCard) {
        val panelTop = 560f
        val panel = RectF(MARGIN, panelTop, WIDTH - MARGIN, panelTop + PANEL_PADDING * 2 + card.rows.size * ROW_HEIGHT)
        canvas.drawRoundRect(panel, PANEL_RADIUS, PANEL_RADIUS, fill(SLATE))
        card.rows.forEachIndexed { index, row ->
            val top = panel.top + PANEL_PADDING + index * ROW_HEIGHT
            drawRow(
                canvas,
                row,
                RectF(panel.left + PANEL_PADDING, top, panel.right - PANEL_PADDING, top + ROW_HEIGHT),
                card.rtl,
            )
        }
    }

    private fun drawRow(canvas: Canvas, row: ResultCardRow, bounds: RectF, rtl: Boolean) {
        val inner = RectF(bounds.left, bounds.top + ROW_GAP / 2, bounds.right, bounds.bottom - ROW_GAP / 2)
        if (row.title == CardTitle.KING) {
            canvas.drawRoundRect(inner, ROW_RADIUS, ROW_RADIUS, fill(GOLD_WASH))
            canvas.drawRoundRect(inner, ROW_RADIUS, ROW_RADIUS, stroke(GOLD))
        }
        val start = if (rtl) inner.right - ROW_PADDING else inner.left + ROW_PADDING
        val end = if (rtl) inner.left + ROW_PADDING else inner.right - ROW_PADDING
        val towardEnd = if (rtl) -1f else 1f
        val nameBaseline = inner.top + if (row.titleLabel == null) inner.height() / 2 + NAME_SIZE / 3 else 72f

        val markPaint = text(bold, MARK_SIZE, MUTED).apply { textAlign = alignAt(start = true, rtl) }
        canvas.drawText(row.mark, start, inner.centerY() + MARK_SIZE / 3, markPaint)

        val scorePaint = text(bold, ROW_SCORE_SIZE, WHITE).apply { textAlign = alignAt(start = false, rtl) }
        canvas.drawText(row.score, end, inner.centerY() + ROW_SCORE_SIZE / 3, scorePaint)

        val nameStart = start + towardEnd * MARK_COLUMN
        val nameWidth = inner.width() - ROW_PADDING * 2 - MARK_COLUMN - SCORE_COLUMN
        val namePaint = text(bold, NAME_SIZE, WHITE).apply { textAlign = alignAt(start = true, rtl) }
        val name = TextUtils.ellipsize(row.name, namePaint, nameWidth, TextUtils.TruncateAt.END).toString()
        canvas.drawText(name, nameStart, nameBaseline, namePaint)

        if (row.title != null && row.titleLabel != null) {
            drawTitlePill(canvas, row, nameStart, nameBaseline + 22f, rtl)
        }
    }

    private fun drawTitlePill(canvas: Canvas, row: ResultCardRow, start: Float, top: Float, rtl: Boolean) {
        val label = row.titleLabel ?: return
        val paint = text(bold, PILL_TEXT_SIZE, NAVY)
        val width = paint.measureText(label) + PILL_PADDING * 2
        val left = if (rtl) start - width else start
        val pill = RectF(left, top, left + width, top + PILL_HEIGHT)
        canvas.drawRoundRect(
            pill,
            PILL_HEIGHT / 2,
            PILL_HEIGHT / 2,
            fill(
                if (row.title ==
                    CardTitle.KING
                ) {
                    GOLD
                } else {
                    CORAL
                },
            ),
        )
        canvas.drawText(label, pill.left + PILL_PADDING, pill.centerY() + PILL_TEXT_SIZE / 3, paint)
    }

    private fun drawCentered(canvas: Canvas, value: String, y: Float, paint: TextPaint) {
        paint.textAlign = Paint.Align.CENTER
        val fitted = TextUtils.ellipsize(value, paint, WIDTH - MARGIN * 2, TextUtils.TruncateAt.END).toString()
        canvas.drawText(fitted, WIDTH / 2f, y, paint)
    }

    /** Canvas aligns by screen side, so "start" means right in a mirrored (RTL) card. */
    private fun alignAt(start: Boolean, rtl: Boolean): Paint.Align = if (start ==
        rtl
    ) {
        Paint.Align.RIGHT
    } else {
        Paint.Align.LEFT
    }

    private fun text(face: Typeface, size: Float, color: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = face
        textSize = size
        this.color = color
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    private fun stroke(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    companion object {
        const val WIDTH = 1080
        const val HEIGHT = 1350

        /** A bold face of [base]: true weight 700 where the platform supports it. */
        fun boldOf(base: Typeface): Typeface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Typeface.create(base, BOLD_WEIGHT, false)
        } else {
            Typeface.create(base, Typeface.BOLD)
        }

        private const val BOLD_WEIGHT = 700
        private const val MARGIN = 72f
        private const val PANEL_PADDING = 28f
        private const val PANEL_RADIUS = 40f
        private const val ROW_HEIGHT = 150f
        private const val ROW_GAP = 12f
        private const val ROW_RADIUS = 28f
        private const val ROW_PADDING = 28f
        private const val MARK_COLUMN = 92f
        private const val SCORE_COLUMN = 180f
        private const val BRAND_SIZE = 34f
        private const val TROPHY_SIZE = 150f
        private const val WINNER_SIZE = 74f
        private const val SCORE_LINE_SIZE = 46f
        private const val MARK_SIZE = 50f
        private const val NAME_SIZE = 48f
        private const val ROW_SCORE_SIZE = 60f
        private const val PILL_TEXT_SIZE = 28f
        private const val PILL_HEIGHT = 44f
        private const val PILL_PADDING = 18f
        private const val FOOTER_SIZE = 30f

        private val NAVY = MidnightTeal.MidnightNavy.toArgb()
        private val SLATE = MidnightTeal.DeepSlate.toArgb()
        private val MINT = MidnightTeal.SoftMint.toArgb()
        private val GOLD = MidnightTeal.WarmGold.toArgb()
        private val CORAL = MidnightTeal.Coral.toArgb()
        private val MUTED = MidnightTeal.MutedGray.toArgb()
        private val WHITE = MidnightTeal.White.toArgb()

        /** Gold at 18% over the slate panel, the dark-card version of the KING highlight. */
        private val GOLD_WASH = MidnightTeal.WarmGold.copy(alpha = 0.18f).toArgb()
    }
}
