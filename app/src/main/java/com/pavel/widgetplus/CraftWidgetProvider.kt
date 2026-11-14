package com.pavel.widgetplus

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast

class CraftWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val store = ConfigStore(context)
        val renderer = WidgetRenderer(context)

        appWidgetIds.forEach { widgetId ->
            val config = store.load(widgetId) ?: store.defaultTest().also {
                store.save(widgetId, it)
            }
            appWidgetManager.updateAppWidget(widgetId, renderer.build(widgetId, config))
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val store = ConfigStore(context)
        appWidgetIds.forEach { store.delete(it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CELL_CLICK) {
            val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
            val slot = intent.getIntExtra(EXTRA_SLOT, -1)
            Log.d(TAG, "cell click slot=$slot label=$label")
            Toast.makeText(context, "Нажата ячейка: $label", Toast.LENGTH_SHORT).show()
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        private const val TAG = "WidgetPlus"
        const val ACTION_CELL_CLICK = "com.pavel.widgetplus.CELL_CLICK"
        const val EXTRA_LABEL = "label"
        const val EXTRA_SLOT = "slot"

        /** Перерисовать один виджет из сохранённого конфига. */
        fun updateWidget(context: Context, widgetId: Int) {
            val config = ConfigStore(context).load(widgetId) ?: return
            AppWidgetManager.getInstance(context)
                .updateAppWidget(widgetId, WidgetRenderer(context).build(widgetId, config))
        }
    }
}
