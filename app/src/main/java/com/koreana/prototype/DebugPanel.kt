package com.koreana.prototype

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.koreana.prototype.ui.K
import com.koreana.prototype.ui.MockSwitch
import com.koreana.prototype.ui.T
import com.koreana.prototype.ui.Type
import com.koreana.prototype.ui.plainClick

/**
 * 목업 시나리오 패널. Figma의 상태별 프레임으로 바로 건너뛴다.
 * 홈 왼쪽 위 "Alex" 알약이나 환영 화면의 제목을 길게 누르면 열린다.
 */
@Composable
fun DebugPanel(model: AppModel, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(Color(0x99000000)).plainClick(onClick = onClose), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .plainClick { }
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 36.dp),
        ) {
            T("목업 시나리오", Type.TitleSmall, K.TextPrimary)
            T("Figma 프레임 상태로 바로 이동해요.", Type.Caption, K.TextSecondary)
            Box(Modifier.height(12.dp))
            Column(Modifier.height(430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Scenario.entries.forEach { s ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(K.SurfaceMuted)
                            .clickable { model.apply(s); onClose() }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                    ) { T(s.label, Type.LabelMedium, K.TextPrimary) }
                }
            }
            Box(Modifier.height(14.dp))
            T("대화 중 예외", Type.Caption, K.TextTertiary)
            Box(Modifier.height(8.dp))
            Toggle("다음에 보내는 말이 잘 안 들림", model.nextQuiet) { model.nextQuiet = it }
            Toggle("마이크 권한 없음 · 장면만 보기", model.forceMicOff) { model.forceMicOff = it }
        }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        T(label, Type.Body, K.TextPrimary, Modifier.weight(1f))
        MockSwitch(checked, onChange)
    }
}
