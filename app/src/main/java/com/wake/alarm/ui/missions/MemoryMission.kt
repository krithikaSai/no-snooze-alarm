package com.wake.alarm.ui.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wake.alarm.mission.MemoryGenerator
import com.wake.alarm.mission.MemoryKind
import com.wake.alarm.mission.MissionSpec
import com.wake.alarm.ui.WakeButton
import kotlinx.coroutines.delay

private enum class MemoryPhase { SHOW, INPUT }

@Composable
fun MemoryMission(spec: MissionSpec, onInteract: (Long) -> Unit, onComplete: () -> Unit) {
    var index by remember { mutableStateOf(0) }
    var challenge by remember { mutableStateOf(MemoryGenerator.generate(spec.difficulty)) }
    var phase by remember { mutableStateOf(MemoryPhase.SHOW) }
    var showToken by remember { mutableStateOf(0) }
    var typed by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(index, showToken, phase) {
        if (phase == MemoryPhase.SHOW) {
            delay(challenge.showMs)
            phase = MemoryPhase.INPUT
        }
    }

    fun check() {
        if (MemoryGenerator.matches(challenge, typed)) {
            if (index + 1 >= spec.count) {
                onComplete()
            } else {
                index += 1
                challenge = MemoryGenerator.generate(spec.difficulty)
                typed = ""
                message = ""
                phase = MemoryPhase.SHOW
            }
        } else {
            message = "Not quite. Look once more."
            typed = ""
            showToken += 1
            phase = MemoryPhase.SHOW
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text("CHALLENGE ${index + 1} OF ${spec.count}", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(24.dp))

        if (phase == MemoryPhase.SHOW) {
            Text("Remember this:", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Text(
                challenge.display,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp
            )
            if (message.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(message, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(24.dp))
            WakeButton("I'VE GOT IT", onClick = { onInteract(0L); phase = MemoryPhase.INPUT }, modifier = Modifier.fillMaxWidth())
        } else {
            Text("Type it back:", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            if (challenge.kind == MemoryKind.SYMBOLS) {
                Text(
                    if (typed.isEmpty()) "_" else typed.toList().joinToString(" "),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
                Spacer(Modifier.height(16.dp))
                MemoryGenerator.symbols.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { s ->
                            WakeButton(s, onClick = { onInteract(0L); typed += s }, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WakeButton("DELETE", onClick = { typed = typed.dropLast(1) }, modifier = Modifier.weight(1f))
                    WakeButton("CHECK", onClick = { check() }, modifier = Modifier.weight(1f), filled = true)
                }
            } else {
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it.take(16); onInteract(0L) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineMedium,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (challenge.kind == MemoryKind.DIGITS) KeyboardType.Number else KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { check() })
                )
                Spacer(Modifier.height(12.dp))
                WakeButton("CHECK", onClick = { check() }, modifier = Modifier.fillMaxWidth(), filled = true)
            }
        }
    }
}
