"""Генерирует res/layout/widget_grid.xml — фиксированную сетку 8×10 для виджета.

RemoteViews не умеет добавлять вьюхи в рантайме, поэтому разметка максимальная,
а лишние ряды и ячейки скрываются через setViewVisibility.

Запуск: python tools/make_grid_layout.py
"""

import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app", "src", "main", "res", "layout", "widget_grid.xml")

COLUMNS = 8
ROWS = 10

HEADER = """<?xml version="1.0" encoding="utf-8"?>
<!-- Сетка 8x10: лишние ряды и ячейки скрываются через setViewVisibility.
     Сгенерировано tools/make_grid_layout.py, руками не править. -->
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <ImageView
        android:id="@+id/bgImage"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="fitXY" />

    <LinearLayout
        android:id="@+id/grid"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical">
"""

FOOTER = """    </LinearLayout>

</FrameLayout>
"""


def cell(index: int) -> str:
    return f"""
            <LinearLayout
                android:id="@+id/cell{index}"
                android:layout_width="0dp"
                android:layout_height="match_parent"
                android:layout_weight="1"
                android:orientation="vertical"
                android:gravity="center"
                android:padding="1dp">

                <ImageView
                    android:id="@+id/icon{index}"
                    android:layout_width="34dp"
                    android:layout_height="34dp"
                    android:scaleType="fitCenter" />

                <TextView
                    android:id="@+id/label{index}"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:gravity="center"
                    android:maxLines="1"
                    android:ellipsize="end"
                    android:paddingTop="2dp"
                    android:textColor="@color/widget_text"
                    android:textSize="10sp" />

            </LinearLayout>
"""


def row(index: int) -> str:
    cells = "".join(cell(index * COLUMNS + column) for column in range(COLUMNS))
    return f"""
        <LinearLayout
            android:id="@+id/row{index}"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:orientation="horizontal">
{cells}
        </LinearLayout>
"""


def main() -> int:
    body = "".join(row(index) for index in range(ROWS))
    with open(OUT, "w", encoding="utf-8") as handle:
        handle.write(HEADER + body + FOOTER)
    print("готово:", OUT, "строк:", len((HEADER + body + FOOTER).splitlines()))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
