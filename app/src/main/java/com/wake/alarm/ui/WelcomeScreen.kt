package com.wake.alarm.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shown once, on the very first launch. START hands over to the normal alarm list. */
@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "NO SNOOZE ALARM",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "A minimalist alarm app that FORCES you to wake up and leaves no room for 'just five more minutes'.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        ClockIcon(Modifier.size(150.dp))
        Spacer(Modifier.height(48.dp))
        WakeButton("START", onClick = onStart, modifier = Modifier.fillMaxWidth(), filled = true)
    }
}

/** Flat old-fashioned alarm clock: ring, two bells, hands, two feet. Same drawing as the launcher icon. */
@Composable
private fun ClockIcon(modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val u = size.width / 108f
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        drawCircle(ink, radius = 22f * u, center = p(54f, 57f), style = Stroke(width = 5f * u))
        drawCircle(accent, radius = 6.5f * u, center = p(36f, 39f))
        drawCircle(accent, radius = 6.5f * u, center = p(72f, 39f))
        val w = 4f * u
        drawLine(ink, p(54f, 57f), p(54f, 44f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(ink, p(54f, 57f), p(63f, 62f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(ink, p(43f, 75f), p(38f, 83f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(ink, p(65f, 75f), p(70f, 83f), strokeWidth = w, cap = StrokeCap.Round)
    }
}
