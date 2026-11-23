package com.koreana.prototype.onboarding

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.koreana.prototype.AppModel
import com.koreana.prototype.Line
import com.koreana.prototype.LocalVoice
import com.koreana.prototype.R
import com.koreana.prototype.Screen
import com.koreana.prototype.Scripts
import com.koreana.prototype.Speaker
import com.koreana.prototype.TalkConfig
import com.koreana.prototype.TalkMode
import com.koreana.prototype.talk.JiwooCard
import com.koreana.prototype.talk.MicButton
import com.koreana.prototype.talk.SayCard
import com.koreana.prototype.talk.TalkScreen
import com.koreana.prototype.ui.GlassChip
import com.koreana.prototype.ui.GlassIconButton
import com.koreana.prototype.ui.Ic
import com.koreana.prototype.ui.K
import com.koreana.prototype.ui.LocalBottomExtra
import com.koreana.prototype.ui.PhotoTextShadow
import com.koreana.prototype.ui.PrimaryButton
import com.koreana.prototype.ui.SceneBackdrop
import com.koreana.prototype.ui.T
import com.koreana.prototype.ui.TextLink
import com.koreana.prototype.ui.Type
import com.koreana.prototype.ui.rememberToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Step { Welcome, Interest, ToneIntro, ToneRepeat, ToneDone, FirstTalk, Done }

/** [col], [row]는 관심사 사진 한 장(3 x 2 묶음) 안에서의 자리. */
private class Interest(val col: Int, val row: Int, val title: String, val sub: String)

private val interests = listOf(
    Interest(1, 0, "친구와\n대화하고 싶어요", "첫 인사부터 취향 이야기까지"),
    Interest(0, 0, "한국 음악을\n더 즐기고 싶어요", "좋아하는 가수와 노래 이야기"),
    Interest(2, 0, "한국에서\n공부하고 싶어요", "강의실에서, 새로운 친구와"),
    Interest(0, 1, "한국어로\n일하고 싶어요", "동료와 나누는 일상과 업무"),
    Interest(1, 1, "한국을\n여행하고 싶어요", "길을 묻고, 나만의 여행을"),
    Interest(2, 1, "한국 음식을\n즐기고 싶어요", "메뉴를 고르고 맛을 나누는 말"),
)

/** A · 온보딩: 환영 → 관심사 선택 → 톤 맞추기 → 첫 대화 → 첫 대화 완료 → 홈. */
@Composable
fun OnboardingFlow(model: AppModel, onOpenDebug: () -> Unit) {
    var step by remember { mutableStateOf(Step.Welcome) }
    var talkNonce by remember { mutableIntStateOf(0) }

    BackHandler(enabled = step != Step.Welcome && step != Step.FirstTalk) {
        step = when (step) {
            Step.Interest -> Step.Welcome
            Step.ToneIntro -> Step.Interest
            Step.ToneRepeat, Step.ToneDone -> Step.ToneIntro
            else -> Step.ToneIntro
        }
    }

    Crossfade(targetState = step, animationSpec = tween(300), label = "onboarding") { s ->
        when (s) {
            Step.Welcome -> Welcome(
                onStart = { step = Step.Interest },
                onLogin = {
                    // 가입과 로그인(J)은 이번 범위 밖이라 기존 사용자로 보고 홈으로 간다.
                    model.signedUp = true
                    model.screen = Screen.Home
                },
                onOpenDebug = onOpenDebug,
            )
            Step.Interest -> InterestSelect(onBack = { step = Step.Welcome }, onPick = { step = Step.ToneIntro })
            Step.ToneIntro -> ToneIntro(
                onBack = { step = Step.Interest },
                onOk = {
                    // 버튼을 누르면 마이크 권한 요청. 거절해도 실패 없이 보기 전용으로 이어간다.
                    model.requestMic { granted ->
                        if (granted) step = Step.ToneRepeat else { talkNonce++; step = Step.FirstTalk }
                    }
                },
            )
            Step.ToneRepeat -> ToneRepeat(
                model = model,
                onBack = { step = Step.ToneIntro },
                onSkip = { talkNonce++; step = Step.FirstTalk },
                onDone = { step = Step.ToneDone },
            )
            Step.ToneDone -> ToneDone(onBack = { step = Step.ToneIntro }, onNext = { talkNonce++; step = Step.FirstTalk })
            Step.FirstTalk -> TalkScreen(
                model = model,
                config = remember(talkNonce) { TalkConfig(Scripts.firstTalk, TalkMode.Onboarding, nonce = talkNonce) },
                onClose = { step = Step.ToneIntro },
                onFinished = { step = Step.Done },
            )
            Step.Done -> FirstTalkDone(onBack = { talkNonce++; step = Step.FirstTalk }, onGo = { model.finishOnboarding() })
        }
    }
}

/** A-1 · 환영. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Welcome(onStart: () -> Unit, onLogin: () -> Unit, onOpenDebug: () -> Unit) {
    val extra = LocalBottomExtra.current
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.img_welcome), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(K.ScrimBottom))
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp + extra),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                // 길게 누르면 목업 시나리오 패널이 열린다.
                Modifier.fillMaxWidth().combinedClickable(
                    interactionSource = null, indication = null, onClick = {}, onLongClick = onOpenDebug,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                T("KOREANA에서는", Type.TitleMedium, Color.White)
                T("그냥\n따라오세요!", Type.Display, Color.White)
            }
            Box(Modifier.height(24.dp))
            PrimaryButton("네, 좋아요", Modifier.fillMaxWidth(), onClick = onStart)
            Box(Modifier.height(4.dp))
            TextLink("이미 계정이 있어요", Type.LabelLarge, vertical = 10.dp, onClick = onLogin)
        }
    }
}

@Composable
private fun BlurredBackdrop(image: Int) {
    Box(Modifier.fillMaxSize().background(K.BrandStrong)) {
        Image(
            painterResource(image), null,
            Modifier.fillMaxSize().scale(1.2f).blur(10.dp),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.fillMaxSize().background(K.ScrimMedium))
    }
}

/** A-2 · 관심사 선택: 카드를 누르면 대화가 시작된다. */
@Composable
private fun InterestSelect(onBack: () -> Unit, onPick: (Int) -> Unit) {
    val extra = LocalBottomExtra.current
    val listState = rememberLazyListState()
    val sheet = ImageBitmap.imageResource(R.drawable.img_interests)
    Box(Modifier.fillMaxSize()) {
        BlurredBackdrop(R.drawable.img_welcome)
        GlassIconButton(R.drawable.ic_chevron_left, Modifier.padding(start = 24.dp, top = 52.dp), contentDescription = "이전으로", onClick = onBack)
        Column(Modifier.padding(start = 80.dp, top = 53.dp).width(286.dp)) {
            Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                T("관심사 선택", Type.LabelMedium, Color.White)
                T("1 / 3", Type.Caption, K.OnPhotoMuted)
            }
            Box(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i ->
                    Box(
                        Modifier.size(width = 67.dp, height = 4.dp).clip(RoundedCornerShape(2.dp))
                            .background(if (i == 0) Color.White else Color.White.copy(alpha = 0.35f)),
                    )
                }
            }
        }
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 120.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            T("한국어를 잘하고 싶은\n순간을 고르세요!", Type.TitleLarge, Color.White)
            T("옆으로 넘겨, 내 이야기를 찾아보세요.", Type.Body, K.OnPhotoMuted)
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(top = 262.dp),
            state = listState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            flingBehavior = rememberSnapFlingBehavior(listState),
        ) {
            items(interests) { item ->
                Box(
                    Modifier
                        .size(width = 306.dp, height = 464.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onPick(interests.indexOf(item)) },
                ) {
                    Image(
                        remember(sheet, item) {
                            // 512px 칸에서 카드에 보이는 창(331 x 502)만 잘라 쓴다.
                            BitmapPainter(sheet, IntOffset(item.col * 512 + 90, item.row * 512 + 5), IntSize(331, 502))
                        },
                        null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                0f to Color(0x00060F0A), 0.45f to Color(0x0F060F0A), 1f to Color(0xEB060F0A),
                            ),
                        ),
                    )
                    Column(
                        Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = 35.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        T(item.title, Type.TitleMedium, Color.White)
                        T(item.sub, Type.Caption, K.OnPhotoMuted)
                    }
                }
            }
        }
        T(
            "카드를 누르면 대화가 시작돼요.", Type.Caption, K.OnPhotoMuted,
            Modifier.align(Alignment.BottomCenter).padding(bottom = 59.dp + extra),
            align = TextAlign.Center,
        )
    }
}

@Composable
private fun SceneTitle(text: String) {
    T(
        text, Type.TitleLarge.copy(shadow = PhotoTextShadow), Color.White,
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 132.dp),
        align = TextAlign.Center,
    )
}

/** A-3.1 · 톤 맞추기 안내. */
@Composable
private fun ToneIntro(onBack: () -> Unit, onOk: () -> Unit) {
    val extra = LocalBottomExtra.current
    Box(Modifier.fillMaxSize()) {
        SceneBackdrop(topAlpha = 1f)
        GlassIconButton(R.drawable.ic_chevron_left, Modifier.padding(start = 18.dp, top = 52.dp), contentDescription = "이전으로", onClick = onBack)
        SceneTitle("이야기하기 전에,\n지우와 톤을 맞춰봐요")
        PrimaryButton(
            "좋아요",
            Modifier.align(Alignment.BottomCenter).padding(start = 24.dp, end = 24.dp, bottom = 72.dp + extra).fillMaxWidth(),
            onClick = onOk,
        )
    }
}

/** A-3.2 / A-3.2b · 따라 말하기. 소리가 작으면 한 번 더 청하고, 두 번 시도하면 그냥 진행한다. */
@Composable
private fun ToneRepeat(model: AppModel, onBack: () -> Unit, onSkip: () -> Unit, onDone: () -> Unit) {
    val extra = LocalBottomExtra.current
    val voice = LocalVoice.current
    val scope = rememberCoroutineScope()
    val toast = rememberToast()
    val line = remember { Line("안녕하세요!", "Hello!", "annyeonghaseyo!") }
    var recording by remember { mutableStateOf(false) }
    var recStart by remember { mutableLongStateOf(0L) }
    var tries by remember { mutableIntStateOf(0) }
    var askLouder by remember { mutableStateOf(false) }
    var hideText by remember { mutableStateOf(false) }
    var showTranslation by remember { mutableStateOf(true) }
    var showRoman by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    fun finish(byTimer: Boolean) {
        if (!recording) return
        recording = false
        val quiet = model.nextQuiet || (!byTimer && SystemClock.uptimeMillis() - recStart < 700)
        model.nextQuiet = false
        tries += 1
        if (quiet && tries < 2) askLouder = true else onDone()
    }

    LaunchedEffect(Unit) {
        delay(400)
        voice.speak(line.ko, Speaker.Jiwoo)
    }
    LaunchedEffect(recording) {
        if (recording) {
            delay(2400)
            finish(byTimer = true)
        }
    }

    Box(Modifier.fillMaxSize()) {
        SceneBackdrop()
        GlassIconButton(R.drawable.ic_chevron_left, Modifier.padding(start = 18.dp, top = 52.dp), contentDescription = "이전으로", onClick = onBack)
        Box(Modifier.align(Alignment.TopEnd).padding(top = 58.dp, end = 18.dp)) {
            TextLink("건너뛰기", Type.LabelMedium, vertical = 8.dp) { voice.stop(); onSkip() }
        }
        JiwooCard(
            line = line,
            modifier = Modifier.align(Alignment.TopCenter).padding(start = 18.dp, end = 18.dp, top = 108.dp),
            onListen = { scope.launch { voice.speak(line.ko, Speaker.Jiwoo) } },
            hideText = hideText,
            onToggleHide = { hideText = !hideText },
            showTranslation = showTranslation,
            onToggleTranslation = { showTranslation = !showTranslation },
            saved = saved,
            onToggleSave = { saved = !saved },
        )
        SayCard(
            line = line,
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 18.dp, end = 18.dp, bottom = 171.dp + extra),
            showRoman = showRoman,
            onToggleRoman = { showRoman = !showRoman },
            onListen = { scope.launch { voice.speak(line.ko, Speaker.Model) } },
        )
        T(
            when {
                recording -> "듣고 있어요"
                askLouder -> "조금 더 크게 말해볼까요?"
                else -> "눌러서 따라 말하기"
            },
            Type.LabelMedium, Color.White,
            Modifier.align(Alignment.BottomCenter).padding(bottom = 138.dp + extra),
            align = TextAlign.Center,
        )
        MicButton(
            recording = recording,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp + extra),
            onClick = {
                if (recording) {
                    finish(byTimer = false)
                } else if (model.micOn) {
                    voice.stop(); recStart = SystemClock.uptimeMillis(); recording = true
                } else {
                    model.requestMic { granted -> if (!granted) toast("마이크를 켜면 따라 말할 수 있어요") }
                }
            },
        )
    }
}

/** A-3.3 · 톤 맞추기 완료: 1~2초 뒤 첫 대화로 저절로 넘어간다. */
@Composable
private fun ToneDone(onBack: () -> Unit, onNext: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1600)
        onNext()
    }
    Box(Modifier.fillMaxSize()) {
        SceneBackdrop(topAlpha = 1f)
        GlassIconButton(R.drawable.ic_chevron_left, Modifier.padding(start = 18.dp, top = 52.dp), contentDescription = "이전으로", onClick = onBack)
        SceneTitle("좋아요,\n톤이 맞았어요")
    }
}

/** A-5 · 첫 대화 완료: 레벨·점수 없이, 시작할 곳을 장소로 보여준다. */
@Composable
private fun FirstTalkDone(onBack: () -> Unit, onGo: () -> Unit) {
    val extra = LocalBottomExtra.current
    val voice = LocalVoice.current
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
        BlurredBackdrop(R.drawable.img_scene)
        GlassIconButton(R.drawable.ic_chevron_left, Modifier.padding(start = 24.dp, top = 52.dp), contentDescription = "이전으로", onClick = onBack)
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 120.dp)) {
            T("벌써 세 마디나\n나눴어요!", Type.TitleLarge, Color.White)
            Box(Modifier.height(8.dp))
            T("지우와 첫 이야기를 나눴어요.", Type.Body, K.OnPhotoMuted)
            Box(Modifier.height(41.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(K.GlassRegular)
                    .border(1.dp, K.GlassRegularStroke, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                T("내 목소리 다시 듣기", Type.LabelMedium, K.OnPhotoMuted)
                Scripts.firstTalk.turns.forEach { turn ->
                    Row(
                        Modifier.fillMaxWidth().clip(CircleShape).clickable { scope.launch { voice.speak(turn.me.ko, Speaker.Me) } },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Ic(R.drawable.ic_volume, tint = K.TextTertiary) }
                        T(turn.me.ko, Type.BodyLarge, Color.White)
                    }
                }
            }
            Box(Modifier.height(28.dp))
            T("여기서부터 시작해요", Type.LabelMedium, K.OnPhotoMuted)
            Box(Modifier.height(10.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(K.GlassRegular)
                    .border(1.dp, K.GlassRegularStroke, RoundedCornerShape(28.dp))
                    .clickable(onClick = onGo)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassChip("시작할 곳")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    T("한강에서 첫 인사", Type.TitleMedium, Color.White, Modifier.weight(1f), maxLines = 1)
                    Ic(R.drawable.ic_chevron_right, size = 20.dp, tint = Color.White)
                }
            }
        }
        PrimaryButton(
            "지우 만나러 가기",
            Modifier.align(Alignment.BottomCenter).padding(start = 24.dp, end = 24.dp, bottom = 44.dp + extra).fillMaxWidth(),
        ) { voice.stop(); onGo() }
    }
}
