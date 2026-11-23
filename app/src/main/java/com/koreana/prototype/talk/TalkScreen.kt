package com.koreana.prototype.talk

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.koreana.prototype.AppModel
import com.koreana.prototype.Line
import com.koreana.prototype.LocalVoice
import com.koreana.prototype.R
import com.koreana.prototype.Scripts
import com.koreana.prototype.Speaker
import com.koreana.prototype.TalkConfig
import com.koreana.prototype.TalkMode
import com.koreana.prototype.ui.GlassChip
import com.koreana.prototype.ui.GlassIconButton
import com.koreana.prototype.ui.Ic
import com.koreana.prototype.ui.K
import com.koreana.prototype.ui.LocalBottomExtra
import com.koreana.prototype.ui.MockSwitch
import com.koreana.prototype.ui.PrimaryButton
import com.koreana.prototype.ui.ProgressDots
import com.koreana.prototype.ui.SceneBackdrop
import com.koreana.prototype.ui.StatPill
import com.koreana.prototype.ui.T
import com.koreana.prototype.ui.TextLink
import com.koreana.prototype.ui.Type
import com.koreana.prototype.ui.plainClick
import com.koreana.prototype.ui.rememberToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class Phase { Enter, Jiwoo, MyTurn, Recording, After, Closing, Replay }

private enum class Group { Enter, Video, Turn, After, Closing }

/** 내 차례에 들어올 때 저절로 나오는 소리. */
private enum class Auto { None, Model, QuietPrompt }

private class Spoken(val isMe: Boolean, val line: Line)

private val quietLine = Line("한 번만 더 들려줄래요?", "Could you say that once more?")
private val speeds = listOf(1f, 0.8f, 1.2f)

/**
 * C · 대화. 영상이 흐르다가 내 차례에서 멈추고, 내가 말하면 이어진다.
 * 온보딩 첫 대화(A-4)와 지난 대화 다시 보기(C-10)도 같은 화면을 쓴다.
 */
@Composable
fun TalkScreen(
    model: AppModel,
    config: TalkConfig,
    onClose: (turn: Int) -> Unit,
    onFinished: (more: Boolean) -> Unit,
) {
    val voice = LocalVoice.current
    val scope = rememberCoroutineScope()
    val toast = rememberToast()
    val density = LocalDensity.current
    val bottomExtra = LocalBottomExtra.current
    val script = config.script
    val turns = script.turns
    val isMain = config.mode == TalkMode.Main
    val isOnboarding = config.mode == TalkMode.Onboarding

    fun firstPhase(turn: Int) = if (turn < turns.size && turns[turn].jiwoo == null) Phase.MyTurn else Phase.Jiwoo

    var turn by remember { mutableIntStateOf(config.startTurn.coerceIn(0, turns.size - 1)) }
    var phase by remember {
        mutableStateOf(
            when {
                config.mode == TalkMode.ReplayOnly -> Phase.Replay
                config.enterFromMap -> Phase.Enter
                else -> firstPhase(config.startTurn)
            },
        )
    }
    var auto by remember { mutableStateOf(Auto.Model) }
    var attempts by remember { mutableIntStateOf(0) }
    var quietTries by remember { mutableIntStateOf(0) }
    var quietPrompt by remember { mutableStateOf(false) }
    var offlineHold by remember { mutableStateOf(false) }
    var badge by remember { mutableStateOf(false) }
    var recStart by remember { mutableLongStateOf(0L) }

    var manualViewOnly by remember { mutableStateOf(false) }
    val viewOnly = !model.micOn || manualViewOnly

    var hideText by remember { mutableStateOf(false) }
    var showTranslation by remember { mutableStateOf(true) }
    var showRoman by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(setOf<String>()) }
    var subtitleOn by remember { mutableStateOf(true) }
    var speedIndex by remember { mutableIntStateOf(0) }
    val speed = speeds[speedIndex]
    var paused by remember { mutableStateOf(false) }
    var playToken by remember { mutableIntStateOf(0) }
    var highlight by remember { mutableIntStateOf(-1) }
    var lineElapsed by remember { mutableLongStateOf(0L) }
    var replayIndex by remember { mutableIntStateOf(0) }
    var futureMe by remember { mutableStateOf(false) }

    var showHistory by remember { mutableStateOf(false) }
    var showWord by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    var shownStars by remember { mutableIntStateOf(model.stars) }
    var starToken by remember { mutableIntStateOf(0) }
    var starCount by remember { mutableIntStateOf(1) }
    var rootPos by remember { mutableStateOf(Offset.Zero) }
    var pillPos by remember { mutableStateOf(Offset.Zero) }
    var badgePos by remember { mutableStateOf(Offset.Zero) }
    var chipsPos by remember { mutableStateOf(Offset.Zero) }
    var starFrom by remember { mutableStateOf(Offset.Zero) }
    val pillScale = remember { Animatable(1f) }
    val enter = remember { Animatable(0f) }

    // 한 편 전체의 순서 (재생 막대와 다시 보기, 기록에 쓴다).
    val sequence = remember(script) {
        buildList {
            turns.forEach { t ->
                t.jiwoo?.let { add(Spoken(false, it)) }
                add(Spoken(true, t.me))
            }
            add(Spoken(false, script.closing))
        }
    }
    val durations = sequence.map { voice.estimateMs(it.line.ko, speed) }
    val totalMs = durations.sum()

    fun sequenceIndexOfJiwoo(t: Int): Int {
        var i = 0
        for (k in 0 until t.coerceAtMost(turns.size)) i += if (turns[k].jiwoo != null) 2 else 1
        return i
    }

    val videoIndex = if (phase == Phase.Replay) replayIndex else sequenceIndexOfJiwoo(turn)
    val elapsedMs = durations.take(videoIndex).sum() + lineElapsed

    var speakJob by remember { mutableStateOf<Job?>(null) }
    fun say(text: String, speaker: Speaker) {
        speakJob?.cancel()
        speakJob = scope.launch { voice.speak(text, speaker) }
    }

    /** 한 줄을 읽으면서 자막 강조와 재생 위치를 움직인다. */
    suspend fun playLine(line: Line, speaker: Speaker) {
        val dur = voice.estimateMs(line.ko, speed)
        val words = line.ko.split(" ")
        val total = words.sumOf { it.length }.coerceAtLeast(1)
        val job = scope.launch { voice.speak(line.ko, speaker, speed) }
        try {
            var t = 0L
            while (t < dur || job.isActive) {
                lineElapsed = t.coerceAtMost(dur)
                val at = (t.toFloat() / dur * total).toInt()
                var acc = 0
                var idx = words.lastIndex
                for (i in words.indices) {
                    acc += words[i].length
                    if (at < acc) { idx = i; break }
                }
                highlight = idx
                delay(40)
                t += 40
                if (t > dur * 3 + 3000) break
            }
            lineElapsed = dur
            highlight = -1
            delay(400)
        } finally {
            job.cancel()
        }
    }

    fun advance() {
        attempts = 0
        badge = false
        quietPrompt = false
        quietTries = 0
        turn += 1
        lineElapsed = 0
        auto = Auto.Model
        phase = firstPhase(turn)
    }

    fun accept(star: Boolean) {
        quietPrompt = false
        quietTries = 0
        offlineHold = false
        attempts += 1
        if (star && attempts == 1 && isMain && !viewOnly) {
            model.stars += 1
            badge = true
            starCount = 1
            starFrom = badgePos
            starToken += 1
        }
        phase = Phase.After
    }

    fun finishRecording(byTimer: Boolean) {
        if (phase != Phase.Recording) return
        val tooShort = !byTimer && SystemClock.uptimeMillis() - recStart < 700
        val quiet = model.nextQuiet || tooShort
        model.nextQuiet = false
        if (quiet) {
            quietTries += 1
            if (quietTries < 2) {
                // C-12 · 소리가 작거나 말이 없음: 지우가 다시 청한다. 별은 없다.
                quietPrompt = true
                auto = Auto.QuietPrompt
                phase = Phase.MyTurn
                return
            }
        }
        if (model.offline) {
            // C-14 · 인터넷 끊김: 녹음은 보관하고 연결되면 이어간다.
            offlineHold = true
            auto = Auto.None
            phase = Phase.MyTurn
            return
        }
        accept(star = !quiet)
    }

    fun startRecording() {
        speakJob?.cancel()
        voice.stop()
        offlineHold = false
        recStart = SystemClock.uptimeMillis()
        phase = Phase.Recording
    }

    fun onMic() {
        when {
            phase == Phase.Recording -> finishRecording(byTimer = false)
            model.micOn -> startRecording()
            else -> model.requestMic { granted -> if (granted) startRecording() else toast("마이크를 켜면 이야기할 수 있어요") }
        }
    }

    fun close() {
        speakJob?.cancel()
        voice.stop()
        when {
            isMain && phase == Phase.Closing -> onFinished(false)
            phase == Phase.Replay && config.mode != TalkMode.ReplayOnly -> { paused = false; phase = Phase.Closing }
            else -> onClose(turn.coerceAtMost(turns.size - 1))
        }
    }

    BackHandler {
        when {
            showWord -> showWord = false
            showHistory -> showHistory = false
            showSettings -> showSettings = false
            else -> close()
        }
    }

    // C-1 · 들어가기: 지도가 지우 쪽으로 확대되며 장소 안으로.
    LaunchedEffect(Unit) {
        if (phase == Phase.Enter) {
            enter.animateTo(1f, tween(1300, easing = FastOutSlowInEasing))
            phase = firstPhase(turn)
        }
    }

    // C-2 · 지우가 말함.
    LaunchedEffect(phase, turn, playToken, paused, speed) {
        if (phase != Phase.Jiwoo || paused) return@LaunchedEffect
        val line = if (turn < turns.size) turns[turn].jiwoo else script.closing
        if (line != null) playLine(line, Speaker.Jiwoo)
        lineElapsed = 0
        phase = if (turn >= turns.size) Phase.Closing else Phase.MyTurn
    }

    // C-10 · 한 편으로 다시 보기: 멈추지 않고 재생.
    LaunchedEffect(phase, replayIndex, playToken, paused, speed, futureMe) {
        if (phase != Phase.Replay || paused) return@LaunchedEffect
        val item = sequence[replayIndex]
        playLine(item.line, if (!item.isMe) Speaker.Jiwoo else if (futureMe) Speaker.FutureMe else Speaker.Me)
        lineElapsed = 0
        if (replayIndex < sequence.lastIndex) {
            replayIndex += 1
        } else if (config.mode == TalkMode.ReplayOnly) {
            onClose(0)
        } else {
            phase = Phase.Closing
        }
    }

    // C-3 · 내 차례: 모범 음성이 한 번 저절로 나온다. 보기 전용이면 모범 음성이 대신 말하고 넘어간다.
    LaunchedEffect(phase, turn, viewOnly, quietTries) {
        if (phase != Phase.MyTurn) return@LaunchedEffect
        delay(350)
        val me = turns[turn].me
        when (auto) {
            Auto.QuietPrompt -> voice.speak(quietLine.ko, Speaker.Jiwoo)
            Auto.Model -> voice.speak(me.ko, Speaker.Model)
            Auto.None -> if (viewOnly) voice.speak(me.ko, Speaker.Model)
        }
        auto = Auto.None
        if (viewOnly) {
            delay(700)
            accept(star = false)
        }
    }

    // C-4 · 말하는 중: 말이 끝나면 저절로 멈춘다 (목업에서는 시간으로 흉내).
    LaunchedEffect(phase) {
        if (phase == Phase.Recording) {
            delay(2600)
            finishRecording(byTimer = true)
        }
    }

    // C-14 · 연결이 돌아오면 보관한 녹음으로 이어간다.
    LaunchedEffect(model.offline, offlineHold) {
        if (offlineHold && !model.offline) {
            delay(500)
            accept(star = true)
        }
    }

    val flagged = if (isMain && !viewOnly && turn < turns.size) turns[turn].flagged else null

    // C-5.2 · 표시가 없으면 버튼 없이 1~2초 뒤 저절로 이어진다.
    LaunchedEffect(phase, turn, flagged) {
        if (phase == Phase.After && flagged == null) {
            delay(1700)
            advance()
        }
    }

    // C-19 · 마무리에서는 별 여러 개가 한꺼번에 날아 들어간다.
    LaunchedEffect(phase) {
        if (phase == Phase.Closing && isMain && !viewOnly) {
            delay(450)
            starCount = 6
            starFrom = chipsPos
            starToken += 1
        }
    }

    val group = when (phase) {
        Phase.Enter -> Group.Enter
        Phase.Jiwoo, Phase.Replay -> Group.Video
        Phase.MyTurn, Phase.Recording -> Group.Turn
        Phase.After -> Group.After
        Phase.Closing -> Group.Closing
    }
    val isVideo = group == Group.Video || group == Group.Enter

    Box(
        Modifier
            .fillMaxSize()
            .background(K.BrandStrong)
            .onGloballyPositioned { rootPos = it.positionInRoot() },
    ) {
        SceneBackdrop(topAlpha = if (isVideo) 1f else 0.7f)

        Crossfade(targetState = group, animationSpec = tween(260), label = "talk") { g ->
            Box(Modifier.fillMaxSize()) {
                when (g) {
                    Group.Enter -> Unit

                    Group.Video -> {
                        val item = if (phase == Phase.Replay) sequence[replayIndex] else null
                        val line = item?.line ?: (if (turn < turns.size) turns[turn].jiwoo else script.closing)
                        if (line != null && subtitleOn) {
                            BigSubtitle(
                                line, highlight, showTranslation,
                                Modifier.align(Alignment.TopCenter).padding(top = 132.dp, start = 24.dp, end = 24.dp),
                            )
                        }
                        if (item?.isMe == true) {
                            // 내 대사는 실제 녹음, 고르면 미래의 나로.
                            Row(
                                Modifier.align(Alignment.BottomCenter).padding(bottom = 176.dp + bottomExtra),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ChoiceChip("내가 말한 것", selected = !futureMe) { futureMe = false }
                                ChoiceChip("미래의 나", selected = futureMe) { futureMe = true }
                            }
                        }
                        PlayerControls(
                            elapsedMs = elapsedMs,
                            totalMs = totalMs,
                            paused = paused,
                            subtitleOn = subtitleOn,
                            speedLabel = if (speed == 1f) "1x" else "${speed}x",
                            onToggleSubtitle = { subtitleOn = !subtitleOn },
                            onRestart = { lineElapsed = 0; paused = false; playToken += 1 },
                            onTogglePause = { paused = !paused },
                            onSkip = {
                                paused = false
                                lineElapsed = 0
                                if (phase == Phase.Replay) {
                                    if (replayIndex < sequence.lastIndex) replayIndex += 1
                                    else if (config.mode == TalkMode.ReplayOnly) onClose(0)
                                    else phase = Phase.Closing
                                } else {
                                    phase = if (turn >= turns.size) Phase.Closing else Phase.MyTurn
                                }
                            },
                            onSpeed = { speedIndex = (speedIndex + 1) % speeds.size },
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp + bottomExtra),
                        )
                    }

                    Group.Turn -> {
                        val t = turns[turn.coerceAtMost(turns.size - 1)]
                        val top = if (quietPrompt) quietLine else t.jiwoo
                        if (top != null) {
                            TopCard(top, hideText, showTranslation, saved,
                                onListen = { say(top.ko, Speaker.Jiwoo) },
                                onHide = { hideText = !hideText },
                                onTranslate = { showTranslation = !showTranslation },
                                onSave = { saved = if (top.ko in saved) saved - top.ko else saved + top.ko },
                            )
                        }
                        SayCard(
                            line = t.me,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 18.dp, end = 18.dp, bottom = 171.dp + bottomExtra),
                            label = if (viewOnly) "모범 음성이 대신 말해요" else "이렇게 말해보세요",
                            note = if (viewOnly && !isOnboarding) "보기 전용 · 별과 스트릭 없음" else if (viewOnly) "보기 전용" else null,
                            showRoman = showRoman,
                            onToggleRoman = { showRoman = !showRoman },
                            onListen = { say(t.me.ko, Speaker.Model) },
                        )
                        if (viewOnly) {
                            // C-15 · 마이크 권한 없음.
                            Box(
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 64.dp + bottomExtra)
                                    .height(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable {
                                        if (manualViewOnly) manualViewOnly = false
                                        if (!model.micOn) {
                                            model.forceMicOff = false
                                            model.requestMic { granted -> if (!granted) toast("설정에서 마이크를 허용해 주세요") }
                                        }
                                    }
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.Center,
                            ) { T("마이크 켜기", Type.LabelLarge, K.BrandStrong) }
                        } else {
                            val label = when {
                                phase == Phase.Recording -> "듣고 있어요"
                                offlineHold -> "연결되면 이어갈게요. 녹음은 보관했어요"
                                t.jiwoo == null && !quietPrompt -> "눌러서 말 걸기"
                                else -> "눌러서 대답하기"
                            }
                            T(
                                label, Type.LabelMedium, Color.White,
                                Modifier.align(Alignment.BottomCenter).padding(bottom = 138.dp + bottomExtra),
                                align = TextAlign.Center,
                            )
                            MicButton(
                                recording = phase == Phase.Recording,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp + bottomExtra),
                                onClick = { onMic() },
                            )
                        }
                        if (isMain) HistoryButton(bottomExtra) { showHistory = true }
                    }

                    Group.After -> {
                        val t = turns[turn.coerceAtMost(turns.size - 1)]
                        t.jiwoo?.let { top ->
                            TopCard(top, hideText, showTranslation, saved,
                                onListen = { say(top.ko, Speaker.Jiwoo) },
                                onHide = { hideText = !hideText },
                                onTranslate = { showTranslation = !showTranslation },
                                onSave = { saved = if (top.ko in saved) saved - top.ko else saved + top.ko },
                            )
                        }
                        MyCard(
                            text = t.me.ko,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(start = 18.dp, end = 18.dp, bottom = (if (flagged != null) 168.dp else 84.dp) + bottomExtra),
                            flagged = flagged,
                            onFlaggedClick = { showWord = true },
                            badge = badge,
                            onBadgePlaced = { badgePos = it },
                        )
                        if (flagged != null) {
                            // C-5.1 / C-5.3 · 다시 말하기(두 번까지)와 이어가기.
                            val canRetry = attempts < 3
                            T(
                                if (canRetry) "표시된 말을 누르면 다시 들어볼 수 있어요" else "두 번 말했어요. 다음에 또 해봐요",
                                Type.LabelMedium, Color.White,
                                Modifier.align(Alignment.BottomCenter).padding(bottom = 138.dp + bottomExtra),
                                align = TextAlign.Center,
                            )
                            GlassIconButton(
                                R.drawable.ic_replay,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp + bottomExtra).offset(x = (-76).dp),
                                size = 56.dp,
                                enabled = canRetry,
                                contentDescription = "다시 말하기",
                                onClick = { auto = Auto.None; phase = Phase.MyTurn },
                            )
                            Box(
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 56.dp + bottomExtra)
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(K.BrandPrimary)
                                    .clickable { advance() },
                                contentAlignment = Alignment.Center,
                            ) { Ic(R.drawable.ic_arrow_right, size = 28.dp, tint = Color.White) }
                        }
                        if (isMain) HistoryButton(bottomExtra) { showHistory = true }
                    }

                    Group.Closing -> {
                        val top = script.closing
                        TopCard(top, hideText, showTranslation, saved,
                            onListen = { say(top.ko, Speaker.Jiwoo) },
                            onHide = { hideText = !hideText },
                            onTranslate = { showTranslation = !showTranslation },
                            onSave = { saved = if (top.ko in saved) saved - top.ko else saved + top.ko },
                        )
                        if (isOnboarding) {
                            // A-4.6 · 마무리.
                            PrimaryButton(
                                "계속하기",
                                Modifier.align(Alignment.BottomCenter).padding(start = 24.dp, end = 24.dp, bottom = 72.dp + bottomExtra).fillMaxWidth(),
                            ) { voice.stop(); onFinished(false) }
                        } else {
                            // C-7 · 마무리: 받은 별과 스트릭.
                            Row(
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 157.dp + bottomExtra)
                                    .onGloballyPositioned { c ->
                                        val p = c.positionInRoot()
                                        chipsPos = Offset(p.x + c.size.width * 0.2f, p.y + c.size.height / 2f)
                                    },
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (!viewOnly) GlassChip("★ +${turns.size}")
                                GlassChip("지우와 ${model.streak}일째")
                            }
                            PrimaryButton(
                                "지도로",
                                Modifier.align(Alignment.BottomCenter).padding(start = 24.dp, end = 24.dp, bottom = 72.dp + bottomExtra).fillMaxWidth(),
                            ) { voice.stop(); onFinished(false) }
                            Row(
                                Modifier.align(Alignment.BottomCenter).padding(bottom = 29.dp + bottomExtra),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                            ) {
                                TextLink("한 편으로 다시 보기") {
                                    replayIndex = 0; lineElapsed = 0; paused = false; phase = Phase.Replay
                                }
                                if (model.doneToday + 1 < model.dailyLimit) {
                                    TextLink("더 하기") { voice.stop(); onFinished(true) }
                                }
                            }
                        }
                    }
                }
            }
        }

        // C-1 · 들어가기 연출.
        if (phase == Phase.Enter) {
            val p = enter.value
            Image(
                painterResource(R.drawable.img_map),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    val s = 1f + 1.15f * p
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0.5f, 0.52f)
                    alpha = 1f - ((p - 0.72f) / 0.28f).coerceIn(0f, 1f)
                },
                contentScale = ContentScale.Crop,
            )
        }

        // 위쪽 줄: 나가기 · 진행 · 별 · 설정.
        if (phase != Phase.Enter) {
            GlassIconButton(
                if (isOnboarding) R.drawable.ic_chevron_left else R.drawable.ic_close,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 52.dp),
                contentDescription = if (isOnboarding) "이전으로" else "나가기",
                onClick = { close() },
            )
            // 마무리(C-19)에서는 스트릭이 그 자리에 오므로 진행 점을 뺀다.
            if (!isVideo && config.mode != TalkMode.ReplayOnly && !(phase == Phase.Closing && isMain)) {
                val done = when (phase) {
                    Phase.After -> turn + 1
                    Phase.Closing -> turns.size
                    else -> turn
                }
                ProgressDots(turns.size, done, Modifier.align(Alignment.TopCenter).padding(top = 62.dp))
            }
            if (!isOnboarding) {
                Row(
                    Modifier.align(Alignment.TopEnd).padding(top = 52.dp, end = if (isVideo) 24.dp else 18.dp).height(44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (phase == Phase.Closing && isMain) {
                        StatPill(R.drawable.ic_streak, model.streak)
                        Spacer(Modifier.width(8.dp))
                    }
                    StatPill(
                        R.drawable.ic_star,
                        shownStars,
                        Modifier
                            .scale(pillScale.value)
                            .onGloballyPositioned { c ->
                                val pos = c.positionInRoot()
                                pillPos = Offset(pos.x + with(density) { 19.dp.toPx() }, pos.y + c.size.height / 2f)
                            },
                    )
                    if (!isVideo && isMain) {
                        Spacer(Modifier.width(14.dp))
                        GlassIconButton(R.drawable.ic_settings, contentDescription = "설정") { showSettings = !showSettings }
                    }
                }
            }
        }

        // C-16 ~ C-19 · 별을 받는 순간.
        StarFx(
            token = starToken,
            from = starFrom - rootPos,
            to = pillPos - rootPos,
            count = starCount,
            onArrive = {
                shownStars = model.stars
                scope.launch {
                    pillScale.snapTo(1.22f)
                    pillScale.animateTo(1f, tween(280))
                }
            },
        )

        // C-9 · 기록: 위에서 내려오는 패널.
        AnimatedVisibility(showHistory, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color(0x33000000)).plainClick { showHistory = false })
        }
        AnimatedVisibility(
            showHistory,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
        ) {
            val shown = sequence.take(
                when (phase) {
                    Phase.After -> sequenceIndexOfJiwoo(turn + 1)
                    Phase.Closing -> sequence.size
                    else -> sequenceIndexOfJiwoo(turn) + if (turn < turns.size && turns[turn].jiwoo != null) 1 else 0
                },
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(Color(0xF210281E))
                    .plainClick { }
                    .padding(start = 24.dp, end = 24.dp, top = 64.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    T("지금까지 나눈 말", Type.TitleSmall, Color.White, Modifier.weight(1f))
                    StatPill(R.drawable.ic_star, shownStars)
                }
                if (shown.isEmpty()) T("아직 나눈 말이 없어요.", Type.Body, K.OnPhotoMuted)
                shown.forEach { s ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            T(if (s.isMe) "나" else "지우", Type.Caption, K.OnPhotoMuted)
                            T(s.line.ko, Type.BodyLarge, Color.White)
                        }
                        GlassIconButton(R.drawable.ic_volume, contentDescription = "문장 듣기") {
                            say(s.line.ko, if (s.isMe) Speaker.Me else Speaker.Jiwoo)
                        }
                    }
                }
            }
        }

        // 설정: 말하기 어려울 때의 스위치 + 목업 스위치.
        if (showSettings) {
            Box(Modifier.fillMaxSize().plainClick { showSettings = false })
            Column(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 104.dp, end = 18.dp)
                    .width(286.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .plainClick { }
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SettingRow("지금은 말하기 어려워요", "모범 음성이 대신 말해요 · 별 없음", manualViewOnly) { manualViewOnly = it }
                Box(Modifier.fillMaxWidth().height(1.dp).background(K.BorderSubtle))
                T("목업 스위치", Type.Caption, K.TextTertiary)
                SettingRow("다음 발화를 ‘소리 작음’으로", "C-12 · 지우가 다시 청해요", model.nextQuiet) { model.nextQuiet = it }
                SettingRow("인터넷 끊김", "C-14 · 연결되면 이어가요", model.offline) { model.offline = it }
                SettingRow("마이크 권한 없음", "C-15 · 보기 전용", model.forceMicOff) { model.forceMicOff = it }
            }
        }

        // C-6 · 어절 안내.
        val info = flagged?.let { Scripts.words[it] }
        AnimatedVisibility(showWord && info != null, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier.fillMaxSize().background(Color(0x52000000)).plainClick { showWord = false },
                contentAlignment = Alignment.Center,
            ) {
                if (info != null) {
                    WordSheet(
                        info = info,
                        saved = info.word in saved,
                        onToggleSave = { saved = if (info.word in saved) saved - info.word else saved + info.word },
                        onNative = { say(info.word, Speaker.Model) },
                        onMine = { say(info.word, Speaker.Me) },
                        onClose = { showWord = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.TopCard(
    line: Line,
    hideText: Boolean,
    showTranslation: Boolean,
    saved: Set<String>,
    onListen: () -> Unit,
    onHide: () -> Unit,
    onTranslate: () -> Unit,
    onSave: () -> Unit,
) {
    JiwooCard(
        line = line,
        modifier = Modifier.align(Alignment.TopCenter).padding(start = 18.dp, end = 18.dp, top = 108.dp),
        onListen = onListen,
        hideText = hideText,
        onToggleHide = onHide,
        showTranslation = showTranslation,
        onToggleTranslation = onTranslate,
        saved = line.ko in saved,
        onToggleSave = onSave,
    )
}

@Composable
private fun BoxScope.HistoryButton(bottomExtra: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    GlassIconButton(
        R.drawable.ic_chat,
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 36.dp, bottom = 70.dp + bottomExtra),
        contentDescription = "기록 열기",
        onClick = onClick,
    )
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (selected) K.BrandSoft else K.GlassThin)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) { T(text, Type.Caption, if (selected) K.BrandPrimary else Color.White) }
}

@Composable
private fun SettingRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            T(title, Type.LabelMedium.copy(fontSize = Type.Body.fontSize), K.TextPrimary)
            T(sub, Type.Caption, K.TextSecondary)
        }
        MockSwitch(checked, onChange)
    }
}

/**
 * 별이 내 말 카드에서 크게 터지고(C-16), 위쪽 숫자로 줄지어 날아가(C-17), 숫자에 들어간다(C-18).
 */
@Composable
private fun StarFx(token: Int, from: Offset, to: Offset, count: Int, onArrive: () -> Unit) {
    val p = remember { Animatable(1f) }
    var running by remember { mutableStateOf(false) }
    LaunchedEffect(token) {
        if (token == 0) return@LaunchedEffect
        running = true
        p.snapTo(0f)
        p.animateTo(0.82f, tween(760, easing = LinearEasing))
        onArrive()
        p.animateTo(1f, tween(160, easing = LinearEasing))
        running = false
    }
    if (!running) return
    val v = p.value
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize()) {
        // 터짐
        val burst = (v / 0.4f).coerceIn(0f, 1f)
        if (v < 0.5f) {
            val size = 44.dp
            val half = with(density) { size.toPx() / 2 }
            Box(
                Modifier
                    .offset { IntOffset((from.x - half * 2.2f).roundToInt(), (from.y - half * 2.2f).roundToInt()) }
                    .size(size * 2.2f)
                    .scale(0.4f + burst)
                    .alpha((1f - burst) * 0.55f)
                    .clip(CircleShape)
                    .background(K.Crown),
            )
            Ic(
                R.drawable.ic_star,
                size = size,
                modifier = Modifier
                    .offset { IntOffset((from.x - half).roundToInt(), (from.y - half).roundToInt()) }
                    .scale(0.5f + burst * 0.9f)
                    .alpha(1f - ((v - 0.35f) / 0.15f).coerceIn(0f, 1f)),
            )
        }
        // 날아감
        repeat(count) { i ->
            val start = 0.22f + i * (0.3f / count.coerceAtLeast(1))
            val f = ((v - start) / 0.5f).coerceIn(0f, 1f)
            if (f > 0f && f < 1f) {
                val ease = f * f * (3 - 2 * f)
                val sway = (if (i % 2 == 0) 1 else -1) * with(density) { 26.dp.toPx() } * (1 - ease) * ease * 4f * 0.5f
                val x = from.x + (to.x - from.x) * ease + sway
                val y = from.y + (to.y - from.y) * ease
                val size = 20.dp
                val half = with(density) { size.toPx() / 2 }
                Ic(
                    R.drawable.ic_star,
                    size = size,
                    modifier = Modifier
                        .offset { IntOffset((x - half).roundToInt(), (y - half).roundToInt()) }
                        .scale(1.1f - 0.5f * ease),
                )
            }
        }
    }
}
