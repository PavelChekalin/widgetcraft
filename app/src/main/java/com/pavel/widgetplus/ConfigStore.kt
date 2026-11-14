package com.pavel.widgetplus

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Хранит конфиги виджетов в filesDir/widgets.json: ключ — appWidgetId.
 * Файл переживает перезапуск и перезагрузку телефона.
 */
class ConfigStore(context: Context) {

    private val file = File(context.filesDir, "widgets.json")

    fun loadAll(): MutableMap<Int, WidgetConfig> {
        val result = mutableMapOf<Int, WidgetConfig>()
        if (!file.exists()) return result
        try {
            val root = JSONObject(file.readText())
            val widgets = root.optJSONObject("widgets") ?: return result
            widgets.keys().forEach { key ->
                val id = key.toIntOrNull() ?: return@forEach
                result[id] = parse(widgets.getJSONObject(key))
            }
        } catch (e: Exception) {
            // Молча терять конфиги нельзя: если файл битый, это должно быть видно в журнале.
            Log.e("WidgetPlus", "Не удалось прочитать widgets.json", e)
        }
        return result
    }

    fun load(widgetId: Int): WidgetConfig? = loadAll()[widgetId]

    fun save(widgetId: Int, config: WidgetConfig) {
        val all = loadAll()
        all[widgetId] = config
        val widgets = JSONObject()
        all.forEach { (id, cfg) -> widgets.put(id.toString(), toJson(cfg)) }
        val root = JSONObject().put("widgets", widgets)
        file.writeText(root.toString(2))
    }

    fun delete(widgetId: Int) {
        val all = loadAll()
        if (all.remove(widgetId) == null) return
        val widgets = JSONObject()
        all.forEach { (id, cfg) -> widgets.put(id.toString(), toJson(cfg)) }
        file.writeText(JSONObject().put("widgets", widgets).toString(2))
    }

    /** Тестовый конфиг из восьми иконок — для проверки сетки до появления редактора. */
    fun defaultTest(): WidgetConfig {
        val config = WidgetConfig(cols = 4, rows = 2)
        val samples = listOf(
            Triple("Wi-Fi", "ic_test_wifi", 0xFF5AD1FF.toInt()),
            Triple("Файлы", "ic_test_folder", 0xFFFFC46B.toInt()),
            Triple("Питание", "ic_test_power", 0xFF7CE38B.toInt()),
            Triple("Свет", "ic_test_flash", 0xFFFFE066.toInt()),
            Triple("QR", "ic_test_qr", 0xFFC792EA.toInt()),
            Triple("ОрС", "ic_test_terminal", 0xFF6FD3D3.toInt()),
            Triple("DSH", "ic_test_robot", 0xFF9DB4FF.toInt()),
            Triple("Общий", "ic_test_share", 0xFFB0BEC5.toInt())
        )
        samples.forEachIndexed { position, (label, icon, color) ->
            // Разметка виджета всегда 8 колонок: индекс ячейки = row * 8 + col.
            val index = (position / config.cols) * GRID_COLUMNS + (position % config.cols)
            config.cells.add(
                CellConfig(
                    index = index,
                    type = "app",
                    label = label,
                    icon = icon,
                    color = color
                )
            )
        }
        return config
    }

    private fun toJson(config: WidgetConfig): JSONObject {
        val cells = JSONArray()
        config.cells.sortedBy { it.index }.forEach { cell ->
            cells.put(
                JSONObject()
                    .put("index", cell.index)
                    .put("type", cell.type)
                    .put("label", cell.label)
                    .put("icon", cell.icon)
                    .put("color", cell.color)
                    .put("pkg", cell.pkg)
                    .put("extra", cell.extra)
                    .put("action", cell.action)
            )
        }
        return JSONObject()
            .put("cols", config.cols)
            .put("rows", config.rows)
            .put("showLabels", config.showLabels)
            .put("labelSize", config.labelSize)
            .put("bgColor", config.bgColor)
            .put("cornerRadius", config.cornerRadius)
            .put("cells", cells)
    }

    private fun parse(json: JSONObject): WidgetConfig {        val config = WidgetConfig(
            cols = json.optInt("cols", 4),
            rows = json.optInt("rows", 2),
            showLabels = json.optBoolean("showLabels", true),
            labelSize = json.optInt("labelSize", 10),
            bgColor = json.optInt("bgColor", 0xCC15161A.toInt()),
            cornerRadius = json.optInt("cornerRadius", 20)
        )
        val cells = json.optJSONArray("cells") ?: return config
        for (i in 0 until cells.length()) {
            val cell = cells.optJSONObject(i) ?: continue
            config.cells.add(
                CellConfig(
                    index = cell.optInt("index"),
                    type = cell.optString("type", "empty"),
                    label = cell.optString("label", ""),
                    icon = cell.optStringOrNull("icon"),
                    color = cell.optInt("color", 0xFFFFFFFF.toInt()),
                    pkg = cell.optStringOrNull("pkg"),
                    extra = cell.optStringOrNull("extra"),
                    action = cell.optStringOrNull("action")
                )
            )
        }
        return config
    }

    companion object {
        /** Разметка виджета фиксирована: 8 колонок, 10 рядов, лишнее скрывается. */
        const val GRID_COLUMNS = 8
        const val GRID_ROWS = 10
    }
}

/**
 * Читает строку, которой может не быть вовсе (JSONObject.NULL тоже считается
 * отсутствующей). Обычный optString(key, null) в такой ситуации роняет разбор.
 */
private fun JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key, "").takeIf { it.isNotEmpty() }
}
