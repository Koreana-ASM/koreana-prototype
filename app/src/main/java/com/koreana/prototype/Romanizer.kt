package com.koreana.prototype

private val initials = listOf("g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s", "ss", "", "j", "jj", "ch", "k", "t", "p", "h")
private val vowels = listOf(
    "a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa", "wae", "oe", "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i",
)

/** 받침이 음절 끝에서 나는 소리. */
private val finals = listOf(
    "", "k", "k", "k", "n", "n", "n", "t", "l", "k", "m", "l", "l", "l", "p", "l", "m", "p", "p", "t", "t", "ng", "t", "t", "k", "t", "p", "t",
)

/** 모음으로 시작하는 음절 앞에서: 받침에 남는 소리 to 다음 음절로 넘어가는 소리. */
private val linked = listOf(
    "" to "", "" to "g", "" to "kk", "k" to "s", "" to "n", "n" to "j", "" to "n", "" to "d", "" to "r", "l" to "g", "l" to "m",
    "l" to "b", "l" to "s", "l" to "t", "l" to "p", "" to "r", "" to "m", "" to "b", "p" to "s", "" to "s", "" to "ss", "ng" to "",
    "" to "j", "" to "ch", "" to "k", "" to "t", "" to "p", "" to "",
)

private const val EMPTY_INITIAL = 11
private const val FINAL_T = 25
private const val VOWEL_I = 20

/**
 * 어절 하나를 로마자로 적는다 (국어의 로마자 표기법을 따른 목업용 간이 규칙).
 * 연음, 비음화, ㄹㄹ, 구개음화(같이 → gachi)만 다루고 문장 부호는 뺀다.
 */
fun romanize(word: String): String {
    val syllables = word.filter { it in '가'..'힣' }
    if (syllables.isEmpty()) return word.filter { it.isLetterOrDigit() }.lowercase()
    val out = StringBuilder()
    var carried: String? = null
    var openVowel: String? = null
    syllables.forEachIndexed { i, ch ->
        val code = ch - '가'
        val initial = code / 588
        val vowel = code % 588 / 28
        val final = code % 28
        val next = syllables.getOrNull(i + 1)?.let { it - '가' }
        val nextInitial = next?.let { it / 588 }

        val head = carried ?: initials[initial]
        carried = null
        // 받침 ㄹ 뒤의 ㄹ은 l로 적는다.
        out.append(if (head == "r" && out.endsWith("l")) "l" else head)
        // 같은 모음이 이어지면 붙임표로 나눈다 (카페에 → kape-e).
        if (head.isEmpty() && openVowel == vowels[vowel]) out.append('-')
        openVowel = if (final == 0) vowels[vowel] else null
        out.append(vowels[vowel])

        if (final == 0) return@forEachIndexed
        if (nextInitial == EMPTY_INITIAL) {
            val (stay, move) = linked[final]
            out.append(stay)
            carried = if (final == FINAL_T && next!! % 588 / 28 == VOWEL_I) "ch" else move
        } else {
            val sound = finals[final]
            val nasal = nextInitial == 2 || nextInitial == 6
            out.append(
                when {
                    nasal && sound == "k" -> "ng"
                    nasal && sound == "t" -> "n"
                    nasal && sound == "p" -> "m"
                    else -> sound
                },
            )
        }
    }
    return out.toString()
}
