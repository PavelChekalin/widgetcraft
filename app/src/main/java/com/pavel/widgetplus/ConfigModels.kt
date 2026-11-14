package com.pavel.widgetplus

/**
 * Конфиг одного виджета. cells содержит только заполненные ячейки,
 * index — позиция в сетке (row * cols + col).
 */
data class CellConfig(
    val index: Int,
    val type: String,
    val label: String,
    val icon: String? = null,
    val color: Int = 0xFFFFFFFF.toInt(),
    val pkg: String? = null,
    val extra: String? = null,
    val action: String? = null
)

data class WidgetConfig(
    var cols: Int = 4,
    var rows: Int = 2,
    var showLabels: Boolean = true,
    var labelSize: Int = 10,
    var bgColor: Int = 0xCC15161A.toInt(),
    var cornerRadius: Int = 20,
    var cells: MutableList<CellConfig> = mutableListOf()
) {
    fun cellAt(index: Int): CellConfig? = cells.firstOrNull { it.index == index }

    fun put(cell: CellConfig) {
        cells.removeAll { it.index == cell.index }
        cells.add(cell)
    }

    fun removeAt(index: Int) {
        cells.removeAll { it.index == index }
    }
}
