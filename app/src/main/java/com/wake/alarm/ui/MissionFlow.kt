package com.wake.alarm.ui

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wake.alarm.data.MissionSettings
import com.wake.alarm.mission.MissionPlan
import com.wake.alarm.mission.MissionSpec
import com.wake.alarm.mission.MissionType
import com.wake.alarm.ui.missions.LogicMission
import com.wake.alarm.ui.missions.MathMission
import com.wake.alarm.ui.missions.MemoryMission
import com.wake.alarm.ui.missions.PhotoMission
import com.wake.alarm.ui.missions.ShakeMission
import com.wake.alarm.ui.missions.WalkMission

private enum class Stage { RUN, DONE, MORE, FINISHED }

private val sendOffs = listOf(
    "Nice work. Go make today yours.",
    "Good. The hard part is behind you. Water, then daylight if you can.",
    "That's the first win of the day. Enjoy the rest of them.",
    "You're up. That's the whole trick."
)

@Composable
fun FlatBar(fraction: Float) {
    val ink = MaterialTheme.colorScheme.onBackground
    Box(Modifier.fillMaxWidth().height(14.dp).border(1.5.dp, ink)) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

/**
 * The whole mission sequence: planned missions -> "Well done!" -> wide awake? -> optional extras.
 *
 * [onInteract] is how the alarm service learns the user is engaged: any touch on this
 * screen (throttled) or sensor event stops the sound, and keeps it from restarting.
 */
@Composable
fun MissionFlow(
    settings: MissionSettings,
    onInteract: (graceMs: Long) -> Unit,
    onAwake: () -> Unit,
    onClose: () -> Unit,
    /** Called once every configured mission is done ("Well done!"): the alarm must never ring again. */
    onMissionsComplete: () -> Unit = {}
) {
    var plan by remember { mutableStateOf(MissionPlan.fromSettings(settings)) }
    var index by remember { mutableStateOf(0) }
    var stage by remember { mutableStateOf(Stage.RUN) }
    val sendOff = remember { sendOffs.random() }
    var lastTouchSent by remember { mutableStateOf(0L) }

    // Finishing the configured missions ends the alarm for good (the service stops re-ringing).
    // The "wide awake?" questions and any extra missions that follow are optional extras.
    LaunchedEffect(stage) {
        if (stage == Stage.DONE) onMissionsComplete()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastTouchSent > 3000) {
                        lastTouchSent = now
                        onInteract(0L)
                    }
                }
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                // Keep the question and CHECK button above the on-screen keyboard (this screen is edge-to-edge).
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            when (stage) {
                Stage.RUN -> {
                    val spec = plan[index]
                    Text(
                        "MISSION ${index + 1} OF ${plan.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(spec.describe(), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(32.dp))
                    key(plan, index) {
                        val finishThis: () -> Unit = {
                            if (index + 1 < plan.size) {
                                index += 1
                            } else {
                                stage = Stage.DONE
                            }
                        }
                        when (spec.type) {
                            MissionType.MATH -> MathMission(spec, onInteract, finishThis)
                            MissionType.MEMORY -> MemoryMission(spec, onInteract, finishThis)
                            MissionType.LOGIC -> LogicMission(spec, onInteract, finishThis)
                            MissionType.SHAKE -> ShakeMission(spec, onInteract, finishThis)
                            MissionType.WALK -> WalkMission(spec, onInteract, finishThis)
                            MissionType.PHOTO -> PhotoMission(spec, onInteract, finishThis)
                        }
                    }
                }

                Stage.DONE -> {
                    Spacer(Modifier.height(48.dp))
                    Text("Well done!", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 40.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "You've taken the first step. Get started with your day.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(48.dp))
                    WakeButton(
                        "I'M WIDE AWAKE",
                        onClick = { onAwake(); stage = Stage.FINISHED },
                        modifier = Modifier.fillMaxWidth(),
                        filled = true
                    )
                    Spacer(Modifier.height(12.dp))
                    WakeButton(
                        "I DON'T FEEL WIDE AWAKE YET",
                        onClick = { stage = Stage.MORE },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Stage.MORE -> {
                    Spacer(Modifier.height(32.dp))
                    Text("Alright. Let's wake you up a little more.", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text("Pick one:", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(24.dp))
                    MissionType.values().forEach { type ->
                        WakeButton(
                            type.label,
                            onClick = {
                                plan = listOf<MissionSpec>(MissionPlan.forType(type, settings))
                                index = 0
                                stage = Stage.RUN
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    WakeButton(
                        "ACTUALLY, I'M WIDE AWAKE",
                        onClick = { onAwake(); stage = Stage.FINISHED },
                        modifier = Modifier.fillMaxWidth(),
                        filled = true
                    )
                }

                Stage.FINISHED -> {
                    Spacer(Modifier.height(64.dp))
                    Text(sendOff, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(32.dp))
                    WakeButton("DONE", onClick = onClose, modifier = Modifier.fillMaxWidth(), filled = true)
                }
            }
        }
    }
}
