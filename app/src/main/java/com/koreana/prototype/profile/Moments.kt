package com.koreana.prototype.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.koreana.prototype.AppModel
import com.koreana.prototype.LocalVoice
import com.koreana.prototype.R
import com.koreana.prototype.Screen
import com.koreana.prototype.Speaker
import com.koreana.prototype.Trophy
import com.koreana.prototype.talk.SceneShades
import com.koreana.prototype.talk.Waveform
import com.koreana.prototype.ui.Ic
import com.koreana.prototype.ui.LocalBottomExtra
import com.koreana.prototype.ui.PK
import com.koreana.prototype.ui.T
import com.koreana.prototype.ui.TK
import com.koreana.prototype.ui.plainClick
import com.koreana.prototype.ui.rememberToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 웃는 지우의 장면을 조금 키워 아래에 맞춘다 (Figma의 자르기와 같게). */
@Composable
private fun SmileScene() {
    Image(
        painterResource(R.drawable.img_scene_smile), null,
        Modifier.fillMaxSize().graphicsLayer { scaleX = 1.19f; scaleY = 1.19f; transformOrigin = TransformOrigin(0.5f, 1f) },
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun OutlineAction(text: String, icon: Int? = null, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).border(1.5.dp, TK.MintStrong.copy(alpha = 0.7f), CircleShape).clickable(onClick = onClick)
            .padding(horizontal = if (icon != null) 20.dp else 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Ic(icon, size = 16.dp)
        T(text, TK.text(15, FontWeight.Bold, 19), TK.Text, maxLines = 1)
    }
}

@Composable
private fun MintAction(text: String, onClick: () -> Unit) {
    Box(Modifier.clip(CircleShape).background(TK.MintStrong).clickable(onClick = onClick).padding(horizontal = 28.dp, vertical = 11.dp)) {
        T(text, TK.text(15, FontWeight.Bold, 19), TK.OnMint, maxLines = 1)
    }
}

/**
 * 트로피 획득. 특별한 순간의 대화 마무리 뒤 한 번: 트로피와 그 순간의 폴라로이드가 생긴다.
 * 매일의 대화는 스트릭 불꽃만 받는다.
 */
@Composable
fun TrophyAward(model: AppModel, trophy: Trophy) {
    val extra = LocalBottomExtra.current
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) { rise.animateTo(1f, tween(700)) }
    BackHandler { model.screen = Screen.Home }

    Box(Modifier.fillMaxSize().background(TK.Scene)) {
        SmileScene()
        SceneShades()
        Box(Modifier.fillMaxSize().background(Color(0x8C0E1E1A)))
        Column(
            Modifier.align(Alignment.Center).offset(y = (-28).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TrophyDisc(
                trophy, 200.dp,
                Modifier.spotGlow(alpha = 0.6f, spread = 1.55f).scale(0.8f + 0.2f * rise.value).alpha(rise.value),
            )
            Spacer(Modifier.height(28.dp))
            T("${trophy.title} 트로피", TK.text(22, FontWeight.Bold, 27), TK.Text)
            Spacer(Modifier.height(9.dp))
            T(
                when (trophy) {
                    Trophy.Heart -> "${trophy.date}, 지우를 처음 만났어요"
                    else -> "${trophy.date}, ${trophy.how}"
                },
                TK.text(15, FontWeight.Medium, 19), TK.Mint,
            )
            Spacer(Modifier.height(19.dp))
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(PK.Lit).padding(horizontal = 12.dp, vertical = 5.dp)) {
                T("새 폴라로이드가 생겼어요", TK.text(12, FontWeight.Bold, 15), PK.Ink)
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 82.dp + extra),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlineAction("프로필에서 보기") { model.screen = Screen.Profile }
            MintAction("닫기") { model.screen = Screen.Home }
        }
    }
}

private val voiceBars = listOf(6, 12, 18, 10, 20, 13, 22, 14, 8, 19, 11, 20, 8, 16, 10, 13, 18, 8)

/** 처음의 나 / 지금의 나: 재생 버튼, 언제 어디서, 목소리 파형. */
@Composable
private fun VoiceCard(title: String, sub: String, tint: Color, active: Boolean, dim: Boolean, played: Float, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (dim) 0.45f else 1f)
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0x99142D26))
            .border(1.5.dp, if (active) tint else Color.White.copy(alpha = 0.15f), RoundedCornerShape(30.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
            if (active) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(2) { Box(Modifier.size(3.dp, 11.dp).clip(RoundedCornerShape(1.dp)).background(TK.OnMint)) }
                }
            } else {
                Ic(R.drawable.ic_play_small, size = 13.dp, tint = TK.OnMint)
            }
        }
        Column {
            T(title, TK.text(14, FontWeight.Bold, 17), TK.Text, maxLines = 1)
            T(sub, TK.text(11, FontWeight.Normal, 14), TK.Muted, maxLines = 1)
        }
        Waveform(color = tint, heights = voiceBars, gap = 3.dp, live = false, played = if (active) played else 1f)
    }
}

/**
 * 돌아보기. 30일째 대화 마무리 뒤 한 번: 지우가 묻고 첫날의 내 목소리가 저절로 재생되고,
 * 같은 문장을 지금의 내 목소리로 이어서 들려준 뒤, 지우가 실제로 해낸 것으로 알아봐 준다. 비교만 하고 평가는 하지 않는다.
 */
@Composable
fun LookBack(model: AppModel) {
    val voice = LocalVoice.current
    val scope = rememberCoroutineScope()
    val toast = rememberToast()
    val extra = LocalBottomExtra.current
    val sentence = "저도 만나서 반가워요."

    // 0 처음의 나 · 1 지금의 나 · 2 지우의 한마디
    var step by remember { mutableIntStateOf(0) }
    var playing by remember { mutableIntStateOf(-1) }
    val job = remember { JobBox() }
    val played = remember { Animatable(0f) }

    suspend fun hear(which: Int) {
        playing = which
        played.snapTo(0f)
        val bar = scope.launch { played.animateTo(1f, tween(voice.estimateMs(sentence).toInt() + 300, easing = LinearEasing)) }
        try {
            voice.speak(sentence, if (which == 0) Speaker.Me else Speaker.FutureMe)
        } finally {
            bar.cancel()
            playing = -1
        }
    }

    val lines = listOf(
        "처음 만난 날 기억나요? 그때 이렇게 말했어요." to "Remember the day we met? This is what you said.",
        "그리고 오늘은 이렇게요." to "And this is you today.",
        "처음엔 인사만 했는데, 이제 공연 표도 사고 주문도 하네요." to "You only said hello at first. Now you buy tickets and order food.",
    )

    fun close() {
        job.value?.cancel()
        voice.stop()
        // 닫으면 지도로.
        model.screen = Screen.Map
    }

    LaunchedEffect(step) {
        job.value?.cancel()
        voice.stop()
        delay(350)
        voice.speak(lines[step].first, Speaker.Jiwoo)
        if (step < 2) {
            delay(300)
            hear(step)
            delay(500)
            step += 1
        }
    }
    BackHandler { close() }

    Box(Modifier.fillMaxSize().background(TK.Scene).plainClick(enabled = step < 2) { step += 1 }) {
        SmileScene()
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0x400D1F1A), 0.5f to Color(0x590D1F1A), 0.62f to Color(0xCC0D1F1A), 1f to Color(0xF20D1F1A),
                ),
            ),
        )
        Box(Modifier.align(Alignment.TopCenter).padding(top = 64.dp).clip(RoundedCornerShape(18.dp)).background(PK.Lit).padding(horizontal = 14.dp, vertical = 7.dp)) {
            T("지우와 30일째", TK.text(14, FontWeight.Bold, 17), PK.Ink)
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(start = 27.dp, end = 27.dp, bottom = 33.dp + extra)) {
            T(sentence, TK.text(16, FontWeight.Bold, 20), TK.Text, Modifier.padding(start = 3.dp))
            Spacer(Modifier.height(12.dp))
            Column(Modifier.padding(horizontal = 3.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                VoiceCard("처음의 나", "10월 24일 · 한강 공원", TK.Text, playing == 0, dim = false, played.value) {
                    if (step == 2) { job.value?.cancel(); voice.stop(); job.value = scope.launch { hear(0) } }
                }
                VoiceCard("지금의 나", "오늘 · 캠퍼스 앞", TK.MintStrong, playing == 1, dim = step == 0, played.value) {
                    if (step == 2) { job.value?.cancel(); voice.stop(); job.value = scope.launch { hear(1) } }
                }
            }
            Spacer(Modifier.height(26.dp))
            Column(Modifier.height(124.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                T("지우", TK.text(15, FontWeight.Bold, 18), TK.Mint)
                T(lines[step].first, TK.text(19, FontWeight.Bold, 23), TK.Text)
                T(lines[step].second, TK.text(13, FontWeight.Normal, 16), TK.Muted)
            }
            Row(
                Modifier.fillMaxWidth().height(41.dp).alpha(if (step == 2) 1f else 0f),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                // 공유는 캐릭터와 한마디 카드만 (목소리는 공유하지 않는다).
                OutlineAction("공유", R.drawable.ic_share) { if (step == 2) toast("지우의 한마디 카드를 공유해요 (목업)") }
                MintAction("닫기") { if (step == 2) close() }
            }
        }
    }
}

/** 재생 중인 작업을 담아 두는 칸. */
private class JobBox { var value: Job? = null }
