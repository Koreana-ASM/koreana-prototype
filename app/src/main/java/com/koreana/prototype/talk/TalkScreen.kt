package com.koreana.prototype.talk

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.koreana.prototype.AppModel
import com.koreana.prototype.Line
import com.koreana.prototype.LocalVoice
import com.koreana.prototype.Mark
import com.koreana.prototype.R
import com.koreana.prototype.Scripts
import com.koreana.prototype.Speaker
import com.koreana.prototype.Spoken
import com.koreana.prototype.TalkConfig
import com.koreana.prototype.TalkMode
import com.koreana.prototype.Words
import com.koreana.prototype.ui.LocalBottomExtra
import com.koreana.prototype.ui.TK
import com.koreana.prototype.ui.plainClick
import com.koreana.prototype.ui.rememberToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 지금 화면이 대화의 어디에 있는지. */
private enum class Stage { Enter, Intro, Question, MyTurn, Recording, Review, Sent, Reaction, AskAgain, Closing }

/** 눌러서 말풍선을 연 어절. [mark]는 보내기 전의 발음 표시, [bounds]는 화면에서의 자리(px). */
class WordPick(val line: Line, val index: Int, val bounds: Rect, val mark: Mark? = null) {
    val word: String get() = line.words[index]
}

/** 장면 오른쪽 위의 두 버튼: 표시와 대화 기록. */
@Composable
fun BoxScope.TalkTopButtons(onDisplay: () -> Unit, onHistory: () -> Unit) {
    Row(Modifier.align(Alignment.TopEnd).padding(top = 52.dp, end = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RoundButton(R.drawable.ic_talk_tune, onClick = onDisplay)
        RoundButton(R.drawable.ic_talk_log, onClick = onHistory)
    }
}

/**
 * 누른 어절 바로 위에 뜨는 말풍선. 대화창과 지우는 그대로 두고, 밖을 누르면 닫히기만 한다.
 * [origin]은 이 화면의 왼쪽 위(px): 어절의 자리를 화면 안의 좌표로 바꾼다.
 */
@Composable
fun BoxScope.WordBubbleLayer(
    pick: WordPick,
    origin: Offset,
    saved: Boolean,
    onListen: () -> Unit,
    onSave: () -> Unit,
    onMore: () -> Unit,
    onMine: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(Modifier.fillMaxSize().plainClick(onClick = onDismiss)) {
        WordBubble(
            word = Words.bare(pick.word),
            meaning = Words.info(pick.word)?.meaning,
            saved = saved,
            modifier = Modifier.padding(start = 27.dp).graphicsLayer {
                translationY = pick.bounds.top - origin.y - 4.dp.toPx() - size.height
            },
            heard = pick.mark?.heard,
            heardColor = pick.mark?.level?.color() ?: TK.Fair,
            onListen = onListen,
            onSave = onSave,
            onMore = onMore,
            onMine = onMine,
        )
        Triangle(
            TK.Bubble,
            Modifier.size(13.86.dp, 7.5.dp).graphicsLayer {
                translationX = pick.bounds.center.x - origin.x - size.width / 2f
                translationY = pick.bounds.top - origin.y - 4.dp.toPx() - 1f
            },
        )
    }
}

/**
 * 대화. 대화창은 화면 맨 아래에 고정이고 "다음" 버튼 없이 화면을 누르면 넘어간다(▼).
 * 점선 어절을 누르면 말풍선이 열리고, 내 차례에 필요한 것(선택지, 마이크, 녹음, 다시 하기, 보내기)은 대화창 바로 위에 잠깐 뜬다.
 * 녹음은 말을 멈추면 저절로 끝난다. 색 표시가 있으면 보내기 전에 한 번 멈추고, 없으면 바로 보내진다.
 * 온보딩 첫 대화와 노트의 "다시 말해 보기"도 같은 틀을 쓰고, 마이크 없이 볼 때는 [ViewOnlyTalk]로 장면만 본다.
 */
@Composable
fun TalkScreen(
    model: AppModel,
    config: TalkConfig,
    onClose: (turn: Int) -> Unit,
    onFinished: (transcript: List<Spoken>) -> Unit,
) {
    if (config.mode == TalkMode.ViewOnly) {
        ViewOnlyTalk(config, onClose = { onClose(0) })
        return
    }

    val voice = LocalVoice.current
    val scope = rememberCoroutineScope()
    val toast = rememberToast()
    val extra = LocalBottomExtra.current
    val script = config.script
    val turns = script.turns
    val review = config.mode == TalkMode.Review

    // 첫 대화에서 한 번만, 지우가 어절을 눌러 보라고 말해 준다.
    val intro = remember {
        if (config.mode == TalkMode.Main && !model.wordHintShown && script.intro.isNotEmpty()) {
            model.wordHintShown = true
            script.intro + Scripts.wordHint
        } else {
            script.intro
        }
    }

    val startTurn = config.startTurn.coerceIn(0, turns.lastIndex)
    val firstStage = if (startTurn == 0 && intro.isNotEmpty()) Stage.Intro else Stage.Question

    var turn by remember { mutableIntStateOf(startTurn) }
    var introIndex by remember { mutableIntStateOf(0) }
    var stage by remember { mutableStateOf(if (config.enterFromMap) Stage.Enter else firstStage) }
    var guided by remember { mutableStateOf(false) }
    var attempts by remember { mutableIntStateOf(0) }
    var recNonce by remember { mutableIntStateOf(0) }
    val transcript = remember { mutableStateListOf<Spoken>() }

    var showRoman by remember { mutableStateOf(true) }
    var showTranslation by remember { mutableStateOf(true) }
    var showDisplay by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var pick by remember { mutableStateOf<WordPick?>(null) }
    var sheet by remember { mutableStateOf<Pair<Line, Int>?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }

    var playing by remember { mutableStateOf<Line?>(null) }
    var playingWord by remember { mutableStateOf(false) }
    var highlight by remember { mutableIntStateOf(-1) }
    var speakJob by remember { mutableStateOf<Job?>(null) }
    val enter = remember { Animatable(0f) }
    val rec = remember { Animatable(0f) }

    fun stopVoice() {
        speakJob?.cancel()
        voice.stop()
        playing = null
        playingWord = false
        highlight = -1
    }

    /** 한 줄을 읽으면서 지금 소리 나는 어절을 밝힌다. 듣는 중에 다시 누르면 멈춘다. */
    fun play(line: Line, speaker: Speaker, toggle: Boolean = true, then: Speaker? = null) {
        val wasPlaying = playing == line
        stopVoice()
        if (wasPlaying && toggle) return
        speakJob = scope.launch {
            playing = line
            try {
                for (who in listOfNotNull(speaker, then)) {
                    val dur = voice.estimateMs(line.ko)
                    val words = line.words
                    val total = words.sumOf { it.length }.coerceAtLeast(1)
                    val speech = launch { voice.speak(line.ko, who) }
                    try {
                        var t = 0L
                        while (speech.isActive && t < dur * 3 + 3000) {
                            val at = (t.toFloat() / dur * total).toInt()
                            var acc = 0
                            highlight = words.indices.firstOrNull { acc += words[it].length; at < acc } ?: words.lastIndex
                            delay(40)
                            t += 40
                        }
                    } finally {
                        speech.cancel()
                    }
                    highlight = -1
                    if (then != null && who == speaker) delay(350)
                }
            } finally {
                if (playing == line) { playing = null; highlight = -1 }
            }
        }
    }

    fun playWord(word: String, speaker: Speaker = Speaker.Model) {
        stopVoice()
        speakJob = scope.launch {
            playingWord = true
            try { voice.speak(word, speaker) } finally { playingWord = false }
        }
    }

    val current = turns[turn.coerceAtMost(turns.lastIndex)]
    val reply = if (guided) current.guided else current.free
    val mine = stage == Stage.MyTurn || stage == Stage.Recording || stage == Stage.Review || stage == Stage.Sent
    val line: Line = when (stage) {
        Stage.Enter, Stage.Intro -> intro.getOrElse(introIndex) { current.question }
        Stage.Question -> current.question
        Stage.MyTurn, Stage.Recording, Stage.Review, Stage.Sent -> reply.me
        Stage.Reaction -> reply.reaction
        Stage.AskAgain -> Scripts.askAgain
        Stage.Closing -> script.closing ?: reply.reaction
    }
    val mood = when (stage) {
        Stage.Reaction -> Mood.Smile
        Stage.Closing -> Mood.Glad
        else -> Mood.Neutral
    }
    // 본편의 첫 시도에만 발음 표시가 붙는다 (목업). 다시 하면 표시 없이 바로 보내진다.
    val mark: Mark? = if (config.mode == TalkMode.Main && attempts == 0) reply.mark else null

    // 지우의 말이 새로 나오면 기록에 쌓고 한 번 읽어 준다.
    LaunchedEffect(stage, turn, introIndex) {
        val speaks = stage == Stage.Intro || stage == Stage.Question || stage == Stage.Reaction || stage == Stage.Closing || stage == Stage.AskAgain
        if (!speaks) return@LaunchedEffect
        if (stage == Stage.AskAgain || transcript.none { !it.isMe && it.line == line }) transcript += Spoken(false, line)
        delay(250)
        play(line, Speaker.Jiwoo, toggle = false)
    }

    // 들어가기: 지도가 지우 쪽으로 확대되며 장소 안으로.
    LaunchedEffect(Unit) {
        if (stage == Stage.Enter) {
            enter.animateTo(1f, tween(1300, easing = FastOutSlowInEasing))
            stage = firstStage
        }
    }

    fun next() {
        stopVoice()
        when (stage) {
            Stage.Intro -> if (introIndex < intro.lastIndex) introIndex += 1 else stage = Stage.Question
            Stage.Question -> if (review) {
                // 다시 말해 보기: 아직 말해 본 적 없는 말은 따라 말하기, 말해 본 말은 자유 답변.
                guided = model.saved.firstOrNull { it.word == config.reviewWord }?.spoken != true
                attempts = 0
                stage = Stage.MyTurn
                if (guided) play(current.guided.me, Speaker.Model, toggle = false)
            }
            Stage.Reaction -> when {
                turn < turns.lastIndex -> { turn += 1; attempts = 0; stage = Stage.Question }
                script.closing != null -> stage = Stage.Closing
                else -> onFinished(transcript.toList())
            }
            Stage.Closing -> onFinished(transcript.toList())
            else -> Unit
        }
    }

    fun choose(withGuide: Boolean) {
        stopVoice()
        guided = withGuide
        attempts = 0
        stage = Stage.MyTurn
        // 추천 문장은 모범 음성을 한 번 들려준다.
        if (withGuide) play(current.guided.me, Speaker.Model, toggle = false)
    }

    fun withMic(onGranted: () -> Unit) {
        if (model.micOn) onGranted()
        else model.requestMic { granted -> if (granted && model.micOn) onGranted() else toast("마이크를 켜면 이야기할 수 있어요") }
    }

    fun startRecording() = withMic {
        stopVoice()
        pick = null
        recNonce += 1
        stage = Stage.Recording
    }

    /** 보낸 말은 소리 없이 내 대사로 나타나고, 1초 뒤 지우가 반응한다. */
    fun send() {
        stopVoice()
        pick = null
        transcript += Spoken(true, reply.me)
        stage = Stage.Sent
    }

    fun endRecording() {
        when {
            // 아무것도 못 알아들었을 때만 지우가 짧게 되묻고 바로 내 차례.
            model.nextQuiet && config.mode != TalkMode.Onboarding -> { model.nextQuiet = false; stage = Stage.AskAgain }
            mark != null -> stage = Stage.Review
            else -> send()
        }
    }

    // 녹음: 말한 만큼 진해지고, 말을 멈추면 1초 뒤 저절로 끝난다.
    LaunchedEffect(stage, recNonce) {
        if (stage != Stage.Recording) return@LaunchedEffect
        rec.snapTo(0f)
        rec.animateTo(1f, tween(voice.estimateMs(reply.me.ko).toInt() + 400, easing = LinearEasing))
        delay(1000)
        endRecording()
    }
    LaunchedEffect(stage) {
        if (stage == Stage.Sent) {
            delay(1000)
            stage = Stage.Reaction
        }
    }

    fun leave() {
        stopVoice()
        onClose(turn)
    }

    BackHandler {
        when {
            sheet != null -> { stopVoice(); sheet = null }
            pick != null -> pick = null
            showDisplay -> showDisplay = false
            showHistory -> { stopVoice(); showHistory = false }
            stage == Stage.MyTurn || stage == Stage.Recording || stage == Stage.Review -> { stopVoice(); stage = Stage.Question }
            else -> leave()
        }
    }

    val canNext = stage == Stage.Intro || stage == Stage.Reaction || stage == Stage.Closing || (review && stage == Stage.Question)
    val choosing = stage == Stage.Question && !review
    val wordCount = line.words.size

    Box(Modifier.fillMaxSize().background(TK.Scene).onGloballyPositioned { origin = it.positionInRoot() }) {
        Crossfade(mood, animationSpec = tween(320), label = "mood") { m ->
            Image(painterResource(m.image), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        SceneShades(strong = choosing || mine)

        if (stage != Stage.Enter) {
            // 화면을 누르면 다음 발언으로.
            Box(Modifier.fillMaxSize().plainClick(enabled = canNext) { next() })

            val tappable = !mine || stage == Stage.Review
            TalkBox(
                speaker = if (mine) "나" else "지우",
                line = line,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 27.dp, bottom = 34.dp + extra),
                onListen = when {
                    !mine -> ({ play(line, Speaker.Jiwoo) })
                    stage == Stage.MyTurn && guided -> ({ play(line, Speaker.Model) })
                    // 보내기 전의 🔊는 지금 나 → 유창한 나를 연달아 들려준다.
                    stage == Stage.Review -> ({ play(line, Speaker.Me, then = Speaker.FutureMe) })
                    else -> null
                },
                showRoman = showRoman,
                showTranslation = showTranslation && !(mine && !guided && stage != Stage.Review && stage != Stage.Sent),
                shown = if (stage == Stage.Recording && !guided) kotlin.math.ceil(rec.value * wordCount).toInt() else Int.MAX_VALUE,
                bright = when {
                    stage == Stage.MyTurn && guided -> 0
                    stage == Stage.Recording && guided -> (rec.value * wordCount + 0.3f).toInt()
                    else -> Int.MAX_VALUE
                },
                placeholder = !guided && (stage == Stage.MyTurn || (stage == Stage.Recording && rec.value * wordCount <= 0.02f)),
                lowered = stage == Stage.Sent,
                selected = pick?.takeIf { it.line == line }?.index ?: -1,
                highlight = if (playing == line && !mine) highlight else -1,
                under = { i ->
                    when {
                        !tappable -> Under.None
                        stage == Stage.Review && mark?.index == i -> mark.level.under()
                        else -> Under.Dots
                    }
                },
                onWord = if (tappable) { i, bounds ->
                    pick = WordPick(line, i, bounds, mark?.takeIf { stage == Stage.Review && it.index == i })
                } else null,
            )
            if (canNext && pick == null) {
                AdvanceMark(Modifier.align(Alignment.BottomEnd).padding(end = 26.dp, bottom = 34.dp + extra))
            }

            // 내 차례에 필요한 것은 대화창 바로 위에 잠깐 뜬다. 말풍선이 열리면 잠시 숨긴다.
            if (pick == null) {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 208.dp + extra).fillMaxWidth().height(66.dp)) {
                    when (stage) {
                        Stage.MyTurn -> {
                            RoundButton(
                                R.drawable.ic_talk_prev, Modifier.align(Alignment.CenterStart).padding(start = 40.dp),
                                size = 44.dp, iconSize = 18.dp, solid = true,
                            ) { stopVoice(); stage = Stage.Question }
                            MicButton(Modifier.align(Alignment.Center)) { startRecording() }
                        }
                        Stage.AskAgain -> MicButton(Modifier.align(Alignment.Center)) { startRecording() }
                        Stage.Recording -> {
                            RoundButton(
                                R.drawable.ic_talk_close, Modifier.align(Alignment.BottomStart).padding(start = 24.dp),
                                size = 44.dp, iconSize = 18.dp, solid = true,
                            ) { stage = Stage.MyTurn }
                            Waveform(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
                            StopButton(Modifier.align(Alignment.BottomEnd).padding(end = 24.dp)) { endRecording() }
                        }
                        Stage.Review -> SendActions(
                            Modifier.align(Alignment.BottomCenter),
                            onRetry = { attempts += 1; startRecording() },
                            onSend = { send() },
                        )
                        else -> Unit
                    }
                }
                if (choosing) {
                    Column(
                        Modifier.align(Alignment.BottomCenter).padding(bottom = 208.dp + extra),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        ChoiceBlock("내 말로 답하기", R.drawable.ic_talk_reply) { choose(false) }
                        ChoiceBlock(current.guided.me.ko, R.drawable.ic_talk_quote, suggested = true) { choose(true) }
                    }
                }
            }

            TalkTopButtons(onDisplay = { showDisplay = true }, onHistory = { stopVoice(); showHistory = true })
        }

        if (stage == Stage.Enter) {
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

        pick?.let { p ->
            WordBubbleLayer(
                pick = p,
                origin = origin,
                saved = model.isSaved(p.word),
                onListen = { playWord(Words.bare(p.word)) },
                onSave = { model.toggleSaved(p.word) },
                onMore = { sheet = p.line to p.index; pick = null },
                onMine = { p.mark?.let { playWord(it.heard, Speaker.Me) } },
                onDismiss = { pick = null },
            )
        }

        if (showHistory) {
            HistorySheet(
                script.place, transcript, playing, showRoman, showTranslation,
                onListen = { play(it.line, if (it.isMe) Speaker.Me else Speaker.Jiwoo) },
                onWord = { l, i -> sheet = l to i },
                onDisplay = { showDisplay = true },
                onClose = { stopVoice(); showHistory = false },
            )
        }
        sheet?.let { (l, i) ->
            WordSheet(
                word = l.words[i],
                sentence = l,
                saved = model.isSaved(l.words[i]),
                playing = playingWord,
                onListen = { playWord(it) },
                onSave = { model.toggleSaved(l.words[i]) },
                onClose = { stopVoice(); sheet = null },
            )
        }
        if (showDisplay) {
            DisplaySheet(showRoman, showTranslation, { showRoman = it }, { showTranslation = it }) { showDisplay = false }
        }
    }
}

/**
 * 장면만 보기. 마이크 없이 보거나 지난 대화를 다시 볼 때: 화면을 누르면 다음 말로 넘어간다.
 * 내 차례에는 추천 문장을 예시 답변으로 들려준다.
 */
@Composable
private fun ViewOnlyTalk(config: TalkConfig, onClose: () -> Unit) {
    val voice = LocalVoice.current
    val extra = LocalBottomExtra.current
    val script = config.script
    val sequence = remember(script) {
        buildList {
            script.intro.forEach { add(Spoken(false, it) to Mood.Neutral) }
            script.turns.forEach { t ->
                add(Spoken(false, t.question) to Mood.Neutral)
                add(Spoken(true, t.guided.me) to Mood.Neutral)
                add(Spoken(false, t.guided.reaction) to Mood.Smile)
            }
            script.closing?.let { add(Spoken(false, it) to Mood.Glad) }
        }
    }
    var index by remember { mutableIntStateOf(0) }
    val (item, mood) = sequence[index]

    BackHandler { voice.stop(); onClose() }
    LaunchedEffect(index) {
        delay(250)
        voice.speak(item.line.ko, if (item.isMe) Speaker.Model else Speaker.Jiwoo)
    }

    Box(
        Modifier.fillMaxSize().background(TK.Scene).plainClick {
            voice.stop()
            if (index < sequence.lastIndex) index += 1 else onClose()
        },
    ) {
        Crossfade(mood, animationSpec = tween(320), label = "mood") { m ->
            Image(painterResource(m.image), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        SceneShades(strong = item.isMe)
        TalkBox(
            speaker = if (item.isMe) "나" else "지우",
            line = item.line,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 27.dp, bottom = 34.dp + extra),
            lowered = item.isMe,
        )
        AdvanceMark(Modifier.align(Alignment.BottomEnd).padding(end = 26.dp, bottom = 34.dp + extra))
    }
}
