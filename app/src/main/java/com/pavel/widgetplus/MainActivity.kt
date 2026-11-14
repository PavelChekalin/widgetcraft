package com.pavel.widgetplus

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val store by lazy { ConfigStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnAddWidget).setOnClickListener { pinWidget(null) }
        findViewById<Button>(R.id.btnBackup).setOnClickListener { exportBackup() }
        findViewById<Button>(R.id.btnRestore).setOnClickListener { importBackup() }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun pinWidget(sourceWidgetId: Int?) {
        val manager = AppWidgetManager.getInstance(this)
        if (!manager.isRequestPinAppWidgetSupported) {
            Toast.makeText(this, getString(R.string.pin_not_supported), Toast.LENGTH_LONG).show()
            return
        }
        val provider = ComponentName(this, CraftWidgetProvider::class.java)

        if (sourceWidgetId == null) {
            manager.requestPinAppWidget(provider, null, null)
            return
        }

        // Копия: запоминаем источник (на случай, если лаунчер откроет настройку)
        // и просим лаунчер сообщить id нового виджета.
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .putInt(KEY_PENDING_COPY, sourceWidgetId)
            .apply()

        val callback = android.app.PendingIntent.getBroadcast(
            this,
            sourceWidgetId,
            Intent(this, CraftWidgetProvider::class.java)
                .setAction(CraftWidgetProvider.ACTION_COPY)
                .putExtra(CraftWidgetProvider.EXTRA_SOURCE_ID, sourceWidgetId)
                .setData(Uri.parse("widgetplus://copy/$sourceWidgetId")),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        manager.requestPinAppWidget(provider, null, callback)
        Toast.makeText(this, getString(R.string.duplicate_hint), Toast.LENGTH_LONG).show()
    }

    private fun refreshList() {
        val container = findViewById<LinearLayout>(R.id.widgetList)
        container.removeAllViews()

        val all = store.loadAll()
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
                setPadding(0, 10, 0, 10)
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

            val configure = Button(this).apply {
                text = getString(R.string.configure)
                setOnClickListener {
                    startActivity(
                        Intent(this@MainActivity, WidgetConfigActivity::class.java)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    )
                }
            }

            val duplicate = Button(this).apply {
                text = getString(R.string.duplicate)
                setOnClickListener { pinWidget(widgetId) }
            }

            row.addView(info)
            row.addView(configure)
            row.addView(duplicate)
            container.addView(row)
        }
    }

    private fun exportBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/json")
            .putExtra(Intent.EXTRA_TITLE, "widgetplus-backup.json")
        startActivityForResult(intent, REQUEST_EXPORT)
    }

    private fun importBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
        startActivityForResult(intent, REQUEST_IMPORT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data?.data == null) return

        val uri = data.data!!
        when (requestCode) {
            REQUEST_EXPORT -> runCatching {
                contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(store.exportAll().toByteArray())
                }
                Toast.makeText(this, getString(R.string.backup_saved), Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(this, getString(R.string.backup_failed), Toast.LENGTH_LONG).show()
            }

            REQUEST_IMPORT -> runCatching {
                val text = contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
                val count = store.importAll(text)
                if (count == 0) {
                    Toast.makeText(this, getString(R.string.backup_empty), Toast.LENGTH_LONG).show()
                } else {
                    store.loadAll().keys.forEach { CraftWidgetProvider.updateWidget(this, it) }
                    refreshList()
                    Toast.makeText(
                        this,
                        getString(R.string.backup_restored, count),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }.onFailure {
                Toast.makeText(this, getString(R.string.backup_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val PREFS = "widgetplus"
        const val KEY_PENDING_COPY = "pendingCopy"
        private const val REQUEST_EXPORT = 301
        private const val REQUEST_IMPORT = 302
    }
}
