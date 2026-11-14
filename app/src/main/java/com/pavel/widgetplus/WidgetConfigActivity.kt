package com.pavel.widgetplus

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Настройка виджета: колонки/ряды и предпросмотр сетки.
 * Если открыта из списка виджетов — правит уже созданный виджет.
 * Если открыта лаунчером при добавлении — по «Готово» создаёт его,
 * по «Назад» лаунчер виджет не поставит.
 */
class WidgetConfigActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var config: WidgetConfig
    private val store by lazy { ConfigStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // По умолчанию — отмена: в режиме добавления это убирает недоделанный виджет.
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_widget_config)

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        config = store.load(widgetId) ?: store.defaultTest().also { store.save(widgetId, it) }

        findViewById<Button>(R.id.btnColsMinus).setOnClickListener { changeCols(-1) }
        findViewById<Button>(R.id.btnColsPlus).setOnClickListener { changeCols(1) }
        findViewById<Button>(R.id.btnRowsMinus).setOnClickListener { changeRows(-1) }
        findViewById<Button>(R.id.btnRowsPlus).setOnClickListener { changeRows(1) }
        findViewById<Button>(R.id.btnDone).setOnClickListener { saveAndFinish() }

        updateCounters()
        renderPreview()
    }

    private fun changeCols(delta: Int) {
        config.cols = (config.cols + delta).coerceIn(MIN_COLS, MAX_COLS)
        pruneCells()
        updateCounters()
        renderPreview()
    }

    private fun changeRows(delta: Int) {
        config.rows = (config.rows + delta).coerceIn(MIN_ROWS, MAX_ROWS)
        pruneCells()
        updateCounters()
        renderPreview()
    }

    /** После уменьшения сетки ячейки за её пределами больше не показываем. */
    private fun pruneCells() {
        config.cells.removeAll { cell ->
            cell.index / ConfigStore.GRID_COLUMNS >= config.rows ||
                cell.index % ConfigStore.GRID_COLUMNS >= config.cols
        }
    }

    private fun updateCounters() {
        findViewById<TextView>(R.id.tvCols).text = config.cols.toString()
        findViewById<TextView>(R.id.tvRows).text = config.rows.toString()
    }

    private fun renderPreview() {
        val container = findViewById<LinearLayout>(R.id.previewContainer)
        container.removeAllViews()
        val density = resources.displayMetrics.density

        for (row in 0 until config.rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (56 * density).toInt()
                )
            }

            for (col in 0 until config.cols) {
                val slot = row * ConfigStore.GRID_COLUMNS + col
                val cell = config.cellAt(slot)

                val cellLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    setOnClickListener {
                        Toast.makeText(
                            this@WidgetConfigActivity,
                            getString(R.string.cell_tap, row + 1, col + 1),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                if (cell != null) {
                    val size = (24 * density).toInt()
                    val resId = resources.getIdentifier(cell.icon ?: "", "drawable", packageName)
                    cellLayout.addView(ImageView(this).apply {
                        layoutParams = LinearLayout.LayoutParams(size, size)
                        if (resId != 0) {
                            setImageDrawable(ContextCompat.getDrawable(this@WidgetConfigActivity, resId))
                            setColorFilter(cell.color)
                        }
                    })
                    cellLayout.addView(TextView(this).apply {
                        text = cell.label
                        textSize = 9f
                        maxLines = 1
                        gravity = Gravity.CENTER
                    })
                }

                rowLayout.addView(cellLayout)
            }

            container.addView(rowLayout)
        }
    }

    private fun saveAndFinish() {
        store.save(widgetId, config)
        CraftWidgetProvider.updateWidget(this, widgetId)

        setResult(
            RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        )
        finish()
    }

    companion object {
        private const val MIN_COLS = 2
        private const val MAX_COLS = 8
        private const val MIN_ROWS = 1
        private const val MAX_ROWS = 10
    }
}
