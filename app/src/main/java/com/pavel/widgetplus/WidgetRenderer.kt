package com.pavel.widgetplus

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews

/**
 * Собирает RemoteViews по конфигу. Разметка фиксированная (8×10),
 * поэтому лишние ряды и ячейки просто скрываются.
 */
class WidgetRenderer(private val context: Context) {

    fun build(widgetId: Int, config: WidgetConfig): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_grid)
        val density = context.resources.displayMetrics.density
        val iconSizePx = (34 * density).toInt()

        for (row in 0 until ConfigStore.GRID_ROWS) {
            views.setViewVisibility(
                viewId("row$row"),
                if (row < config.rows) View.VISIBLE else View.GONE
            )
        }

        val total = ConfigStore.GRID_COLUMNS * ConfigStore.GRID_ROWS
        for (slot in 0 until total) {
            val row = slot / ConfigStore.GRID_COLUMNS
            val col = slot % ConfigStore.GRID_COLUMNS
            val visible = row < config.rows && col < config.cols
            val cell = if (visible) config.cellAt(slot) else null

            if (cell == null || cell.type == "empty") {
                views.setViewVisibility(viewId("cell$slot"), View.GONE)
                continue
            }

            views.setViewVisibility(viewId("cell$slot"), View.VISIBLE)
            views.setTextViewText(
                viewId("label$slot"),
                if (config.showLabels) cell.label else ""
            )
            views.setTextViewTextSize(
                viewId("label$slot"),
                TypedValue.COMPLEX_UNIT_SP,
                config.labelSize.toFloat()
            )

            val bitmap = cell.icon?.let { IconBitmap.bitmap(context, it, cell.color, iconSizePx) }
            if (bitmap != null) {
                views.setImageViewBitmap(viewId("icon$slot"), bitmap)
            }

            views.setOnClickPendingIntent(
                viewId("cell$slot"),
                cellPendingIntent(widgetId, slot, cell)
            )
        }

        return views
    }

    private fun viewId(name: String): Int =
        context.resources.getIdentifier(name, "id", context.packageName)

    private fun cellPendingIntent(
        widgetId: Int,
        slot: Int,
        cell: CellConfig
    ): PendingIntent {
        val intent = Intent(context, CraftWidgetProvider::class.java)
            .setAction(CraftWidgetProvider.ACTION_CELL_CLICK)
            .putExtra(CraftWidgetProvider.EXTRA_LABEL, cell.label)
            .putExtra(CraftWidgetProvider.EXTRA_SLOT, slot)
            .setData(Uri.parse("widgetplus://cell/$widgetId/$slot"))

        return PendingIntent.getBroadcast(
            context,
            widgetId * 100 + slot,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
