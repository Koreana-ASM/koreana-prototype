package com.koreana.prototype.map

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.koreana.prototype.Alley
import com.koreana.prototype.AppModel
import com.koreana.prototype.R
import com.koreana.prototype.Scripts
import com.koreana.prototype.Town
import com.koreana.prototype.talk.MintButton
import com.koreana.prototype.talk.Triangle
import com.koreana.prototype.ui.Ic
import com.koreana.prototype.ui.LocalBottomExtra
import com.koreana.prototype.ui.PK
import com.koreana.prototype.ui.T
import com.koreana.prototype.ui.TK
import com.koreana.prototype.ui.Tab
import com.koreana.prototype.ui.TabBar
import com.koreana.prototype.ui.plainClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 지도 틀(390 너비) 안에서 동네 그림이 놓이는 자리. 그림(1024 x 1536)은 0.58배로 놓인다.
private const val IMG_X = -118f
private const val IMG_Y = 100f
private const val IMG_W = 593.92f
private const val IMG_H = 890.88f
private const val IMG_SCALE = 0.58f
private const val MAP_H = IMG_Y + IMG_H

/** 지우 표시가 놓이는 자리: 이름표 바로 왼쪽. */
private fun markerSpot(a: Alley) = Offset(a.labelX - 50f, a.labelY - 14f)

/**
 * 지도 탭. 내려다본 서울 동네 한 장: 골목이 될 랜드마크 8곳만 원래 색으로 떠오르고 나머지는 색을 낮춘다.
 * 가 본 곳은 이름표에 불이 켜지고, 지금 골목에는 지우 얼굴이 붙는다. 순서, 번호, 자물쇠는 없다.
 * 랜드마크를 누르면 지도가 움직여 누른 곳이 시트 위에 오고, "여기로 가기"는 확인 창 없이 지우가 걸어간다.
 */
@Composable
fun MapScreen(model: AppModel) {
    val scope = rememberCoroutineScope()
    val extra = LocalBottomExtra.current
    val town = ImageBitmap.imageResource(R.drawable.img_town)
    val density = LocalDensity.current.density

    var open by remember { mutableStateOf<Int?>(null) }
    var shownAlley by remember { mutableIntStateOf(model.currentAlley) }
    var travelling by remember { mutableStateOf(false) }
    val scroll = remember { Animatable(0f) }
    val walk = remember { Animatable(1f) }
    val zoom = remember { Animatable(0f) }
    var walkFrom by remember { mutableStateOf(markerSpot(Town.alleys[model.recommended ?: model.currentAlley])) }

    BoxWithConstraints(Modifier.fillMaxSize().background(PK.Town)) {
        val viewH = maxHeight.value
        val minScroll = -(MAP_H - viewH).coerceAtLeast(0f)

        fun rest(alley: Int) = (viewH * 0.45f - Town.alleys[alley].cy).coerceIn(minScroll, 0f)

        // 시트가 열리면 누른 곳이 시트 위에 오도록 지도가 움직인다.
        fun focus(alley: Int): Float {
            val sheetTop = viewH - 452f - extra.value
            return (sheetTop * 0.69f - Town.alleys[alley].cy).coerceIn(sheetTop - MAP_H, 60f)
        }

        LaunchedEffect(Unit) { scroll.snapTo(rest(model.recommended ?: model.currentAlley)) }

        fun show(alley: Int) {
            if (travelling) return
            shownAlley = alley
            open = alley
            scope.launch { scroll.animateTo(focus(alley), tween(420, easing = FastOutSlowInEasing)) }
        }

        fun close() {
            open = null
            scope.launch { scroll.animateTo(scroll.value.coerceIn(minScroll, 0f), tween(360, easing = FastOutSlowInEasing)) }
        }

        // 여기로 가기: 시트가 내려가고 지우가 동네 길을 따라 걸어가 도착하면, 그대로 그 골목 쪽으로 확대되며 홈이 된다.
        fun go(alley: Int) {
            val from = model.recommended ?: model.currentAlley
            open = null
            travelling = true
            scope.launch {
                walkFrom = markerSpot(Town.alleys[from])
                walk.snapTo(0f)
                model.moveTo(alley)
                launch { scroll.animateTo(rest(alley), tween(700, easing = FastOutSlowInEasing)) }
                delay(350)
                walk.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
                delay(650)
                zoom.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
                model.enterAlley()
            }
        }

        BackHandler(enabled = open != null) { close() }

        val jiwooAt = model.recommended ?: model.currentAlley
        val target = markerSpot(Town.alleys[jiwooAt])
        val marker = Offset(
            walkFrom.x + (target.x - walkFrom.x) * walk.value,
            walkFrom.y + (target.y - walkFrom.y) * walk.value,
        )
        val pivot = Town.alleys[model.currentAlley]

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .requiredSize(390.dp, (MAP_H + 560f).dp)
                .offset(y = ((MAP_H + 560f - viewH) / 2f + scroll.value).dp)
                .graphicsLayer {
                    val s = 1f + 1.9f * zoom.value
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(pivot.cx / 390f, pivot.cy / (MAP_H + 560f))
                }
                .draggable(
                    rememberDraggableState { delta ->
                        if (open == null && !travelling) {
                            scope.launch { scroll.snapTo((scroll.value + delta / density).coerceIn(minScroll, 0f)) }
                        }
                    },
                    Orientation.Vertical,
                ),
        ) {
            TownCanvas(town, selected = open)

            Town.alleys.forEachIndexed { i, a ->
                // 랜드마크 그림을 눌러도 시트가 열린다.
                Box(
                    Modifier.offset((a.cx - a.rx).dp, (a.cy - a.ry).dp).size((a.rx * 2).dp, (a.ry * 2).dp).clip(CircleShape)
                        .plainClick { show(i) },
                )
            }
            Town.alleys.forEachIndexed { i, a ->
                val lit = i in model.visited || (model.progress[i] ?: 0) >= 12
                NameTag(a, lit, big = i == model.currentAlley && model.recommended != null, Modifier.offset(a.labelX.dp, a.labelY.dp)) { show(i) }
            }

            // 걸어갈 길.
            if (travelling && walk.value < 1f) {
                listOf(0.3f, 0.55f, 0.8f).filter { it > walk.value + 0.08f }.forEach { f ->
                    Box(
                        Modifier.offset((walkFrom.x + (target.x - walkFrom.x) * f + 22f).dp, (walkFrom.y + (target.y - walkFrom.y) * f + 40f).dp)
                            .size(8.dp).clip(CircleShape).background(PK.Ink).padding(1.5.dp).clip(CircleShape).background(TK.Bubble),
                    )
                }
            }
            JiwooMarker(Modifier.offset(marker.x.dp, marker.y.dp), waving = model.recommended != null && !travelling) { show(jiwooAt) }
        }

        // 골목 안으로: 확대되면서 화면이 밝아진다.
        if (zoom.value > 0f) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        0f to Color(0x00F3EEDC),
                        0.55f to Color(0xFFF3EEDC).copy(alpha = 0.55f * zoom.value),
                        1f to Color(0xFFF3EEDC).copy(alpha = zoom.value),
                    ),
                ),
            )
        }
        Box(
            Modifier.fillMaxWidth().height(60.dp)
                .background(Brush.verticalGradient(listOf(PK.Town.copy(alpha = 0.55f), PK.Town.copy(alpha = 0f)))),
        )

        if (open != null) Box(Modifier.fillMaxSize().plainClick { close() })
        AnimatedVisibility(
            open != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(320)) { it },
            exit = slideOutVertically(tween(260)) { it },
        ) {
            AlleySheet(
                model = model,
                index = shownAlley,
                town = town,
                onGo = { go(shownAlley) },
                onContinue = { model.startToday(enterFromMap = false) },
                onReplay = { n -> model.watch(listOf(Scripts.store, Scripts.pocha, Scripts.hanok)[n % 3]) },
            )
        }

        if (open == null && !travelling) TabBar(model, Tab.Map)
    }
}

/** 색을 낮춘 동네 위에, 랜드마크만 원래 색으로 겹쳐 그린다. [selected]는 누른 곳(점선). */
@Composable
private fun TownCanvas(town: ImageBitmap, selected: Int?) {
    val muted = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.22f) }) }
    Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }) {
        val k = size.width / 390f
        val dst = IntOffset((IMG_X * k).toInt(), (IMG_Y * k).toInt())
        val dstSize = IntSize((IMG_W * k).toInt(), (IMG_H * k).toInt())

        drawRect(Brush.verticalGradient(listOf(Color(0xFFDBDEE3), Color(0xFFCCD4DE)), endY = 120f * k), size = Size(size.width, 120f * k))
        drawImage(town, dstOffset = dst, dstSize = dstSize, colorFilter = muted)
        drawRect(Color(0xFFEDEBE0).copy(alpha = 0.38f), Offset(0f, IMG_Y * k), Size(size.width, IMG_H * k))

        // 랜드마크 뒤의 옅은 빛.
        Town.alleys.forEach { a -> softEllipse(a.cx * k, a.cy * k, (a.rx + 13f) * k, (a.ry + 13f) * k, Color(0xFFFFFCF0).copy(alpha = 0.55f), 0.55f) }

        // 랜드마크만 원래 색으로.
        val bounds = Rect(0f, 0f, size.width, size.height)
        drawContext.canvas.saveLayer(bounds, Paint())
        Town.alleys.forEach { a -> softEllipse(a.cx * k, a.cy * k, (a.rx + 5f) * k, (a.ry + 5f) * k, Color.Black, 0.82f) }
        drawContext.canvas.saveLayer(bounds, Paint().apply { blendMode = BlendMode.SrcIn })
        drawImage(town, dstOffset = dst, dstSize = dstSize)
        drawContext.canvas.restore()
        drawContext.canvas.restore()

        // 아래로 이어지는 바탕.
        drawRect(
            Brush.verticalGradient(
                0f to PK.Town.copy(alpha = 0f), 0.12f to PK.Town, 1f to PK.Town,
                startY = (MAP_H - 40f) * k, endY = (MAP_H + 520f) * k,
            ),
            Offset(0f, (MAP_H - 40f) * k), Size(size.width, 600f * k),
        )

        selected?.let { i ->
            val a = Town.alleys[i]
            drawOval(
                Color(0xFF2E9C74),
                Offset((a.cx - a.rx - 4f) * k, (a.cy - a.ry - 4f) * k),
                Size((a.rx + 4f) * 2 * k, (a.ry + 4f) * 2 * k),
                style = Stroke(2.5f * k, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f * k, 6f * k))),
            )
        }
    }
}

/** 가장자리가 부드러운 타원. [solid]는 가운데에서 색이 꽉 찬 비율. */
private fun DrawScope.softEllipse(cx: Float, cy: Float, rx: Float, ry: Float, color: Color, solid: Float) {
    translate(cx, cy) {
        scale(1f, ry / rx, pivot = Offset.Zero) {
            drawCircle(
                Brush.radialGradient(0f to color, solid to color, 1f to color.copy(alpha = 0f), center = Offset.Zero, radius = rx),
                radius = rx, center = Offset.Zero,
            )
        }
    }
}

/** 랜드마크에 꽂힌 이름표: 한글 크게, 영어 작게. 가 본 곳은 불이 켜진다. */
@Composable
private fun NameTag(a: Alley, lit: Boolean, big: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(modifier.plainClick(onClick = onClick)) {
        Column(
            Modifier
                .drawBehind {
                    if (lit) {
                        // 켜진 불: 이름표 둘레로 번지는 빛.
                        val steps = if (big) 12 else 7
                        for (i in 1..steps) {
                            val d = i * 2.dp.toPx()
                            drawRoundRect(
                                PK.Glow.copy(alpha = 0.13f), Offset(-d, -d), Size(size.width + d * 2, size.height + d * 2),
                                CornerRadius(10.dp.toPx() + d),
                            )
                        }
                    }
                }
                .then(if (lit) Modifier else Modifier.shadow(5.dp, shape, ambientColor = Color(0x40000000), spotColor = Color(0x40000000)))
                .clip(shape)
                .background(if (lit) PK.Lit else TK.Bubble)
                .border(1.2.dp, if (lit) PK.LitStroke else PK.Ink.copy(alpha = 0.25f), shape)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            T(a.name, TK.text(13, FontWeight.Bold, 16), PK.Ink, maxLines = 1)
            T(a.en, TK.text(8, FontWeight.Normal, 10), PK.Muted, maxLines = 1)
        }
        Triangle(if (lit) PK.Lit else TK.Bubble, Modifier.offset(x = (a.tailX - a.labelX + 0.67f).dp).size(8.66.dp, 5.25.dp))
    }
}

/** 지금 골목에 붙는 지우 얼굴. [waving]이면 추천 골목에서 손을 흔든다. */
@Composable
private fun JiwooMarker(modifier: Modifier, waving: Boolean, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "wave")
    val wave by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "w")
    Box(modifier.size(52.dp, 60.dp).plainClick(onClick = onClick)) {
        Triangle(TK.Bubble, Modifier.align(Alignment.BottomCenter).padding(bottom = 3.2.dp).size(10.4.dp, 6.75.dp))
        Box(
            Modifier.size(52.dp).shadow(8.dp, CircleShape, ambientColor = Color(0x4D000000), spotColor = Color(0x4D000000))
                .clip(CircleShape).background(TK.Bubble).padding(4.dp),
        ) { Image(painterResource(R.drawable.img_jiwoo_face), null, Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop) }
        if (waving) {
            Canvas(Modifier.offset((-12).dp, (-6).dp).size(16.dp).alpha(wave)) {
                val w = 1.8.dp.toPx()
                listOf(0.45f, 0.8f).forEach { f ->
                    drawArc(
                        PK.Ink, 190f, 70f, false,
                        topLeft = Offset(size.width * (1 - f), size.height * (1 - f)), size = Size(size.width * f * 2, size.height * f * 2),
                        style = Stroke(w, cap = StrokeCap.Round),
                    )
                }
            }
        }
    }
}

/** 대화 점: 한 편에 4개. 나눈 만큼 채워진다. */
@Composable
private fun TalkDots(filled: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(4) { i ->
            Box(
                Modifier.size(7.dp).clip(CircleShape)
                    .then(if (i < filled) Modifier.background(TK.MintStrong) else Modifier.border(1.2.dp, TK.MintStrong, CircleShape)),
            )
        }
    }
}

/**
 * 골목 시트. 지금 골목이면 "이어서 하기", 다른 곳이면 "여기로 가기".
 * 다 본 편은 다시 보기, 처음 가는 골목은 1편만 또렷하게 보인다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlleySheet(
    model: AppModel,
    index: Int,
    town: ImageBitmap,
    onGo: () -> Unit,
    onContinue: () -> Unit,
    onReplay: (Int) -> Unit,
) {
    val extra = LocalBottomExtra.current
    val a = Town.alleys[index]
    val progress = model.progress[index] ?: 0
    val here = index == model.currentAlley
    var phrasesOpen by remember(index) { mutableStateOf(false) }
    val thumb = remember(town, index) {
        // 랜드마크 둘레만 잘라 쓴다 (56 x 44 칸에 0.24배).
        val cx = (a.cx - IMG_X) / IMG_SCALE
        val cy = (a.cy - IMG_Y) / IMG_SCALE
        val left = (cx - 116f).toInt().coerceIn(0, town.width - 233)
        val top = (cy - 91f).toInt().coerceIn(0, town.height - 183)
        BitmapPainter(town, IntOffset(left, top), IntSize(233, 183))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 452.dp + extra)
            .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(TK.Sheet2)
            .plainClick { }
            .padding(start = 24.dp, end = 24.dp, bottom = 34.dp + extra),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column {
        Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(36.dp, 4.dp).clip(CircleShape).background(Color(0xB3778A72)))
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            T(a.name, TK.text(22, FontWeight.Bold, 27), TK.Text, maxLines = 1)
            T(a.en, TK.text(12, FontWeight.Normal, 15), TK.Muted, Modifier.padding(bottom = 3.dp), maxLines = 1)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(painterResource(R.drawable.img_jiwoo_face), null, Modifier.size(26.dp).clip(CircleShape))
            T("“${a.quote}”", TK.text(14, FontWeight.Medium, 17), TK.Mint, maxLines = 1)
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            a.episodes.forEachIndexed { n, title ->
                val filled = (progress - n * 4).coerceIn(0, 4)
                val done = filled == 4
                val dim = filled == 0 && n > 0
                Row(
                    Modifier
                        .fillMaxWidth()
                        .alpha(if (dim) 0.38f else 1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (filled in 1..3) Color.White.copy(alpha = 0.07f) else Color.Transparent)
                        .then(if (done) Modifier.clickable { onReplay(n) } else Modifier)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Image(thumb, null, Modifier.size(56.dp, 44.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            T("${n + 1}편", TK.text(12, FontWeight.Medium, 15), TK.Muted, maxLines = 1)
                            T(title, TK.text(15, FontWeight.Bold, 18), TK.Text, maxLines = 1)
                        }
                        if (!dim) TalkDots(filled)
                    }
                    if (done) Ic(R.drawable.ic_retry, size = 18.dp)
                }
            }
        }
        if (progress > 0) {
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x66778A72)))
            Row(
                Modifier.fillMaxWidth().clickable { phrasesOpen = !phrasesOpen }.padding(horizontal = 10.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                T("여기서 쓰는 말", TK.text(14, FontWeight.Medium, 17), TK.Text, Modifier.weight(1f), maxLines = 1)
                Ic(R.drawable.ic_chevron_down, size = 18.dp, modifier = Modifier.graphicsLayer { rotationZ = if (phrasesOpen) 180f else 0f })
            }
            if (phrasesOpen) {
                FlowRow(
                    Modifier.padding(start = 10.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    a.phrases.forEach { (ko, en) ->
                        Column(
                            Modifier.clip(RoundedCornerShape(12.dp)).background(TK.Pill).border(1.dp, TK.ListenStroke, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            T(ko, TK.text(15, FontWeight.Bold, 18), TK.Text, maxLines = 1)
                            T(en, TK.text(12, FontWeight.Normal, 15), TK.Muted, maxLines = 1)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
      }
        MintButton(if (here) "이어서 하기" else "여기로 가기", onClick = if (here) onContinue else onGo)
    }
}
