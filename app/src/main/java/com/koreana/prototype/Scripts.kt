package com.koreana.prototype

/**
 * 한 줄의 대사. 로마자는 [romanize]로 어절마다 만든다.
 * [emphasis]는 강조해 보일 어절(단어 안내의 "눌러").
 */
data class Line(val ko: String, val en: String, val emphasis: Int = -1) {
    val words: List<String> get() = ko.split(" ")
}

/** 보내기 전에 보이는 발음 표시: 주황은 조금 다르게, 빨강은 다른 소리로 들린 어절. */
enum class MarkLevel { Fair, Poor }

/** [index]번째 어절이 [heard]로 들렸다 (목업). */
data class Mark(val index: Int, val heard: String, val level: MarkLevel)

/** 내가 한 말과 그 말에 대한 지우의 반응. [mark]가 있으면 보내기 전에 한 번 멈춘다. */
data class Reply(val me: Line, val reaction: Line, val mark: Mark? = null)

/**
 * 한 마디: 지우의 질문에 추천 문장([guided])을 따라 말하거나 내 말([free])로 답한다.
 * [free]의 내 말은 목업에서 "이렇게 들었다"고 보여줄 예시 문장이다.
 */
data class Turn(val question: Line, val guided: Reply, val free: Reply)

data class Script(
    val place: String,
    /** 질문 전에 지우가 먼저 건네는 말. */
    val intro: List<Line>,
    val turns: List<Turn>,
    val closing: Line?,
)

/** 대화에서 실제로 오간 한 줄 (대화 기록에 쌓인다). */
data class Spoken(val isMe: Boolean, val line: Line)

/** 대사는 모두 Figma(v2 · KOREANA)의 예시 문장이다. */
object Scripts {
    private val firstTalkTurns = listOf(
        // 0 · 친구와 대화
        listOf(
            Turn(
                Line("안녕하세요! 저는 지우예요. 만나서 반가워요.", "Hello! I'm Jiwoo. Nice to meet you."),
                guided = Reply(Line("저도 만나서 반가워요.", "Nice to meet you, too."), Line("우리 친하게 지내요!", "Let's be good friends!")),
                free = Reply(Line("안녕하세요! 반가워요.", "Hello! Nice to meet you."), Line("네, 반가워요! 우리 친하게 지내요.", "Nice to meet you, too! Let's be good friends.")),
            ),
            Turn(
                Line("쉬는 날에는 주로 뭘 해요?", "What do you usually do on days off?"),
                guided = Reply(Line("쉬는 날에는 카페에 가요.", "I go to a café on my days off."), Line("카페를 좋아하는군요. 저도 그래요!", "You like cafés. So do I!")),
                free = Reply(Line("저는 산책을 좋아해요.", "I like going for walks."), Line("산책 좋죠! 저도 자주 걸어요.", "Walks are great! I often go walking too.")),
            ),
            Turn(
                Line("저도 카페에 가는 걸 좋아해요. 이번 주말에 같이 갈까요?", "I like cafés too. Shall we go this weekend?"),
                guided = Reply(Line("좋아요. 이번 주말에 같이 가요.", "Sure. Let's go together this weekend."), Line("이번 주말에 봐요! 제가 카페를 찾아볼게요.", "See you this weekend! I'll look for a café.")),
                free = Reply(Line("다음 주에는 어때요?", "How about next week?"), Line("다음 주 좋아요! 그때 같이 가요.", "Next week sounds good! Let's go together then.")),
            ),
        ),
        // 1 · 한국 음악
        listOf(
            Turn(
                Line("어떤 가수의 노래를 좋아해요?", "Which singer's songs do you like?"),
                guided = Reply(Line("저는 아이유의 노래를 좋아해요.", "I like IU's songs."), Line("아이유를 좋아하는군요!", "So you like IU!")),
                free = Reply(Line("저는 악뮤를 좋아해요.", "I like AKMU."), Line("악뮤 노래 좋죠. 저도 즐겨 들어요!", "AKMU's songs are great. I enjoy listening to them too!")),
            ),
            Turn(
                Line("그 노래의 어떤 점이 좋아요?", "What do you like about that song?"),
                guided = Reply(Line("가사가 좋아서 자주 들어요.", "I listen to it often because I like the lyrics."), Line("가사를 들으면서 노래를 즐기는군요.", "You enjoy the song by listening to the lyrics.")),
                free = Reply(Line("목소리가 참 따뜻해요.", "The voice is really warm."), Line("따뜻한 목소리를 좋아하는군요.", "You like warm voices.")),
            ),
            Turn(
                Line("친구에게 추천한다면 어떻게 소개하고 싶어요?", "If you recommended it to a friend, how would you like to describe it?"),
                guided = Reply(Line("기분이 좋아지는 노래예요.", "It's a song that lifts your mood."), Line("기분이 좋아지는 노래군요. 꼭 들어 볼게요!", "It's a song that lifts your mood. I'll definitely listen!")),
                free = Reply(Line("잔잔한 노래를 들어 보세요.", "Try listening to a mellow song."), Line("오늘 밤에 들어 볼게요. 고마워요!", "I'll listen tonight. Thanks!")),
            ),
        ),
        // 2 · 한국에서 공부
        listOf(
            Turn(
                Line("안녕하세요! 무슨 공부를 해요?", "Hello! What are you studying?"),
                guided = Reply(Line("저는 한국어를 공부해요.", "I study Korean."), Line("한국어를 공부하는군요!", "So you're studying Korean!")),
                free = Reply(Line("저는 역사를 공부해요.", "I study history."), Line("역사요? 저도 옛날이야기를 좋아해요.", "History? I also like stories about the past.")),
            ),
            Turn(
                Line("오늘 수업은 어땠어요?", "How was class today?"),
                guided = Reply(Line("재미있었지만 조금 어려웠어요.", "It was interesting but a little difficult."), Line("어렵지만 재미있는 수업이었군요.", "So it was a difficult but interesting class.")),
                free = Reply(Line("오늘 수업은 재미있었어요.", "Today's class was fun."), Line("수업이 재미있었다니 다행이에요!", "I'm glad class was fun!")),
            ),
            Turn(
                Line("다음 주 발표를 함께 준비해볼까요?", "Shall we prepare together for the presentation next week?"),
                guided = Reply(Line("좋아요. 도서관에서 같이 준비해요.", "Sure. Let's prepare together at the library."), Line("좋아요. 도서관에서 같이 준비해요.", "Sure. Let's prepare together at the library.")),
                free = Reply(Line("수업 끝나고 만날까요?", "Shall we meet after class?"), Line("네, 수업이 끝나면 만나요!", "Yes, let's meet when class ends!")),
            ),
        ),
        // 3 · 한국어로 일
        listOf(
            Turn(
                Line("안녕하세요! 어떤 일을 해요?", "Hello! What kind of work do you do?"),
                guided = Reply(Line("저는 개발자로 일하고 있어요.", "I work as a developer."), Line("개발자시군요. 만나서 반가워요!", "So you're a developer. Nice to meet you!")),
                free = Reply(Line("저는 디자이너예요.", "I'm a designer."), Line("디자이너시군요. 만나서 반가워요!", "So you're a designer. Nice to meet you!")),
            ),
            Turn(
                Line("지금 어떤 일을 하고 있어요?", "What kind of work are you doing right now?"),
                guided = Reply(Line("새로운 앱을 만들고 있어요.", "I'm building a new app."), Line("새 앱을 만들고 계시는군요.", "You're developing a new app.")),
                free = Reply(Line("새 로고를 그리고 있어요.", "I'm drawing a new logo."), Line("로고가 완성되면 보고 싶어요.", "I'd like to see the logo when it's finished.")),
            ),
            Turn(
                Line("회의 시간을 바꾸고 싶어요. 어떻게 이야기하면 좋을까요?", "I'd like to change the meeting time. How should I say it?"),
                guided = Reply(Line("회의를 오후 세 시로 바꿀 수 있을까요?", "Could we move the meeting to three in the afternoon?"), Line("네, 오후 세 시에 뵐게요.", "Yes, I'll see you at three in the afternoon.")),
                free = Reply(Line("오전 열 시는 어떠세요?", "How about ten in the morning?"), Line("네, 오전 열 시로 옮길게요.", "Yes, I'll move it to ten in the morning.")),
            ),
        ),
        // 4 · 한국 여행
        listOf(
            Turn(
                Line("한국에서 어디에 가보고 싶어요?", "Where in Korea would you like to go?"),
                guided = Reply(Line("저는 제주도에 가보고 싶어요.", "I would like to visit Jeju Island."), Line("제주도에 가고 싶군요!", "You want to go to Jeju Island!")),
                free = Reply(Line("부산에 가 보고 싶어요.", "I'd like to visit Busan."), Line("부산 좋죠! 바다가 참 예뻐요.", "Busan is great! The sea is really beautiful.")),
            ),
            Turn(
                Line("그곳에서 무엇을 하고 싶어요?", "What would you like to do there?"),
                guided = Reply(Line("바다를 보고 맛있는 음식도 먹고 싶어요.", "I'd like to see the sea and eat delicious food too."), Line("바다도 보고 맛있는 것도 먹으면 좋겠어요.", "It would be nice to see the sea and eat something delicious too.")),
                free = Reply(Line("시장에서 사진을 찍고 싶어요.", "I'd like to take photos at the market."), Line("시장에서 찍은 사진, 나중에 보여 주세요!", "Please show me the photos you take at the market later!")),
            ),
            Turn(
                Line("길을 잃었어요. 지나가는 사람에게 물어볼까요?", "We're lost. Shall we ask someone passing by for directions?"),
                guided = Reply(Line("실례합니다. 지하철역은 어디예요?", "Excuse me. Where is the subway station?"), Line("지하철역은 저쪽이에요. 같이 갈까요?", "The subway station is over there. Shall we go together?")),
                free = Reply(Line("이 버스는 어디로 가요?", "Where does this bus go?"), Line("이 버스는 시장 쪽으로 가요.", "This bus goes toward the market.")),
            ),
        ),
        // 5 · 한국 음식
        listOf(
            Turn(
                Line("어떤 한국 음식을 좋아해요?", "Which Korean foods do you like?"),
                guided = Reply(Line("저는 비빔밥을 좋아해요.", "I like bibimbap."), Line("비빔밥을 좋아하는군요!", "So you like bibimbap!")),
                free = Reply(Line("저는 김밥을 좋아해요.", "I like gimbap."), Line("김밥 좋죠! 저도 자주 먹어요.", "Gimbap is great! I often eat it too.")),
            ),
            Turn(
                Line("맵기는 어느 정도가 좋아요?", "How spicy do you like it?"),
                guided = Reply(Line("조금 매운 음식이 좋아요.", "I like food that is a little spicy."), Line("그럼 조금만 맵게 부탁해 봐요.", "Then ask for it just a little spicy.")),
                free = Reply(Line("매운 음식은 잘 못 먹어요.", "I can't really eat spicy food."), Line("그럼 순한 음식으로 골라 봐요.", "Then let's choose a mild dish.")),
            ),
            Turn(
                Line("음식점에서 주문해볼까요? 무엇을 드시겠어요?", "Shall we order at the restaurant? What would you like to have?"),
                guided = Reply(Line("비빔밥 한 그릇 주세요.", "Please bring me one bowl of bibimbap."), Line("비빔밥 하나요? 네, 맛있게 드세요!", "One bibimbap? Of course. Enjoy your meal!")),
                free = Reply(Line("김밥 두 줄 주세요.", "Please bring me two rolls of gimbap."), Line("김밥 두 줄이요? 네, 곧 나와요!", "Two rolls of gimbap? Yes, they'll be ready soon!")),
            ),
        ),
    )

    /** A-4 · 온보딩 첫 대화. 고른 관심사([interest], A-2의 카드 순서)에 따라 이야기가 달라진다. */
    fun firstTalk(interest: Int) = Script(
        place = "한강",
        intro = listOf(Line("우리, 한국어로 이야기해 볼까요?", "Shall we talk in Korean?")),
        turns = firstTalkTurns[interest.coerceIn(0, firstTalkTurns.lastIndex)],
        closing = Line("오늘 반가웠어요. 또 만나요!", "It was nice to meet you today. See you again!"),
    )

    /** 지우가 못 알아들었을 때만 건네는 말. */
    val askAgain = Line("네? 다시 말해줄래요?", "Sorry? Could you say that again?")

    /** 첫 대화에서 한 번만: 안내 문구 대신 지우의 대사로 어절을 눌러 보라고 알려 준다. */
    val wordHint = Line("모르는 말은 눌러 봐요.", "Tap any word you don't know.", emphasis = 2)

    /** 한강 대화 · 편의점. */
    val store = Script(
        place = "한강",
        intro = listOf(Line("여기서 저녁 먹고 갈까요?", "Shall we have dinner here before we go?")),
        turns = listOf(
            Turn(
                Line("오늘은 뭐 먹고 싶어요?", "What would you like to eat today?"),
                guided = Reply(
                    Line("라면 먹고 싶어요.", "I'd like to have ramyeon."),
                    Line("라면 좋죠! 그럼 제가 뜨거운 물 받아 올게요.", "Ramyeon sounds good! Then I'll get hot water."),
                    mark = Mark(1, "머고", MarkLevel.Fair),
                ),
                free = Reply(
                    Line("삼각김밥 먹고 싶어요.", "I'd like to have a triangular rice ball."),
                    Line("삼각김밥이요? 참치마요가 맛있어요.", "A rice ball? The tuna mayo one is delicious."),
                    mark = Mark(0, "상각김빱", MarkLevel.Poor),
                ),
            ),
        ),
        closing = Line("저기 자리가 있네요. 같이 앉아요.", "There's a seat over there. Let's sit together."),
    )

    /** 노트 · 다시 말해 보기. 저장한 말 하나로 나누는 짧은 대화 (마무리 인사 없음). */
    fun review(word: String): Script = when (word) {
        "삼각김밥" -> Script(
            place = "한강 공원",
            intro = emptyList(),
            turns = listOf(
                Turn(
                    Line("편의점에서 뭐 살까요?", "What shall we buy at the store?"),
                    guided = Reply(Line("삼각김밥 사고 싶어요.", "I'd like to buy a triangular rice ball."), Line("좋아요! 참치마요로 골라요.", "Sounds good! Let's pick tuna mayo.")),
                    free = Reply(Line("삼각김밥 사고 싶어요.", "I'd like to buy a triangular rice ball."), Line("좋아요! 참치마요로 골라요.", "Sounds good! Let's pick tuna mayo.")),
                ),
            ),
            closing = null,
        )
        "갈까요" -> Script(
            place = "한강 공원",
            intro = emptyList(),
            turns = listOf(
                Turn(
                    Line("날씨가 참 좋네요.", "The weather is really nice."),
                    guided = Reply(Line("한강에 같이 갈까요?", "Shall we go to the Han River together?"), Line("좋아요! 지금 가요.", "Sounds good! Let's go now.")),
                    free = Reply(Line("한강에 같이 갈까요?", "Shall we go to the Han River together?"), Line("좋아요! 지금 가요.", "Sounds good! Let's go now.")),
                ),
            ),
            closing = null,
        )
        "신나요" -> Script(
            place = "음악의 거리",
            intro = emptyList(),
            turns = listOf(
                Turn(
                    Line("공연 어땠어요?", "How was the show?"),
                    guided = Reply(Line("정말 신나요!", "It's really exciting!"), Line("그렇죠? 저도 신나요!", "Right? I'm excited too!")),
                    free = Reply(Line("정말 신나요!", "It's really exciting!"), Line("그렇죠? 저도 신나요!", "Right? I'm excited too!")),
                ),
            ),
            closing = null,
        )
        "같이" -> Script(
            place = "음악의 거리",
            intro = emptyList(),
            turns = listOf(
                Turn(
                    Line("버스킹 보러 갈래요?", "Do you want to go see the busking?"),
                    guided = Reply(Line("네, 같이 가요.", "Yes, let's go together."), Line("좋아요! 같이 가요.", "Great! Let's go together.")),
                    free = Reply(Line("네, 같이 가요.", "Yes, let's go together."), Line("좋아요! 같이 가요.", "Great! Let's go together.")),
                ),
            ),
            closing = null,
        )
        else -> Script(
            place = "한강 공원",
            intro = emptyList(),
            turns = listOf(
                Turn(
                    Line("우리 이제 뭐 할까요?", "What shall we do now?"),
                    guided = Reply(Line("저녁 먹으러 갈까요?", "Shall we go get dinner?"), Line("좋아요! 저도 배고팠어요.", "Sounds good! I was hungry too.")),
                    free = Reply(Line("저녁 먹으러 갈까요?", "Shall we go get dinner?"), Line("좋아요! 저도 배고팠어요.", "Sounds good! I was hungry too.")),
                ),
            ),
            closing = null,
        )
    }

    /** B-11 · 지난 곳 다시 보기 (Figma에 대사가 없어 임시로 넣은 문장). */
    val pocha = Script(
        place = "포장마차",
        intro = emptyList(),
        turns = listOf(
            Turn(
                Line("떡볶이 좋아해요?", "Do you like tteokbokki?"),
                guided = Reply(Line("네, 좋아해요.", "Yes, I like it."), Line("그럼 같이 먹어요!", "Then let's eat together!")),
                free = Reply(Line("네, 좋아해요.", "Yes, I like it."), Line("그럼 같이 먹어요!", "Then let's eat together!")),
            ),
        ),
        closing = Line("오늘도 맛있었어요.", "It was delicious again today."),
    )
    val hanok = Script(
        place = "한옥 카페",
        intro = emptyList(),
        turns = listOf(
            Turn(
                Line("차 마실래요?", "Would you like some tea?"),
                guided = Reply(Line("네, 마시고 싶어요.", "Yes, I'd like some."), Line("여기 차가 맛있어요.", "The tea here is good.")),
                free = Reply(Line("네, 마시고 싶어요.", "Yes, I'd like some."), Line("여기 차가 맛있어요.", "The tea here is good.")),
            ),
        ),
        closing = Line("천천히 마셔요.", "Take your time."),
    )
}
