package com.pavel.widgetplus

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Формат widgets.json — то, что переживает перезапуск и попадает в резервную копию.
 * Тесты фиксируют, что поля не теряются, «пустые» значения читаются как null,
 * а импорт копии не удаляет виджеты, которых нет в файле.
 */
class ConfigStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun store(): ConfigStore = ConfigStore(File(folder.root, "widgets.json"))

    private fun sampleConfig(): WidgetConfig = WidgetConfig(cols = 5, rows = 3).apply {
        showLabels = false
        labelSize = 11
        bgColor = 0x00000000
        cornerRadius = 8
        padding = 2
        cellShape = "circle"
        cellFill = 0x554F7CFF.toInt()
        cells.add(
            CellConfig(
                index = 0,
                type = "app",
                label = "Камера",
                pkg = "com.oplus.camera",
                extra = "com.oplus.camera.Camera",
                shape = "rounded",
                fill = 0x33FFFFFF.toInt()
            )
        )
        cells.add(
            CellConfig(
                index = 1,
                type = "command",
                label = "VPN",
                icon = "ic_lc_shield",
                color = 0xFF34D399.toInt(),
                pkg = "com.v2raytun.android",
                extra = "com.v2raytun.android.receiver.WidgetProvider1x1",
                action = "com.v2raytun.android.action.widget.click"
            )
        )
        cells.add(
            CellConfig(index = 2, type = "link", label = "ya.ru", extra = "https://ya.ru")
        )
    }

    @Test
    fun `конфиг переживает сохранение и чтение`() {
        val config = sampleConfig()
        store().save(31, config)

        val loaded = store().load(31)!!

        assertEquals(5, loaded.cols)
        assertEquals(3, loaded.rows)
        assertEquals(false, loaded.showLabels)
        assertEquals(11, loaded.labelSize)
        assertEquals(0x00000000, loaded.bgColor)
        assertEquals(8, loaded.cornerRadius)
        assertEquals(2, loaded.padding)
        assertEquals("circle", loaded.cellShape)
        assertEquals(0x554F7CFF.toInt(), loaded.cellFill)
        assertEquals(3, loaded.cells.size)
    }

    @Test
    fun `поля действия и оформления не теряются`() {
        store().save(7, sampleConfig())
        val command = store().load(7)!!.cellAt(1)!!

        assertEquals("com.v2raytun.android", command.pkg)
        assertEquals("com.v2raytun.android.receiver.WidgetProvider1x1", command.extra)
        assertEquals("com.v2raytun.android.action.widget.click", command.action)
        assertEquals("ic_lc_shield", command.icon)

        val app = store().load(7)!!.cellAt(0)!!
        assertEquals("rounded", app.shape)
        assertEquals(0x33FFFFFF.toInt(), app.fill)
    }

    @Test
    fun `отсутствующие необязательные поля читаются как null`() {
        store().save(3, sampleConfig())
        val link = store().load(3)!!.cellAt(2)!!

        assertNull(link.icon)
        assertNull(link.pkg)
        assertNull(link.action)
        assertEquals("link", link.type)
    }

    @Test
    fun `удаление виджета не трогает остальные`() {
        val store = store()
        store.save(1, sampleConfig())
        store.save(2, sampleConfig())

        store.delete(1)

        assertNull(store.load(1))
        assertTrue(store.load(2) != null)
    }

    @Test
    fun `экспорт и импорт возвращают те же настройки`() {
        val store = store()
        store.save(9, sampleConfig())
        val backup = store.exportAll()

        val other = ConfigStore(File(folder.root, "second.json"))
        assertEquals(1, other.importAll(backup))
        assertEquals(5, other.load(9)!!.cols)

        val raw = JSONObject(backup)
        assertTrue(raw.has("widgets"))
        assertTrue(raw.getJSONObject("widgets").has("9"))
    }

    @Test
    fun `импорт не удаляет виджеты, которых нет в копии`() {
        val store = store()
        store.save(1, sampleConfig())
        val backup = store.exportAll()

        store.save(2, sampleConfig())
        store.importAll(backup)

        assertTrue("виджет 2 должен остаться", store.load(2) != null)
        assertTrue("виджет 1 восстановлен", store.load(1) != null)
    }

    @Test
    fun `битый файл не роняет приложение`() {
        val file = File(folder.root, "widgets.json")
        file.writeText("{ это не json")

        assertTrue(store().loadAll().isEmpty())
    }
}
