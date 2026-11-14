package com.pavel.widgetplus

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.drawable.DrawableCompat

/**
 * Превращает векторную иконку в Bitmap нужного размера, с нужным цветом.
 * Кэш обязателен: одна и та же иконка на нескольких ячейках не должна
 * заново растеризоваться при каждом обновлении виджета.
 */
object IconBitmap {

    private val cache = HashMap<String, Bitmap>()

    fun bitmap(context: Context, resName: String, color: Int, sizePx: Int): Bitmap? {
        if (sizePx <= 0) return null
        val key = "$resName|$color|$sizePx"
        cache[key]?.let { return it }

        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
        if (resId == 0) return null

        val original = context.getDrawable(resId) ?: return null
        val drawable = DrawableCompat.wrap(original.mutate())
        DrawableCompat.setTint(drawable, color)

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)

        cache[key] = bitmap
        return bitmap
    }
}
