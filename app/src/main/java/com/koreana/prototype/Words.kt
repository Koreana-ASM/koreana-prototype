package com.koreana.prototype

/** 단어의 짜임 한 조각: 가다 · go. */
data class WordPart(val ko: String, val en: String)

/** 지난 대화에서 같은 표현이 쓰인 예문. [focus]는 예문 안에서 밝혀 보일 부분. */
data class WordExample(val ko: String, val focus: String, val en: String, val from: String)

data class WordInfo(val meaning: String, val parts: List<WordPart> = emptyList(), val example: WordExample? = null)

/** 어절 말풍선과 단어 시트에 보여 줄 뜻 (목업). 없는 표현은 로마자와 문장만 보여 준다. */
object Words {
    private val table = mapOf(
        "갈까요" to WordInfo(
            "shall we go?",
            listOf(WordPart("가다", "go"), WordPart("-(으)ㄹ까요", "shall we ~?")),
            WordExample("내일 같이 자전거 탈까요?", "탈까요", "Shall we ride bikes tomorrow?", "한강 편"),
        ),
        "먹고" to WordInfo("eat and", listOf(WordPart("먹다", "eat"), WordPart("-고", "and"))),
        "싶어요" to WordInfo("would like to", listOf(WordPart("싶다", "want"), WordPart("-어요", "polite ending"))),
        "저녁" to WordInfo("dinner", example = WordExample("저녁에 뭐 해요?", "저녁", "What are you doing this evening?", "한강 편")),
        "삼각김밥" to WordInfo("triangular rice ball", listOf(WordPart("삼각", "triangle"), WordPart("김밥", "gimbap"))),
        "신나요" to WordInfo("it's exciting", listOf(WordPart("신나다", "be excited"), WordPart("-아요", "polite ending"))),
        "여기서" to WordInfo("here"), "오늘은" to WordInfo("today"), "뭐" to WordInfo("what"), "라면" to WordInfo("ramyeon"),
        "좋죠" to WordInfo("sounds good"), "그럼" to WordInfo("then"), "제가" to WordInfo("I"), "뜨거운" to WordInfo("hot"),
        "물" to WordInfo("water"), "받아" to WordInfo("get"), "올게요" to WordInfo("I'll be back"),
        "삼각김밥이요" to WordInfo("a rice ball?"), "참치마요가" to WordInfo("tuna mayo"), "맛있어요" to WordInfo("is delicious"),
        "저기" to WordInfo("over there"), "자리가" to WordInfo("a seat"), "있네요" to WordInfo("there is"),
        "같이" to WordInfo("together"), "앉아요" to WordInfo("let's sit"), "모르는" to WordInfo("unknown"), "말은" to WordInfo("word"),
        "눌러" to WordInfo("tap"), "봐요" to WordInfo("try"), "네" to WordInfo("pardon?"), "다시" to WordInfo("again"),
        "말해줄래요" to WordInfo("could you say"), "안녕하세요" to WordInfo("hello"), "저는" to WordInfo("I"),
        "지우예요" to WordInfo("am Jiwoo"), "만나서" to WordInfo("to meet you"), "반가워요" to WordInfo("nice"),
        "저도" to WordInfo("me too"), "우리" to WordInfo("we"), "친하게" to WordInfo("closely"), "지내요" to WordInfo("let's get along"),
        "한국어로" to WordInfo("in Korean"), "이야기해" to WordInfo("talk"), "볼까요" to WordInfo("shall we try?"),
        "쉬는" to WordInfo("resting"), "날에는" to WordInfo("on days"), "주로" to WordInfo("usually"), "뭘" to WordInfo("what"),
        "해요" to WordInfo("do"), "카페에" to WordInfo("to a café"), "가요" to WordInfo("go"), "카페를" to WordInfo("cafés"),
        "좋아하는군요" to WordInfo("you like"), "그래요" to WordInfo("so do I"), "가는" to WordInfo("going"), "걸" to WordInfo("thing"),
        "좋아해요" to WordInfo("like"), "이번" to WordInfo("this"), "주말에" to WordInfo("on the weekend"), "좋아요" to WordInfo("sounds good"),
        "오늘" to WordInfo("today"), "반가웠어요" to WordInfo("it was nice"), "또" to WordInfo("again"), "만나요" to WordInfo("see you"),
        "이제" to WordInfo("now"), "할까요" to WordInfo("shall we do?"), "먹으러" to WordInfo("to eat"), "배고팠어요" to WordInfo("was hungry"),
        "조금" to WordInfo("a little"), "더" to WordInfo("more"), "크게" to WordInfo("loudly"), "말해볼까요" to WordInfo("shall we say?"),
    )

    /** 문장부호를 뗀 표제어. */
    fun bare(word: String) = word.filter { it.isLetterOrDigit() }

    fun info(word: String): WordInfo? = table[bare(word)]
}
