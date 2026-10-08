package com.wake.alarm.mission

import com.wake.alarm.data.Difficulty
import kotlin.random.Random

data class MathProblem(val text: String, val answer: Int)

/**
 * Generated locally, always whole-number, always non-negative answers.
 * Meant to wake the brain, not to punish it: hard is "awkward", not "impossible".
 */
object MathGenerator {

    fun generate(difficulty: Difficulty, r: Random = Random.Default): MathProblem = when (difficulty) {
        Difficulty.EASY -> easy(r)
        Difficulty.MEDIUM -> medium(r)
        Difficulty.HARD -> hard(r)
    }

    private fun easy(r: Random): MathProblem = when (r.nextInt(5)) {
        0 -> { val a = r.nextInt(5, 50); val b = r.nextInt(3, 40); MathProblem("$a + $b", a + b) }
        1 -> { val a = r.nextInt(20, 80); val b = r.nextInt(3, 20); MathProblem("$a − $b", a - b) }
        2 -> { val a = r.nextInt(2, 10); val b = r.nextInt(2, 10); MathProblem("$a × $b", a * b) }
        3 -> { val b = r.nextInt(2, 10); val q = r.nextInt(2, 10); MathProblem("${b * q} ÷ $b", q) }
        else -> { val n = r.nextInt(2, 11); MathProblem("$n²", n * n) }
    }

    private fun medium(r: Random): MathProblem = when (r.nextInt(6)) {
        0 -> { val a = r.nextInt(40, 200); val b = r.nextInt(20, 100); MathProblem("$a + $b", a + b) }
        1 -> { val a = r.nextInt(100, 300); val b = r.nextInt(20, 100); MathProblem("$a − $b", a - b) }
        2 -> { val a = r.nextInt(6, 16); val b = r.nextInt(6, 16); MathProblem("$a × $b", a * b) }
        3 -> { val b = r.nextInt(3, 13); val q = r.nextInt(6, 20); MathProblem("${b * q} ÷ $b", q) }
        4 -> { val n = r.nextInt(11, 21); MathProblem("$n²", n * n) }
        else -> { val a = r.nextInt(3, 10); val b = r.nextInt(3, 10); val c = r.nextInt(5, 41); MathProblem("$a × $b + $c", a * b + c) }
    }

    private fun hard(r: Random): MathProblem = when (r.nextInt(7)) {
        0 -> { val a = r.nextInt(120, 900); val b = r.nextInt(120, 900); MathProblem("$a + $b", a + b) }
        1 -> { val a = r.nextInt(300, 1000); val b = r.nextInt(100, a - 50); MathProblem("$a − $b", a - b) }
        2 -> { val a = r.nextInt(12, 26); val b = r.nextInt(6, 20); MathProblem("$a × $b", a * b) }
        3 -> { val b = r.nextInt(6, 20); val q = r.nextInt(12, 40); MathProblem("${b * q} ÷ $b", q) }
        4 -> { val n = r.nextInt(21, 36); MathProblem("$n²", n * n) }
        5 -> {
            val a = r.nextInt(12, 31); val b = r.nextInt(4, 13)
            val c = r.nextInt(10, minOf(99, a * b - 5))
            MathProblem("$a × $b − $c", a * b - c)
        }
        else -> { val a = r.nextInt(5, 31); val b = r.nextInt(5, 31); val c = r.nextInt(3, 10); MathProblem("($a + $b) × $c", (a + b) * c) }
    }
}
