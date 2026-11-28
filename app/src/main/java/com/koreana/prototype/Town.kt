package com.koreana.prototype

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 지도 탭의 골목(랜드마크). 좌표는 Figma 지도 틀(390 너비) 기준이다.
 * [cx], [cy], [rx], [ry]는 원래 색으로 떠오르는 타원, [labelX], [labelY]는 이름표의 왼쪽 위, [tailX]는 이름표 꼬리의 왼쪽.
 */
class Alley(
    val name: String,
    val en: String,
    val quote: String,
    val episodes: List<String>,
    val phrases: List<Pair<String, String>>,
    val cx: Float,
    val cy: Float,
    val rx: Float,
    val ry: Float,
    val labelX: Float,
    val labelY: Float,
    val tailX: Float,
)

object Town {
    const val Namsan = 0
    const val Hanok = 1
    const val Campus = 2
    const val Terminal = 3
    const val Office = 4
    const val Hangang = 5
    const val Food = 6
    const val Music = 7

    val alleys = listOf(
        Alley(
            "남산 전망대", "Namsan Lookout", "남산에 올라가면 서울이 다 보여요!",
            listOf("전망대 가는 길", "야경 보기", "자물쇠 걸기"),
            listOf("올라가요" to "let's go up", "보여요" to "I can see it"),
            194.8f, 197f, 55.1f, 78.3f, 210f, 151f, 248f,
        ),
        Alley(
            "한옥 마을", "Hanok Village", "한옥 마을은 천천히 걸어요.",
            listOf("차 한 잔", "한복 입기", "골목 산책"),
            listOf("천천히" to "slowly", "마셔요" to "let's drink"),
            52.1f, 284f, 87f, 63.8f, 8f, 165f, 37f,
        ),
        Alley(
            "캠퍼스 앞", "Campus Gate", "수업 끝나고 같이 도서관 가요!",
            listOf("도서관에서", "수업 끝나고", "축제 날"),
            listOf("끝나고" to "after it ends", "도서관" to "library", "같이" to "together"),
            356f, 310.1f, 87f, 66.7f, 309f, 188f, 341f,
        ),
        Alley(
            "여행자 터미널", "Travel Terminal", "어디로 떠나 볼까요?",
            listOf("표 사기", "길 묻기", "기차 타기"),
            listOf("어디로" to "where to", "표" to "ticket"),
            61.4f, 449.3f, 101.5f, 66.7f, 8f, 327f, 46f,
        ),
        Alley(
            "오피스 거리", "Office Street", "점심에 뭐 먹을지 얘기해요!",
            listOf("첫 출근", "점심 약속", "회의 시간"),
            listOf("점심" to "lunch", "회의" to "meeting"),
            334.5f, 458f, 66.7f, 89.9f, 282f, 319f, 320f,
        ),
        Alley(
            "한강 공원", "Hangang Park", "한강에서 라면 먹을래요?",
            listOf("편의점에서", "자전거 타기", "치킨 시키기"),
            listOf("갈까요" to "shall we go?", "저녁" to "dinner", "삼각김밥" to "triangular rice ball"),
            183.2f, 695.8f, 87f, 55.1f, 136f, 588f, 168f,
        ),
        Alley(
            "먹자골목", "Food Alley", "맛있는 거 먹으러 가요!",
            listOf("떡볶이 먹기", "주문하기", "계산하기"),
            listOf("주세요" to "please give me", "매워요" to "it's spicy"),
            38.7f, 864f, 81.2f, 55.1f, 8f, 754f, 24f,
        ),
        Alley(
            "음악의 거리", "Music Street", "버스킹이랑 공연 이야기 많이 해요!",
            listOf("버스킹 보러", "공연장 앞에서", "팬미팅 날"),
            listOf("신나요" to "it's exciting", "같이" to "together"),
            370.5f, 876.8f, 81.2f, 63.8f, 297f, 757f, 355f,
        ),
    )
}

enum class TrophyKind(val label: String) { First("처음"), Alley("골목"), Steady("꾸준함"), League("리그") }

/** [col], [row]는 트로피 그림(4 x 2 묶음) 안에서의 자리. */
enum class Trophy(
    val col: Int,
    val row: Int,
    val title: String,
    val kind: TrophyKind,
    val about: String,
    val how: String,
    val date: String,
) {
    Cal(2, 1, "30일", TrophyKind.Steady, "지우와 30일을 함께한 것을 기념하는 트로피", "30일 연속으로 이야기하기", "11월 24일"),
    Stick(0, 1, "음악의 거리", TrophyKind.Alley, "음악의 거리 이야기를 끝까지 함께한 것을 기념하는 트로피", "이야기 3편 모두 함께하기", "11월 21일"),
    Ttok(3, 0, "먹자골목", TrophyKind.Alley, "먹자골목 이야기를 끝까지 함께한 것을 기념하는 트로피", "이야기 3편 모두 함께하기", "11월 12일"),
    Star(1, 0, "내 말로 대답", TrophyKind.First, "추천 문장 없이 내 말로 대답한 것을 기념하는 트로피", "내 말로 한 번 대답하기", "10월 26일"),
    Ramen(2, 0, "한강 공원", TrophyKind.Alley, "한강 공원 이야기를 끝까지 함께한 것을 기념하는 트로피", "이야기 3편 모두 함께하기", "11월 2일"),
    Heart(0, 0, "처음 만남", TrophyKind.First, "지우를 처음 만난 날을 기념하는 트로피", "첫 대화 마치기", "10월 24일"),
}

/**
 * 이야기 속 장면을 한 장씩 남기는 폴라로이드: 사진, 손글씨 한 줄, 그때 내 목소리.
 * [col], [row]는 장면 그림(2 x 2 묶음) 안에서의 자리, [said]는 ▶로 듣는 그때 내 말.
 */
class Polaroid(
    val col: Int,
    val row: Int,
    val note: String,
    val date: String,
    val place: String,
    val en: String,
    val said: String,
    val month: String,
    val trophy: Trophy? = null,
)

object Album {
    val rice = Polaroid(0, 0, "지우가 사 준 삼각김밥.\n맛있었다!", "11.24", "편의점 앞", "Jiwoo bought me a triangle kimbap. It was delicious!", "삼각김밥 먹고 싶어요.", "11월")
    val day30 = Polaroid(1, 0, "지우랑 30일째!", "11.24", "한강 공원", "Day 30 with Jiwoo!", "저도 만나서 반가워요.", "11월", Trophy.Cal)
    val music = Polaroid(1, 1, "음악의 거리,\n끝까지 함께했다!", "11.21", "음악의 거리", "Music Street — we stayed together till the end!", "정말 신나요!", "11월", Trophy.Stick)
    val busking = Polaroid(0, 1, "처음 본 버스킹.\n신났다!", "11.03", "홍대 거리", "My first busking show. So exciting!", "네, 같이 가요.", "11월")
    val first = Polaroid(1, 0, "지우를 처음 만났다.\n떨렸다", "10.25", "한강 공원", "I met Jiwoo for the first time. I was nervous.", "저도 만나서 반가워요.", "10월", Trophy.Heart)

    val all = listOf(rice, day30, music, busking, first)
}

/**
 * 노트에 저장한 말. 상태는 표시가 하나씩 늘어나는 것으로만 보인다:
 * 저장만 함 → 말해 봄([spoken], 내 목소리 ▶) → 지우가 알아들음([understood], 불).
 */
class SavedWord(
    val word: String,
    val roman: String,
    val meaning: String,
    val alley: Int,
    /** 이 말을 만난 문장. */
    val sentence: Line? = null,
    spoken: Boolean = false,
    understood: Boolean = false,
) {
    var spoken by mutableStateOf(spoken)
    var understood by mutableStateOf(understood)
}
