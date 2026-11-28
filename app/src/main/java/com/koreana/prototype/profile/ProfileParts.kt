package com.koreana.prototype.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.koreana.prototype.Polaroid
import com.koreana.prototype.R
import com.koreana.prototype.Trophy
import com.koreana.prototype.ui.Ic
import com.koreana.prototype.ui.PK
import com.koreana.prototype.ui.T
import com.koreana.prototype.ui.scenePainter
import com.koreana.prototype.ui.trophyPainter

/** Figma 프레임(390 x 844) 좌표 그대로 놓고, 화면이 더 길면 덮도록 키운다. */
@Composable
fun Frame390(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val cover = maxOf(1f, maxHeight.value / 844f)
        Box(Modifier.align(Alignment.Center).requiredSize(390.dp, 844.dp).scale(cover), content = content)
    }
}

/**
 * 폴라로이드: 사진, 손글씨 한 줄, 날짜와 곳, 그때 내 목소리(▶).
 * 치수는 160 x 216을 기준으로 [width]에 맞춰 키운다. [onPlay]가 없으면 ▶가 없다(마이크 권한 없음).
 */
@Composable
fun PolaroidCard(p: Polaroid, width: Dp, modifier: Modifier = Modifier, onPlay: (() -> Unit)? = null) {
    val s = width.value / 160f
    Box(
        modifier
            .size(width, (216 * s).dp)
            .shadow((6 * s).dp, RoundedCornerShape(2.dp), ambientColor = Color(0x5533261A), spotColor = Color(0x5533261A))
            .clip(RoundedCornerShape(2.dp))
            .background(PK.Polaroid),
    ) {
        Image(
            scenePainter(p.col, p.row), null,
            Modifier.offset((10 * s).dp, (10 * s).dp).size((140 * s).dp), contentScale = ContentScale.Crop,
        )
        T(p.note, PK.hand(14 * s), PK.PolaroidInk, Modifier.offset((12 * s).dp, (154 * s).dp).width((138 * s).dp), maxLines = 2)
        T(
            "${p.date} · ${p.place}", PK.hand(11 * s, FontWeight.Normal), PK.PolaroidSub,
            Modifier.offset((12 * s).dp, (190 * s).dp), maxLines = 1,
        )
        if (onPlay != null) {
            Box(
                Modifier.offset((128 * s).dp, (186 * s).dp).size((22 * s).dp).clip(CircleShape).background(PK.Play).clickable(onClick = onPlay),
                contentAlignment = Alignment.Center,
            ) { Ic(R.drawable.ic_play_small, size = (10 * s).dp, tint = PK.PlayInk) }
        }
    }
}

/** 받침 위의 트로피 하나. 밝은 동그라미 안에 그림을 넣는다. */
@Composable
fun TrophyDisc(trophy: Trophy, size: Dp, modifier: Modifier = Modifier) {
    Image(trophyPainter(trophy), null, modifier.size(size).clip(CircleShape).background(PK.TrophyBase), contentScale = ContentScale.Crop)
}

/** 조명을 받은 것처럼 번지는 빛. */
fun Modifier.spotGlow(alpha: Float = 0.5f, spread: Float = 1.3f) = drawBehind {
    val r = size.minDimension / 2f * spread
    drawCircle(
        Brush.radialGradient(0f to Color(0xFFFFE6A0).copy(alpha = alpha), 0.7f to Color(0xFFFFE6A0).copy(alpha = alpha * 0.8f), 1f to Color(0x00FFE6A0), radius = r),
        radius = r,
    )
}

/** 큰 트로피: 음악의 거리 트로피는 배경 없는 그림이 있어 테두리 없이 띄운다. */
@Composable
fun TrophyHero(trophy: Trophy, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (trophy == Trophy.Stick) {
            Image(painterResource(R.drawable.img_trophy_stick), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        } else {
            TrophyDisc(trophy, size * 0.81f)
        }
    }
}

/** 작은 닫기(X). */
@Composable
fun CloseMark(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Ic(R.drawable.ic_talk_close, size = 22.dp, tint = Color(0xFFF7F8EF))
    }
}

/** 안에 든 것을 세로로 가운데에 쌓는다. */
@Composable
fun Centered(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) { content() }
}
