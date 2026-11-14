package com.pavel.widgetplus

/**
 * Готовые broadcast-команды для ячеек: не нужно вручную вписывать пакет,
 * получателя и действие. Список пополняется по мере надобности.
 */
object KnownCommands {

    data class Command(
        val label: String,
        val packageName: String,
        val component: String,
        val action: String
    )

    fun all(): List<Command> = listOf(
        Command(
            label = "VPN v2rayTun — включить",
            packageName = "com.v2raytun.android",
            component = "com.v2raytun.android.receiver.WidgetProvider1x1",
            action = "com.v2raytun.android.action.widget.click"
        ),
        Command(
            label = "VPN v2rayTun — выключить",
            packageName = "com.v2raytun.android",
            component = "com.v2raytun.android.receiver.WidgetProvider1x1",
            action = "com.v2raytun.android.action.widget.click"
        )
    )
}
