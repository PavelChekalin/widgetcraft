package com.pavel.widgetplus

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.drawable.DrawableCompat

/**
 * Превращает векторную иконку в Bitmap нужного размера, с нужным цветом
 * и подложкой (круг / скруглённый квадрат / квадрат).
 * Кэш обязателен: одна и та же иконка на нескольких ячейках не должна
 * заново растеризоваться при каждом обновлении виджета.
 */
object IconBitmap {

    private val cache = HashMap<String, Bitmap>()
    private val backgroundCache = HashMap<String, Bitmap>()

    fun bitmap(
        context: Context,
        resName: String,
        color: Int,
        sizePx: Int,
        shape: String? = null,
        fill: Int? = null,
        indicator: Int? = null
    ): Bitmap? {
        if (sizePx <= 0) return null
        val key = "$resName|$color|$sizePx|$shape|$fill|$indicator"
        cache[key]?.let { return it }

        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
        if (resId == 0) return null

        val original = context.getDrawable(resId) ?: return null
        val drawable = DrawableCompat.wrap(original.mutate())
        DrawableCompat.setTint(drawable, color)

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        var inset = 0
        if (shape != null && fill != null) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fill }
            when (shape) {
                "circle" -> canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)
                "rounded" -> {
                    val radius = sizePx * 0.28f
                    canvas.drawRoundRect(
                        RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()),
                        radius,
                        radius,
                        paint
                    )
                }
                else -> canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)
            }
            // Иконка внутри подложки не должна касаться её краёв.
            inset = (sizePx * 0.18f).toInt()
        }

        drawable.setBounds(inset, inset, sizePx - inset, sizePx - inset)
        drawable.draw(canvas)

        // Точка состояния для переключателей (например, включён ли VPN).
        if (indicator != null) {
            val dotRadius = sizePx * 0.16f
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                setColor(indicator)
                style = Paint.Style.FILL
            }
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                setColor(0xFF15161A.toInt())
                style = Paint.Style.FILL
            }
            canvas.drawCircle(sizePx - dotRadius * 1.1f, sizePx - dotRadius * 1.1f, dotRadius * 1.35f, border)
            canvas.drawCircle(sizePx - dotRadius * 1.1f, sizePx - dotRadius * 1.1f, dotRadius, dotPaint)
        }

        cache[key] = bitmap
        return bitmap
    }

    /** Прозрачная картинка: ею очищаем ImageView, когда у ячейки нет иконки. */
    fun empty(): Bitmap = empty

    private val empty: Bitmap by lazy {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    /** Фон виджета: скруглённый прямоугольник нужного цвета (может быть прозрачным). */
    fun roundedBackground(
        context: Context,
        color: Int,
        radiusDp: Int
    ): Bitmap {
        val key = "$color|$radiusDp"
        backgroundCache[key]?.let { return it }

        val density = context.resources.displayMetrics.density
        val width = 600
        val height = 300
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val radius = (radiusDp * density).coerceAtMost(height / 2f)
        canvas.drawRoundRect(
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            radius,
            radius,
            paint
        )

        backgroundCache[key] = bitmap
        return bitmap
    }
}
