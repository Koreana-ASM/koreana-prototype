package com.koreana.prototype.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.koreana.prototype.AppModel
import com.koreana.prototype.HomeVariant
import com.koreana.prototype.R
import com.koreana.prototype.Script
import com.koreana.prototype.Scripts
import com.koreana.prototype.ui.At
import com.koreana.prototype.ui.CenteredAt
import com.koreana.prototype.ui.GlassChip
import com.koreana.prototype.ui.Ic
import com.koreana.prototype.ui.K
import com.koreana.prototype.ui.LocalBottomExtra
import com.koreana.prototype.ui.StatPill
import com.koreana.prototype.ui.T
import com.koreana.prototype.ui.Type
import com.koreana.prototype.ui.floatingCard
import com.koreana.prototype.ui.plainClick
import com.koreana.prototype.ui.rememberToast
import kotlinx.coroutines.delay
import kotlin.math.hypot

private const val OUT_OF_SCOPE = "이번 목업 범위 밖이에요 (A · B · C만 구현)"

/** 지나온 길 (Figma 프레임 좌표). 한옥 카페 → 포장마차 → 오늘의 편의점. */
private val walkedPath = listOf(Offset(140f, 632f), Offset(292f, 690f), Offset(302f, 520f), Offset(184f, 446f))

/** 오늘 대화를 끝내면 다음 곳(다리 위)까지 이어지는 길. */
private val nextPath = listOf(
    Offset(184f, 446f), Offset(227f, 414f), Offset(255f, 359f), Offset(255f, 326f), Offset(233f, 257f), Offset(186f, 186f),
)

private fun pointsAlong(path: List<Offset>, step: Float, fraction: Float = 1f): List<Offset> {
    val lengths = path.zipWithNext { a, b -> hypot(b.x - a.x, b.y - a.y) }
    val limit = lengths.sum() * fraction
    val out = mutableListOf<Offset>()
    var walked = 0f
    for (i in lengths.indices) {
        var d = 0f
        while (d < lengths[i]) {
            if (walked + d > limit) return out
            val t = d / lengths[i]
            out += Offset(path[i].x + (path[i + 1].x - path[i].x) * t, path[i].y + (path[i + 1].y - path[i].y) * t)
            d += step
        }
        walked += lengths[i]
    }
    if (fraction >= 1f) out += path.last()
    return out
}

private class PastPlace(val name: String, val title: String, val sub: String, val x: Int, val y: Int, val chipY: Int, val script: Script)

private val pastPlaces = listOf(
    PastPlace("한옥 카페", "한옥 카페 · 차 한 잔", "나흘 전에 지우와 나눈 이야기", 118, 610, 579, Scripts.hanok),
    PastPlace("포장마차", "포장마차 · 떡볶이", "사흘 전에 지우와 나눈 이야기", 288, 448, 498, Scripts.pocha),
)

private class Helper(val label: String, val action: () -> Unit)

/**
 * B · 홈. 동네 지도 위에서 지우가 기다린다. 다녀온 길과 주변만 색이 있고,
 * 대화를 끝내면 다음 곳까지 색이 번진다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(model: AppModel, onOpenDebug: () -> Unit) {
    val toast = rememberToast()
    val extra = LocalBottomExtra.current
    var openPlace by remember { mutableStateOf<PastPlace?>(null) }

    val done = model.doneToday >= 1
    val complete = model.neighborhoodDone
    val limit = model.doneToday >= model.dailyLimit
    val inProgress = model.inProgressTurn
    val micOff = !model.micOn

    // 대화를 끝내고 나오면 다음 곳까지 색이 번지고 길이 밝아진다 (C-8).
    val spread = remember { Animatable(if (done && !model.justFinished) 1f else 0f) }
    LaunchedEffect(done, model.justFinished) {
        if (!done) {
            spread.snapTo(0f)
        } else if (model.justFinished) {
            if (model.doneToday == 1) {
                spread.snapTo(0f)
                delay(350)
                spread.animateTo(1f, tween(1800, easing = FastOutSlowInEasing))
            } else {
                spread.snapTo(1f)
            }
            model.justFinished = false
        } else {
            spread.snapTo(1f)
        }
    }

    // B-12 · 동네에 들어올 때 장소 이름이 잠깐 떴다 사라진다.
    LaunchedEffect(model.showPlaceToast) {
        if (model.showPlaceToast) {
            delay(2600)
            model.showPlaceToast = false
        }
    }

    BackHandler(enabled = openPlace != null) { openPlace = null }

    val start: () -> Unit = { model.startToday() }
    val replayToday: () -> Unit = { model.replay(model.lastScript) }

    // 상태마다 지우의 말, 주 행동, 보조 행동이 달라진다.
    val label: String
    val message: String
    val primary: () -> Unit
    val helpers: List<Helper>
    when {
        complete -> {
            label = "지우"; message = "한강은 다 돌아봤네요. 다음 동네로 가 볼까요?"
            primary = { toast("다음 동네는 $OUT_OF_SCOPE") }
            helpers = listOf(Helper("받은 기념품 보기") { toast(OUT_OF_SCOPE) })
        }
        limit -> {
            label = "지우"; message = "오늘은 여기까지 해요. 내일 또 만나요!"
            primary = replayToday
            helpers = listOf(Helper("오늘 대화 다시 보기", replayToday))
        }
        done -> {
            label = "지우"; message = "오늘 즐거웠어요. 내일은 다리 위에서 만나요!"
            primary = start
            helpers = listOf(Helper("오늘 대화 다시 보기", replayToday), Helper("더 하기", start))
        }
        inProgress != null -> {
            label = "지우"; message = "어디까지 했죠? 이어서 이야기해요!"
            primary = start
            helpers = listOf(Helper("${inProgress + 1} / ${model.currentScript.turns.size} · 이어서 하기", start))
        }
        micOff -> {
            label = "지우"; message = "마이크를 켜면 이야기할 수 있어요"
            primary = start
            helpers = listOf(
                Helper("마이크 켜기") {
                    model.forceMicOff = false
                    model.requestMic { granted -> if (!granted) toast("설정에서 마이크를 허용해 주세요") }
                },
                Helper("장면만 보기", start),
            )
        }
        model.variant == HomeVariant.MeFirst -> {
            label = "지금 지우는"; message = "라면을 고르고 있어요"
            primary = start
            helpers = listOf(Helper("말 걸기", start))
        }
        model.variant == HomeVariant.Returning -> {
            label = "지우"; message = "오랜만이에요! 보고 싶었어요."
            primary = start; helpers = emptyList()
        }
        model.variant == HomeVariant.VoiceJustOn -> {
            label = "지우"; message = "왔어요? 기다렸어요!"
            primary = start
            helpers = listOf(Helper("오늘부터 내 목소리로") { toast("내 목소리(E)는 $OUT_OF_SCOPE") })
        }
        else -> {
            label = "지우"; message = "왔어요? 기다렸어요!"
            primary = start
            helpers = if (model.signedUp) emptyList()
            else listOf(Helper("캐릭터와 별을 저장해 두세요") { toast("가입(J)은 $OUT_OF_SCOPE") })
        }
    }
    val waiting = !done && !complete
    val showBubble = waiting && model.variant != HomeVariant.MeFirst

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFFE5EBE0))) {
        val cover = maxOf(1f, maxHeight.value / 844f)

        // 지도와 핑: Figma 프레임 좌표 그대로 놓고 화면을 덮도록 키운다.
        Box(Modifier.align(Alignment.Center).requiredSize(390.dp, 844.dp).scale(cover)) {
            MapLayers(fullColor = complete, spread = spread.value)

            pastPlaces.forEach { place ->
                At(place.x, place.y) { CheckPing { openPlace = if (openPlace == place) null else place } }
                CenteredAt(place.x + 22, place.chipY) { GlassChip(place.name) }
            }

            if (waiting) {
                // 오늘의 핑: 누르면 장면으로.
                val t = rememberInfiniteTransition(label = "ping")
                val pulse by t.animateFloat(0.85f, 1.25f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "pulse")
                At(150, 412) {
                    Box(
                        Modifier.size(68.dp).scale(pulse).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)).border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                    )
                }
                At(156, 418) {
                    Box(
                        Modifier.size(56.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(Color.White).clickable(onClick = primary),
                        contentAlignment = Alignment.Center,
                    ) { Ic(R.drawable.ic_chat, tint = K.BrandPrimary) }
                }
                // 지우(지도 그림 속)를 눌러도 장면으로.
                At(170, 352) { Box(Modifier.size(width = 50.dp, height = 62.dp).plainClick(onClick = primary)) }
            } else {
                At(162, 424) { CheckPing { replayToday() } }
                CenteredAt(184, 474) { GlassChip("편의점") }
            }

            // 다음 곳.
            when {
                complete -> {
                    At(164, 164) { CheckPing { toast(OUT_OF_SCOPE) } }
                    CenteredAt(186, 214) { GlassChip("다리 위") }
                }
                done -> {
                    At(164, 164) {
                        Box(
                            Modifier.size(44.dp).alpha(spread.value.coerceIn(0.3f, 1f)).shadow(6.dp, CircleShape).clip(CircleShape)
                                .background(Color.White).clickable { toast("내일 다리 위에서 만나요") },
                            contentAlignment = Alignment.Center,
                        ) { Ic(R.drawable.ic_chat, tint = K.BrandPrimary) }
                    }
                    CenteredAt(186, 214) { GlassChip("내일") }
                }
                else -> At(164, 164) {
                    Box(
                        Modifier.size(44.dp).alpha(0.85f).clip(CircleShape).background(K.GlassRegular)
                            .border(1.dp, K.GlassRegularStroke, CircleShape).clickable { toast("오늘 대화를 마치면 열려요") },
                        contentAlignment = Alignment.Center,
                    ) { Ic(R.drawable.ic_lock, tint = Color.White) }
                }
            }

            if (showBubble) {
                At(156, 316) {
                    Box(
                        Modifier.floatingCard(14.dp).background(Color.White).clickable(onClick = primary)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) { T("· · ·", Type.LabelLarge, K.BrandPrimary) }
                }
            }

            // B-11 · 지난 핑을 누르면 그때의 대화를 다시 볼 수 있다.
            openPlace?.let { place ->
                val left = (place.x + 22 - 86).coerceIn(16, 390 - 172 - 16)
                At(left, place.chipY + 34) {
                    Column(
                        Modifier.width(172.dp).floatingCard(20.dp).background(Color.White).plainClick { }.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        T(place.title, Type.LabelLarge, K.TextPrimary)
                        T(place.sub, Type.Micro, K.TextSecondary)
                        Box(
                            Modifier.height(48.dp).clip(CircleShape).background(K.BrandPrimary)
                                .clickable { openPlace = null; model.replay(place.script) }.padding(horizontal = 28.dp),
                            contentAlignment = Alignment.Center,
                        ) { T("다시 보기", Type.LabelLarge, Color.White) }
                    }
                }
            }
        }

        if (openPlace != null) Box(Modifier.fillMaxSize().plainClick { openPlace = null })

        // 왼쪽 위: 나 (누르면 프로필, 길게 누르면 목업 시나리오).
        Row(
            Modifier
                .padding(start = 24.dp, top = 50.dp)
                .floatingCard(100.dp)
                .background(Color.White)
                .combinedClickable(onClick = { toast("프로필(H)은 $OUT_OF_SCOPE · 길게 누르면 시나리오") }, onLongClick = onOpenDebug)
                .padding(start = 6.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(K.BrandSoft)) {
                Image(
                    painterResource(R.drawable.avatar), null,
                    Modifier.requiredSize(60.dp, 90.dp).offset(x = 0.dp, y = 28.dp),
                )
            }
            Column {
                T("Alex", Type.LabelLarge, K.TextPrimary)
                T("초등학생 리그", Type.Micro, K.TextSecondary)
            }
        }

        // 오른쪽 위: 재화.
        Row(
            Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatPill(R.drawable.ic_streak, model.streak)
            StatPill(R.drawable.ic_star, model.stars)
        }

        // B-12 · 동네 이름.
        AnimatedVisibility(
            model.showPlaceToast,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 130.dp),
            enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.9f),
            exit = fadeOut(tween(500)) + scaleOut(targetScale = 0.95f),
        ) {
            Column(
                Modifier.clip(RoundedCornerShape(28.dp)).background(Color(0xE6203F36)).padding(horizontal = 28.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                T("첫 번째 동네", Type.Caption, K.OnPhotoMuted)
                T("한강", Type.TitleLarge, Color.White)
            }
        }

        // 아래: 지도 조작 · 보조 행동 · 지우의 대화창.
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 116.dp + extra),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(44.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(Color.White)
                    .clickable { toast("지우가 있는 곳으로 돌아왔어요") },
                contentAlignment = Alignment.Center,
            ) { Ic(R.drawable.ic_locate) }
            if (helpers.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    helpers.forEach { h -> GlassChip(h.label, Modifier.background(Color(0x4D10281E), CircleShape), onClick = h.action) }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .floatingCard(24.dp)
                    .background(Color.White)
                    .clickable(onClick = primary)
                    .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(painterResource(R.drawable.img_jiwoo_thumb), null, Modifier.size(44.dp).clip(CircleShape))
                Column(Modifier.weight(1f)) {
                    T(label, Type.Caption, K.TextSecondary)
                    T(message, Type.LabelLarge, K.TextPrimary)
                }
                Box(Modifier.size(44.dp).clip(CircleShape).background(K.BrandPrimary), contentAlignment = Alignment.Center) {
                    Ic(R.drawable.ic_arrow_right, tint = Color.White)
                }
            }
        }

        TabBar(Modifier.align(Alignment.BottomCenter).padding(bottom = 42.dp + extra)) { toast("$it 탭은 $OUT_OF_SCOPE") }
    }
}

@Composable
private fun CheckPing(onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(K.BrandPrimary)
            .border(3.dp, Color.White, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Ic(R.drawable.ic_check, tint = Color.White) }
}

/** 회색 지도 위에, 다녀온 길 주변만 색이 있는 지도를 겹친다. */
@Composable
private fun BoxScope.MapLayers(fullColor: Boolean, spread: Float) {
    val map: Painter = painterResource(R.drawable.img_map)
    val grey = remember {
        ColorFilter.colorMatrix(
            ColorMatrix().apply { setToSaturation(0.08f) },
        )
    }
    Image(map, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, colorFilter = if (fullColor) null else grey)
    if (fullColor) return
    Box(Modifier.fillMaxSize().background(Color(0x14203F36)))

    val reveal = remember(spread) { pointsAlong(walkedPath, 38f) + pointsAlong(nextPath, 30f, spread) }
    val glow = remember(spread) { pointsAlong(walkedPath, 7f) + pointsAlong(nextPath, 7f, spread) }

    Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val k = size.width / 390f
        // 색이 보일 자리 (부드러운 원을 길 따라 이어 붙임)
        reveal.forEach { p ->
            val c = Offset(p.x * k, p.y * k)
            val r = 125f * k
            drawCircle(
                Brush.radialGradient(0f to Color.Black, 0.45f to Color.Black, 1f to Color.Transparent, center = c, radius = r),
                radius = r, center = c,
            )
        }
        // 그 자리에만 색이 있는 지도를 그린다. ContentScale.Crop과 같은 맞춤.
        val src = map.intrinsicSize
        val s = maxOf(size.width / src.width, size.height / src.height)
        val w = src.width * s
        val h = src.height * s
        val dx = (size.width - w) / 2f
        val dy = (size.height - h) / 2f
        drawContext.canvas.saveLayer(
            androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
            androidx.compose.ui.graphics.Paint().apply { blendMode = BlendMode.SrcIn },
        )
        drawContext.transform.translate(dx, dy)
        with(map) { draw(Size(w, h)) }
        drawContext.transform.translate(-dx, -dy)
        drawContext.canvas.restore()
    }

    // 지나온 길의 빛
    Canvas(Modifier.fillMaxSize()) {
        val k = size.width / 390f
        glow.forEach { p ->
            val c = Offset(p.x * k, p.y * k)
            val r = 19f * k
            drawCircle(
                Brush.radialGradient(0f to Color.White.copy(alpha = 0.3f), 1f to Color.Transparent, center = c, radius = r),
                radius = r, center = c, blendMode = BlendMode.Overlay,
            )
        }
    }
}

@Composable
private fun TabBar(modifier: Modifier = Modifier, onOther: (String) -> Unit) {
    Box(modifier.width(342.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(CircleShape)
                .background(K.GlassRegular)
                .border(1.dp, K.GlassRegularStroke, CircleShape)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tabs = listOf("홈" to R.drawable.ic_home, "코스" to R.drawable.ic_book, "노트" to R.drawable.ic_book, "프로필" to R.drawable.ic_profile)
            tabs.forEachIndexed { i, (name, icon) ->
                val selected = i == 0
                Column(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White else Color.Transparent)
                        .clickable(enabled = !selected) { onOther(name) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Ic(icon, size = 22.dp, tint = if (selected) K.BrandPrimary else Color.White.copy(alpha = 0.72f))
                    Spacer(Modifier.height(2.dp))
                    T(name, Type.Caption, if (selected) K.BrandPrimary else K.OnPhotoMuted)
                }
            }
        }
        // 새 소식 표시
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = (-24).dp, y = 10.dp).size(9.dp).clip(CircleShape)
                .background(Color.White).padding(1.5.dp).clip(CircleShape).background(K.Alert),
        )
    }
}

/** B-13 · 알림 예시: 지우의 메시지. 누르면 바로 장면으로. */
@Composable
fun NotificationMock(model: AppModel) {
    BackHandler { model.screen = com.koreana.prototype.Screen.Home }
    Box(Modifier.fillMaxSize().background(K.BrandStrong)) {
        Image(
            painterResource(R.drawable.img_map), null,
            Modifier.fillMaxSize().blur(12.dp),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.fillMaxSize().background(Color(0xB3182E25)))
        Column(
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 72.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xCC14251E))
                .border(1.dp, K.GlassRegularStroke, RoundedCornerShape(24.dp))
                .clickable { model.startToday(enterFromMap = false) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            T("KOREANA · 지금", Type.Caption, K.OnPhotoMuted)
            T("지우", Type.LabelLarge, Color.White)
            T("오늘 라면 먹으러 갈래요?", Type.Body, Color.White)
        }
        T(
            "알림을 누르면 바로 장면으로 들어가요", Type.Caption, K.OnPhotoMuted,
            Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp),
        )
    }
}
