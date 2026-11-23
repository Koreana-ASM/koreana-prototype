package com.koreana.prototype

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class Line(val ko: String, val en: String, val roman: String = "")

/** 한 마디: 지우의 말(없으면 내가 먼저) + 내가 할 말. [flagged]는 "다시 들어볼 어절". */
data class Turn(val jiwoo: Line?, val me: Line, val flagged: String? = null)

data class Script(
    val place: String,
    val turns: List<Turn>,
    val closing: Line,
)

/** 어절 안내(C-6)에 보여줄 정보. */
data class WordInfo(
    val word: String,
    val ipa: String,
    val parts: List<Pair<String, String>>,
    val explanation: String,
    val translation: String,
)

object Scripts {
    /** A-4 · 온보딩 첫 대화. */
    val firstTalk = Script(
        place = "한강",
        turns = listOf(
            Turn(Line("안녕하세요! 저는 지우예요.", "Hello! I'm Jiwoo."), Line("만나서 반가워요.", "Nice to meet you.", "mannaseo bangawoyo.")),
            // 지우의 두 번째 말은 Figma에 없어 임시로 넣은 문장.
            Turn(Line("쉬는 날에 뭐 해요?", "What do you do on your days off?"), Line("쉬는 날에는 카페에 가요.", "On my days off, I go to a cafe.", "swineun nareneun kapee gayo.")),
            Turn(Line("주말에 같이 카페 갈까요?", "Shall we go to a cafe this weekend?"), Line("좋아요. 주말에 같이 가요.", "Sure. Let's go this weekend.", "joayo. jumare gachi gayo.")),
        ),
        closing = Line("오늘 반가웠어요. 또 만나요!", "It was nice meeting you. See you again!"),
    )

    /** C · 편의점 (지우가 먼저). */
    val store = Script(
        place = "편의점",
        turns = listOf(
            Turn(Line("뭐 먹을래요?", "What do you want to eat?"), Line("라면 먹고 싶어요.", "I want to eat ramen.", "ramyeon meokgo sipeoyo."), flagged = "먹고"),
            // 두 번째 내 문장은 Figma에 없어 임시로 넣은 문장.
            Turn(Line("좋아요! 매운 거 괜찮아요?", "Great! Is spicy food okay?"), Line("네, 괜찮아요.", "Yes, that's fine.", "ne, gwaenchanayo.")),
        ),
        closing = Line("그럼 이걸로 해요. 내일은 다리 위에서 만나요!", "Let's get this one. See you on the bridge tomorrow!"),
    )

    /** C-11 · 내가 먼저인 대화. 첫 문장만 Figma, 나머지는 임시. */
    val meFirst = Script(
        place = "편의점",
        turns = listOf(
            Turn(null, Line("이거 뭐예요?", "What is this?", "igeo mwoyeyo?")),
            Turn(Line("삼각김밥이에요. 맛있어요!", "It's a triangle kimbap. It's tasty!"), Line("그럼 이것도 주세요.", "Then I'll take this too, please.", "geureom igeotdo juseyo.")),
        ),
        closing = Line("좋아요, 같이 계산해요. 내일은 다리 위에서 만나요!", "Okay, let's pay together. See you on the bridge tomorrow!"),
    )

    /** 하루 세 번째 대화 (임시 문장). */
    val outside = Script(
        place = "편의점",
        turns = listOf(
            Turn(Line("어디에서 먹을까요?", "Where shall we eat?"), Line("밖에서 먹어요.", "Let's eat outside.", "bakkeseo meogeoyo.")),
            Turn(Line("좋아요! 오늘 날씨가 좋네요.", "Sounds good! The weather is nice today."), Line("네, 정말 좋아요.", "Yes, it's really nice.", "ne, jeongmal joayo.")),
        ),
        closing = Line("오늘 즐거웠어요. 내일은 다리 위에서 만나요!", "I had fun today. See you on the bridge tomorrow!"),
    )

    /** B-11 · 지난 곳 다시 보기 (임시 문장). */
    val pocha = Script(
        place = "포장마차 · 떡볶이",
        turns = listOf(Turn(Line("떡볶이 좋아해요?", "Do you like tteokbokki?"), Line("네, 좋아해요.", "Yes, I like it."))),
        closing = Line("그럼 같이 먹어요!", "Then let's eat together!"),
    )
    val hanok = Script(
        place = "한옥 카페 · 차 한 잔",
        turns = listOf(Turn(Line("차 마실래요?", "Would you like some tea?"), Line("네, 마시고 싶어요.", "Yes, I'd like some."))),
        closing = Line("여기 차가 맛있어요.", "The tea here is good."),
    )

    val words = mapOf(
        "먹고" to WordInfo(
            word = "먹고",
            ipa = "[mʌk̚.k͈o]",
            parts = listOf("먹" to "동사", "고" to "연결 어미"),
            explanation = "‘먹다’에 ‘-고 싶다’가 이어진 표현이에요. ‘먹고 싶어요’는 먹기를 바란다는 뜻이에요.",
            translation = "want to eat",
        ),
    )
}

enum class TalkMode { Onboarding, Main, ReplayOnly }

data class TalkConfig(
    val script: Script,
    val mode: TalkMode,
    val startTurn: Int = 0,
    val enterFromMap: Boolean = false,
    val nonce: Int = 0,
)

sealed interface Screen {
    data object Onboarding : Screen
    data object Home : Screen
    data object Notification : Screen
    data class Talk(val config: TalkConfig) : Screen
}

/** 홈에서 "오늘 시작 전"일 때의 변형. */
enum class HomeVariant { Default, MeFirst, Returning, VoiceJustOn }

/** 목업 시나리오 (Figma 프레임 이름과 맞춤). */
enum class Scenario(val label: String) {
    A1("A-1 · 온보딩 처음부터"),
    B1("B-1 · 오늘 시작 전 · 지우가 기다림"),
    B2("B-2 · 오늘 시작 전 · 내가 먼저"),
    B3("B-3 · 하다가 나감"),
    B4("B-4 · 오늘 완료"),
    B5("B-5 · 하루 한도 도달"),
    B6("B-6 · 며칠 만에 복귀"),
    B7("B-7 · 동네 완결"),
    B8("B-8 · 내 목소리를 켠 직후"),
    B9("B-9 · 마이크 권한 없음"),
    B10("B-10 · 가입 전"),
    B12("B-12 · 동네에 들어올 때"),
    B13("B-13 · 알림 예시"),
}

class AppModel {
    var screen by mutableStateOf<Screen>(Screen.Onboarding)
    var onboardingKey by mutableIntStateOf(0)

    var stars by mutableIntStateOf(24)
    var streak by mutableIntStateOf(3)

    /** 오늘 끝낸 새 대화 수. 임시 한도 3. */
    var doneToday by mutableIntStateOf(0)

    /** 하다가 나간 대화의 마디 (없으면 null). */
    var inProgressTurn by mutableStateOf<Int?>(null)
    var variant by mutableStateOf(HomeVariant.Default)
    var signedUp by mutableStateOf(false)
    var neighborhoodDone by mutableStateOf(false)
    var showPlaceToast by mutableStateOf(false)

    /** 대화를 막 끝내고 지도로 나왔을 때 색이 번지는 연출을 한 번 보여준다. */
    var justFinished by mutableStateOf(false)

    var micPermission by mutableStateOf(false)
    var forceMicOff by mutableStateOf(false)
    val micOn: Boolean get() = micPermission && !forceMicOff

    /** 목업 스위치: 인터넷 끊김(C-14), 다음 발화를 "소리가 작음"으로(C-12 / A-3.2b). */
    var offline by mutableStateOf(false)
    var nextQuiet by mutableStateOf(false)

    var requestMic: ((onResult: (Boolean) -> Unit) -> Unit) = { it(false) }

    private var nonce = 0
    val dailyLimit = 3

    private fun todayScripts(): List<Script> =
        if (variant == HomeVariant.MeFirst) listOf(Scripts.meFirst, Scripts.store, Scripts.outside)
        else listOf(Scripts.store, Scripts.meFirst, Scripts.outside)

    val currentScript: Script get() = todayScripts()[doneToday.coerceIn(0, dailyLimit - 1)]
    val lastScript: Script get() = todayScripts()[(doneToday - 1).coerceIn(0, dailyLimit - 1)]
    val canDoMore: Boolean get() = doneToday < dailyLimit && !neighborhoodDone

    fun startToday(enterFromMap: Boolean = true) {
        screen = Screen.Talk(
            TalkConfig(currentScript, TalkMode.Main, startTurn = inProgressTurn ?: 0, enterFromMap = enterFromMap, nonce = ++nonce),
        )
    }

    fun replay(script: Script) {
        screen = Screen.Talk(TalkConfig(script, TalkMode.ReplayOnly, nonce = ++nonce))
    }

    /** C-13 · 도중에 나감: 확인 없이 저장하고 홈이 "이어서 하기"로. */
    fun leaveTalk(atTurn: Int) {
        inProgressTurn = atTurn
        screen = Screen.Home
    }

    fun finishTalk(more: Boolean) {
        inProgressTurn = null
        doneToday = (doneToday + 1).coerceAtMost(dailyLimit)
        if (more && canDoMore) {
            startToday(enterFromMap = false)
        } else {
            justFinished = true
            screen = Screen.Home
        }
    }

    fun finishOnboarding() {
        signedUp = false
        showPlaceToast = true
        screen = Screen.Home
    }

    fun apply(s: Scenario) {
        stars = 24; streak = 3; doneToday = 0; inProgressTurn = null
        variant = HomeVariant.Default; signedUp = true; neighborhoodDone = false
        showPlaceToast = false; justFinished = false; forceMicOff = false
        screen = Screen.Home
        when (s) {
            Scenario.A1 -> { signedUp = false; onboardingKey++; screen = Screen.Onboarding }
            Scenario.B1 -> Unit
            Scenario.B2 -> variant = HomeVariant.MeFirst
            Scenario.B3 -> inProgressTurn = 1
            Scenario.B4 -> { doneToday = 1; stars = 26 }
            Scenario.B5 -> { doneToday = 3; stars = 30 }
            Scenario.B6 -> { variant = HomeVariant.Returning; streak = 1 }
            Scenario.B7 -> { neighborhoodDone = true; doneToday = 1; stars = 34; streak = 4 }
            Scenario.B8 -> variant = HomeVariant.VoiceJustOn
            Scenario.B9 -> forceMicOff = true
            Scenario.B10 -> signedUp = false
            Scenario.B12 -> showPlaceToast = true
            Scenario.B13 -> screen = Screen.Notification
        }
    }
}
