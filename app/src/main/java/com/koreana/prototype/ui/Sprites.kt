package com.koreana.prototype.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.koreana.prototype.R
import com.koreana.prototype.Town
import com.koreana.prototype.Trophy

/** 장면 그림(1024, 2 x 2 묶음)에서 한 칸만 잘라 쓴다. Figma에서 한 칸은 그림의 49.4%다. */
@Composable
fun scenePainter(col: Int, row: Int): Painter {
    val sheet = ImageBitmap.imageResource(R.drawable.img_polaroids)
    return remember(sheet, col, row) { BitmapPainter(sheet, IntOffset(col * 518, row * 518), IntSize(506, 506)) }
}

/** 골목을 대표하는 장면 (노트의 골목 그림). */
@Composable
fun alleyScene(alley: Int): Painter = when (alley) {
    Town.Hangang -> scenePainter(0, 0)
    Town.Music -> scenePainter(1, 1)
    Town.Food -> scenePainter(0, 1)
    else -> scenePainter(1, 0)
}

/** 트로피 그림(1536 x 1024, 4 x 2 묶음)에서 트로피 하나만 정사각형으로 잘라 쓴다. */
@Composable
fun trophyPainter(trophy: Trophy): Painter {
    val sheet = ImageBitmap.imageResource(R.drawable.img_trophies)
    return remember(sheet, trophy) {
        val cx = 192 + 384 * trophy.col
        val cy = if (trophy.row == 0) 268 else 750
        BitmapPainter(sheet, IntOffset(cx - 200, (cy - 200).coerceIn(0, 1024 - 400)), IntSize(400, 400))
    }
}
