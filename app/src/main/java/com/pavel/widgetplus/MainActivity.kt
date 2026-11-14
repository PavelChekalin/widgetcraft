package com.pavel.widgetplus

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.Button
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
                Toast.makeText(
                    this,
                    getString(R.string.pin_not_supported),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
