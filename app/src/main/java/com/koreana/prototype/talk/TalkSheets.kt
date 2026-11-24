package com.koreana.prototype.talk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.koreana.prototype.Line
import com.koreana.prototype.R
import com.koreana.prototype.Spoken
import com.koreana.prototype.romanize
import com.koreana.prototype.ui.Ic
import com.koreana.prototype.ui.LocalBottomExtra
import com.koreana.prototype.ui.T
import com.koreana.prototype.ui.TK
import com.koreana.prototype.ui.plainClick

/** 시트 뒤의 어두운 바탕. 누르면 닫힌다. */
@Composable
fun SheetScrim(alpha: Float = 0.74f, onDismiss: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(TK.SheetScrim.copy(alpha = alpha)).plainClick(onClick = onDismiss), content = content)
}

/** 아래에서 올라오는 시트의 바탕과 손잡이. */
@Composable
private fun BoxScope.BottomSheet(radius: Dp = 26.dp, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val extra = LocalBottomExtra.current
    Column(
        modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = radius, topEnd = radius))
            .background(TK.Sheet)
            .plainClick { }
            .padding(bottom = extra),
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(42.dp, 4.dp).clip(CircleShape).background(TK.Handle))
        }
        content()
    }
}

@Composable
private fun RoundClose(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(40.dp).clip(CircleShape).background(TK.Pill).border(1.dp, TK.CloseStroke, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Ic(R.drawable.ic_talk_close, size = 18.dp) }
}

@Composable
private fun PlainClose(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Ic(R.drawable.ic_talk_close, size = 20.dp)
    }
}

@Composable
private fun Switch(on: Boolean) {
    Box(
        Modifier.size(48.dp, 27.dp).clip(CircleShape).background(if (on) TK.Mint else TK.SwitchOff).padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) { Box(Modifier.size(21.dp).clip(CircleShape).background(if (on) TK.OnMint else TK.SwitchKnobOff)) }
}

/** 대사 표시 설정: 로마자 발음과 모국어 번역을 켜고 끈다. */
@Composable
fun DisplaySheet(
    showRoman: Boolean,
    showTranslation: Boolean,
    onRoman: (Boolean) -> Unit,
    onTranslation: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    SheetScrim(onDismiss = onClose) {
        BottomSheet {
            Row(Modifier.fillMaxWidth().padding(start = 23.dp, end = 21.dp, top = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                T("대사 표시", TK.text(24, FontWeight.Bold, 33), TK.Text, Modifier.weight(1f))
                RoundClose(onClick = onClose)
            }
            Spacer(Modifier.height(15.dp))
            SettingRow("로마자 발음", "한글 위에 읽는 법을 표시해요.", showRoman) { onRoman(!showRoman) }
            Spacer(Modifier.height(11.dp))
            SettingRow("모국어 번역", "English · 한국어 아래에 표시해요.", showTranslation) { onTranslation(!showTranslation) }
            Spacer(Modifier.height(10.dp))
            MintPill("완료", width = 134, modifier = Modifier.align(Alignment.CenterHorizontally), onClick = onClose)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingRow(title: String, sub: String, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 18.dp).fillMaxWidth().height(71.dp).clip(RoundedCornerShape(13.dp)).background(TK.Pill)
            .clickable(onClick = onClick).padding(start = 16.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            T(title, TK.text(16, FontWeight.Bold, 22), TK.Text)
            T(sub, TK.text(13, FontWeight.Normal, 18), TK.Muted)
        }
        Switch(on)
    }
}

/** 기록과 지난 대사에서 쓰는 한 줄. 내 말은 "내 녹음"을 듣는다. */
@Composable
private fun SpokenBlock(
    item: Spoken,
    playing: Boolean,
    showRoman: Boolean,
    showTranslation: Boolean,
    onListen: () -> Unit,
    onWord: (Int) -> Unit,
) {
    DialogueBlock(
        speaker = if (item.isMe) "나" else "지우",
        line = item.line,
        listenText = if (item.isMe) "내 녹음" else "다시 듣기",
        speakerColor = if (item.isMe) TK.Me else TK.Mint,
        listenIcon = if (item.isMe) R.drawable.ic_talk_mic_small else R.drawable.ic_talk_speaker,
        playing = playing,
        showRoman = showRoman,
        showTranslation = showTranslation,
        onListen = onListen,
        onWord = onWord,
    )
}

/** 대화 기록: 이번 대화에서 지금까지 나눈 말. */
@Composable
fun HistorySheet(
    place: String,
    items: List<Spoken>,
    playing: Line?,
    showRoman: Boolean,
    showTranslation: Boolean,
    onListen: (Spoken) -> Unit,
    onWord: (Line, Int) -> Unit,
    onDisplay: () -> Unit,
    onClose: () -> Unit,
) {
    val scroll = rememberScrollState()
    LaunchedEffect(items.size) { scroll.scrollTo(scroll.maxValue) }
    SheetScrim(alpha = 0.52f, onDismiss = onClose) {
        BottomSheet(radius = 23.dp, modifier = Modifier.padding(top = 52.dp).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 21.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                T("이번 대화", TK.text(24, FontWeight.Bold, 33), TK.Text, Modifier.weight(1f))
                DarkPill("표시", R.drawable.ic_talk_tune, width = 81, onClick = onDisplay)
                Spacer(Modifier.width(22.dp))
                RoundClose(onClick = onClose)
            }
            T("${place}에서 지우와 나눈 말", TK.text(14, FontWeight.Medium, 19), TK.Muted, Modifier.padding(start = 24.dp, top = 2.dp))
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(TK.Divider))
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(vertical = 15.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items.forEach { item ->
                    Box(
                        Modifier
                            .padding(start = if (item.isMe) 44.dp else 13.dp)
                            .width(333.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (item.isMe) TK.MeCard else TK.JiwooCard)
                            .padding(start = 9.dp, end = 10.dp, top = 5.dp, bottom = 10.dp),
                    ) {
                        SpokenBlock(item, playing == item.line, showRoman, showTranslation, { onListen(item) }) { onWord(item.line, it) }
                    }
                }
            }
            MintPill("대화로 돌아가기", width = 208, height = 45, modifier = Modifier.align(Alignment.CenterHorizontally), onClick = onClose)
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 지난 대사: "이전"을 누르면 앞서 나눈 말을 한 줄씩 다시 본다. 마지막에서 "다음"을 누르면 닫힌다. */
@Composable
fun PastLineSheet(
    items: List<Spoken>,
    start: Int,
    playing: Line?,
    showRoman: Boolean,
    showTranslation: Boolean,
    onListen: (Spoken) -> Unit,
    onWord: (Line, Int) -> Unit,
    onClose: () -> Unit,
) {
    val extra = LocalBottomExtra.current
    var index by remember { mutableIntStateOf(start.coerceIn(0, items.lastIndex)) }
    val item = items[index]
    SheetScrim(onDismiss = onClose) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp + extra)
                .size(364.dp, 351.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(TK.Sheet)
                .border(1.dp, TK.PillStroke, RoundedCornerShape(22.dp))
                .plainClick { },
        ) {
            T("지난 대사", TK.text(14, FontWeight.Bold, 19), TK.Mint, Modifier.padding(start = 19.dp, top = 16.dp))
            RoundClose(Modifier.align(Alignment.TopEnd).padding(top = 11.dp, end = 13.dp), onClose)
            Box(Modifier.padding(start = 23.dp, top = 61.dp)) {
                SpokenBlock(item, playing == item.line, showRoman, showTranslation, { onListen(item) }) { onWord(item.line, it) }
            }
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(59.dp),
            ) {
                DarkPill("이전", R.drawable.ic_talk_prev, width = 112, enabled = index > 0) { index -= 1 }
                MintPill("다음", width = 112, arrow = true) { if (index < items.lastIndex - 1) index += 1 else onClose() }
            }
        }
    }
}

/** 예시로 뜻을 넣어 둔 표현 (편의점 대화). 없는 표현은 로마자와 문장만 보여준다. */
private val glossary = mapOf(
    "여기서" to "here", "저녁" to "dinner", "먹고" to "eat", "갈까요" to "shall we go", "저는" to "I", "따뜻한" to "warm",
    "게" to "something", "당기네요" to "feel like having", "오늘은" to "today", "뭐" to "what", "싶어요" to "would like to",
    "라면" to "ramyeon", "좋죠" to "sounds good", "그럼" to "then", "제가" to "I", "뜨거운" to "hot", "물" to "water",
    "받아" to "get", "올게요" to "I'll be back", "저기" to "over there", "자리가" to "a seat", "있네요" to "there is",
    "같이" to "together", "앉아요" to "let's sit", "삼각김밥" to "a triangular rice ball",
)

private fun bare(word: String) = word.filter { it.isLetterOrDigit() }

/** 대화 속 표현: 어절을 누르면 읽는 법과 뜻을 보고, 문장 안의 다른 어절로 옮겨 갈 수 있다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordSheet(line: Line, start: Int, playing: Boolean, onListen: (String) -> Unit, onClose: () -> Unit) {
    var index by remember(line) { mutableIntStateOf(start.coerceIn(0, line.words.lastIndex)) }
    val word = bare(line.words[index])
    SheetScrim(alpha = 0.52f, onDismiss = onClose) {
        BottomSheet {
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 17.dp), verticalAlignment = Alignment.CenterVertically) {
                T("대화 속 표현", TK.text(14, FontWeight.Bold, 19), TK.Mint, Modifier.weight(1f))
                PlainClose(onClick = onClose)
            }
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 43.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    T(word, TK.text(28, FontWeight.Bold, 40), TK.Text)
                    T(romanize(word), TK.text(17, FontWeight.Normal, 24), TK.Mint)
                }
                ListenPill("단어 듣기", R.drawable.ic_talk_speaker, playing, Modifier.width(105.dp)) { onListen(word) }
            }
            glossary[word]?.let { T(it, TK.text(17, FontWeight.Normal, 24), TK.Text, Modifier.padding(start = 25.dp, top = 11.dp)) }
            Spacer(Modifier.height(18.dp))
            Box(Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(1.dp).background(TK.Divider))
            Spacer(Modifier.height(14.dp))
            FlowRow(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                line.words.forEachIndexed { i, w ->
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp)).background(if (i == index) TK.Divider else TK.Sheet)
                            .clickable { index = i }.padding(horizontal = 2.dp),
                    ) { T(w, TK.text(17, FontWeight.Bold, 26), TK.Text, maxLines = 1) }
                }
            }
            T(line.en, TK.text(16, FontWeight.Normal, 24), TK.SwitchKnobOff, Modifier.padding(start = 26.dp, end = 24.dp, top = 10.dp))
            Spacer(Modifier.height(48.dp))
        }
    }
}

/** 확인 시트의 공통 머리: 작은 제목, 닫기, 큰 질문. */
@Composable
private fun CheckHeader(label: String, title: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 26.dp, end = 24.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        T(label, TK.text(14, FontWeight.Bold, 19), TK.Mint, Modifier.weight(1f))
        PlainClose(onClick = onClose)
    }
    T(title, TK.text(24, FontWeight.Bold, 33), TK.Text, Modifier.padding(start = 25.dp, top = 4.dp))
}

@Composable
private fun TwoButtons(left: String, right: String, onLeft: () -> Unit, onRight: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinePill(left, Modifier.width(153.dp), onLeft)
        Box(
            Modifier.weight(1f).height(42.dp).clip(CircleShape).background(TK.Mint).clickable(onClick = onRight),
            contentAlignment = Alignment.Center,
        ) { T(right, TK.text(15, FontWeight.Bold, 21), TK.OnMint, maxLines = 1) }
    }
}

/** 자유 답변에서 일부가 잘 안 들렸을 때: 들린 대로 이어가거나 다시 말한다. 틀렸다고 하지 않는다. */
@Composable
fun UnclearSheet(heard: String, onKeep: () -> Unit, onRetry: () -> Unit) {
    SheetScrim(alpha = 0.52f, onDismiss = onKeep) {
        BottomSheet {
            CheckHeader("내가 보낸 말", "어떤 말이었나요?", onKeep)
            T("이렇게 들렸어요", TK.text(14, FontWeight.Medium, 19), TK.Mint, Modifier.padding(start = 27.dp, top = 22.dp))
            T(heard, TK.text(20, FontWeight.Medium, 29), TK.Text, Modifier.padding(start = 27.dp, top = 12.dp))
            T("일부 소리가 잘 들리지 않았어요.", TK.text(14, FontWeight.Medium, 19), TK.Mint, Modifier.padding(start = 27.dp, top = 40.dp))
            Spacer(Modifier.height(60.dp))
            TwoButtons("이대로 대화하기", "다시 말하기", onKeep, onRetry)
            Spacer(Modifier.height(20.dp))
        }
    }
}

private enum class Practice { Compare, Ready, Recording, Done }

/**
 * 발음 확인과 단어 연습: 추천 문장에서 한 단어가 다르게 들렸을 때 그 단어만 따로 말해 본다.
 * [heard]는 들린 말, [word]는 연습할 단어, [sentence]는 그 단어가 있던 문장.
 */
@Composable
fun PronounceSheet(
    heard: String,
    word: String,
    hint: String,
    sentence: String,
    playing: Boolean,
    onListen: () -> Unit,
    onMic: (onGranted: () -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    var stage by remember { mutableStateOf(Practice.Compare) }
    val syllables = word.toList().joinToString("·")
    SheetScrim(alpha = 0.52f, onDismiss = onBack) {
        BottomSheet {
            if (stage == Practice.Compare) {
                CheckHeader("내가 보낸 말", "이 단어를 확인해 볼까요?", onBack)
                Row(
                    Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp).fillMaxWidth().height(94.dp)
                        .clip(RoundedCornerShape(13.dp)).background(TK.Pill).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        T("이렇게 들렸어요", TK.text(12, FontWeight.Medium, 17), TK.Muted)
                        T(heard, TK.text(24, FontWeight.Bold, 33), TK.Recording, maxLines = 1)
                    }
                    Ic(R.drawable.ic_talk_chevron, size = 12.dp, modifier = Modifier.padding(horizontal = 12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        T("연습할 단어", TK.text(12, FontWeight.Medium, 17), TK.Muted)
                        T(word, TK.text(24, FontWeight.Bold, 33), TK.Text, maxLines = 1)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(start = 27.dp, end = 26.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    T(syllables, TK.text(17, FontWeight.Medium, 24), TK.Text, Modifier.weight(1f))
                    ListenPill("다시 듣기", R.drawable.ic_talk_speaker, playing, onClick = onListen)
                }
                T(hint, TK.text(14, FontWeight.Normal, 20), TK.Muted, Modifier.padding(start = 27.dp, end = 27.dp, top = 14.dp))
                Spacer(Modifier.height(22.dp))
                TwoButtons("대화로 돌아가기", "이 단어 연습", onBack) { stage = Practice.Ready }
            } else {
                Row(Modifier.fillMaxWidth().padding(start = 26.dp, end = 24.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    T("단어 연습", TK.text(14, FontWeight.Bold, 19), TK.Mint, Modifier.weight(1f))
                    PlainClose(onClick = onBack)
                }
                T(word, TK.text(40, FontWeight.Bold, 56), TK.Text, Modifier.padding(start = 26.dp, top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(start = 27.dp, end = 26.dp).height(34.dp), verticalAlignment = Alignment.CenterVertically) {
                    T(syllables, TK.text(17, FontWeight.Medium, 24), TK.Mint, Modifier.weight(1f))
                    if (stage == Practice.Ready) ListenPill("다시 듣기", R.drawable.ic_talk_speaker, playing, onClick = onListen)
                }
                T(sentence, TK.text(16, FontWeight.Medium, 23), TK.Text, Modifier.padding(start = 27.dp, top = 26.dp))
                Box(Modifier.padding(start = 27.dp, top = 28.dp).height(28.dp), contentAlignment = Alignment.CenterStart) {
                    when (stage) {
                        Practice.Recording -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Waveform(color = TK.Recording, bars = 5)
                            T("녹음 중", TK.text(14, FontWeight.Bold, 20), TK.Recording)
                        }
                        Practice.Done -> T("한 번 더 말해 봤어요.", TK.text(14, FontWeight.Normal, 20), TK.Muted)
                        else -> T("마이크를 누르고 이 단어만 말해 보세요.", TK.text(14, FontWeight.Normal, 20), TK.Muted)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(48.dp).clip(CircleShape).background(TK.Mint).clickable {
                        when (stage) {
                            Practice.Ready -> onMic { stage = Practice.Recording }
                            Practice.Recording -> stage = Practice.Done
                            else -> onBack()
                        }
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    T(
                        when (stage) {
                            Practice.Ready -> "마이크 켜기"
                            Practice.Recording -> "보내기"
                            else -> "대화로 돌아가기"
                        },
                        TK.text(15, FontWeight.Bold, 21), TK.OnMint,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}
