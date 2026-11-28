package com.koreana.prototype.note

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.koreana.prototype.AppModel
import com.koreana.prototype.LocalVoice
import com.koreana.prototype.R
import com.koreana.prototype.Screen
import com.koreana.prototype.SavedWord
import com.koreana.prototype.Speaker
import com.koreana.prototype.Town
import com.koreana.prototype.talk.MintButton
import com.koreana.prototype.talk.WordSheet
import com.koreana.prototype.ui.Ic
import com.koreana.prototype.ui.K
import com.koreana.prototype.ui.LocalBottomExtra
import com.koreana.prototype.ui.PK
import com.koreana.prototype.ui.T
import com.koreana.prototype.ui.TK
import com.koreana.prototype.ui.Tab
import com.koreana.prototype.ui.TabBar
import com.koreana.prototype.ui.Type
import com.koreana.prototype.ui.alleyScene
import com.koreana.prototype.ui.rememberToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 흰 바탕에 옅은 테두리의 카드. */
private fun Modifier.paperCard(radius: Int = 20) =
    clip(RoundedCornerShape(radius.dp)).background(Color.White).border(1.5.dp, PK.Line, RoundedCornerShape(radius.dp))

/**
 * 노트. 대화에서 🔖로 남긴 말이 "통한 말"이 되어 가는 곳.
 * 맨 위에는 지우가 오늘 꺼낼 두세 마디, 아래에는 만난 골목별로 묶인 저장한 말이 있다. 개수, 숙련도, 밀린 표시는 없다.
 * 상태는 표시가 하나씩 늘어나는 것으로만 보인다 (저장만 함 → 내 목소리 ▶ → 지우가 알아들으면 불).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteScreen(model: AppModel, onOpenDebug: () -> Unit) {
    val voice = LocalVoice.current
    val scope = rememberCoroutineScope()
    val toast = rememberToast()
    val extra = LocalBottomExtra.current
    var detail by remember { mutableStateOf<SavedWord?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var playingWord by remember { mutableStateOf(false) }

    fun say(text: String, speaker: Speaker, then: Speaker? = null) {
        job?.cancel()
        voice.stop()
        job = scope.launch {
            playingWord = speaker == Speaker.Model
            try {
                voice.speak(text, speaker)
                if (then != null) voice.speak(text, then)
            } finally {
                playingWord = false
            }
        }
    }

    fun allowMic() {
        model.forceMicOff = false
        // 이미 거절했으면 설정 앱으로.
        model.requestMic { granted -> if (!granted) { toast("설정에서 마이크를 허용해 주세요"); model.openAppSettings() } }
    }

    BackHandler {
        voice.stop()
        if (detail != null) detail = null else model.screen = Screen.Home
    }

    Box(Modifier.fillMaxSize().background(PK.Paper)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 64.dp, bottom = 150.dp + extra),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            T(
                "노트", TK.text(26, FontWeight.Bold, 32), PK.Ink,
                // 길게 누르면 목업 시나리오 패널이 열린다.
                Modifier.combinedClickable(interactionSource = null, indication = null, onClick = {}, onLongClick = onOpenDebug),
            )
            if (model.saved.isEmpty()) {
                FirstDay(onStart = { model.startToday(enterFromMap = false) })
            } else {
                if (model.micOn) {
                    TodayCard(
                        words = model.todayWords,
                        onStart = { model.todayWords.firstOrNull()?.let { model.review(it) } },
                        onWord = { model.review(it) },
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    T("저장한 말", TK.text(19, FontWeight.Bold, 23), PK.Ink)
                    model.saved.map { it.alley }.distinct().forEach { alley ->
                        AlleyGroup(
                            alley = alley,
                            words = model.saved.filter { it.alley == alley },
                            mic = model.micOn,
                            onOpen = { detail = it },
                            onMine = { say(it.sentence?.ko ?: it.word, Speaker.Me) },
                            onListen = { say(it.word, Speaker.Model) },
                        )
                    }
                }
                if (!model.micOn) {
                    Column(Modifier.fillMaxWidth().paperCard(14).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        T("마이크를 허용하면 저장한 말을 지우와 다시 말해 볼 수 있어요.", TK.text(14, FontWeight.Medium, 18), PK.Ink)
                        T("마이크 허용하기", TK.text(14, FontWeight.Bold, 18), K.BrandPrimary, Modifier.clip(CircleShape).clickable { allowMic() })
                    }
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(100.dp + extra)
                .background(Brush.verticalGradient(listOf(PK.Paper.copy(alpha = 0f), PK.Paper))),
        )
        TabBar(model, Tab.Note)

        detail?.let { w ->
            WordSheet(
                word = w.word,
                sentence = w.sentence,
                saved = true,
                playing = playingWord,
                onListen = { say(it, Speaker.Model) },
                // 채워진 🔖를 누르면 저장이 해제된다.
                onSave = { model.toggleSaved(w.word); detail = null },
                onClose = { voice.stop(); detail = null },
                label = "저장한 말",
                roman = w.roman,
                meaning = w.meaning,
            ) {
                if (w.spoken && model.micOn) {
                    T("내 목소리", TK.text(13, FontWeight.Bold, 16), TK.Muted)
                    // 처음 → 지금 이어 듣기.
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoicePill("처음의 나") { say(w.sentence?.ko ?: w.word, Speaker.Me, then = Speaker.FutureMe) }
                        T("→", TK.text(15, FontWeight.Medium, 18), TK.Muted)
                        VoicePill("지금의 나") { say(w.sentence?.ko ?: w.word, Speaker.FutureMe) }
                    }
                }
                if (model.micOn) {
                    MintButton("다시 말해 보기", height = 46.dp) { voice.stop(); detail = null; model.review(w.word) }
                }
            }
        }
    }
}

@Composable
private fun VoicePill(text: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(TK.Pill).border(1.dp, TK.ListenStroke, CircleShape).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Ic(R.drawable.ic_play_small, size = 12.dp)
        T(text, TK.text(13, FontWeight.Medium, 16), TK.Text, maxLines = 1)
    }
}

/** 지우의 대사 한 줄 (오늘의 말, 첫날 안내). */
@Composable
private fun JiwooLine(text: String, modifier: Modifier = Modifier, avatar: Boolean = true, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (avatar) Image(painterResource(R.drawable.img_jiwoo_thumb), null, Modifier.size(44.dp).clip(CircleShape))
        Column(Modifier.weight(1f)) {
            T("지우", Type.Caption, K.TextSecondary)
            T(text, Type.LabelLarge, K.TextPrimary)
        }
        action?.invoke()
    }
}

/** 오늘의 말: 지우가 오늘 꺼낼 두세 마디. →나 칩을 누르면 짧은 대화로. 마친 뒤에는 지우의 대사만 남는다. */
@Composable
private fun TodayCard(words: List<String>, onStart: () -> Unit, onWord: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().paperCard().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (words.isEmpty()) {
            JiwooLine("오늘도 잘 통했어요!")
        } else {
            JiwooLine("이 말, 기억나요?") {
                Box(Modifier.size(44.dp).clip(CircleShape).background(K.BrandPrimary).clickable(onClick = onStart), contentAlignment = Alignment.Center) {
                    Ic(R.drawable.ic_arrow_right, tint = Color.White)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                words.forEach { w ->
                    Box(Modifier.clip(CircleShape).background(PK.Chip).clickable { onWord(w) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        T(w, TK.text(14, FontWeight.Bold, 17), PK.Ink, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** 만난 골목별로 묶인 저장한 말. */
@Composable
private fun AlleyGroup(
    alley: Int,
    words: List<SavedWord>,
    mic: Boolean,
    onOpen: (SavedWord) -> Unit,
    onMine: (SavedWord) -> Unit,
    onListen: (SavedWord) -> Unit,
) {
    val a = Town.alleys[alley]
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(alleyScene(alley), null, Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
            Column {
                T(a.name, TK.text(15, FontWeight.Bold, 18), PK.Ink, maxLines = 1)
                T(a.en, TK.text(12, FontWeight.Normal, 15), PK.Muted, maxLines = 1)
            }
        }
        Column(Modifier.fillMaxWidth().paperCard(14)) {
            words.forEachIndexed { i, w ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(PK.Line))
                WordRow(w, mic, { onOpen(w) }, { onMine(w) }, { onListen(w) })
            }
        }
    }
}

/** 저장한 말 한 줄: 저장만 함(둘 다 끔) → 말해 봄(내 목소리 ▶) → 지우가 알아들음(불). */
@Composable
private fun WordRow(w: SavedWord, mic: Boolean, onOpen: () -> Unit, onMine: () -> Unit, onListen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                T(w.word, TK.text(17, FontWeight.Bold, 21), PK.Ink, maxLines = 1)
                if (w.understood) {
                    Box(
                        Modifier.size(8.dp).drawBehind {
                            drawCircle(
                                Brush.radialGradient(listOf(PK.Flame.copy(alpha = 0.7f), PK.Flame.copy(alpha = 0f)), radius = size.width * 1.4f),
                                radius = size.width * 1.4f,
                            )
                            drawCircle(PK.Flame)
                        },
                    )
                }
            }
            T(if (w.meaning.isEmpty()) w.roman else "${w.roman} · ${w.meaning}", TK.text(13, FontWeight.Normal, 16), PK.Muted, maxLines = 1)
        }
        if (w.spoken && mic) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(PK.Play).clickable(onClick = onMine), contentAlignment = Alignment.Center) {
                Ic(R.drawable.ic_play_small, size = 13.dp, tint = PK.PlayInk)
            }
        }
        Ic(R.drawable.ic_note_sound, size = 22.dp, modifier = Modifier.clip(CircleShape).clickable(onClick = onListen))
    }
}

/**
 * 첫날: 안내 문구 대신 지우의 대사. 대화 화면의 단어 말풍선에서 🔖만 강조해 보여 준다.
 * 빈 칸과 "0개"는 없고, 버튼은 홈의 오늘 대화로 간다.
 */
@Composable
private fun FirstDay(onStart: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.img_jiwoo_thumb), null, Modifier.size(72.dp).clip(CircleShape))
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().paperCard().padding(horizontal = 18.dp, vertical = 12.dp)) {
            T("지우", Type.Caption, K.TextSecondary)
            T("마음에 드는 말을 만나면 남겨 둬요.\n여기서 다시 꺼내 줄게요.", TK.text(16, FontWeight.Bold, 20), K.TextPrimary)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.paperCard(18).padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            T("저녁", TK.text(18, FontWeight.Bold, 22), PK.Ink)
            Spacer(Modifier.width(10.dp))
            T("dinner", TK.text(14, FontWeight.Normal, 17), PK.Muted)
            Spacer(Modifier.width(28.dp))
            Ic(R.drawable.ic_sound, size = 18.dp, tint = PK.Ink)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(34.dp).clip(CircleShape).background(TK.MintStrong), contentAlignment = Alignment.Center) {
                Ic(R.drawable.ic_mark_fill, size = 18.dp, tint = PK.Ink)
            }
            Spacer(Modifier.width(10.dp))
            Ic(R.drawable.ic_more, size = 18.dp, tint = PK.Ink)
        }
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier.fillMaxWidth().height(46.dp).clip(CircleShape).background(K.BrandPrimary).clickable(onClick = onStart),
            contentAlignment = Alignment.Center,
        ) { T("대화하러 가기", TK.text(16, FontWeight.Bold, 20), Color.White) }
    }
}
