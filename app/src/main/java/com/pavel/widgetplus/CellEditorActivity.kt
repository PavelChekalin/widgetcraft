package com.pavel.widgetplus

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * Выбор содержимого ячейки: пока только приложение и «пусто».
 * Ссылки и команды добавятся отдельными вкладками позже.
 */
class CellEditorActivity : AppCompatActivity() {

    private val store by lazy { ConfigStore(this) }
    private lateinit var adapter: AppAdapter

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var slot = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cell_editor)

        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        slot = intent.getIntExtra(EXTRA_SLOT, 0)
        val row = intent.getIntExtra(EXTRA_ROW, 1)
        val col = intent.getIntExtra(EXTRA_COL, 1)

        findViewById<TextView>(R.id.tvCellTitle).text = getString(R.string.cell_title, row, col)

        val apps = AppCatalog(this).load()
        adapter = AppAdapter { entry -> chooseApp(entry) }

        findViewById<RecyclerView>(R.id.rvApps).apply {
            layoutManager = LinearLayoutManager(this@CellEditorActivity)
            adapter = this@CellEditorActivity.adapter
        }
        adapter.submitList(apps)

        findViewById<EditText>(R.id.etSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim()?.lowercase().orEmpty()
                adapter.submitList(
                    if (query.isEmpty()) apps else apps.filter { it.label.lowercase().contains(query) }
                )
            }
        })

        findViewById<Button>(R.id.btnClearCell).setOnClickListener { clearCell() }
    }

    private fun chooseApp(entry: AppCatalog.AppEntry) {
        val config = store.load(widgetId) ?: return
        config.put(
            CellConfig(
                index = slot,
                type = "app",
                label = entry.label,
                icon = null,
                color = 0xFFFFFFFF.toInt(),
                pkg = entry.packageName,
                extra = entry.activityName
            )
        )
        store.save(widgetId, config)
        CraftWidgetProvider.updateWidget(this, widgetId)
        setResult(RESULT_OK)
        finish()
    }

    private fun clearCell() {
        val config = store.load(widgetId) ?: return
        config.removeAt(slot)
        store.save(widgetId, config)
        CraftWidgetProvider.updateWidget(this, widgetId)
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        const val EXTRA_SLOT = "slot"
        const val EXTRA_ROW = "row"
        const val EXTRA_COL = "col"
    }
}

private class AppAdapter(
    private val onClick: (AppCatalog.AppEntry) -> Unit
) : ListAdapter<AppCatalog.AppEntry, AppAdapter.Holder>(DIFF) {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val label: TextView = view.findViewById(R.id.appLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = getItem(position)
        holder.label.text = entry.label
        holder.icon.setImageDrawable(entry.icon)
        holder.itemView.setOnClickListener { onClick(entry) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AppCatalog.AppEntry>() {
            override fun areItemsTheSame(oldItem: AppCatalog.AppEntry, newItem: AppCatalog.AppEntry): Boolean =
                oldItem.packageName == newItem.packageName && oldItem.activityName == newItem.activityName

            override fun areContentsTheSame(oldItem: AppCatalog.AppEntry, newItem: AppCatalog.AppEntry): Boolean =
                oldItem == newItem
        }
    }
}
