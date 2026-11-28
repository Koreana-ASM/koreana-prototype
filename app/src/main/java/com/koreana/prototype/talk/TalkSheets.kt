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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.koreana.prototype.Line
import com.koreana.prototype.R
import com.koreana.prototype.Spoken
import com.koreana.prototype.Words
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

/** 문장 안의 [focus]만 밝게 보인다. */
private fun focused(sentence: String, focus: String) = buildAnnotatedString {
    val at = sentence.indexOf(focus)
    if (at < 0 || focus.isEmpty()) {
        append(sentence)
    } else {
        append(sentence.substring(0, at))
        withStyle(SpanStyle(color = TK.MintStrong)) { append(focus) }
        append(sentence.substring(at + focus.length))
    }
}

@Composable
private fun SheetLabel(text: String) {
    T(text, TK.text(13, FontWeight.Bold, 16), TK.Muted, maxLines = 1)
}

/**
 * 단어 시트(2단계): 어절 말풍선의 "더 보기"로 열린다. 짜임, 문장 속 위치, 지난 대화의 예문.
 * 노트에서는 같은 시트 아래에 내 목소리와 "다시 말해 보기"가 [more]로 붙는다.
 */
@Composable
fun WordSheet(
    word: String,
    sentence: Line?,
    saved: Boolean,
    playing: Boolean,
    onListen: (String) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    label: String = "대화 속 표현",
    roman: String = romanize(Words.bare(word)),
    meaning: String? = Words.info(word)?.meaning,
    more: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val extra = LocalBottomExtra.current
    val bare = Words.bare(word)
    val info = Words.info(word)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).plainClick(onClick = onClose)) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(TK.Sheet2)
                .plainClick { }
                .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 34.dp + extra),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(40.dp, 5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { SheetLabel(label) }
                Ic(
                    if (saved) R.drawable.ic_mark_fill else R.drawable.ic_mark, size = 20.dp, tint = TK.Text,
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onSave),
                )
                Spacer(Modifier.width(14.dp))
                Ic(R.drawable.ic_talk_close, size = 20.dp, tint = TK.Text, modifier = Modifier.clip(CircleShape).clickable(onClick = onClose))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    T(bare, TK.text(30, FontWeight.Bold, 36), TK.Text, maxLines = 1)
                    T(if (meaning.isNullOrEmpty()) roman else "$roman · $meaning", TK.text(15, FontWeight.Normal, 18), TK.Muted, maxLines = 1)
                }
                Row(
                    Modifier.clip(CircleShape).background(if (playing) TK.Divider else TK.Pill).border(1.dp, TK.ListenStroke, CircleShape)
                        .clickable { onListen(bare) }.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Ic(R.drawable.ic_sound, size = 13.dp, tint = TK.Text)
                    T(if (playing) "듣는 중" else "단어 듣기", TK.text(12, FontWeight.Medium, 15), TK.Text, maxLines = 1)
                }
            }
            if (info != null && info.parts.isNotEmpty()) {
                SheetLabel("짜임")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    info.parts.forEachIndexed { i, part ->
                        if (i > 0) T("+", TK.text(16, FontWeight.Bold, 19), TK.Muted)
                        Column(
                            Modifier.clip(RoundedCornerShape(12.dp)).background(TK.Pill).border(1.dp, TK.ListenStroke, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            T(part.ko, TK.text(15, FontWeight.Bold, 18), TK.Text, maxLines = 1)
                            T(part.en, TK.text(12, FontWeight.Normal, 15), TK.Muted, maxLines = 1)
                        }
                    }
                }
            }
            if (sentence != null) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(TK.SheetLine))
                SheetLabel("이 문장에서")
                T(focused(sentence.ko, bare), TK.text(16, FontWeight.Bold, 20), TK.Text)
                T(sentence.en, TK.text(13, FontWeight.Normal, 16), TK.Muted)
            }
            info?.example?.let { example ->
                SheetLabel("지난 대화에서")
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TK.Pill).border(1.dp, TK.ListenStroke, RoundedCornerShape(14.dp))
                        .clickable { onListen(example.ko) }.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        T(focused(example.ko, example.focus), TK.text(15, FontWeight.Bold, 18), TK.Text)
                        T("${example.en} · ${example.from}", TK.text(12, FontWeight.Normal, 15), TK.Muted, maxLines = 1)
                    }
                    Ic(R.drawable.ic_sound, size = 18.dp, tint = TK.Text)
                }
            }
            more?.invoke(this)
        }
    }
}
