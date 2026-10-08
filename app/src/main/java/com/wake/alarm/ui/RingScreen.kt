package com.wake.alarm.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wake.alarm.data.Alarm
import com.wake.alarm.mission.MissionPlan
import com.wake.alarm.util.Times

/**
 * The one place the app is allowed to be loud and dramatic. Everything else is quiet.
 * The background image is dimmed so the text and buttons always stay readable.
 */
@Composable
fun RingScreen(
    alarm: Alarm,
    isTest: Boolean,
    snoozeAvailable: Boolean,
    onSnooze: () -> Unit,
    onStartChallenge: () -> Unit
) {
    val context = LocalContext.current
    val blink = rememberInfiniteTransition(label = "blink")
    val alpha by blink.animateFloat(
        initialValue = 1f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "blinkAlpha"
    )

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AssetImage(alarm.background, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f)))

        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                Times.formatClock(context, alarm.hour, alarm.minute),
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 22.sp
            )
            if (isTest) {
                Text("TEST ALARM", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            }

            Spacer(Modifier.weight(1f))
            Text(
                "WAKE UP!!",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = TextStyle(
                    color = Color.White.copy(alpha = alpha),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 52.sp,
                    shadow = Shadow(color = Color(0xFFFF2A2A), offset = Offset.Zero, blurRadius = 48f)
                )
            )
            Spacer(Modifier.weight(1f))

            Text(
                "To switch this off, do:",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            MissionPlan.summary(alarm.missions).forEach {
                Text("• $it", color = Color.White, fontSize = 15.sp)
            }
            Spacer(Modifier.height(20.dp))

            RingButton("START CHALLENGE", filled = true, onClick = onStartChallenge)
            if (snoozeAvailable) {
                Spacer(Modifier.height(12.dp))
                RingButton(if (isTest) "SNOOZE 15 SEC (TEST)" else "SNOOZE 15 MIN", filled = false, onClick = onSnooze)
                Spacer(Modifier.height(6.dp))
                Text(
                    "One snooze only. After that, only the challenge stops the alarm.",
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RingButton(text: String, filled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .background(if (filled) Color.White else Color.Black.copy(alpha = 0.55f))
            .border(2.dp, Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (filled) Color.Black else Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}
