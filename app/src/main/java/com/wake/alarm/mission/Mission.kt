package com.wake.alarm.mission

import com.wake.alarm.data.Difficulty
import com.wake.alarm.data.MissionSettings

enum class MissionType(val label: String) {
    MATH("Math"),
    MEMORY("Memory"),
    LOGIC("Logic"),
    SHAKE("Shaking"),
    WALK("Steps / Jumps"),
    PHOTO("Photo")
}

/** One stage of a wake-up: "5 easy math problems", "50 steps", ... */
data class MissionSpec(
    val type: MissionType,
    val difficulty: Difficulty = Difficulty.EASY,
    /** Problems / challenges / shakes / steps, depending on [type]. */
    val count: Int = 1,
    val instruction: String = ""
) {
    fun describe(): String {
        val d = difficulty.name.lowercase()
        return when (type) {
            MissionType.MATH -> "$count $d math ${plural(count, "problem")}"
            MissionType.MEMORY -> "$count $d memory ${plural(count, "challenge")}"
            MissionType.LOGIC -> "$count $d logic ${plural(count, "question")}"
            MissionType.SHAKE -> "Shake the phone $count times"
            MissionType.WALK -> "$count steps or jumps"
            MissionType.PHOTO -> "Take a photo in another room"
        }
    }

    private fun plural(n: Int, word: String) = if (n == 1) word else word + "s"
}

object MissionPlan {
    /** Fixed order: brain first, then body, then the photo (which gets you out of the room). */
    fun fromSettings(s: MissionSettings): List<MissionSpec> {
        val list = buildList {
            if (s.mathEnabled) add(forType(MissionType.MATH, s))
            if (s.memoryEnabled) add(forType(MissionType.MEMORY, s))
            if (s.logicEnabled) add(forType(MissionType.LOGIC, s))
            if (s.shakeEnabled) add(forType(MissionType.SHAKE, s))
            if (s.walkEnabled) add(forType(MissionType.WALK, s))
            if (s.photoEnabled) add(forType(MissionType.PHOTO, s))
        }
        // Safety net: an alarm must never end up with nothing to do.
        return list.ifEmpty { listOf(MissionSpec(MissionType.MATH, Difficulty.EASY, 3)) }
    }

    /** The configured values for [type], used for the extra missions after "not wide awake yet". */
    fun forType(type: MissionType, s: MissionSettings): MissionSpec = when (type) {
        MissionType.MATH -> MissionSpec(type, s.mathDifficulty, s.mathCount)
        MissionType.MEMORY -> MissionSpec(type, s.memoryDifficulty, s.memoryCount)
        MissionType.LOGIC -> MissionSpec(type, s.logicDifficulty, s.logicCount)
        MissionType.SHAKE -> MissionSpec(type, Difficulty.EASY, s.shakeCount)
        MissionType.WALK -> MissionSpec(type, Difficulty.EASY, s.walkSteps)
        MissionType.PHOTO -> MissionSpec(type, Difficulty.EASY, 1, s.photoInstruction)
    }

    /** Used in the editor so you know exactly what you are signing up for. */
    fun summary(s: MissionSettings): List<String> = fromSettings(s).map { it.describe() }
}

object PhotoPrompts {
    val all = listOf(
        "Go somewhere away from your bed for this one.",
        "Head to the bathroom and take a picture of your toothbrush.",
        "Go find something in another room.",
        "Don't take this one from bed.",
        "Walk to the kitchen and take a picture of your kettle, or the fridge.",
        "Find a window and take a picture of the view.",
        "Take a picture of your front door.",
        "Take a picture of your shoes. They are probably not next to your bed.",
        "Go to the nearest room with a light switch you haven't touched yet and photograph it."
    )

    fun pick(custom: String): String = custom.ifBlank { all.random() }
}
