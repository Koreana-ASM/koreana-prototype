package com.koreana.prototype.talk

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.koreana.prototype.Line
import com.koreana.prototype.MarkLevel
import com.koreana.prototype.R
import com.koreana.prototype.romanize
import com.koreana.prototype.ui.Ic
import com.koreana.prototype.ui.T
import com.koreana.prototype.ui.TK
import com.koreana.prototype.ui.plainClick

/** 지우의 표정. 영상 대신 표정 그림을 돌려 쓴다. */
enum class Mood(@DrawableRes val image: Int) {
    Neutral(R.drawable.img_scene),
    Smile(R.drawable.img_scene_smile),
    Glad(R.drawable.img_scene_glad),
}

/** 장면 그림 위의 명암. [strong]은 내 차례처럼 대화창 위에 무언가 떠 있을 때. */
@Composable
fun SceneShades(strong: Boolean = false) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxWidth().height(135.dp)
                .background(Brush.verticalGradient(listOf(TK.Scene.copy(alpha = 0.49f), TK.Scene.copy(alpha = 0f)))),
        )
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(if (strong) 494.dp else 422.dp).background(
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

/** 동그란 짙은 버튼 (표시, 대화 기록, 다시 고르기, 취소). */
@Composable
fun RoundButton(@DrawableRes icon: Int, modifier: Modifier = Modifier, size: Dp = 40.dp, iconSize: Dp = 17.dp, solid: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier.size(size).clip(CircleShape).background(if (solid) TK.Pill else TK.Round)
            .border(1.dp, TK.ListenStroke, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Ic(icon, size = iconSize, tint = TK.Text) }
}

/** 짙은 알약 버튼 (시트 안의 표시). */
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

/** 밝은 알약 버튼 (완료, 대화로 돌아가기). */
@Composable
fun MintPill(text: String, width: Int, modifier: Modifier = Modifier, height: Int = 42, onClick: () -> Unit) {
    Box(
        modifier.size(width.dp, height.dp).clip(CircleShape).background(TK.Mint).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { T(text, TK.text(15, FontWeight.Bold, 21), TK.OnMint, maxLines = 1) }
}

/** 화면 아래의 큰 밝은 버튼 (이어서 하기, 다시 말해 보기). */
@Composable
fun MintButton(text: String, modifier: Modifier = Modifier, height: Dp = 52.dp, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().height(height).clip(CircleShape).background(TK.MintStrong).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { T(text, TK.text(16, FontWeight.Bold, 23), TK.OnMint, maxLines = 1) }
}

/** 대사 옆의 작은 듣기 버튼 (대화 기록, 단어 듣기). 재생 중에는 "듣는 중"으로 바뀐다. */
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
 * 대화 기록의 대사 한 줄: 말한 사람, 듣기 버튼, 어절(위에 로마자), 모국어 번역.
 * 어절을 누르면 [onWord]로 그 표현을 살펴본다.
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
                    T(word, TK.text(18, FontWeight.Bold, 25), TK.Text, maxLines = 1)
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

val waveHeights = listOf(6, 12, 19, 10, 24, 13, 28, 16, 8, 21, 13, 23, 8, 18, 11, 15)

/** 음성 파형. [live]이면 막대가 번갈아 출렁이고, [played]까지는 진하게 나머지는 흐리게 보인다. */
@Composable
fun Waveform(
    modifier: Modifier = Modifier,
    color: Color = TK.MintStrong,
    heights: List<Int> = waveHeights,
    gap: Dp = 2.dp,
    live: Boolean = true,
    played: Float = 1f,
) {
    val a: Float
    val b: Float
    if (live) {
        val t = rememberInfiniteTransition(label = "wave")
        val a0 by t.animateFloat(0.45f, 1f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "a")
        val b0 by t.animateFloat(1f, 0.5f, infiniteRepeatable(tween(560), RepeatMode.Reverse), label = "b")
        a = a0; b = b0
    } else {
        a = 1f; b = 1f
    }
    Row(modifier.height(28.dp), horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
        heights.forEachIndexed { i, h ->
            val on = (i + 0.5f) / heights.size <= played
            Box(
                Modifier.size(3.dp, h.dp).graphicsLayer { scaleY = if (i % 2 == 0) a else b }
                    .clip(RoundedCornerShape(2.dp)).background(color.copy(alpha = if (on) 1f else 0.3f)),
            )
        }
    }
}

/** 대화창의 어절 아래 표시: 누를 수 있으면 점선, 보내기 전의 발음 표시는 색 실선. */
enum class Under { None, Dots, Fair, Poor }

fun MarkLevel.under() = if (this == MarkLevel.Fair) Under.Fair else Under.Poor
fun MarkLevel.color() = if (this == MarkLevel.Fair) TK.Fair else TK.Poor

/**
 * 화면 맨 아래에 고정된 대화창: 말한 사람(옆에 🔊), 어절(위에 로마자), 번역.
 * [shown]은 보여 줄 어절 수(자유 답변에서 들린 말이 쌓일 때), [bright]는 진하게 보일 어절 수(따라 말한 만큼).
 * [placeholder]는 아직 한 말이 없는 빈 내 차례, [lowered]는 보낸 뒤 내 대사로 나타날 때.
 * 어절을 누르면 [onWord]에 그 어절의 자리(화면 기준)를 함께 넘긴다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TalkBox(
    speaker: String,
    line: Line?,
    modifier: Modifier = Modifier,
    onListen: (() -> Unit)? = null,
    showRoman: Boolean = true,
    showTranslation: Boolean = true,
    shown: Int = Int.MAX_VALUE,
    bright: Int = Int.MAX_VALUE,
    placeholder: Boolean = false,
    lowered: Boolean = false,
    selected: Int = -1,
    highlight: Int = -1,
    under: (Int) -> Under = { Under.None },
    onWord: ((index: Int, bounds: Rect) -> Unit)? = null,
) {
    Column(modifier.width(336.dp).height(160.dp)) {
        Row(
            Modifier.height(if (lowered) 30.dp else 18.dp).padding(top = if (lowered) 6.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            T(speaker, TK.text(15, FontWeight.Bold, 18), TK.Mint, maxLines = 1)
            if (onListen != null) {
                Ic(R.drawable.ic_sound, size = 18.dp, tint = TK.Mint, modifier = Modifier.clip(CircleShape).clickable(onClick = onListen))
            }
        }
        Spacer(Modifier.height(6.dp))
        if (placeholder || line == null) {
            T("···", TK.text(18, FontWeight.Bold, 22), TK.Muted.copy(alpha = 0.6f))
            return@Column
        }
        val words = line.words
        val coords = remember(line) { arrayOfNulls<LayoutCoordinates>(words.size) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            words.take(shown).forEachIndexed { i, word ->
                val u = under(i)
                val tone = if (i < bright) 1f else 0.3f
                val wordColor = when {
                    u == Under.Fair -> TK.Fair
                    u == Under.Poor -> TK.Poor
                    i == highlight -> TK.Mint
                    else -> TK.Text
                }
                Column(
                    Modifier
                        .onGloballyPositioned { coords[i] = it }
                        .then(
                            if (onWord != null) Modifier.plainClick { coords[i]?.takeIf { it.isAttached }?.let { onWord(i, it.boundsInRoot()) } }
                            else Modifier,
                        )
                        .then(
                            if (i == selected || (i == line.emphasis && selected < 0)) {
                                Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.16f)).padding(horizontal = 4.dp)
                            } else Modifier,
                        )
                        .drawBehind {
                            if (u == Under.None) return@drawBehind
                            val y = size.height - 0.75.dp.toPx()
                            val w = 1.5.dp.toPx()
                            when (u) {
                                Under.Dots -> drawLine(
                                    TK.Dots, Offset(w / 2, y), Offset(size.width - w / 2, y), w, StrokeCap.Round,
                                    PathEffect.dashPathEffect(floatArrayOf(1.2.dp.toPx(), 3.65.dp.toPx())),
                                )
                                else -> drawLine(if (u == Under.Fair) TK.Fair else TK.Poor, Offset(w / 2, y), Offset(size.width - w / 2, y), w, StrokeCap.Round)
                            }
                        }
                        .padding(bottom = 1.5.dp),
                ) {
                    if (showRoman) T(romanize(word), TK.text(12, FontWeight.Normal, 14), TK.Muted.copy(alpha = tone), maxLines = 1)
                    Spacer(Modifier.height(1.dp))
                    T(word, TK.text(18, FontWeight.Bold, 22), wordColor.copy(alpha = tone), maxLines = 1)
                }
            }
        }
        if (showTranslation) {
            Spacer(Modifier.height(6.dp))
            T(line.en, TK.text(14, FontWeight.Normal, 17), TK.Muted)
        }
    }
}

/** 넘김 표시(▼): 화면을 누르면 넘어간다. */
@Composable
fun AdvanceMark(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "advance")
    val dy by t.animateFloat(0f, 3f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "dy")
    Triangle(TK.MintStrong, modifier.size(10.4.dp, 6.dp).graphicsLayer { translationY = dy.dp.toPx() })
}

/** 아래를 향한 삼각형 (넘김 표시, 말풍선 꼬리, 이름표 꼬리). */
@Composable
fun Triangle(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val p = Path().apply {
            moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2f, size.height); close()
        }
        drawPath(p, color)
    }
}

/** 내 차례의 큰 마이크 버튼. */
@Composable
fun MicButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(66.dp).shadow(10.dp, CircleShape, ambientColor = Color(0x40061714), spotColor = Color(0x40061714))
            .clip(CircleShape).background(TK.MintStrong).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Ic(R.drawable.ic_mic, size = 28.dp, tint = TK.OnMint) }
}

/** 녹음을 바로 끝내는 ■ 버튼. */
@Composable
fun StopButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(44.dp).clip(CircleShape).background(TK.Pill).border(1.dp, TK.ListenStroke, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Box(Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(TK.Text)) }
}

/** 보내기 전의 두 행동: 다시 하기와 보내기. 글자는 이런 행동에만 쓴다. */
@Composable
fun SendActions(modifier: Modifier = Modifier, onRetry: () -> Unit, onSend: () -> Unit) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.height(43.dp).clip(CircleShape).background(TK.Round).border(1.5.dp, TK.Text.copy(alpha = 0.85f), CircleShape)
                .clickable(onClick = onRetry).padding(horizontal = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Ic(R.drawable.ic_retry, size = 16.dp, tint = TK.Text)
            T("다시 하기", TK.text(15, FontWeight.Bold, 18), TK.Text, maxLines = 1)
        }
        Row(
            Modifier.height(40.dp).clip(CircleShape).background(TK.MintStrong).clickable(onClick = onSend).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            T("보내기", TK.text(15, FontWeight.Bold, 18), TK.OnMint, maxLines = 1)
            Ic(R.drawable.ic_talk_send, size = 14.dp)
        }
    }
}

/**
 * 어절 말풍선(1단계): 단어와 뜻, 🔊 본보기 듣기, 🔖 저장, › 더 보기. 아이콘만 쓴다.
 * 보내기 전의 표시된 어절이면 아래에 들린 말([heard])과 ▶ 내 소리가 붙는다.
 */
@Composable
fun WordBubble(
    word: String,
    meaning: String?,
    saved: Boolean,
    modifier: Modifier = Modifier,
    heard: String? = null,
    heardColor: Color = TK.Fair,
    onListen: () -> Unit,
    onSave: () -> Unit,
    onMore: () -> Unit,
    onMine: () -> Unit = {},
) {
    Column(
        modifier
            .width(336.dp)
            .shadow(18.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x40000000), spotColor = Color(0x40000000))
            .clip(RoundedCornerShape(16.dp))
            .background(TK.Bubble)
            .plainClick { }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(word, TK.text(18, FontWeight.Bold, 22), TK.BubbleInk, maxLines = 1)
            Spacer(Modifier.width(8.dp))
            T(meaning ?: romanize(word), TK.text(14, FontWeight.Normal, 17), TK.BubbleMuted, Modifier.weight(1f), maxLines = 1)
            BubbleTool(R.drawable.ic_sound, onListen)
            BubbleTool(if (saved) R.drawable.ic_mark_fill else R.drawable.ic_mark, onSave)
            BubbleTool(R.drawable.ic_more, onMore)
        }
        if (heard != null) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(TK.BubbleInk.copy(alpha = 0.12f)))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                T(heard, TK.text(16, FontWeight.Bold, 19), heardColor, maxLines = 1)
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(TK.BubbleInk).clickable(onClick = onMine),
                    contentAlignment = Alignment.Center,
                ) { Ic(R.drawable.ic_play_small, size = 12.dp) }
            }
        }
    }
}

@Composable
private fun BubbleTool(@DrawableRes icon: Int, onClick: () -> Unit) {
    Box(Modifier.padding(start = 2.dp).size(30.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Ic(icon, size = 18.dp, tint = TK.BubbleInk)
    }
}
