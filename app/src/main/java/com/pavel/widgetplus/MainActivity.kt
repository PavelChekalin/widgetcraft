package com.pavel.widgetplus

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnAddWidget).setOnClickListener {
            val manager = AppWidgetManager.getInstance(this)
            if (manager.isRequestPinAppWidgetSupported) {
                val provider = ComponentName(this, CraftWidgetProvider::class.java)
                manager.requestPinAppWidget(provider, null, null)
            } else {
                Toast.makeText(this, getString(R.string.pin_not_supported), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val container = findViewById<LinearLayout>(R.id.widgetList)
        container.removeAllViews()

        val all = ConfigStore(this).loadAll()
        if (all.isEmpty()) {
            container.addView(TextView(this).apply {
                text = getString(R.string.no_widgets)
                textSize = 15f
            })
            return
        }

        all.toSortedMap().forEach { (widgetId, config) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 14, 0, 14)
            }

            val info = TextView(this).apply {
                text = getString(
                    R.string.widget_item,
                    widgetId,
                    config.cols,
                    config.rows,
                    config.cells.size
                )
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val button = Button(this).apply {
                text = getString(R.string.configure)
                setOnClickListener {
                    startActivity(
                        Intent(this@MainActivity, WidgetConfigActivity::class.java)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    )
                }
            }

            row.addView(info)
            row.addView(button)
            container.addView(row)
        }
    }
}
