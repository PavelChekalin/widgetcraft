package com.pavel.widgetplus

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
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
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pendingSource = prefs.getInt(KEY_PENDING_COPY, -1)

        appWidgetIds.forEach { widgetId ->
            var config = store.load(widgetId)

            if (config == null && pendingSource != -1 && pendingSource != widgetId) {
                val source = store.load(pendingSource)
                if (source != null) {
                    config = source.copyOf()
                    store.save(widgetId, config)
                    prefs.edit().remove(KEY_PENDING_COPY).apply()
                }
            }

            if (config == null) {
                config = store.defaultTest()
                store.save(widgetId, config)
            }

            appWidgetManager.updateAppWidget(
                widgetId,
                renderer.build(widgetId, config, currentPage(context, widgetId))
            )
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val store = ConfigStore(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        appWidgetIds.forEach { widgetId ->
            store.delete(widgetId)
            prefs.edit().remove(KEY_PAGE_PREFIX + widgetId).apply()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PAGE_NEXT -> {
                nextPage(context, intent)
                return
            }
            ACTION_COPY -> {
                copyConfig(context, intent)
                return
            }
            ACTION_CELL_CLICK -> {
                val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
                val slot = intent.getIntExtra(EXTRA_SLOT, -1)
                Log.d(TAG, "cell click slot=$slot label=$label")
                Toast.makeText(context, "Нажата ячейка: $label", Toast.LENGTH_SHORT).show()
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) updateWidget(context, widgetId)
                return
            }
        }
        super.onReceive(context, intent)
    }

    /** Перелистывание страниц: текущая страница хранится в настройках приложения. */
    private fun nextPage(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val config = ConfigStore(context).load(widgetId) ?: return
        if (config.pages <= 1) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = (prefs.getInt(KEY_PAGE_PREFIX + widgetId, 0) + 1) % config.pages
        prefs.edit().putInt(KEY_PAGE_PREFIX + widgetId, next).apply()
        updateWidget(context, widgetId)
    }

    /** Лаунчер добавил копию виджета: переносим в неё настройки исходного. */
    private fun copyConfig(context: Context, intent: Intent) {
        val newId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val sourceId = intent.getIntExtra(EXTRA_SOURCE_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (newId == AppWidgetManager.INVALID_APPWIDGET_ID || sourceId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val store = ConfigStore(context)
        val source = store.load(sourceId) ?: return
        store.save(newId, source.copyOf())
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PENDING_COPY)
            .apply()
        updateWidget(context, newId)
    }

    companion object {
        private const val TAG = "WidgetPlus"
        private const val PREFS = "widgetplus"
        const val KEY_PENDING_COPY = "pendingCopy"
        const val KEY_PAGE_PREFIX = "page_"

        const val ACTION_CELL_CLICK = "com.pavel.widgetplus.CELL_CLICK"
        const val ACTION_COPY = "com.pavel.widgetplus.COPY"
        const val ACTION_PAGE_NEXT = "com.pavel.widgetplus.PAGE_NEXT"
        const val EXTRA_WIDGET_ID = "widgetId"
        const val EXTRA_SOURCE_ID = "sourceId"
        const val EXTRA_LABEL = "label"
        const val EXTRA_SLOT = "slot"

        /** Номер открытой страницы для виджета (0, если страниц нет). */
        fun currentPage(context: Context, widgetId: Int): Int {
            val config = ConfigStore(context).load(widgetId) ?: return 0
            if (config.pages <= 1) return 0
            val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_PAGE_PREFIX + widgetId, 0)
            return saved.coerceIn(0, config.pages - 1)
        }

        /** Перерисовать один виджет из сохранённого конфига. */
        fun updateWidget(context: Context, widgetId: Int) {
            val config = ConfigStore(context).load(widgetId) ?: return
            AppWidgetManager.getInstance(context).updateAppWidget(
                widgetId,
                WidgetRenderer(context).build(widgetId, config, currentPage(context, widgetId))
            )
        }
    }
}
