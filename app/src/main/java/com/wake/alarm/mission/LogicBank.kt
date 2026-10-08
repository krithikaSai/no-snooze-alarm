package com.wake.alarm.mission

import android.content.Context
import com.wake.alarm.data.Difficulty
import com.wake.alarm.data.WakeStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.random.Random

@Serializable
data class LogicQuestion(
    val q: String,
    val options: List<String>,
    val answer: Int,
    /** "easy" | "medium" | "hard" */
    val d: String = "easy"
)

/**
 * Logic questions come from a bundled JSON file (assets/logic_questions.json) mixed with
 * number-sequence questions generated on the spot. Everything is offline.
 */
object LogicBank {
    private var cache: List<LogicQuestion>? = null

    private fun bank(context: Context): List<LogicQuestion> {
        cache?.let { return it }
        val loaded = try {
            val text = context.assets.open("logic_questions.json").bufferedReader().use { it.readText() }
            WakeStore.json.decodeFromString<List<LogicQuestion>>(text)
                .filter { it.options.size >= 2 && it.answer in it.options.indices }
        } catch (e: Exception) {
            emptyList()
        }
        cache = loaded
        return loaded
    }

    /** [count] distinct questions for the given difficulty. */
    fun pick(context: Context, difficulty: Difficulty, count: Int, r: Random = Random.Default): List<LogicQuestion> {
        val all = bank(context)
        val matching = all.filter { it.d.equals(difficulty.name, ignoreCase = true) }.ifEmpty { all }.shuffled(r)
        val result = ArrayList<LogicQuestion>()
        var fromBank = 0
        while (result.size < count) {
            val useSequence = matching.isEmpty() || r.nextInt(3) == 0 || fromBank >= matching.size
            if (useSequence) {
                result += sequenceQuestion(difficulty, r)
            } else {
                result += matching[fromBank++]
            }
        }
        return result
    }

    /** "What comes next? 2, 4, 8, 16, ?" */
    fun sequenceQuestion(difficulty: Difficulty, r: Random = Random.Default): LogicQuestion {
        val terms: List<Int>
        val answer: Int
        when (difficulty) {
            Difficulty.EASY -> {
                val start = r.nextInt(1, 21)
                val step = r.nextInt(2, 10)
                terms = List(5) { start + it * step }
                answer = start + 5 * step
            }
            Difficulty.MEDIUM -> if (r.nextBoolean()) {
                val start = r.nextInt(1, 6)
                val ratio = r.nextInt(2, 4)
                var v = start
                val list = ArrayList<Int>()
                repeat(4) { list += v; v *= ratio }
                terms = list
                answer = v
            } else {
                // alternating +a, +b
                val a = r.nextInt(2, 7)
                var b = r.nextInt(2, 7)
                if (b == a) b += 1
                var v = r.nextInt(1, 10)
                val list = ArrayList<Int>()
                repeat(5) { i -> list += v; v += if (i % 2 == 0) a else b }
                terms = list
                answer = v
            }
            Difficulty.HARD -> if (r.nextBoolean()) {
                // differences grow by a constant
                val start = r.nextInt(1, 10)
                val d0 = r.nextInt(1, 5)
                val growth = r.nextInt(1, 4)
                var v = start
                var diff = d0
                val list = ArrayList<Int>()
                repeat(5) { list += v; v += diff; diff += growth }
                terms = list
                answer = v
            } else {
                // n squared plus an offset
                val offset = r.nextInt(0, 6)
                val from = r.nextInt(2, 6)
                terms = List(5) { (from + it) * (from + it) + offset }
                answer = (from + 5) * (from + 5) + offset
            }
        }
        val options = linkedSetOf(answer)
        var guard = 0
        while (options.size < 4 && guard++ < 100) {
            val candidate = answer + r.nextInt(-9, 10)
            if (candidate > 0 && candidate != answer) options += candidate
        }
        while (options.size < 4) options += answer + options.size * 11
        val shuffled = options.toList().shuffled(r)
        return LogicQuestion(
            q = "What comes next?\n\n" + terms.joinToString(",  ") + ",  ?",
            options = shuffled.map { it.toString() },
            answer = shuffled.indexOf(answer),
            d = difficulty.name.lowercase()
        )
    }
}
