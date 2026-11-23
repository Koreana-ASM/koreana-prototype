package com.koreana.prototype.talk

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.koreana.prototype.Line
import com.koreana.prototype.R
import com.koreana.prototype.WordInfo
import com.koreana.prototype.ui.GlassChip
import com.koreana.prototype.ui.GlassIconButton
import com.koreana.prototype.ui.Ic
import com.koreana.prototype.ui.K
import com.koreana.prototype.ui.PhotoTextShadow
import com.koreana.prototype.ui.SoftIconButton
import com.koreana.prototype.ui.T
import com.koreana.prototype.ui.Type
import com.koreana.prototype.ui.floatingCard
import com.koreana.prototype.ui.plainClick

/** 상대의 말 카드 (위쪽 흰 카드). */
@Composable
fun JiwooCard(
    line: Line,
    modifier: Modifier = Modifier,
    onListen: () -> Unit,
    hideText: Boolean,
    onToggleHide: () -> Unit,
    showTranslation: Boolean,
    onToggleTranslation: () -> Unit,
    saved: Boolean,
    onToggleSave: () -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .floatingCard(28.dp)
            .background(Color.White)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        T("지우", Type.Caption, K.TextSecondary)
        T(if (hideText) "· · · · ·" else line.ko, Type.TitleSmall, K.TextPrimary)
        if (showTranslation && !hideText) T(line.en, Type.Body, K.TextSecondary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftIconButton(R.drawable.ic_eye_off, active = hideText, iconSize = 22.dp, onClick = onToggleHide)
                SoftIconButton(R.drawable.ic_translate, active = !showTranslation, onClick = onToggleTranslation)
                SoftIconButton(R.drawable.ic_volume, onClick = onListen)
            }
            SoftIconButton(R.drawable.ic_bookmark, active = saved, iconSize = 22.dp, onClick = onToggleSave)
        }
    }
}

/** "이렇게 말해보세요" 초록 카드. */
@Composable
fun SayCard(
    line: Line,
    modifier: Modifier = Modifier,
    label: String = "이렇게 말해보세요",
    note: String? = null,
    showRoman: Boolean,
    onToggleRoman: () -> Unit,
    onListen: () -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .floatingCard(24.dp)
            .background(K.BrandPrimary)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        T(label, Type.Caption, K.OnPhotoMuted)
        T(line.ko, Type.TitleSmall, Color.White)
        T(if (showRoman && line.roman.isNotEmpty()) line.roman else line.en, Type.Body, K.OnPhotoMuted)
        if (note != null) GlassChip(note)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(R.drawable.ic_volume, contentDescription = "모범 음성 듣기", onClick = onListen)
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (showRoman) Color.White else K.GlassRegular)
                    .border(1.dp, K.GlassRegularStroke, CircleShape)
                    .clickable(onClick = onToggleRoman),
                contentAlignment = Alignment.Center,
            ) { T("/a/", Type.LabelMedium, if (showRoman) K.BrandPrimary else Color.White) }
        }
    }
}

/** 내 말 카드. [flagged]가 있으면 그 어절만 한 가지 색 밑줄로 표시하고 누를 수 있다. */
@Composable
fun MyCard(
    text: String,
    modifier: Modifier = Modifier,
    flagged: String? = null,
    onFlaggedClick: () -> Unit = {},
    badge: Boolean = false,
    onBadgePlaced: (Offset) -> Unit = {},
) {
    Box(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .floatingCard(24.dp)
                .background(Color.White)
                .then(if (flagged != null) Modifier.clickable(onClick = onFlaggedClick) else Modifier)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            T("나", Type.Caption, K.TextSecondary)
            val start = if (flagged != null) text.indexOf(flagged) else -1
            if (flagged != null && start >= 0) {
                T(
                    buildAnnotatedString {
                        append(text.substring(0, start))
                        withStyle(SpanStyle(color = K.Fair, textDecoration = TextDecoration.Underline)) { append(flagged) }
                        append(text.substring(start + flagged.length))
                    },
                    Type.TitleSmall,
                    K.TextPrimary,
                )
            } else {
                T(text, Type.TitleSmall, K.TextPrimary)
            }
        }
        // 받은 별 배지 자리는 별이 터지는 위치의 기준이므로 항상 잡아 둔다.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-14).dp, y = (-21).dp)
                .onGloballyPositioned { c ->
                    val p = c.positionInRoot()
                    onBadgePlaced(Offset(p.x + c.size.width * 0.3f, p.y + c.size.height / 2f))
                },
        ) {
            if (badge) {
                Row(
                    Modifier
                        .shadow(10.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(3.dp, K.Crown, CircleShape)
                        .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Ic(R.drawable.ic_star, size = 21.6.dp)
                    T("+1", Type.LabelLarge, K.TextPrimary)
                }
            } else {
                Spacer(Modifier.size(width = 66.dp, height = 36.dp))
            }
        }
    }
}

/** 마이크 / 멈춤 버튼 (72). 녹음 중에는 물결이 번진다. */
@Composable
fun MicButton(recording: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.size(72.dp), contentAlignment = Alignment.Center) {
        if (recording) {
            val t = rememberInfiniteTransition(label = "mic")
            val pulse by t.animateFloat(
                1f, 1.45f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse",
            )
            Box(Modifier.size(72.dp).scale(pulse).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)))
        }
        Box(
            Modifier
                .size(72.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(K.BrandPrimary)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Ic(if (recording) R.drawable.ic_stop else R.drawable.ic_mic, size = 28.dp, tint = Color.White) }
    }
}

/** 큰 자막: 지금 말하는 어절만 강조색. */
@Composable
fun BigSubtitle(line: Line, highlight: Int, showTranslation: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val words = line.ko.split(" ")
        T(
            buildAnnotatedString {
                words.forEachIndexed { i, w ->
                    if (i > 0) append(" ")
                    if (i == highlight) withStyle(SpanStyle(color = K.Crown)) { append(w) } else append(w)
                }
            },
            Type.TitleLarge.copy(shadow = PhotoTextShadow),
            Color.White,
            Modifier.fillMaxWidth(),
            align = TextAlign.Center,
        )
        if (showTranslation) {
            T(line.en, Type.BodyLarge.copy(shadow = PhotoTextShadow), Color.White, Modifier.fillMaxWidth(), align = TextAlign.Center)
        }
    }
}

private fun mmss(ms: Long): String {
    val s = (ms / 1000).toInt()
    return "%02d:%02d".format(s / 60, s % 60)
}

/** 영상 조작 줄: 재생 위치 + 자막 / 처음부터 / 멈춤 / 넘기기 / 속도. */
@Composable
fun PlayerControls(
    elapsedMs: Long,
    totalMs: Long,
    paused: Boolean,
    subtitleOn: Boolean,
    speedLabel: String,
    onToggleSubtitle: () -> Unit,
    onRestart: () -> Unit,
    onTogglePause: () -> Unit,
    onSkip: () -> Unit,
    onSpeed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            T(mmss(elapsedMs), Type.Caption, K.OnPhotoMuted)
            T("-" + mmss((totalMs - elapsedMs).coerceAtLeast(0)), Type.Caption, K.OnPhotoMuted)
        }
        Spacer(Modifier.height(7.dp))
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.35f))) {
            val f = if (totalMs > 0) (elapsedMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxWidth(f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
        }
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                GlassChip(if (subtitleOn) "자막" else "자막 끔", onClick = onToggleSubtitle)
            }
            GlassIconButton(R.drawable.ic_replay, contentDescription = "처음부터", onClick = onRestart)
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(Color.White).clickable(onClick = onTogglePause),
                contentAlignment = Alignment.Center,
            ) { Ic(if (paused) R.drawable.ic_play else R.drawable.ic_pause) }
            GlassIconButton(R.drawable.ic_arrow_right, contentDescription = "넘기기", onClick = onSkip)
            Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) { GlassChip(speedLabel, onClick = onSpeed) }
        }
    }
}

/** C-6 · 어절 안내. */
@Composable
fun WordSheet(
    info: WordInfo,
    saved: Boolean,
    onToggleSave: () -> Unit,
    onNative: () -> Unit,
    onMine: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .width(342.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .plainClick { }
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            T("표현 살펴보기", Type.LabelMedium, K.TextSecondary, Modifier.weight(1f))
            Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onToggleSave), contentAlignment = Alignment.Center) {
                Ic(R.drawable.ic_bookmark, size = 22.dp, tint = if (saved) K.BrandPrimary else K.TextPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Ic(R.drawable.ic_close, tint = K.TextPrimary)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                T(info.word, Type.TitleLarge, Color.Black)
                T(info.ipa, Type.BodyLarge, K.TextSecondary)
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(K.SurfaceMuted).clickable(onClick = onNative),
                contentAlignment = Alignment.Center,
            ) { Ic(R.drawable.ic_volume, tint = K.TextPrimary) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                info.parts.forEachIndexed { i, (part, kind) ->
                    if (i > 0) T("+", Type.LabelMedium, K.TextTertiary)
                    Row(
                        Modifier
                            .height(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, K.BorderSubtle, CircleShape)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        T(part, Type.BodyLarge, K.BrandPrimary)
                        T(kind, Type.Caption, K.TextSecondary)
                    }
                }
            }
            T("부분을 누르면 따로 볼 수 있어요.", Type.Caption, K.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf("원어민 소리" to onNative, "내 소리" to onMine).forEach { (label, action) ->
                    Row(
                        Modifier.clip(CircleShape).clickable(onClick = action).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                            Ic(R.drawable.ic_volume, tint = K.TextTertiary)
                        }
                        T(label, Type.LabelMedium, K.TextPrimary)
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            T("설명", Type.Caption, K.TextSecondary)
            T(info.explanation, Type.Body, K.TextPrimary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            T("모국어 번역 · 영어", Type.Caption, K.TextSecondary)
            T(info.translation, Type.Body, K.TextPrimary)
        }
    }
}
