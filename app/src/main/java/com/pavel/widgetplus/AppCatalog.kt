package com.pavel.widgetplus

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable

/**
 * Список установленных приложений, которые можно запустить с рабочего стола.
 * Иконки берём родные: через PackageManager, без интернета.
 */
class AppCatalog(private val context: Context) {

    data class AppEntry(
        val label: String,
        val packageName: String,
        val activityName: String,
        val icon: Drawable?
    ) {
        val component: ComponentName get() = ComponentName(packageName, activityName)
    }

    private val iconCache = HashMap<String, Bitmap>()

    fun load(): List<AppEntry> {
        val manager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = manager.queryIntentActivities(intent, 0)

        return resolved
            .map { info ->
                val packageName = info.activityInfo.packageName
                val activityName = info.activityInfo.name
                AppEntry(
                    label = info.loadLabel(manager).toString(),
                    packageName = packageName,
                    activityName = activityName,
                    icon = runCatching { info.loadIcon(manager) }.getOrNull()
                )
            }
            .filter { it.label.isNotBlank() }
            .sortedBy { it.label.lowercase() }
    }

    /** Bitmap родной иконки приложения, уменьшенный до sizePx. */
    fun appIconBitmap(packageName: String, activityName: String, sizePx: Int): Bitmap? {
        val key = "$packageName/$activityName@$sizePx"
        iconCache[key]?.let { return it }

        val manager = context.packageManager
        val drawable = runCatching {
            manager.getActivityIcon(ComponentName(packageName, activityName))
        }.getOrNull() ?: return null

        val bitmap = drawableToBitmap(drawable, sizePx)
        iconCache[key] = bitmap
        return bitmap
    }

    /** Адаптивные иконки рисуем в «безопасной зоне», иначе края обрезаются. */
    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (drawable is AdaptiveIconDrawable) {
            val inset = (sizePx * 0.12f).toInt()
            drawable.setBounds(inset, inset, sizePx - inset, sizePx - inset)
        } else {
            drawable.setBounds(0, 0, sizePx, sizePx)
        }
        drawable.draw(canvas)
        return bitmap
    }

    companion object {
        fun launcherIntent(packageName: String, activityName: String): Intent =
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(ComponentName(packageName, activityName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

        fun isInstalled(context: Context, packageName: String): Boolean = runCatching {
            context.packageManager.getPackageInfo(packageName, 0)
        }.isSuccess
    }
}
