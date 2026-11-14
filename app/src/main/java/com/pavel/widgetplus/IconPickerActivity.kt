package com.pavel.widgetplus

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * Выбор иконки из встроенной коллекции: поиск по-русски, категории, цвет.
 * Возвращает имя drawable и цвет, либо пустое имя — «родная иконка приложения».
 */
class IconPickerActivity : AppCompatActivity() {

    private val catalog by lazy { IconCatalog(this) }
    private lateinit var adapter: IconAdapter
    private lateinit var colorRow: LinearLayout

    private var color = 0xFFFFFFFF.toInt()
    private var category: String? = null
    private var query = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_icon_picker)

        color = intent.getIntExtra(EXTRA_COLOR, 0xFFFFFFFF.toInt())

        adapter = IconAdapter(
            colorProvider = { color },
            onClick = { entry -> pick(entry) }
        )
        findViewById<RecyclerView>(R.id.rvIcons).apply {
            layoutManager = GridLayoutManager(this@IconPickerActivity, 5)
            adapter = this@IconPickerActivity.adapter
        }

        colorRow = findViewById(R.id.colorRow)
        renderColors()
        renderCategories()
        refresh()

        findViewById<EditText>(R.id.etIconSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString().orEmpty()
                refresh()
            }
        })

        findViewById<Button>(R.id.btnNativeIcon).setOnClickListener {
            setResult(
                RESULT_OK,
                Intent()
                    .putExtra(EXTRA_ICON, "")
                    .putExtra(EXTRA_COLOR, color)
            )
            finish()
        }
    }

    private fun pick(entry: IconCatalog.IconEntry) {
        setResult(
            RESULT_OK,
            Intent()
                .putExtra(EXTRA_ICON, entry.file)
                .putExtra(EXTRA_COLOR, color)
        )
        finish()
    }

    private fun refresh() {
        val list = catalog.search(query, category)
        adapter.submitList(list)
        findViewById<TextView>(R.id.tvIconCount).text = getString(R.string.icon_count, list.size)
    }

    private fun renderCategories() {
        val row = findViewById<LinearLayout>(R.id.categoryRow)
        row.removeAllViews()
        val items = listOf<String?>(null) + catalog.categories()
        items.forEach { item ->
            val button = Button(this).apply {
                text = item ?: getString(R.string.icon_all)
                isAllCaps = false
                textSize = 13f
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.marginEnd = 6
                layoutParams = params
                alpha = if (item == category) 1f else 0.55f
                setOnClickListener {
                    category = item
                    renderCategories()
                    refresh()
                }
            }
            row.addView(button)
        }
    }

    private fun renderColors() {
        colorRow.removeAllViews()
        IconCatalog.PALETTE.forEach { paletteColor ->
            val swatch = FrameLayout(this)
            val params = LinearLayout.LayoutParams((40 * resources.displayMetrics.density).toInt(), (40 * resources.displayMetrics.density).toInt())
            params.marginEnd = 6
            swatch.layoutParams = params

            val circle = View(this)
            val d = resources.displayMetrics.density
            val selected = paletteColor == color
            circle.layoutParams = FrameLayout.LayoutParams((30 * d).toInt(), (30 * d).toInt(), android.view.Gravity.CENTER)
            circle.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(paletteColor)
                setStroke(
                    if (selected) (3 * d).toInt() else (1 * d).toInt(),
                    ColorStateList.valueOf(if (selected) 0xFF4F7CFF.toInt() else 0x66FFFFFF)
                )
            }
            circle.setOnClickListener {
                color = paletteColor
                renderColors()
                adapter.notifyDataSetChanged()
            }
            swatch.addView(circle)
            colorRow.addView(swatch)
        }
    }

    companion object {
        const val EXTRA_ICON = "icon"
        const val EXTRA_COLOR = "color"
    }
}

private class IconAdapter(
    private val colorProvider: () -> Int,
    private val onClick: (IconCatalog.IconEntry) -> Unit
) : ListAdapter<IconCatalog.IconEntry, IconAdapter.Holder>(DIFF) {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.iconImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_icon, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = getItem(position)
        val resId = holder.itemView.context.resources
            .getIdentifier(entry.file, "drawable", holder.itemView.context.packageName)
        if (resId != 0) {
            holder.icon.setImageResource(resId)
            holder.icon.setColorFilter(colorProvider())
        }
        holder.itemView.setOnClickListener { onClick(entry) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<IconCatalog.IconEntry>() {
            override fun areItemsTheSame(a: IconCatalog.IconEntry, b: IconCatalog.IconEntry) = a.file == b.file
            override fun areContentsTheSame(a: IconCatalog.IconEntry, b: IconCatalog.IconEntry) = a == b
        }
    }
}
