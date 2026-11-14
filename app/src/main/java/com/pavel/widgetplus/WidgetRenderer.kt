package com.pavel.widgetplus

import android.app.PendingIntent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import kotlin.math.min

/**
 * Собирает RemoteViews по конфигу. Разметка фиксированная (8×10),
 * поэтому лишние ряды и ячейки просто скрываются.
 *
 * Иконка уменьшается тем сильнее, чем больше кнопок: у обновления виджета
 * есть лимит Binder около 1 МБ, и крупные Bitmap его переполняют.
 */
class WidgetRenderer(private val context: Context) {

    private val catalog by lazy { AppCatalog(context) }

    fun build(widgetId: Int, config: WidgetConfig, page: Int = 0): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_grid)
        views.setImageViewBitmap(
            R.id.bgImage,
            IconBitmap.roundedBackground(context, config.bgColor, config.cornerRadius)
        )
        val density = context.resources.displayMetrics.density

        val maxIconPx = when {
            config.cells.size > 24 -> 48
            config.cells.size > 16 -> 64
            else -> 96
        }
        val iconSizePx = min((34 * density).toInt(), maxIconPx)

        val padPx = (config.padding * density).toInt()
        views.setViewPadding(R.id.grid, padPx, padPx, padPx, padPx)

        for (row in 0 until ConfigStore.GRID_ROWS) {
            views.setViewVisibility(
                viewId("row$row"),
                if (row < config.rows) View.VISIBLE else View.GONE
            )
        }

        val total = ConfigStore.GRID_COLUMNS * ConfigStore.GRID_ROWS
        for (slot in 0 until total) {
            val row = slot / ConfigStore.GRID_COLUMNS
            val col = slot % ConfigStore.GRID_COLUMNS
            val visible = row < config.rows && col < config.cols
            val cell = if (visible) config.cellAt(slot, page) else null

            if (cell == null || cell.type == "empty" || cell.type == "draft") {
                views.setViewVisibility(viewId("cell$slot"), View.GONE)
                continue
            }

            views.setViewVisibility(viewId("cell$slot"), View.VISIBLE)
            views.setTextViewText(
                viewId("label$slot"),
                if (config.showLabels) cell.label else ""
            )
            views.setTextViewTextSize(
                viewId("label$slot"),
                TypedValue.COMPLEX_UNIT_SP,
                config.labelSize.toFloat()
            )

            val iconShape = cell.shape ?: config.cellShape.takeIf { it != "none" }
            val indicator = if (cell.type == "toggle") {
                if (isVpnOn()) 0xFF34D399.toInt() else 0xFF94A3B8.toInt()
            } else null
            val iconFill = cell.fill ?: if (iconShape != null) config.cellFill else null
            val bitmap = when {
                cell.icon != null -> IconBitmap.bitmap(
                    context, cell.icon, cell.color, iconSizePx, iconShape, iconFill, indicator
                )
                cell.type == "app" && cell.pkg != null ->
                    catalog.appIconBitmap(cell.pkg, cell.extra.orEmpty(), iconSizePx)
                else -> null
            }
            views.setImageViewBitmap(viewId("icon$slot"), bitmap ?: IconBitmap.empty())

            views.setOnClickPendingIntent(
                viewId("cell$slot"),
                cellPendingIntent(widgetId, slot, cell)
            )
        }

        return views
    }

    private fun viewId(name: String): Int =
        context.resources.getIdentifier(name, "id", context.packageName)

/** Включён ли сейчас VPN — видно по активному соединению, без разрешений. */
    private fun isVpnOn(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    private fun cellPendingIntent(widgetId: Int, slot: Int, cell: CellConfig): PendingIntent {
        val requestCode = widgetId * 100 + slot

        // Приложение: обычный запуск через launcher-intent.
        if (cell.type == "app" && cell.pkg != null && cell.extra != null) {
            val launch = AppCatalog.launcherIntent(cell.pkg, cell.extra)
            return PendingIntent.getActivity(
                context,
                requestCode,
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Ссылка: открыть в браузере или в приложении, которое её умеет.
        if (cell.type == "link" && !cell.extra.isNullOrBlank()) {
            val open = Intent(Intent.ACTION_VIEW, Uri.parse(cell.extra))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(
                context,
                requestCode,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

// Переключатель: команда идёт через нас, чтобы потом обновить индикатор.
        if (cell.type == "toggle" && !cell.action.isNullOrBlank()) {
            val toggle = Intent(context, CraftWidgetProvider::class.java)
                .setAction(CraftWidgetProvider.ACTION_TOGGLE)
                .putExtra(CraftWidgetProvider.EXTRA_WIDGET_ID, widgetId)
                .putExtra(CraftWidgetProvider.EXTRA_PKG, cell.pkg)
                .putExtra(CraftWidgetProvider.EXTRA_COMPONENT, cell.extra)
                .putExtra(CraftWidgetProvider.EXTRA_ACTION, cell.action)
                .setData(Uri.parse("widgetplus://toggle/$widgetId/$slot"))
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                toggle,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Команда: broadcast без открытия приложения (VPN, фонарик и подобное).
        if (cell.type == "command" && !cell.action.isNullOrBlank()) {
            val command = Intent(cell.action).apply {
                if (!cell.pkg.isNullOrBlank() && !cell.extra.isNullOrBlank()) {
                    component = android.content.ComponentName(cell.pkg, cell.extra)
                } else if (!cell.pkg.isNullOrBlank()) {
                    `package` = cell.pkg
                }
                setData(Uri.parse("widgetplus://cmd/$widgetId/$slot"))
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                command,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        if (cell.type == "pager") {
            val pager = Intent(context, CraftWidgetProvider::class.java)
                .setAction(CraftWidgetProvider.ACTION_PAGE_NEXT)
                .putExtra(CraftWidgetProvider.EXTRA_WIDGET_ID, widgetId)
                .setData(Uri.parse("widgetplus://pager/$widgetId/$slot"))
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                pager,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val intent = Intent(context, CraftWidgetProvider::class.java)
            .setAction(CraftWidgetProvider.ACTION_CELL_CLICK)
            .putExtra(CraftWidgetProvider.EXTRA_LABEL, cell.label)
            .putExtra(CraftWidgetProvider.EXTRA_SLOT, slot)
            .setData(Uri.parse("widgetplus://cell/$widgetId/$slot"))

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
