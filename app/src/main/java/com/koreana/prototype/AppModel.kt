package com.koreana.prototype

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * [ViewOnly]는 장면만 보기: 마이크 없이 보거나, 지난 대화를 다시 볼 때.
 * [Review]는 노트의 "다시 말해 보기": 저장한 말 하나로 나누는 짧은 대화.
 */
enum class TalkMode { Onboarding, Main, ViewOnly, Review }

data class TalkConfig(
    val script: Script,
    val mode: TalkMode,
    val startTurn: Int = 0,
    val enterFromMap: Boolean = false,
    val nonce: Int = 0,
    /** [TalkMode.Review]에서 다시 말해 볼 말. */
    val reviewWord: String? = null,
)

sealed interface Screen {
    data object Onboarding : Screen
    data object Home : Screen
    data object Map : Screen
    data object Note : Screen
    data object Profile : Screen
    data object Notification : Screen
    data class Talk(val config: TalkConfig) : Screen

    /** 특별한 순간의 대화 마무리 뒤 한 번. */
    data class TrophyAward(val trophy: Trophy) : Screen

    /** 30일째 대화 마무리 뒤 한 번: 처음의 나와 지금의 나를 이어 듣는다. */
    data object LookBack : Screen
}

/** 홈에서 "오늘 시작 전"일 때의 변형. */
enum class HomeVariant { Default, MeFirst, Returning, VoiceJustOn }

/** 프로필에 쌓인 기록의 양. */
enum class ProfileStage { Full, FirstDay, Early }

/** 목업 시나리오 (Figma 프레임 이름과 맞춤). */
enum class Scenario(val group: String, val label: String) {
    A1("온보딩", "온보딩 처음부터"),
    B1("홈 지도", "오늘 시작 전 · 지우가 기다림"),
    B2("홈 지도", "오늘 시작 전 · 내가 먼저"),
    B3("홈 지도", "하다가 나감"),
    B4("홈 지도", "오늘 완료"),
    B5("홈 지도", "하루 한도 도달"),
    B6("홈 지도", "며칠 만에 복귀"),
    B7("홈 지도", "동네 완결"),
    B8("홈 지도", "내 목소리를 켠 직후"),
    B9("홈 지도", "마이크 권한 없음"),
    B10("홈 지도", "가입 전"),
    B12("홈 지도", "동네에 들어올 때"),
    B13("홈 지도", "알림 예시"),
    M1("지도 탭", "기본 · 첫날"),
    M2("지도 탭", "기본 · 몇 골목 다녀온 뒤"),
    M3("지도 탭", "기본 · 많이 다녀온 뒤"),
    M4("지도 탭", "골목을 다 마쳤을 때"),
    P1("프로필", "기본"),
    P2("프로필", "첫날"),
    P3("프로필", "가입 전"),
    P4("프로필", "마이크 권한 없음"),
    P5("프로필", "트로피 획득"),
    P6("프로필", "돌아보기 · 30일째"),
    N1("노트", "기본"),
    N2("노트", "첫날"),
    N3("노트", "오늘의 말을 마친 뒤"),
    N4("노트", "마이크 권한 없음"),
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

    /** 목업 스위치: 다음에 하는 말을 지우가 못 알아듣는다 (톤 맞추기에서는 목소리가 작음). */
    var nextQuiet by mutableStateOf(false)

    /** 첫 대화의 단어 안내("모르는 말은 눌러 봐요.")는 한 번만 나온다. */
    var wordHintShown by mutableStateOf(false)

    var requestMic: ((onResult: (Boolean) -> Unit) -> Unit) = { it(false) }
    var openAppSettings: () -> Unit = {}

    // 지도 탭.
    var currentAlley by mutableIntStateOf(Town.Hangang)

    /** 다녀온 골목: 이름표에 불이 켜진다. */
    val visited = mutableStateListOf<Int>()

    /** 골목마다 나눈 대화 점 (한 편에 4개, 세 편). */
    val progress = mutableStateMapOf<Int, Int>()

    /** 지금 골목의 마지막 이야기를 막 끝냈을 때: 지우가 추천 골목에서 손을 흔든다. */
    var recommended by mutableStateOf<Int?>(null)

    // 프로필.
    var profileStage by mutableStateOf(ProfileStage.Full)

    // 노트.
    val saved = mutableStateListOf<SavedWord>()

    /** 지우가 오늘 꺼낼 두세 마디. */
    val todayWords = mutableStateListOf<String>()

    private var nonce = 0
    val dailyLimit = 3

    /** 오늘의 대화. Figma에 있는 본편 대화는 편의점 한 편이라 "더 하기"도 같은 대화를 쓴다. */
    val currentScript: Script get() = Scripts.store
    val lastScript: Script get() = Scripts.store
    val canDoMore: Boolean get() = doneToday < dailyLimit && !neighborhoodDone
    val placeName: String get() = Town.alleys[currentAlley].name

    init {
        fillRecords()
    }

    fun startToday(enterFromMap: Boolean = true) {
        // 마이크 권한이 없으면 장면만 본다.
        if (!micOn) return watch(currentScript)
        screen = Screen.Talk(
            TalkConfig(currentScript, TalkMode.Main, startTurn = inProgressTurn ?: 0, enterFromMap = enterFromMap, nonce = ++nonce),
        )
    }

    /** 장면만 보기 (지난 대화 다시 보기, 마이크 없이 보기). */
    fun watch(script: Script) {
        screen = Screen.Talk(TalkConfig(script, TalkMode.ViewOnly, nonce = ++nonce))
    }

    /** 노트 · 다시 말해 보기. 마이크가 필요하다. */
    fun review(word: String) {
        screen = Screen.Talk(TalkConfig(Scripts.review(word), TalkMode.Review, nonce = ++nonce, reviewWord = word))
    }

    /** 다시 말해 보기를 마침: 지우가 알아들은 말에 ▶와 불이 켜진다. */
    fun finishReview(word: String) {
        saved.firstOrNull { it.word == word }?.let { it.spoken = true; it.understood = true }
        todayWords.remove(word)
        screen = Screen.Note
    }

    /** 도중에 나감: 확인 없이 저장하고 홈이 "이어서 하기"로. */
    fun leaveTalk(atTurn: Int) {
        inProgressTurn = atTurn
        screen = Screen.Home
    }

    fun finishTalk(more: Boolean) {
        inProgressTurn = null
        doneToday = (doneToday + 1).coerceAtMost(dailyLimit)
        stars += 2
        progress[currentAlley] = ((progress[currentAlley] ?: 0) + 4).coerceAtMost(12)
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
        firstDay()
        screen = Screen.Home
    }

    fun isSaved(word: String) = saved.any { it.word == Words.bare(word) }

    /** 어절 말풍선의 🔖: 저장하거나, 이미 저장한 말이면 해제한다. */
    fun toggleSaved(word: String) {
        val bare = Words.bare(word)
        val existing = saved.firstOrNull { it.word == bare }
        if (existing != null) {
            saved.remove(existing)
            todayWords.remove(bare)
        } else {
            saved.add(0, SavedWord(bare, romanize(bare), Words.info(bare)?.meaning ?: "", currentAlley))
        }
    }

    /** 여기로 가기: 떠나는 골목은 다녀온 곳이 되고, 도착한 곳이 지금 골목이 된다. */
    fun moveTo(alley: Int) {
        if (alley == currentAlley) return
        if (currentAlley !in visited) visited.add(currentAlley)
        visited.remove(alley)
        currentAlley = alley
        recommended = null
    }

    /** 지금 골목의 홈으로 들어간다. */
    fun enterAlley() {
        showPlaceToast = true
        screen = Screen.Home
    }

    /** 가입한 지 한 달쯤 된 기록 (프로필 · 노트 · 지도의 기본 모습). */
    private fun fillRecords() {
        profileStage = ProfileStage.Full
        currentAlley = Town.Campus
        visited.clear(); visited.addAll(listOf(Town.Hangang, Town.Food, Town.Music))
        progress.clear()
        progress.putAll(mapOf(Town.Hangang to 12, Town.Food to 12, Town.Music to 8, Town.Campus to 6))
        recommended = null
        saved.clear()
        saved.addAll(
            listOf(
                SavedWord("갈까요", "galkkayo", "shall we go?", Town.Hangang, spoken = true, understood = true),
                SavedWord("삼각김밥", "samgak-gimbap", "triangle kimbap", Town.Hangang, spoken = true),
                SavedWord("저녁", "jeonyeok", "dinner", Town.Hangang),
                SavedWord("신나요", "sinnayo", "it's exciting", Town.Music, spoken = true, understood = true),
                SavedWord("같이", "gachi", "together", Town.Music),
            ),
        )
        todayWords.clear(); todayWords.addAll(listOf("삼각김밥", "저녁"))
    }

    /** 첫날: 한강 공원에서 막 시작했고, 저장한 말이 없다. */
    private fun firstDay() {
        profileStage = ProfileStage.FirstDay
        currentAlley = Town.Hangang
        visited.clear(); progress.clear(); recommended = null
        saved.clear(); todayWords.clear()
    }

    fun apply(s: Scenario) {
        stars = 24; streak = 3; doneToday = 0; inProgressTurn = null
        variant = HomeVariant.Default; signedUp = true; neighborhoodDone = false
        showPlaceToast = false; justFinished = false; forceMicOff = false
        fillRecords()
        screen = Screen.Home
        when (s) {
            Scenario.A1 -> { signedUp = false; wordHintShown = false; onboardingKey++; screen = Screen.Onboarding }
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
            Scenario.M1 -> { firstDay(); screen = Screen.Map }
            Scenario.M2 -> screen = Screen.Map
            Scenario.M3 -> {
                currentAlley = Town.Hanok
                visited.clear()
                visited.addAll(listOf(Town.Namsan, Town.Campus, Town.Terminal, Town.Office, Town.Hangang, Town.Food, Town.Music))
                visited.forEach { progress[it] = 12 }
                progress[Town.Hanok] = 2
                screen = Screen.Map
            }
            Scenario.M4 -> {
                progress[Town.Campus] = 12
                recommended = Town.Office
                screen = Screen.Map
            }
            Scenario.P1 -> screen = Screen.Profile
            Scenario.P2 -> { firstDay(); streak = 1; screen = Screen.Profile }
            Scenario.P3 -> { signedUp = false; profileStage = ProfileStage.Early; screen = Screen.Profile }
            Scenario.P4 -> { forceMicOff = true; screen = Screen.Profile }
            Scenario.P5 -> screen = Screen.TrophyAward(Trophy.Heart)
            Scenario.P6 -> screen = Screen.LookBack
            Scenario.N1 -> screen = Screen.Note
            Scenario.N2 -> { firstDay(); screen = Screen.Note }
            Scenario.N3 -> {
                todayWords.clear()
                saved.forEach { if (it.word == "삼각김밥") it.understood = true; if (it.word == "저녁") it.spoken = true }
                screen = Screen.Note
            }
            Scenario.N4 -> { forceMicOff = true; screen = Screen.Note }
        }
    }
}
