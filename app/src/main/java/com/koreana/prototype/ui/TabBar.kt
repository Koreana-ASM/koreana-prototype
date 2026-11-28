package com.koreana.prototype.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.koreana.prototype.AppModel
import com.koreana.prototype.R
import com.koreana.prototype.Screen

enum class Tab(val label: String, @DrawableRes val icon: Int) {
    Home("홈", R.drawable.ic_home),
    Map("지도", R.drawable.ic_map),
    Note("노트", R.drawable.ic_book),
    Profile("프로필", R.drawable.ic_profile),
}

/**
 * 하단 탭 바: 홈 / 지도 / 노트 / 프로필. 선택된 탭만 밝은 알약 배경을 가진다.
 * [glass]는 탭 바의 바탕: 밝은 화면(프로필, 노트) 위에서는 조금 더 짙게 쓴다. [dot]은 프로필의 새 소식 표시.
 */
@Composable
fun BoxScope.TabBar(model: AppModel, selected: Tab, glass: Color = K.GlassRegular, dot: Boolean = false) {
    val extra = LocalBottomExtra.current
    Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 42.dp + extra).width(342.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(12.dp, CircleShape, ambientColor = Color(0x1F000000), spotColor = Color(0x1F000000))
                .clip(CircleShape)
                .background(glass)
                .border(1.dp, K.GlassRegularStroke, CircleShape)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { tab ->
                val on = tab == selected
                Column(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(if (on) Color.White else Color.Transparent)
                        .clickable(enabled = !on) {
                            model.screen = when (tab) {
                                Tab.Home -> Screen.Home
                                Tab.Map -> Screen.Map
                                Tab.Note -> Screen.Note
                                Tab.Profile -> Screen.Profile
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Ic(tab.icon, size = 22.dp, tint = if (on) K.BrandPrimary else Color.White.copy(alpha = 0.72f))
                    Spacer(Modifier.height(2.dp))
                    T(tab.label, Type.Caption, if (on) K.BrandPrimary else K.OnPhotoMuted)
                }
            }
        }
        if (dot) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = (-24).dp, y = 10.dp).size(9.dp).clip(CircleShape)
                    .background(Color.White).padding(1.5.dp).clip(CircleShape).background(K.Alert),
            )
        }
    }
}
