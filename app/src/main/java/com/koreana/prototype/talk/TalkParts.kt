package com.koreana.prototype.talk

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.koreana.prototype.Line
import com.koreana.prototype.R
import com.koreana.prototype.romanize
import com.koreana.prototype.ui.Ic
import com.koreana.prototype.ui.T
import com.koreana.prototype.ui.TK
import com.koreana.prototype.ui.plainClick

/** 지우의 표정. 영상 대신 표정 그림을 돌려 쓴다 (C′). */
enum class Mood(@DrawableRes val image: Int) {
    Neutral(R.drawable.img_scene),
    Smile(R.drawable.img_scene_smile),
    Glad(R.drawable.img_scene_glad),
}

/** 장면 그림 위의 명암. [shadeHeight]는 아래쪽 대사 배경의 높이, [strong]은 선택지가 떠 있을 때. */
@Composable
fun SceneShades(shadeHeight: Int = 422, strong: Boolean = false) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxWidth().height(135.dp)
                .background(Brush.verticalGradient(listOf(TK.Scene.copy(alpha = 0.49f), TK.Scene.copy(alpha = 0f)))),
        )
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(shadeHeight.dp).background(
                Brush.verticalGradient(
                    0f to TK.Scene.copy(alpha = 0f),
                    0.28f to TK.Scene.copy(alpha = if (strong) 0.58f else 0.24f),
                    0.59f to TK.Scene.copy(alpha = if (strong) 0.94f else 0.86f),
                    1f to TK.Scene.copy(alpha = 0.99f),
                ),
            ),
        )
    }
}

/** 짙은 알약 버튼 (표시, 대화 기록, 이전). */
@Composable
fun DarkPill(
    text: String,
    @DrawableRes icon: Int,
    width: Int,
    modifier: Modifier = Modifier,
    height: Int = 42,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.34f)
            .size(width.dp, height.dp)
            .clip(CircleShape)
            .background(TK.Pill)
            .border(1.dp, TK.PillStroke, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Ic(icon, size = 17.dp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp))
        T(
            text, TK.text(14, FontWeight.Medium, 19), TK.Text,
            Modifier.align(Alignment.Center).padding(start = 26.dp), maxLines = 1,
        )
    }
}

/** 밝은 알약 버튼 (다음, 마치기, 완료, 대화로 돌아가기). [arrow]가 있으면 오른쪽에 화살표. */
@Composable
fun MintPill(
    text: String,
    width: Int,
    modifier: Modifier = Modifier,
    height: Int = 42,
    arrow: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.34f)
            .size(width.dp, height.dp)
            .clip(CircleShape)
            .background(TK.Mint)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        T(
            text, TK.text(15, FontWeight.Bold, 21), TK.OnMint,
            Modifier.align(Alignment.Center).padding(end = if (arrow) 18.dp else 0.dp), maxLines = 1,
        )
        if (arrow) Ic(R.drawable.ic_talk_next, size = 17.dp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 11.dp))
    }
}

/** 테두리만 있는 알약 버튼 (시트의 보조 행동). */
@Composable
fun OutlinePill(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(42.dp).clip(CircleShape).background(TK.Pill).border(1.dp, TK.PillStroke, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { T(text, TK.text(14, FontWeight.Bold, 19), TK.Text, maxLines = 1) }
}

/** 대사 옆의 작은 듣기 버튼 (다시 듣기, 예문 듣기, 내 녹음, 단어 듣기). 재생 중에는 "듣는 중"으로 바뀐다. */
@Composable
fun ListenPill(text: String, @DrawableRes icon: Int, playing: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(101.dp, 28.dp)
            .clip(CircleShape)
            .background(if (playing) TK.Divider else TK.Pill)
            .border(1.dp, TK.ListenStroke, CircleShape)
            .clickable(onClick = onClick),
    ) {
        if (playing) {
            Box(Modifier.align(Alignment.CenterStart).padding(start = 13.dp).size(8.dp).clip(RoundedCornerShape(1.5.dp)).background(TK.Mint))
        } else {
            Ic(icon, size = 15.dp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp))
        }
        T(
            if (playing) "듣는 중" else text, TK.text(12, FontWeight.Medium, 17), TK.Text,
            Modifier.align(Alignment.Center).padding(start = 25.dp), maxLines = 1,
        )
    }
}

/**
 * 대사 한 줄: 말한 사람, 듣기 버튼, 어절(위에 로마자), 모국어 번역.
 * 어절을 누르면 [onWord]로 그 표현을 살펴본다. [highlight]는 지금 소리 나는 어절.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogueBlock(
    speaker: String,
    line: Line,
    listenText: String?,
    modifier: Modifier = Modifier,
    speakerColor: Color = TK.Mint,
    @DrawableRes listenIcon: Int = R.drawable.ic_talk_speaker,
    playing: Boolean = false,
    highlight: Int = -1,
    showRoman: Boolean = true,
    showTranslation: Boolean = true,
    onListen: () -> Unit = {},
    onWord: ((Int) -> Unit)? = null,
) {
    Column(modifier.width(314.dp)) {
        Box(Modifier.fillMaxWidth().height(36.dp)) {
            T(speaker, TK.text(15, FontWeight.Bold, 21), speakerColor, Modifier.align(Alignment.CenterStart).offset(y = (-2).dp))
            if (listenText != null) {
                ListenPill(listenText, listenIcon, playing, Modifier.align(Alignment.CenterEnd).offset(y = 2.dp), onListen)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            line.words.forEachIndexed { i, word ->
                Column(
                    Modifier
                        .then(if (onWord != null) Modifier.plainClick { onWord(i) } else Modifier)
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (showRoman) T(romanize(word), TK.text(12, FontWeight.Normal, 17), TK.Muted, maxLines = 1)
                    T(word, TK.text(18, FontWeight.Bold, 25), if (i == highlight) TK.Mint else TK.Text, maxLines = 1)
                }
            }
        }
        if (showTranslation) {
            Spacer(Modifier.height(10.dp))
            T(line.en, TK.text(14, FontWeight.Normal, 19), TK.Muted, Modifier.padding(bottom = 6.dp))
        }
    }
}

/** 내 차례의 선택지 (내 말로 답하기 / 추천 문장). 긴 추천 문장은 표시를 문장 위로 올린다. */
@Composable
fun ChoiceBlock(text: String, @DrawableRes icon: Int, modifier: Modifier = Modifier, suggested: Boolean = false, onClick: () -> Unit) {
    val tall = suggested && text.length > 10
    Box(
        modifier
            .size(336.dp, if (tall) 76.dp else 54.dp)
            .shadow(14.dp, RoundedCornerShape(8.dp), ambientColor = Color(0x38061714), spotColor = Color(0x38061714))
            .plainClick(onClick = onClick),
    ) {
        Image(painterResource(R.drawable.bg_talk_choice), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        Ic(icon, size = 20.dp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp))
        if (tall) {
            Column(Modifier.align(Alignment.CenterStart).padding(start = 61.dp, end = 44.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SuggestedTag()
                T(text, TK.text(16, FontWeight.Bold, 24), TK.Choice, maxLines = 1)
            }
        } else {
            Row(
                Modifier.align(Alignment.CenterStart).padding(start = 61.dp, end = 47.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                T(text, TK.text(18, FontWeight.Bold, 27), TK.Choice, Modifier.weight(1f), maxLines = 1)
                if (suggested) SuggestedTag()
            }
        }
        Ic(R.drawable.ic_talk_chevron, size = 10.dp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp))
    }
}

@Composable
private fun SuggestedTag() {
    Box(Modifier.size(66.dp, 24.dp).clip(CircleShape).background(TK.ChoiceTag), contentAlignment = Alignment.Center) {
        T("추천 문장", TK.text(12, FontWeight.Bold, 17), Color(0xFF17382F), maxLines = 1)
    }
}

private val waveHeights = listOf(6, 12, 19, 10, 24, 13, 28, 16, 8, 21, 13, 23, 8, 18, 11, 15, 6)

/** 녹음 중 음성 파형. 막대가 번갈아 출렁인다. */
@Composable
fun Waveform(modifier: Modifier = Modifier, color: Color = TK.MintStrong, bars: Int = waveHeights.size) {
    val t = rememberInfiniteTransition(label = "wave")
    val a by t.animateFloat(0.45f, 1f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "a")
    val b by t.animateFloat(1f, 0.5f, infiniteRepeatable(tween(560), RepeatMode.Reverse), label = "b")
    Row(modifier.height(28.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        waveHeights.take(bars).forEachIndexed { i, h ->
            Box(
                Modifier.size(3.dp, h.dp).graphicsLayer { scaleY = if (i % 2 == 0) a else b }
                    .clip(RoundedCornerShape(1.5.dp)).background(color),
            )
        }
    }
}

/**
 * 음성 입력 카드. 기다릴 때는 마이크, 녹음 중에는 보내기 버튼이 오른쪽에 온다.
 * [guided]는 추천 문장을 따라 말할 때: 안내 문구가 달라진다.
 */
@Composable
fun VoiceCard(recording: Boolean, guided: Boolean, modifier: Modifier = Modifier, onMic: () -> Unit, onSend: () -> Unit) {
    Box(
        modifier
            .size(342.dp, 96.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(TK.InputCard)
            .border(1.dp, TK.InputStroke, RoundedCornerShape(22.dp)),
    ) {
        if (recording) {
            Row(Modifier.padding(start = 20.dp, top = 14.dp).height(28.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(TK.Recording))
                Spacer(Modifier.width(8.dp))
                T("녹음 중", TK.text(14, FontWeight.Bold, 20), TK.TextBright, maxLines = 1)
                if (guided) Waveform(Modifier.padding(start = 28.dp))
            }
            if (guided) {
                T("말을 마치면 보내세요", TK.text(15, FontWeight.Medium, 21), TK.TextBright, Modifier.padding(start = 19.dp, top = 52.dp), maxLines = 1)
            } else {
                Waveform(Modifier.padding(start = 20.dp, top = 50.dp))
            }
            Row(
                Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).size(91.dp, 52.dp).clip(CircleShape)
                    .background(TK.MintStrong).clickable(onClick = onSend),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                T("보내기", TK.text(16, FontWeight.Bold, 23), Color(0xFF17382F), maxLines = 1)
                Spacer(Modifier.width(3.dp))
                Ic(R.drawable.ic_talk_send, size = 13.dp)
            }
        } else {
            if (guided) {
                T(
                    "마이크를 눌러 따라 말하세요", TK.text(17, FontWeight.Bold, 25), TK.TextBright,
                    Modifier.align(Alignment.CenterStart).padding(start = 19.dp), maxLines = 1,
                )
            } else {
                Column(Modifier.padding(start = 20.dp, top = 17.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    T("내 답변", TK.text(13, FontWeight.Medium, 19), Color(0xFFB9CEBF), maxLines = 1)
                    T("마이크를 눌러 말해 보세요", TK.text(17, FontWeight.Bold, 25), TK.TextBright, maxLines = 1)
                }
            }
            Box(
                Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).size(62.dp).clip(CircleShape)
                    .background(TK.MintStrong).clickable(onClick = onMic),
                contentAlignment = Alignment.Center,
            ) { Ic(R.drawable.ic_talk_mic, size = 22.dp) }
        }
    }
}

/** 아래쪽 대사 이동: 이전 / 다음. */
@Composable
fun TalkNav(
    canPrev: Boolean,
    canNext: Boolean,
    modifier: Modifier = Modifier,
    nextText: String = "다음",
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(36.dp)) {
        DarkPill("이전", R.drawable.ic_talk_prev, width = 119, height = 44, enabled = canPrev, onClick = onPrev)
        MintPill(nextText, width = 121, height = 44, arrow = true, enabled = canNext, onClick = onNext)
    }
}

/** 장면만 보기와 톤 맞추기에 쓰는 단순한 대사: 말한 사람, 구분선, 문장. */
@Composable
fun PlainCaption(
    speaker: String,
    modifier: Modifier = Modifier,
    speakerColor: Color = TK.Mint,
    action: (@Composable () -> Unit)? = null,
    text: @Composable () -> Unit,
) {
    Column(modifier.width(336.dp)) {
        Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
            T(speaker, TK.text(17, FontWeight.Bold, 24), speakerColor, Modifier.weight(1f))
            action?.invoke()
        }
        Spacer(Modifier.height(9.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.28f)))
        text()
    }
}

@Composable
fun CaptionText(text: String, modifier: Modifier = Modifier) {
    T(text, TK.text(20, FontWeight.Bold, 29), TK.TextBright, modifier, align = TextAlign.Start)
}
