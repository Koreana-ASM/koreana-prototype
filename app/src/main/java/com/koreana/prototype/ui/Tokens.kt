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

/** 대화 화면(C)과 온보딩 첫 대화의 색. 장면 그림 위에 짙은 초록 바탕을 쓴다. */
object TK {
    val Scene = Color(0xFF10241F)
    val Pill = Color(0xFF213D33)
    val PillStroke = Color(0x8C789282)
    val ListenStroke = Color(0x6BA2C2A8)
    val CloseStroke = Color(0x5C789282)
    val Mint = Color(0xFFC8E9CB)
    val MintStrong = Color(0xFFAFDEBE)
    val OnMint = Color(0xFF19392F)
    val Text = Color(0xFFF7F8EF)
    val TextBright = Color(0xFFFBFAF2)
    val Muted = Color(0xFFB7CABF)
    val Me = Color(0xFFF2D08C)
    val Recording = Color(0xFFFF8E80)
    val Sheet = Color(0xFF142C25)
    val SheetScrim = Color(0xFF081810)
    val Handle = Color(0xFF789282)
    val Divider = Color(0xFF3B584B)
    val JiwooCard = Color(0xFF203B31)
    val MeCard = Color(0xFF344537)
    val InputCard = Color(0xF518362F)
    val InputStroke = Color(0x61AFDEBE)
    val SwitchOff = Color(0xFF52645C)
    val SwitchKnobOff = Color(0xFFD6E0D8)
    val Choice = Color(0xFF18362F)
    val ChoiceTag = Color(0xFFDFEAE2)

    /** 글자 크기, 굵기, 줄 높이(sp). 자간은 Figma와 같이 크기의 -1%. */
    fun text(size: Int, weight: FontWeight, lineHeight: Int) = TextStyle(
        fontSize = size.sp,
        fontWeight = weight,
        lineHeight = lineHeight.sp,
        letterSpacing = (-size * 0.01f).sp,
    )
}
