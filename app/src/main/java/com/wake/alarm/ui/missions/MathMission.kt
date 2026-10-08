package com.wake.alarm.ui.missions

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wake.alarm.data.Difficulty
import com.wake.alarm.mission.MathGenerator
import com.wake.alarm.mission.MissionSpec
import com.wake.alarm.mission.MissionType
import com.wake.alarm.ui.WakeButton

@Composable
fun MathMission(spec: MissionSpec, onInteract: (Long) -> Unit, onComplete: () -> Unit) {
    var index by remember { mutableStateOf(0) }
    var problem by remember { mutableStateOf(MathGenerator.generate(spec.difficulty)) }
    var input by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    LaunchedEffect(index) { runCatching { focus.requestFocus() } }

    fun submit() {
        if (input.trim() == problem.answer.toString()) {
            if (index + 1 >= spec.count) {
                onComplete()
            } else {
                index += 1
                problem = MathGenerator.generate(spec.difficulty)
                input = ""
                message = ""
            }
        } else {
            message = "Not quite. Try again."
            input = ""
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text("PROBLEM ${index + 1} OF ${spec.count}", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(24.dp))
        Text(
            "${problem.text} = ?",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 44.sp
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { new ->
                input = new.filter { it.isDigit() }.take(6)
                onInteract(0L)
            },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() })
        )
        Spacer(Modifier.height(12.dp))
        WakeButton("CHECK", onClick = { submit() }, modifier = Modifier.fillMaxWidth(), filled = true)
        if (message.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * Used when a sensor or permission is missing: say what happened, then do three
 * easy math problems instead so nobody is locked out of finishing their wake-up.
 */
@Composable
fun SensorFallback(reason: String, onInteract: (Long) -> Unit, onComplete: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(reason, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        Text("Three easy math problems instead.", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        MathMission(MissionSpec(MissionType.MATH, Difficulty.EASY, 3), onInteract, onComplete)
    }
}
