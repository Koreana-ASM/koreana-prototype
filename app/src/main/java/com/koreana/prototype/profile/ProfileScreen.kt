package com.koreana.prototype.profile

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.koreana.prototype.Album
import com.koreana.prototype.AppModel
import com.koreana.prototype.LocalVoice
import com.koreana.prototype.Polaroid
import com.koreana.prototype.ProfileStage
import com.koreana.prototype.R
import com.koreana.prototype.Screen
import com.koreana.prototype.Speaker
import com.koreana.prototype.Trophy
import com.koreana.prototype.TrophyKind
import com.koreana.prototype.talk.MintButton
import com.koreana.prototype.ui.Ic
import com.koreana.prototype.ui.K
import com.koreana.prototype.ui.LocalBottomExtra
import com.koreana.prototype.ui.PK
import com.koreana.prototype.ui.T
import com.koreana.prototype.ui.TK
import com.koreana.prototype.ui.Tab
import com.koreana.prototype.ui.TabBar
import com.koreana.prototype.ui.plainClick
import com.koreana.prototype.ui.rememberToast
import kotlinx.coroutines.launch
import kotlin.random.Random

/** 프로필에 쌓인 기록 (목업). */
private class Record(
    val sub: String,
    val days: String,
    val sentences: String,
    val third: Pair<String, String>,
    val alleys: String,
    val polaroids: List<Polaroid>,
    val trophies: List<Trophy>,
)

private fun record(stage: ProfileStage, signedUp: Boolean) = when (stage) {
    ProfileStage.Full -> Record(
        "2026년 10월 가입 · 미국", "30일", "214개", "초등학생" to "이번 주 리그", "4곳",
        Album.all, listOf(Trophy.Cal, Trophy.Stick, Trophy.Ttok, Trophy.Star, Trophy.Ramen, Trophy.Heart),
    )
    ProfileStage.FirstDay -> Record(
        if (signedUp) "2026년 11월 가입 · 미국" else "1일째 · 가입 전 · 미국", "1일", "3개", "1개" to "받은 트로피", "1곳",
        listOf(Album.first), listOf(Trophy.Heart),
    )
    ProfileStage.Early -> Record(
        if (signedUp) "2026년 10월 가입 · 미국" else "4일째 · 가입 전 · 미국", "4일", "26개", "2개" to "받은 트로피", "2곳",
        listOf(Album.rice, Album.first), listOf(Trophy.Star, Trophy.Heart),
    )
}

private fun Modifier.paperCard(radius: Int = 14) =
    clip(RoundedCornerShape(radius.dp)).background(Color.White).border(1.5.dp, PK.Line, RoundedCornerShape(radius.dp))

/**
 * 프로필. 머리(이니셜과 이름), 개요 4칸(연속 일수 · 말해 본 문장 · 리그 · 다녀온 골목),
 * 이야기 속 장면을 한 장씩 남기는 폴라로이드, 특별한 순간에만 받는 트로피.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(model: AppModel, onOpenDebug: () -> Unit) {
    val voice = LocalVoice.current
    val scope = rememberCoroutineScope()
    val toast = rememberToast()
    val extra = LocalBottomExtra.current
    val rec = record(model.profileStage, model.signedUp)
    val mic = model.micOn

    var board by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<Int?>(null) }
    var case by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Trophy?>(null) }

    fun play(p: Polaroid) {
        voice.stop()
        scope.launch { voice.speak(p.said, Speaker.Me) }
    }

    fun allowMic() {
        model.forceMicOff = false
        // 이미 거절했으면 설정 앱으로.
        model.requestMic { granted -> if (!granted) { toast("설정에서 마이크를 허용해 주세요"); model.openAppSettings() } }
    }

    BackHandler {
        voice.stop()
        when {
            detail != null -> detail = null
            viewer != null -> viewer = null
            case -> case = false
            board -> board = false
            else -> model.screen = Screen.Home
        }
    }

    Box(Modifier.fillMaxSize().background(PK.Paper)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 100.dp, bottom = 150.dp + extra),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 머리: 이니셜 동그라미와 이름. 길게 누르면 목업 시나리오 패널이 열린다.
            Row(
                Modifier.combinedClickable(interactionSource = null, indication = null, onClick = {}, onLongClick = onOpenDebug),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(TK.MintStrong), contentAlignment = Alignment.Center) {
                    T("A", TK.text(30, FontWeight.Bold, 36), PK.Ink)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    T("Alex", TK.text(26, FontWeight.Bold, 32), PK.Ink)
                    T(rec.sub, TK.text(13, FontWeight.Medium, 16), PK.Muted)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("개요")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stat(R.drawable.ic_flame, rec.days, "연속 일수", Modifier.weight(1f))
                    Stat(R.drawable.ic_bubble, rec.sentences, "말해 본 문장", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stat(R.drawable.ic_trophy, rec.third.first, rec.third.second, Modifier.weight(1f))
                    Stat(R.drawable.ic_pin, rec.alleys, "다녀온 골목", Modifier.weight(1f))
                }
            }

            if (!model.signedUp) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF4DA))
                        .border(1.5.dp, Color(0xFFF2CF87), RoundedCornerShape(14.dp)).padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    T("가입하면 기록이 사라지지 않아요.", TK.text(14, FontWeight.Bold, 18), PK.Ink, Modifier.weight(1f))
                    DarkChip("가입하기") { model.signedUp = true; toast("가입했어요 (목업)") }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("이야기", onAll = if (rec.polaroids.size > 1) ({ board = true }) else null)
                StoryPreview(rec.polaroids, mic, onOpen = { viewer = it }, onPlay = ::play)
                if (!mic) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PK.Chip).padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Ic(R.drawable.ic_mic, size = 20.dp, tint = K.BrandPrimary)
                        T("마이크를 허용하면 사진마다 그때 내 목소리가 남아요.", TK.text(13, FontWeight.Medium, 17), PK.Ink, Modifier.weight(1f))
                        DarkChip("허용") { allowMic() }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("트로피", onAll = { case = true })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rec.trophies.take(4).forEach { t ->
                        Column(
                            Modifier.width(80.dp).plainClick { case = true; detail = t },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            TrophyDisc(t, 78.dp)
                            T(t.title, TK.text(11, FontWeight.Medium, 14), PK.Ink, align = TextAlign.Center, maxLines = 1)
                        }
                    }
                }
            }
        }

        Box(Modifier.align(Alignment.TopEnd).padding(top = 52.dp, end = 16.dp).size(40.dp).clip(CircleShape).clickable { toast("설정은 이번 목업 범위 밖이에요") }, contentAlignment = Alignment.Center) {
            Ic(R.drawable.ic_settings, size = 22.dp, tint = PK.Ink)
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(100.dp + extra)
                .background(Brush.verticalGradient(listOf(PK.Paper.copy(alpha = 0f), PK.Paper))),
        )
        TabBar(model, Tab.Profile)

        if (board) {
            StoryBoard(rec.polaroids, mic, onBack = { board = false }, onOpen = { viewer = it }, onPlay = ::play)
        }
        if (case) {
            TrophyCase(
                earned = rec.trophies,
                detail = detail,
                polaroids = rec.polaroids,
                onBack = { case = false; detail = null },
                onPick = { detail = it },
                onCloseDetail = { detail = null },
                onPolaroid = { i -> detail = null; viewer = i },
            )
        }
        viewer?.let { start ->
            PolaroidViewer(
                rec.polaroids, start, mic,
                onClose = { voice.stop(); viewer = null },
                onPlay = ::play,
                onTrophy = { t -> viewer = null; case = true; detail = t },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, onAll: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        T(text, TK.text(19, FontWeight.Bold, 23), PK.Ink, Modifier.weight(1f))
        if (onAll != null) {
            Row(Modifier.clip(CircleShape).clickable(onClick = onAll), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                T("모두 보기", TK.text(13, FontWeight.Bold, 16), K.BrandPrimary)
                Ic(R.drawable.ic_more, size = 14.dp, tint = K.BrandPrimary)
            }
        }
    }
}

@Composable
private fun Stat(@DrawableRes icon: Int, value: String, label: String, modifier: Modifier) {
    Row(
        modifier.paperCard().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Ic(icon, size = 24.dp)
        Column {
            T(value, TK.text(18, FontWeight.Bold, 22), PK.Ink, maxLines = 1)
            T(label, TK.text(12, FontWeight.Medium, 15), PK.Muted, maxLines = 1)
        }
    }
}

@Composable
private fun DarkChip(text: String, onClick: () -> Unit) {
    Box(Modifier.clip(CircleShape).background(PK.Ink).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)) {
        T(text, TK.text(13, FontWeight.Bold, 16), Color.White, maxLines = 1)
    }
}

/** 최신 폴라로이드 세 장을 살짝 기울여 겹쳐 놓는다. 한 장뿐이면 기울이지 않는다. 누르면 크게 보기. */
@Composable
private fun StoryPreview(list: List<Polaroid>, mic: Boolean, onOpen: (Int) -> Unit, onPlay: (Polaroid) -> Unit) {
    Box(Modifier.fillMaxWidth().height(236.dp)) {
        // 뒤에서부터: (목록의 자리, 가운데 x, 가운데 y, 기울기)
        val spots = when (list.size) {
            1 -> listOf(Slot(0, 175f, 118f, 0f))
            2 -> listOf(Slot(1, 232f, 116f, 5f), Slot(0, 112f, 124f, -3f))
            else -> listOf(Slot(3.coerceAtMost(list.lastIndex), 274.5f, 123.5f, -7f), Slot(2, 166.3f, 128.6f, 5f), Slot(0, 90.2f, 132.7f, 2f))
        }
        spots.forEach { s ->
            val p = list[s.index]
            PolaroidCard(
                p, 160.dp,
                Modifier.offset((s.cx - 80f).dp, (s.cy - 108f).dp).rotate(s.tilt).plainClick { onOpen(s.index) },
                onPlay = if (mic) ({ onPlay(p) }) else null,
            )
        }
    }
}

private class Slot(val index: Int, val cx: Float, val cy: Float, val tilt: Float)

/** 이야기 모아 보기: 코르크판에 폴라로이드가 2열로, 압정으로 꽂혀 있다. 최신이 위, 달 라벨만 있다. */
@Composable
private fun StoryBoard(list: List<Polaroid>, mic: Boolean, onBack: () -> Unit, onOpen: (Int) -> Unit, onPlay: (Polaroid) -> Unit) {
    val extra = LocalBottomExtra.current
    val specks = remember {
        val r = Random(7)
        List(150) { Triple(r.nextFloat(), r.nextFloat(), r.nextInt(4)) }
    }
    val tilts = listOf(-3f, 4f, 2f, -2.5f, 1.5f, -4f)
    Box(Modifier.fillMaxSize().background(PK.Cork).plainClick { }) {
        Canvas(Modifier.fillMaxSize()) {
            specks.forEach { (x, y, kind) ->
                drawCircle(
                    if (kind % 2 == 0) Color(0xFFDDBE96).copy(alpha = 0.5f) else Color(0xFFA9835A).copy(alpha = 0.5f),
                    radius = (1.2f + kind * 0.5f).dp.toPx(), center = Offset(x * size.width, y * size.height),
                )
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 100.dp, bottom = 40.dp + extra),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            var n = 0
            list.map { it.month }.distinct().forEach { month ->
                T(month, PK.hand(24f, FontWeight.Normal), PK.PolaroidInk, Modifier.padding(start = 4.dp, top = 6.dp))
                list.withIndex().filter { it.value.month == month }.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        pair.forEach { (i, p) ->
                            val tilt = tilts[n++ % tilts.size]
                            Box(Modifier.padding(top = 8.dp).rotate(tilt).plainClick { onOpen(i) }) {
                                PolaroidCard(p, 160.dp, onPlay = if (mic) ({ onPlay(p) }) else null)
                                // 압정.
                                Box(Modifier.align(Alignment.TopCenter).offset(y = (-6).dp).size(13.dp).clip(CircleShape).background(Color(0xFFD4564A)))
                            }
                        }
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(96.dp).background(Brush.verticalGradient(listOf(PK.Cork, PK.Cork, PK.Cork.copy(alpha = 0f)))))
        Box(Modifier.padding(start = 14.dp, top = 50.dp).size(40.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Ic(R.drawable.ic_chevron_left, size = 22.dp, tint = PK.PolaroidInk)
        }
        T("이야기", TK.text(17, FontWeight.Bold, 22), PK.PolaroidInk, Modifier.align(Alignment.TopCenter).padding(top = 58.dp))
    }
}

/**
 * 폴라로이드 크게 보기. 손글씨 한 줄 아래에 번역, ▶는 그때 내 목소리, 좌우로 넘긴다.
 * 트로피와 함께 생긴 장에는 아래에 "이 순간의 트로피"가 붙는다.
 */
@Composable
private fun PolaroidViewer(
    list: List<Polaroid>,
    start: Int,
    mic: Boolean,
    onClose: () -> Unit,
    onPlay: (Polaroid) -> Unit,
    onTrophy: (Trophy) -> Unit,
) {
    val extra = LocalBottomExtra.current
    val pager = rememberPagerState(initialPage = start.coerceIn(0, list.lastIndex)) { list.size }
    Box(Modifier.fillMaxSize().background(PK.Dark).plainClick { }) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            val p = list[page]
            Column(Modifier.fillMaxSize().padding(top = 126.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PolaroidCard(p, 320.dp, Modifier.rotate(if (page % 2 == 0) 2.5f else -2f), onPlay = if (mic) ({ onPlay(p) }) else null)
                Spacer(Modifier.height(26.dp))
                T(p.en, TK.text(14, FontWeight.Normal, 17), Color(0xFFD8D4CC), Modifier.width(320.dp), align = TextAlign.Center)
                p.trophy?.let { t ->
                    Spacer(Modifier.height(24.dp))
                    Row(
                        Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable { onTrophy(t) }
                            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TrophyDisc(t, 44.dp)
                        T("이 순간의 트로피 · ${t.title}", TK.text(14, FontWeight.Bold, 17), Color.White, maxLines = 1)
                        Ic(R.drawable.ic_more, size = 16.dp, tint = Color.White)
                    }
                }
            }
        }
        CloseMark(Modifier.align(Alignment.TopEnd).padding(top = 47.dp, end = 15.dp), onClose)
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 77.dp + extra),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(list.size) { i ->
                val on = i == pager.currentPage
                Box(Modifier.size(if (on) 18.dp else 7.dp, 7.dp).clip(CircleShape).background(Color(0xFFF1EADB).copy(alpha = if (on) 1f else 0.45f)))
            }
        }
    }
}

/**
 * 트로피 진열장. 선반 위 입체 트로피: 받은 것은 조명을 받고, 아직은 "?" 받침이다.
 * 트로피를 누르면 진열장이 흐려지고 트로피와 글이 테두리 없이 가운데에 뜬다.
 */
@Composable
private fun TrophyCase(
    earned: List<Trophy>,
    detail: Trophy?,
    polaroids: List<Polaroid>,
    onBack: () -> Unit,
    onPick: (Trophy) -> Unit,
    onCloseDetail: () -> Unit,
    onPolaroid: (Int) -> Unit,
) {
    var kind by remember { mutableStateOf<TrophyKind?>(null) }
    val shown = earned.filter { kind == null || it.kind == kind }
    val bigY = listOf(122.5f, 295f)
    val smallY = listOf(519.4f, 700.8f)
    val bigX = listOf(30f, 137f, 244f)
    val smallX = listOf(62f, 169f, 276f)

    Box(Modifier.fillMaxSize().background(Color(0xFF1B0F07)).plainClick { }) {
        Frame390(if (detail != null) Modifier.blur(9.dp) else Modifier) {
            Image(
                painterResource(R.drawable.img_shelf), null,
                Modifier.offset(y = 38.dp).requiredSize(573.44.dp, 860.16.dp), contentScale = ContentScale.FillBounds,
            )
            // 위 두 칸은 큰 자리, 아래 두 칸은 작은 자리. 받은 것부터 놓고 남은 자리는 "아직".
            repeat(12) { slot ->
                val row = slot / 3
                val col = slot % 3
                val t = shown.getOrNull(slot)
                val cx = if (row < 2) bigX[col] + 58f else smallX[col] + 26f
                val cy = if (row < 2) bigY[row] + 58f else smallY[row - 2] + 26f
                if (t != null) {
                    Box(Modifier.offset((cx - 58f).dp, (cy - 58f).dp).size(116.dp).spotGlow().plainClick { onPick(t) }, contentAlignment = Alignment.Center) {
                        TrophyDisc(t, 86.dp)
                    }
                    Plate(t.title, cx, cy + 56f)
                } else {
                    Box(
                        Modifier.offset((cx - 26f).dp, (cy - 26f + if (row < 2) 22f else 0f).dp).size(52.dp).clip(CircleShape)
                            .background(Color(0x4D2A1608)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawCircle(
                                Color(0xFFD9B36A).copy(alpha = 0.75f), radius = size.minDimension / 2f - 1.dp.toPx(),
                                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                            )
                        }
                        T("?", TK.text(20, FontWeight.Bold, 26), Color(0xFFD9B36A))
                    }
                    Plate("아직", cx, cy + if (row < 2) 56f else 40f)
                }
            }
            Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.verticalGradient(listOf(Color(0xE61B0F07), Color(0x001B0F07)))))
            Box(Modifier.offset(14.dp, 50.dp).size(40.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Ic(R.drawable.ic_chevron_left, size = 22.dp, tint = TK.Text)
            }
            T("트로피", TK.text(17, FontWeight.Bold, 22), TK.Text, Modifier.align(Alignment.TopCenter).offset(y = 58.dp))
            Row(Modifier.align(Alignment.TopCenter).offset(y = 100.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf<TrophyKind?>(null) + TrophyKind.entries).forEach { k ->
                    val on = k == kind
                    Box(
                        Modifier.height(26.dp).clip(CircleShape).background(if (on) Color(0xFFFFF1C9) else Color(0xB32A1608))
                            .clickable { kind = k }.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { T(k?.label ?: "전체", TK.text(12, FontWeight.Bold, 14), if (on) Color(0xFF3A2410) else Color(0xFFF3E6C8), maxLines = 1) }
                }
            }
        }

        detail?.let { t ->
            val at = polaroids.indexOfFirst { it.trophy == t }
            Box(Modifier.fillMaxSize().background(Color(0xB8140B05)).plainClick(onClick = onCloseDetail)) {
                Column(
                    Modifier.align(Alignment.Center).width(330.dp).plainClick { },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TrophyHero(t, 210.dp, Modifier.spotGlow(alpha = 0.32f, spread = 1.15f))
                    Spacer(Modifier.height(8.dp))
                    T("${t.title} 트로피", TK.text(22, FontWeight.Bold, 26), TK.Text)
                    Spacer(Modifier.height(10.dp))
                    T(t.about, TK.text(14, FontWeight.Normal, 17), Color(0xFFE0D5BF), Modifier.width(200.dp), align = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Column(Modifier.width(290.dp)) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.16f)))
                        Column(Modifier.padding(horizontal = 4.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Info("받는 방법", t.how)
                            Info("받은 날", t.date)
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.16f)))
                        if (at >= 0) {
                            Spacer(Modifier.height(10.dp))
                            MintButton("이 순간의 폴라로이드 보기", height = 49.dp) { onPolaroid(at) }
                        }
                    }
                }
                CloseMark(Modifier.align(Alignment.TopEnd).padding(top = 46.dp, end = 12.dp), onCloseDetail)
            }
        }
    }
}

@Composable
private fun Info(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        T(label, TK.text(13, FontWeight.Normal, 16), Color(0xFFD5C9B3), Modifier.weight(1f))
        T(value, TK.text(13, FontWeight.Bold, 16), TK.Text)
    }
}

/** 선반의 이름표. [cx]를 가운데로 놓는다. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.Plate(text: String, cx: Float, y: Float) {
    Box(Modifier.offset((cx - 60f).dp, y.dp).width(120.dp), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.height(16.dp).clip(CircleShape).background(Color(0xCC2A1608)).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            T(text, TK.text(10, FontWeight.Medium, 12), Color(0xFFF7E9C8), maxLines = 1)
        }
    }
}
