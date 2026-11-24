package com.koreana.prototype.talk

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.koreana.prototype.AppModel
import com.koreana.prototype.Line
import com.koreana.prototype.LocalVoice
import com.koreana.prototype.R
import com.koreana.prototype.Speaker
import com.koreana.prototype.Spoken
import com.koreana.prototype.TalkConfig
import com.koreana.prototype.TalkMode
import com.koreana.prototype.ui.LocalBottomExtra
import com.koreana.prototype.ui.TK
import com.koreana.prototype.ui.plainClick
import com.koreana.prototype.ui.rememberToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 지금 화면이 대화의 어디에 있는지. */
private enum class Stage { Enter, Intro, Question, Answer, Recording, Reaction, Closing }

/** 보낸 말을 한 번 더 확인하는 시트. */
private sealed interface Check {
    data class Unclear(val heard: String) : Check
    data class Pronounce(val heard: String, val word: String, val hint: String) : Check
}

private class WordPick(val line: Line, val index: Int)

/** 추천 문장의 마지막 단어에서 한 글자가 빠져 들린 것으로 한다 (목업). */
private fun pronounceCheck(sentence: Line): Check.Pronounce {
    val word = sentence.words.last().filter { it.isLetterOrDigit() }
    val drop = (word.length - 2).coerceAtLeast(1).coerceAtMost(word.lastIndex)
    val heard = word.removeRange(drop, drop + 1)
    return Check.Pronounce(heard, word, "“${word[drop - 1]}” 다음에 “${word[drop]}”를 한 번 넣어 보세요.")
}

/** 자유 답변의 가운데가 잘 안 들린 것으로 한다 (목업). */
private fun unclear(sentence: Line): String {
    val words = sentence.words.toMutableList()
    val last = words.last()
    words[words.lastIndex] = if (last.length > 4) "${last.take(1)}…${last.takeLast(3)}" else "…${last.takeLast(2)}"
    return words.joinToString(" ")
}

/**
 * C · 대화. 지우의 말을 하나씩 넘기며 읽다가, 내 차례에는 내 말로 답하거나 추천 문장을 따라 말한다.
 * 온보딩 첫 대화(A-4)도 같은 화면을 쓰고, 마이크 없이 볼 때는 [ViewOnlyTalk]로 장면만 본다.
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

    val startTurn = config.startTurn.coerceIn(0, turns.lastIndex)
    val firstStage = if (startTurn == 0 && script.intro.isNotEmpty()) Stage.Intro else Stage.Question

    var turn by remember { mutableIntStateOf(startTurn) }
    var introIndex by remember { mutableIntStateOf(0) }
    var stage by remember { mutableStateOf(if (config.enterFromMap) Stage.Enter else firstStage) }
    var guided by remember { mutableStateOf(false) }
    val transcript = remember { mutableStateListOf<Spoken>() }

    var showRoman by remember { mutableStateOf(true) }
    var showTranslation by remember { mutableStateOf(true) }
    var showDisplay by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var pastStart by remember { mutableStateOf<Int?>(null) }
    var wordPick by remember { mutableStateOf<WordPick?>(null) }
    var check by remember { mutableStateOf<Check?>(null) }

    var playing by remember { mutableStateOf<Line?>(null) }
    var playingWord by remember { mutableStateOf(false) }
    var highlight by remember { mutableIntStateOf(-1) }
    var speakJob by remember { mutableStateOf<Job?>(null) }
    val enter = remember { Animatable(0f) }

    fun stopVoice() {
        speakJob?.cancel()
        voice.stop()
        playing = null
        playingWord = false
        highlight = -1
    }

    /** 한 줄을 읽으면서 지금 소리 나는 어절을 밝힌다. 듣는 중에 다시 누르면 멈춘다. */
    fun play(line: Line, speaker: Speaker, toggle: Boolean = true) {
        val wasPlaying = playing == line
        stopVoice()
        if (wasPlaying && toggle) return
        speakJob = scope.launch {
            playing = line
            val dur = voice.estimateMs(line.ko)
            val words = line.words
            val total = words.sumOf { it.length }.coerceAtLeast(1)
            val speech = launch { voice.speak(line.ko, speaker) }
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
                if (playing == line) { playing = null; highlight = -1 }
            }
        }
    }

    fun playWord(word: String) {
        stopVoice()
        speakJob = scope.launch {
            playingWord = true
            try { voice.speak(word, Speaker.Model) } finally { playingWord = false }
        }
    }

    val current = turns[turn.coerceAtMost(turns.lastIndex)]
    val reply = if (guided) current.guided else current.free
    val line: Line = when (stage) {
        Stage.Enter, Stage.Intro -> script.intro.getOrElse(introIndex) { current.question }
        Stage.Question, Stage.Answer, Stage.Recording -> current.question
        Stage.Reaction -> reply.reaction
        Stage.Closing -> script.closing
    }
    val mood = when (stage) {
        Stage.Reaction -> Mood.Smile
        Stage.Closing -> Mood.Glad
        else -> Mood.Neutral
    }

    // 지우의 말이 새로 나오면 기록에 쌓고 한 번 읽어 준다.
    LaunchedEffect(stage, turn, introIndex) {
        val speaks = stage == Stage.Intro || stage == Stage.Question || stage == Stage.Reaction || stage == Stage.Closing
        if (!speaks) return@LaunchedEffect
        if (transcript.none { !it.isMe && it.line == line }) transcript += Spoken(false, line)
        delay(250)
        play(line, Speaker.Jiwoo, toggle = false)
    }

    // C-1 · 들어가기: 지도가 지우 쪽으로 확대되며 장소 안으로.
    LaunchedEffect(Unit) {
        if (stage == Stage.Enter) {
            enter.animateTo(1f, tween(1300, easing = FastOutSlowInEasing))
            stage = firstStage
        }
    }

    fun next() {
        stopVoice()
        when (stage) {
            Stage.Intro -> if (introIndex < script.intro.lastIndex) introIndex += 1 else stage = Stage.Question
            Stage.Reaction -> if (turn < turns.lastIndex) { turn += 1; stage = Stage.Question } else stage = Stage.Closing
            Stage.Closing -> onFinished(transcript.toList())
            else -> Unit
        }
    }

    fun choose(withGuide: Boolean) {
        stopVoice()
        guided = withGuide
        stage = Stage.Answer
        // 추천 문장은 예문을 한 번 들려준다.
        if (withGuide) play(current.guided.me, Speaker.Model, toggle = false)
    }

    fun withMic(onGranted: () -> Unit) {
        if (model.micOn) onGranted()
        else model.requestMic { granted -> if (granted && model.micOn) onGranted() else toast("마이크를 켜면 이야기할 수 있어요") }
    }

    /** 보낸 말을 받아들이고 지우의 반응으로 넘어간다. 실패는 없다. */
    fun accept() {
        check = null
        transcript += Spoken(true, reply.me)
        stage = Stage.Reaction
    }

    fun send() {
        if (model.nextQuiet) {
            model.nextQuiet = false
            stage = Stage.Answer
            check = if (guided) pronounceCheck(reply.me) else Check.Unclear(unclear(reply.me))
        } else {
            accept()
        }
    }

    fun leave() {
        stopVoice()
        onClose(turn)
    }

    BackHandler {
        when {
            wordPick != null -> { stopVoice(); wordPick = null }
            showDisplay -> showDisplay = false
            showHistory -> { stopVoice(); showHistory = false }
            pastStart != null -> { stopVoice(); pastStart = null }
            check != null -> accept()
            stage == Stage.Answer || stage == Stage.Recording -> { stopVoice(); stage = Stage.Question }
            else -> leave()
        }
    }

    val answering = stage == Stage.Answer || stage == Stage.Recording
    val canNext = stage == Stage.Intro || stage == Stage.Reaction || stage == Stage.Closing

    Box(Modifier.fillMaxSize().background(TK.Scene)) {
        Crossfade(mood, animationSpec = tween(320), label = "mood") { m ->
            Image(painterResource(m.image), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        SceneShades(strong = stage == Stage.Question)

        if (stage != Stage.Enter) {
            // 화면을 누르면 다음 발언으로.
            Box(Modifier.fillMaxSize().plainClick(enabled = canNext) { next() })

            if (stage == Stage.Question) {
                ChoiceBlock("내 말로 답하기", R.drawable.ic_talk_reply, Modifier.align(Alignment.TopCenter).padding(top = 296.dp)) { choose(false) }
                ChoiceBlock(
                    current.guided.me.ko, R.drawable.ic_talk_quote,
                    Modifier.align(Alignment.TopCenter).padding(top = 362.dp), suggested = true,
                ) { choose(true) }
            }

            val blockModifier = Modifier.align(Alignment.BottomStart).padding(start = 38.dp, bottom = (if (answering) 194.dp else 90.dp) + extra)
            if (answering && guided) {
                DialogueBlock(
                    speaker = "따라 말할 문장",
                    line = reply.me,
                    listenText = if (stage == Stage.Answer) "예문 듣기" else null,
                    modifier = blockModifier,
                    playing = playing == reply.me,
                    highlight = if (playing == reply.me) highlight else -1,
                    showRoman = showRoman,
                    showTranslation = showTranslation,
                    onListen = { play(reply.me, Speaker.Model) },
                    onWord = { wordPick = WordPick(reply.me, it) },
                )
            } else {
                DialogueBlock(
                    speaker = "지우",
                    line = line,
                    listenText = "다시 듣기",
                    modifier = blockModifier,
                    playing = playing == line,
                    highlight = if (playing == line) highlight else -1,
                    showRoman = showRoman,
                    showTranslation = showTranslation,
                    onListen = { play(line, Speaker.Jiwoo) },
                    onWord = { wordPick = WordPick(line, it) },
                )
            }

            if (answering) {
                VoiceCard(
                    recording = stage == Stage.Recording,
                    guided = guided,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 85.dp + extra),
                    onMic = { withMic { stopVoice(); stage = Stage.Recording } },
                    onSend = { send() },
                )
            }

            Row(
                Modifier.align(Alignment.TopEnd).padding(top = 43.dp, end = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                DarkPill("표시", R.drawable.ic_talk_tune, width = 79) { showDisplay = true }
                DarkPill("대화 기록", R.drawable.ic_talk_log, width = 109) { stopVoice(); showHistory = true }
            }

            TalkNav(
                canPrev = answering || transcript.size > 1,
                canNext = canNext,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp + extra),
                nextText = if (stage == Stage.Closing) "마치기" else "다음",
                onPrev = {
                    stopVoice()
                    // 답하는 중이면 선택지로, 아니면 지난 대사를 다시 본다.
                    if (answering) stage = Stage.Question else pastStart = transcript.size - 2
                },
                onNext = { next() },
            )
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

        val listen: (Spoken) -> Unit = { play(it.line, if (it.isMe) Speaker.Me else Speaker.Jiwoo) }
        pastStart?.let { start ->
            PastLineSheet(
                transcript, start, playing, showRoman, showTranslation,
                onListen = listen,
                onWord = { l, i -> wordPick = WordPick(l, i) },
                onClose = { stopVoice(); pastStart = null },
            )
        }
        if (showHistory) {
            HistorySheet(
                script.place, transcript, playing, showRoman, showTranslation,
                onListen = listen,
                onWord = { l, i -> wordPick = WordPick(l, i) },
                onDisplay = { showDisplay = true },
                onClose = { stopVoice(); showHistory = false },
            )
        }
        when (val c = check) {
            is Check.Unclear -> UnclearSheet(c.heard, onKeep = { accept() }, onRetry = { check = null })
            is Check.Pronounce -> PronounceSheet(
                heard = c.heard,
                word = c.word,
                hint = c.hint,
                sentence = reply.me.ko,
                playing = playingWord,
                onListen = { playWord(c.word) },
                onMic = { withMic(it) },
                onBack = { stopVoice(); accept() },
            )
            null -> Unit
        }
        wordPick?.let { pick ->
            WordSheet(pick.line, pick.index, playingWord, onListen = { playWord(it) }, onClose = { stopVoice(); wordPick = null })
        }
        if (showDisplay) {
            DisplaySheet(showRoman, showTranslation, { showRoman = it }, { showTranslation = it }) { showDisplay = false }
        }
    }
}

/**
 * C · 장면만 보기. 마이크 없이 보거나 지난 대화를 다시 볼 때: 화면을 누르면 다음 말로 넘어간다.
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
            add(Spoken(false, script.closing) to Mood.Glad)
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
        SceneShades(shadeHeight = 315)
        PlainCaption(
            speaker = if (item.isMe) "나" else "지우",
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 27.dp, bottom = 43.dp + extra),
        ) {
            // 두 줄까지의 자리를 잡아 말한 사람의 위치가 흔들리지 않게 한다.
            Box(Modifier.padding(top = 15.dp).height(78.dp)) { CaptionText(item.line.ko) }
        }
    }
}
