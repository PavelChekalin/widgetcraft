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

        // Если виджет только что добавлен кнопкой «Копия», забираем настройки источника.
        val prefs = context.getSharedPreferences("widgetplus", Context.MODE_PRIVATE)
        val pendingSource = prefs.getInt("pendingCopy", -1)

        appWidgetIds.forEach { widgetId ->
            var config = store.load(widgetId)

            if (config == null && pendingSource != -1 && pendingSource != widgetId) {
                val source = store.load(pendingSource)
                if (source != null) {
                    config = source.copyOf()
                    store.save(widgetId, config)
                    prefs.edit().remove("pendingCopy").apply()
                }
            }

            if (config == null) {
                config = store.defaultTest()
                store.save(widgetId, config)
            }

            appWidgetManager.updateAppWidget(widgetId, renderer.build(widgetId, config))
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val store = ConfigStore(context)
        appWidgetIds.forEach { store.delete(it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_COPY) {
            copyConfig(context, intent)
            return
        }
        if (intent.action == ACTION_CELL_CLICK) {
            val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
            val slot = intent.getIntExtra(EXTRA_SLOT, -1)
            Log.d(TAG, "cell click slot=$slot label=$label")
            Toast.makeText(context, "Нажата ячейка: $label", Toast.LENGTH_SHORT).show()
            return
        }
        super.onReceive(context, intent)
    }

/** Лаунчер добавил копию виджета: переносим в неё настройки исходного. */
    private fun copyConfig(context: Context, intent: Intent) {
        val newId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val sourceId = intent.getIntExtra(EXTRA_SOURCE_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (newId == AppWidgetManager.INVALID_APPWIDGET_ID || sourceId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val store = ConfigStore(context)
        val source = store.load(sourceId) ?: return
        store.save(newId, source.copyOf())
        context.getSharedPreferences("widgetplus", Context.MODE_PRIVATE)
            .edit()
            .remove("pendingCopy")
            .apply()
        updateWidget(context, newId)
    }

    companion object {
        private const val TAG = "WidgetPlus"
        const val ACTION_CELL_CLICK = "com.pavel.widgetplus.CELL_CLICK"
        const val ACTION_COPY = "com.pavel.widgetplus.COPY"
        const val EXTRA_SOURCE_ID = "sourceId"
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
