package com.wake.alarm.ui.missions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wake.alarm.mission.LogicBank
import com.wake.alarm.mission.MissionSpec
import com.wake.alarm.ui.WakeButton

@Composable
fun LogicMission(spec: MissionSpec, onInteract: (Long) -> Unit, onComplete: () -> Unit) {
    val context = LocalContext.current
    val questions = remember { LogicBank.pick(context, spec.difficulty, spec.count) }
    var index by remember { mutableStateOf(0) }
    var wrong by remember { mutableStateOf(setOf<Int>()) }
    var message by remember { mutableStateOf("") }
    val q = questions[index]

    Column(Modifier.fillMaxWidth()) {
        Text("QUESTION ${index + 1} OF ${questions.size}", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(24.dp))
        Text(q.q, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        q.options.forEachIndexed { i, option ->
            WakeButton(
                option,
                onClick = {
                    onInteract(0L)
                    if (i == q.answer) {
                        if (index + 1 >= questions.size) {
                            onComplete()
                        } else {
                            index += 1
                            wrong = emptySet()
                            message = ""
                        }
                    } else {
                        wrong = wrong + i
                        message = "Not quite. Have another look."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = i !in wrong
            )
            Spacer(Modifier.height(8.dp))
        }
        if (message.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
