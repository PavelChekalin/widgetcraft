package com.pavel.widgetplus

import android.content.Context
import org.json.JSONArray

/**
 * Коллекция иконок из assets/icons.json (Lucide, MIT).
 * Поиск идёт по русскому имени, ключевым словам и категории.
 */
class IconCatalog(private val context: Context) {

    data class IconEntry(
        val file: String,
        val name: String,
        val category: String,
        val keywords: String
    ) {
        fun matches(query: String): Boolean {
            if (query.isBlank()) return true
            val needle = query.trim().lowercase()
            return name.lowercase().contains(needle) ||
                keywords.lowercase().contains(needle) ||
                category.lowercase().contains(needle) ||
                file.contains(needle)
        }
    }

    private var entries: List<IconEntry>? = null

    fun all(): List<IconEntry> {
        entries?.let { return it }
        val loaded = load()
        entries = loaded
        return loaded
    }

    fun categories(): List<String> = all().map { it.category }.distinct()

    fun byCategory(category: String?): List<IconEntry> =
        if (category == null) all() else all().filter { it.category == category }

    fun search(query: String, category: String?): List<IconEntry> =
        byCategory(category).filter { it.matches(query) }

    fun displayName(file: String): String =
        all().firstOrNull { it.file == file }?.name.orEmpty()

    fun drawableId(file: String): Int =
        context.resources.getIdentifier(file, "drawable", context.packageName)

    private fun load(): List<IconEntry> {
        val result = mutableListOf<IconEntry>()
        runCatching {
            val json = context.assets.open("icons.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                result.add(
                    IconEntry(
                        file = item.optString("file"),
                        name = item.optString("name"),
                        category = item.optString("category"),
                        keywords = item.optString("keywords")
                    )
                )
            }
        }
        return result
    }

    companion object {
        /** Палитра для иконок: 16 готовых цветов, свой вводить не нужно. */
        val PALETTE = intArrayOf(
            0xFFFFFFFF.toInt(),
            0xFFE2E8F0.toInt(),
            0xFF94A3B8.toInt(),
            0xFF60A5FA.toInt(),
            0xFF818CF8.toInt(),
            0xFFC084FC.toInt(),
            0xFFF472B6.toInt(),
            0xFFFB7185.toInt(),
            0xFFFF5C5C.toInt(),
            0xFFFF9F45.toInt(),
            0xFFFFD166.toInt(),
            0xFFA3E635.toInt(),
            0xFF34D399.toInt(),
            0xFF22D3EE.toInt(),
            0xFF2DD4BF.toInt(),
            0xFFF59E0B.toInt()
        )
    }
}
