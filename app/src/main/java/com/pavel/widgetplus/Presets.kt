package com.pavel.widgetplus

/**
 * Готовые наборы кнопок. Пользователь применяет набор к виджету и правит под себя.
 * Приложения в наборах заданы «заготовками» (иконка + подпись, без пакета):
 * конкретное приложение человек выбирает сам, потому что у всех оно своё.
 */
object Presets {

    data class Preset(
        val name: String,
        val description: String,
        val build: () -> WidgetConfig
    )

    fun builtin(): List<Preset> = listOf(
        Preset("VPN", "Кнопка VPN командой, без открытия приложения") {
            WidgetConfig(cols = 2, rows = 1).apply {
                cells.add(
                    CellConfig(
                        index = 0,
                        type = "command",
                        label = "VPN вкл",
                        icon = "ic_lc_shield",
                        color = 0xFF34D399.toInt(),
                        pkg = "com.v2raytun.android",
                        extra = "com.v2raytun.android.receiver.WidgetProvider1x1",
                        action = "com.v2raytun.android.action.widget.click"
                    )
                )
                cells.add(
                    CellConfig(
                        index = 1,
                        type = "command",
                        label = "VPN выкл",
                        icon = "ic_lc_shield_off",
                        color = 0xFFFB7185.toInt(),
                        pkg = "com.v2raytun.android",
                        extra = "com.v2raytun.android.receiver.WidgetProvider1x1",
                        action = "com.v2raytun.android.action.widget.click"
                    )
                )
            }
        },
        Preset("Связь", "Звонок, сообщения, почта, интернет") {
            WidgetConfig(cols = 4, rows = 1).apply {
                placeholders(
                    this,
                    listOf(
                        Triple("Звонок", "ic_lc_phone", 0xFF34D399.toInt()),
                        Triple("СМС", "ic_lc_message_circle", 0xFF60A5FA.toInt()),
                        Triple("Почта", "ic_lc_mail", 0xFFFFD166.toInt()),
                        Triple("Интернет", "ic_lc_globe", 0xFF22D3EE.toInt())
                    )
                )
            }
        },
        Preset("Склад", "Заготовки под склад: посылки, штрих-код, файлы, заметки") {
            WidgetConfig(cols = 4, rows = 2).apply {
                placeholders(
                    this,
                    listOf(
                        Triple("Посылки", "ic_lc_package", 0xFFFF9F45.toInt()),
                        Triple("Штрих-код", "ic_lc_barcode", 0xFFC084FC.toInt()),
                        Triple("Файлы", "ic_lc_folder", 0xFFFFD166.toInt()),
                        Triple("Заметки", "ic_lc_sticky_note", 0xFF60A5FA.toInt()),
                        Triple("Склад", "ic_lc_warehouse", 0xFF34D399.toInt()),
                        Triple("Грузовик", "ic_lc_truck", 0xFFF472B6.toInt()),
                        Triple("Чек-лист", "ic_lc_list_checks", 0xFF22D3EE.toInt()),
                        Triple("Сканер", "ic_lc_scan", 0xFF94A3B8.toInt())
                    )
                )
            }
        },
        Preset("Файлы", "Папки и документы") {
            WidgetConfig(cols = 3, rows = 2).apply {
                placeholders(
                    this,
                    listOf(
                        Triple("Папка", "ic_lc_folder", 0xFFFFD166.toInt()),
                        Triple("Документы", "ic_lc_file_text", 0xFF60A5FA.toInt()),
                        Triple("Загрузки", "ic_lc_download", 0xFF34D399.toInt()),
                        Triple("Фото", "ic_lc_images", 0xFFF472B6.toInt()),
                        Triple("Архив", "ic_lc_archive", 0xFFC084FC.toInt()),
                        Triple("Корзина", "ic_lc_trash_2", 0xFFFB7185.toInt())
                    )
                )
            }
        },
        Preset("Рабочий", "Склад, файлы, камера и кнопка VPN в одном виджете") {
            WidgetConfig(cols = 4, rows = 2).apply {
                placeholders(
                    this,
                    listOf(
                        Triple("Посылки", "ic_lc_package", 0xFFFF9F45.toInt()),
                        Triple("Файлы", "ic_lc_folder", 0xFFFFD166.toInt()),
                        Triple("Заметки", "ic_lc_sticky_note", 0xFF60A5FA.toInt()),
                        Triple("Камера", "ic_lc_camera", 0xFFE2E8F0.toInt()),
                        Triple("Склад", "ic_lc_warehouse", 0xFF34D399.toInt()),
                        Triple("Почта", "ic_lc_mail", 0xFFF472B6.toInt()),
                        Triple("Общий", "ic_lc_share_2", 0xFFB0BEC5.toInt())
                    )
                )
                // Восьмая ячейка — переключатель VPN с индикатором.
                cells.add(
                    CellConfig(
                        index = 1 * ConfigStore.GRID_COLUMNS + 3,
                        type = "toggle",
                        label = "VPN",
                        icon = "ic_lc_shield",
                        color = 0xFF34D399.toInt(),
                        pkg = "com.v2raytun.android",
                        extra = "com.v2raytun.android.receiver.WidgetProvider1x1",
                        action = "com.v2raytun.android.action.widget.click"
                    )
                )
            }
        },
    )

    /** Ячейка-заготовка: иконка и подпись есть, приложение выбирается потом. */
    private fun placeholders(
        config: WidgetConfig,
        items: List<Triple<String, String, Int>>
    ) {
        items.forEachIndexed { position, (label, icon, color) ->
            val index = (position / config.cols) * ConfigStore.GRID_COLUMNS + position % config.cols
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
    }
}
