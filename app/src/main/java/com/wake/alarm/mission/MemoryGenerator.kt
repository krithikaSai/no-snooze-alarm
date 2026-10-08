package com.wake.alarm.mission

import com.wake.alarm.data.Difficulty
import kotlin.random.Random

enum class MemoryKind { DIGITS, LETTERS, SYMBOLS }

data class MemoryChallenge(
    val kind: MemoryKind,
    val sequence: List<String>,
    /** How long the sequence stays on screen. */
    val showMs: Long
) {
    val display: String get() = sequence.joinToString(" ")
}

object MemoryGenerator {
    val symbols = listOf("#", "@", "&", "%", "$", "?", "!", "*")
    private const val letters = "ABCDEFGHJKLMNPQRSTUVWXYZ" // no I / O: easy to confuse with 1 / 0

    fun generate(difficulty: Difficulty, r: Random = Random.Default): MemoryChallenge {
        val kind = MemoryKind.values().random(r)
        val level = difficulty.ordinal // 0..2
        val length = when (kind) {
            MemoryKind.DIGITS -> listOf(4, 6, 8)[level]
            MemoryKind.LETTERS -> listOf(4, 5, 7)[level]
            MemoryKind.SYMBOLS -> listOf(4, 5, 6)[level]
        }
        val seq: List<String> = when (kind) {
            MemoryKind.DIGITS -> List(length) { r.nextInt(10).toString() }
            MemoryKind.LETTERS -> letters.toList().shuffled(r).take(length).map { it.toString() }
            MemoryKind.SYMBOLS -> List(length) { symbols.random(r) }
        }
        return MemoryChallenge(kind, seq, showMs = 1500L + length * 750L)
    }

    fun matches(challenge: MemoryChallenge, typed: String): Boolean =
        typed.filter { !it.isWhitespace() }.uppercase() == challenge.sequence.joinToString("")
}
