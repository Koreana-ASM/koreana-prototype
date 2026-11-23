package com.koreana.prototype.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Figma 디자인 토큰 (v2 · KOREANA). */
object K {
    val BrandPrimary = Color(0xFF286653)
    val BrandStrong = Color(0xFF203F36)
    val BrandSoft = Color(0xFFE4EEDF)
    val Surface = Color.White
    val SurfaceMuted = Color(0xFFEFF1F0)
    val TextPrimary = Color(0xFF242629)
    val TextSecondary = Color(0xFF62666C)
    val TextTertiary = Color(0xFF767B82)
    val OnPhoto = Color.White
    val OnPhotoMuted = Color(0xFFD9E9E1)
    val Crown = Color(0xFFE6B544)
    val Fair = Color(0xFF9A6700)
    val Alert = Color(0xFFD94238)
    val BorderSubtle = Color(0xFFE8EAED)

    val GlassThin = Color(0x5210281E)
    val GlassThinStroke = Color(0x38FFFFFF)
    val GlassRegular = Color(0x7A10281E)
    val GlassRegularStroke = Color(0x52FFFFFF)
    val GlassThick = Color(0xD910281E)
    val ScrimMedium = Color(0xA310281E)

    private val scrim = Color(0xFF10281E)

    val ScrimTop = Brush.verticalGradient(
        0f to scrim.copy(alpha = 0.56f),
        0.3f to scrim.copy(alpha = 0f),
        1f to scrim.copy(alpha = 0f),
    )
    val ScrimBottom = Brush.verticalGradient(
        0f to scrim.copy(alpha = 0f),
        0.4f to scrim.copy(alpha = 0f),
        0.62f to scrim.copy(alpha = 0.64f),
        1f to scrim.copy(alpha = 0.88f),
    )
}

object Type {
    private fun style(size: Int, weight: FontWeight, lineHeightEm: Float = 1.4f, spacing: Float = 0f) =
        TextStyle(
            fontSize = size.sp,
            fontWeight = weight,
            lineHeight = lineHeightEm.em,
            letterSpacing = spacing.sp,
        )

    val Display = style(42, FontWeight.Black, 60f / 42f, -0.7f)
    val TitleLarge = style(28, FontWeight.Bold, spacing = -0.4f)
    val TitleMedium = style(22, FontWeight.Bold, spacing = -0.2f)
    val TitleSmall = style(18, FontWeight.Bold)
    val BodyLarge = style(16, FontWeight.Medium)
    val Body = style(15, FontWeight.Normal)
    val LabelLarge = style(16, FontWeight.Bold, 1.5f)
    val LabelMedium = style(13, FontWeight.Medium)
    val Caption = style(12, FontWeight.Medium)
    val Micro = style(11, FontWeight.Medium)
}

/** 3버튼 내비게이션처럼 하단 시스템 바가 클 때 아래쪽 요소를 올려주는 여유값. */
val LocalBottomExtra = compositionLocalOf<Dp> { 0.dp }
