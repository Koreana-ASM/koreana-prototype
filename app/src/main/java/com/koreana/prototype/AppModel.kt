package com.koreana.prototype

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** [ViewOnly]는 장면만 보기: 마이크 없이 보거나, 지난 대화를 다시 볼 때. */
enum class TalkMode { Onboarding, Main, ViewOnly }

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

    /** 목업 스위치: 다음에 보내는 말이 잘 안 들린 것으로 한다 (A-3.2b, 발음 확인, 잘 안 들린 부분 확인). */
    var nextQuiet by mutableStateOf(false)

    var requestMic: ((onResult: (Boolean) -> Unit) -> Unit) = { it(false) }

    private var nonce = 0
    val dailyLimit = 3

    /** 오늘의 대화. Figma에 있는 본편 대화는 편의점 한 편이라 "더 하기"도 같은 대화를 쓴다. */
    val currentScript: Script get() = Scripts.store
    val lastScript: Script get() = Scripts.store
    val canDoMore: Boolean get() = doneToday < dailyLimit && !neighborhoodDone

    fun startToday(enterFromMap: Boolean = true) {
        // B-9 · 마이크 권한이 없으면 장면만 본다.
        if (!micOn) return watch(currentScript)
        screen = Screen.Talk(
            TalkConfig(currentScript, TalkMode.Main, startTurn = inProgressTurn ?: 0, enterFromMap = enterFromMap, nonce = ++nonce),
        )
    }

    /** 장면만 보기 (지난 대화 다시 보기, 마이크 없이 보기). */
    fun watch(script: Script) {
        screen = Screen.Talk(TalkConfig(script, TalkMode.ViewOnly, nonce = ++nonce))
    }

    /** 도중에 나감: 확인 없이 저장하고 홈이 "이어서 하기"로 (B-3). */
    fun leaveTalk(atTurn: Int) {
        inProgressTurn = atTurn
        screen = Screen.Home
    }

    fun finishTalk(more: Boolean) {
        inProgressTurn = null
        doneToday = (doneToday + 1).coerceAtMost(dailyLimit)
        stars += 2
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
            Scenario.B3 -> inProgressTurn = 0
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
