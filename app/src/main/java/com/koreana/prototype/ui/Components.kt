package com.koreana.prototype.ui

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.koreana.prototype.R

val PhotoTextShadow = Shadow(color = Color(0x73000000), offset = Offset(0f, 1f), blurRadius = 16f)

@Composable
fun T(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, textAlign = align ?: TextAlign.Unspecified),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun T(
    text: AnnotatedString,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    align: TextAlign? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, textAlign = align ?: TextAlign.Unspecified),
    )
}

@Composable
fun Ic(@DrawableRes res: Int, size: Dp = 24.dp, tint: Color? = null, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = modifier.size(size),
        colorFilter = tint?.let { ColorFilter.tint(it) },
    )
}

/** 누름 표시 없이 누를 수 있게 한다 (사진 위 글자 버튼용). */
@Composable
fun Modifier.plainClick(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

/** Icon Button · Glass. */
@Composable
fun GlassIconButton(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    enabled: Boolean = true,
    contentDescription: String? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(K.GlassRegular)
            .border(1.dp, K.GlassRegularStroke, CircleShape)
            .clickable(enabled = enabled, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Ic(icon, tint = Color.White) }
}

/** Button · Primary (사진 위 흰색 주 버튼). */
@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(56.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) { T(text, Type.LabelLarge, K.BrandStrong) }
}

/** Chip · Glass. */
@Composable
fun GlassChip(text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Box(
        modifier
            .clip(CircleShape)
            .background(K.GlassThin)
            .border(1.dp, K.GlassThinStroke, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) { T(text, Type.Caption, Color.White, maxLines = 1) }
}

@Composable
fun TextLink(text: String, style: TextStyle = Type.Caption, vertical: Dp = 6.dp, onClick: () -> Unit) {
    T(text, style, Color.White, Modifier.plainClick(onClick = onClick).padding(vertical = vertical))
}

/** 재화 알약 (별 / 스트릭). */
@Composable
fun StatPill(@DrawableRes icon: Int, value: Int, modifier: Modifier = Modifier, iconSize: Dp = 18.dp) {
    Row(
        modifier
            .shadow(10.dp, CircleShape, spotColor = Color(0x24121A17), ambientColor = Color(0x24121A17))
            .clip(CircleShape)
            .background(Color.White)
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Ic(icon, size = iconSize)
        T(value.toString(), Type.LabelLarge, K.TextPrimary)
    }
}

fun Modifier.floatingCard(radius: Dp): Modifier =
    shadow(12.dp, RoundedCornerShape(radius), spotColor = Color(0x33121A17), ambientColor = Color(0x1F121A17))
        .clip(RoundedCornerShape(radius))

/** 장면 그림 + 위/아래 스크림. */
@Composable
fun SceneBackdrop(topAlpha: Float = 0.7f, bottomAlpha: Float = 0.8f) {
    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.img_scene),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.fillMaxSize().alpha(topAlpha).background(K.ScrimTop))
        Box(Modifier.fillMaxSize().alpha(bottomAlpha).background(K.ScrimBottom))
    }
}

/** 목업용 간단한 스위치. */
@Composable
fun MockSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        Modifier
            .size(width = 46.dp, height = 28.dp)
            .clip(CircleShape)
            .background(if (checked) K.BrandPrimary else Color(0xFFCDD2D0))
            .clickable { onChange(!checked) }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) { Box(Modifier.size(22.dp).clip(CircleShape).background(Color.White)) }
}

@Composable
fun rememberToast(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) { { msg: String -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() } }
}

/** 디자인 좌표(390 x 844 기준)에 요소를 놓는다. */
@Composable
fun BoxScope.At(x: Int, y: Int, content: @Composable () -> Unit) {
    Box(Modifier.align(Alignment.TopStart).offset(x.dp, y.dp)) { content() }
}

/** 가로 중심을 [cx]에 맞춰 놓는다 (칩처럼 폭이 글자에 따라 달라지는 요소). */
@Composable
fun BoxScope.CenteredAt(cx: Int, y: Int, content: @Composable () -> Unit) {
    Box(
        Modifier.align(Alignment.TopStart).offset((cx - 100).dp, y.dp).width(200.dp),
        contentAlignment = Alignment.TopCenter,
    ) { content() }
}
