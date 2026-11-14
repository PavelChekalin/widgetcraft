package com.pavel.widgetplus

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * Выбор содержимого ячейки: приложение, ссылка или команда-broadcast.
 * Для ссылки и команды иконка обязательна — родной иконки у них нет.
 */
class CellEditorActivity : AppCompatActivity() {

    private val store by lazy { ConfigStore(this) }
    private lateinit var adapter: AppAdapter

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var slot = 0
    private var mode = TYPE_APP

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

        findViewById<Button>(R.id.btnTypeApp).setOnClickListener { setMode(TYPE_APP) }
        findViewById<Button>(R.id.btnTypeLink).setOnClickListener { setMode(TYPE_LINK) }
        findViewById<Button>(R.id.btnTypeCommand).setOnClickListener { setMode(TYPE_COMMAND) }
        findViewById<Button>(R.id.btnPickIcon).setOnClickListener { pickIcon() }
        findViewById<Button>(R.id.btnClearCell).setOnClickListener { clearCell() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveActionCell() }
        findViewById<Button>(R.id.btnCommandLibrary).setOnClickListener { showCommandLibrary() }

        prefillFromExisting()
    }

    /** Если ячейка уже настроена, открываем её на нужной вкладке с готовыми полями. */
    private fun prefillFromExisting() {
        val cell = store.load(widgetId)?.cellAt(slot) ?: return
        when (cell.type) {
            "link" -> {
                setMode(TYPE_LINK)
                findViewById<EditText>(R.id.etUrl).setText(cell.extra.orEmpty())
                findViewById<EditText>(R.id.etLinkLabel).setText(cell.label)
            }
            "command" -> {
                setMode(TYPE_COMMAND)
                findViewById<EditText>(R.id.etPkg).setText(cell.pkg.orEmpty())
                findViewById<EditText>(R.id.etComponent).setText(cell.extra.orEmpty())
                findViewById<EditText>(R.id.etAction).setText(cell.action.orEmpty())
                findViewById<EditText>(R.id.etCommandLabel).setText(cell.label)
            }
            else -> setMode(TYPE_APP)
        }
    }

    private fun setMode(newMode: String) {
        mode = newMode
        findViewById<LinearLayout>(R.id.appsBlock).visibility =
            if (mode == TYPE_APP) View.VISIBLE else View.GONE
        findViewById<LinearLayout>(R.id.linkBlock).visibility =
            if (mode == TYPE_LINK) View.VISIBLE else View.GONE
        findViewById<LinearLayout>(R.id.commandBlock).visibility =
            if (mode == TYPE_COMMAND) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnSave).visibility =
            if (mode == TYPE_APP) View.GONE else View.VISIBLE

        findViewById<Button>(R.id.btnTypeApp).alpha = if (mode == TYPE_APP) 1f else 0.55f
        findViewById<Button>(R.id.btnTypeLink).alpha = if (mode == TYPE_LINK) 1f else 0.55f
        findViewById<Button>(R.id.btnTypeCommand).alpha = if (mode == TYPE_COMMAND) 1f else 0.55f
    }

    private fun pickIcon() {
        val cell = store.load(widgetId)?.cellAt(slot)
        if (cell == null && mode == TYPE_APP) {
            Toast.makeText(this, getString(R.string.icon_need_app), Toast.LENGTH_SHORT).show()
            return
        }
        startActivityForResult(
            Intent(this, IconPickerActivity::class.java)
                .putExtra(IconPickerActivity.EXTRA_COLOR, cell?.color ?: 0xFFFFFFFF.toInt()),
            REQUEST_ICON
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_ICON || resultCode != RESULT_OK || data == null) return

        val icon = data.getStringExtra(IconPickerActivity.EXTRA_ICON) ?: return
        val color = data.getIntExtra(IconPickerActivity.EXTRA_COLOR, 0xFFFFFFFF.toInt())

        val config = store.load(widgetId) ?: return
        val cell = config.cellAt(slot)

        if (cell != null) {
            config.put(cell.copy(icon = icon.ifEmpty { null }, color = color))
        } else {
            // Иконку выбирают раньше содержимого: запоминаем её как черновик ячейки.
            config.put(
                CellConfig(
                    index = slot,
                    type = "draft",
                    label = "",
                    icon = icon.ifEmpty { null },
                    color = color
                )
            )
        }
        store.save(widgetId, config)
        CraftWidgetProvider.updateWidget(this, widgetId)
        setResult(RESULT_OK)
    }

    private fun chooseApp(entry: AppCatalog.AppEntry) {
        val config = store.load(widgetId) ?: return
        val previous = config.cellAt(slot)
        config.put(
            CellConfig(
                index = slot,
                type = "app",
                label = entry.label,
                icon = previous?.icon,
                color = previous?.color ?: 0xFFFFFFFF.toInt(),
                pkg = entry.packageName,
                extra = entry.activityName
            )
        )
        store.save(widgetId, config)
        CraftWidgetProvider.updateWidget(this, widgetId)
        setResult(RESULT_OK)
        finish()
    }

/** Готовые команды, чтобы не вписывать пакет и действие руками. */
    private fun showCommandLibrary() {
        val commands = KnownCommands.all()
        val names = commands.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.command_library_title)
            .setItems(names) { _, which ->
                val command = commands[which]
                findViewById<EditText>(R.id.etPkg).setText(command.packageName)
                findViewById<EditText>(R.id.etComponent).setText(command.component)
                findViewById<EditText>(R.id.etAction).setText(command.action)
                findViewById<EditText>(R.id.etCommandLabel).setText(command.label)
            }
            .show()
    }

    private fun saveActionCell() {
        val config = store.load(widgetId) ?: return
        val previous = config.cellAt(slot)
        val icon = previous?.icon
        if (icon.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.icon_need_for_action), Toast.LENGTH_SHORT).show()
            return
        }
        val color = previous?.color ?: 0xFFFFFFFF.toInt()

        if (mode == TYPE_LINK) {
            val url = findViewById<EditText>(R.id.etUrl).text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, getString(R.string.link_empty), Toast.LENGTH_SHORT).show()
                return
            }
            val label = findViewById<EditText>(R.id.etLinkLabel).text.toString().trim()
                .ifEmpty { hostOf(url) }
            config.put(
                CellConfig(
                    index = slot,
                    type = "link",
                    label = label,
                    icon = icon,
                    color = color,
                    extra = url
                )
            )
        } else {
            val action = findViewById<EditText>(R.id.etAction).text.toString().trim()
            if (action.isEmpty()) {
                Toast.makeText(this, getString(R.string.command_empty), Toast.LENGTH_SHORT).show()
                return
            }
            val pkg = findViewById<EditText>(R.id.etPkg).text.toString().trim()
            val component = findViewById<EditText>(R.id.etComponent).text.toString().trim()
            val label = findViewById<EditText>(R.id.etCommandLabel).text.toString().trim()
                .ifEmpty { action.substringAfterLast('.').ifEmpty { action } }
            config.put(
                CellConfig(
                    index = slot,
                    type = "command",
                    label = label,
                    icon = icon,
                    color = color,
                    pkg = pkg.ifEmpty { null },
                    extra = component.ifEmpty { null },
                    action = action
                )
            )
        }

        store.save(widgetId, config)
        CraftWidgetProvider.updateWidget(this, widgetId)
        setResult(RESULT_OK)
        finish()
    }

    private fun hostOf(url: String): String = runCatching {
        Uri.parse(url).host?.removePrefix("www.").orEmpty().ifEmpty { url }
    }.getOrDefault(url)

    private fun clearCell() {
        val config = store.load(widgetId) ?: return
        config.removeAt(slot)
        store.save(widgetId, config)
        CraftWidgetProvider.updateWidget(this, widgetId)
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        private const val REQUEST_ICON = 202
        private const val TYPE_APP = "app"
        private const val TYPE_LINK = "link"
        private const val TYPE_COMMAND = "command"

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
