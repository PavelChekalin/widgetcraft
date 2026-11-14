package com.pavel.widgetplus

import android.appwidget.AppWidgetManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Оформление виджета: фон, скругление, форма и цвет подложки, подписи, плотность.
 * Каждое изменение сразу сохраняется и перерисовывает виджет — предпросмотр
 * на рабочем столе виден без «Готово».
 */
class WidgetStyleActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var config: WidgetConfig
    private val store by lazy { ConfigStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_style)

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        config = store.load(widgetId) ?: return finish()

        findViewById<Button>(R.id.btnBgTransparent).setOnClickListener {
            apply { bgColor = 0x00000000 }
        }
        findViewById<Button>(R.id.btnBgDark).setOnClickListener {
            apply { bgColor = 0xCC15161A.toInt() }
        }
        findViewById<Button>(R.id.btnBgLight).setOnClickListener {
            apply { bgColor = 0xF2FFFFFF.toInt() }
        }

        findViewById<Button>(R.id.btnCornerMinus).setOnClickListener {
            apply { cornerRadius = (cornerRadius - 4).coerceIn(0, 48) }
        }
        findViewById<Button>(R.id.btnCornerPlus).setOnClickListener {
            apply { cornerRadius = (cornerRadius + 4).coerceIn(0, 48) }
        }

        findViewById<Button>(R.id.btnShapeNone).setOnClickListener { apply { cellShape = "none" } }
        findViewById<Button>(R.id.btnShapeCircle).setOnClickListener { apply { cellShape = "circle" } }
        findViewById<Button>(R.id.btnShapeRounded).setOnClickListener { apply { cellShape = "rounded" } }
        findViewById<Button>(R.id.btnShapeSquare).setOnClickListener { apply { cellShape = "square" } }

        findViewById<Switch>(R.id.swLabels).setOnCheckedChangeListener { _, checked ->
            apply { showLabels = checked }
        }

        findViewById<Button>(R.id.btnLabelMinus).setOnClickListener {
            apply { labelSize = (labelSize - 1).coerceIn(8, 14) }
        }
        findViewById<Button>(R.id.btnLabelPlus).setOnClickListener {
            apply { labelSize = (labelSize + 1).coerceIn(8, 14) }
        }

        findViewById<Button>(R.id.btnDensityTight).setOnClickListener { apply { padding = 2 } }
        findViewById<Button>(R.id.btnDensityNormal).setOnClickListener { apply { padding = 4 } }
        findViewById<Button>(R.id.btnDensityLoose).setOnClickListener { apply { padding = 8 } }

        findViewById<Button>(R.id.btnStyleDone).setOnClickListener { finish() }

        renderFillRow()
        renderState()
    }

    private fun apply(change: WidgetConfig.() -> Unit) {
        change(config)
        store.save(widgetId, config)
        CraftWidgetProvider.updateWidget(this, widgetId)
        renderState()
    }

    private fun renderState() {
        findViewById<TextView>(R.id.tvCorner).text = config.cornerRadius.toString()
        findViewById<TextView>(R.id.tvLabelSize).text = config.labelSize.toString()
        findViewById<Switch>(R.id.swLabels).isChecked = config.showLabels

        mark(findViewById(R.id.btnBgTransparent), config.bgColor == 0)
        mark(findViewById(R.id.btnBgDark), config.bgColor == 0xCC15161A.toInt())
        mark(findViewById(R.id.btnBgLight), config.bgColor == 0xF2FFFFFF.toInt())

        mark(findViewById(R.id.btnShapeNone), config.cellShape == "none")
        mark(findViewById(R.id.btnShapeCircle), config.cellShape == "circle")
        mark(findViewById(R.id.btnShapeRounded), config.cellShape == "rounded")
        mark(findViewById(R.id.btnShapeSquare), config.cellShape == "square")

        mark(findViewById(R.id.btnDensityTight), config.padding == 2)
        mark(findViewById(R.id.btnDensityNormal), config.padding == 4)
        mark(findViewById(R.id.btnDensityLoose), config.padding == 8)
    }

    private fun mark(button: Button, selected: Boolean) {
        button.alpha = if (selected) 1f else 0.5f
    }

    private fun renderFillRow() {
        val row = findViewById<LinearLayout>(R.id.fillRow)
        row.removeAllViews()
        val density = resources.displayMetrics.density

        FillPalette.forEach { color ->
            val swatch = FrameLayout(this)
            val params = LinearLayout.LayoutParams((44 * density).toInt(), (44 * density).toInt())
            params.marginEnd = 6
            swatch.layoutParams = params

            val circle = View(this)
            circle.layoutParams = FrameLayout.LayoutParams(
                (32 * density).toInt(),
                (32 * density).toInt(),
                Gravity.CENTER
            )
            val selected = config.cellFill == color
            circle.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
                setStroke(
                    if (selected) (3 * density).toInt() else (1 * density).toInt(),
                    if (selected) 0xFF4F7CFF.toInt() else 0x66FFFFFF
                )
            }
            circle.setOnClickListener {
                apply { cellFill = color }
                renderFillRow()
            }
            swatch.addView(circle)
            row.addView(swatch)
        }
    }

    companion object {
        /** Полупрозрачные подложки, чтобы иконка читалась на любом фоне. */
        val FillPalette = intArrayOf(
            0x33FFFFFF.toInt(),
            0x33000000.toInt(),
            0x554F7CFF.toInt(),
            0x5534D399.toInt(),
            0x55C084FC.toInt(),
            0x55FF9F45.toInt()
        )
    }
}
