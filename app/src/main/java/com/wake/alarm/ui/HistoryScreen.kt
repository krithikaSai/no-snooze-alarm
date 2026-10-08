package com.wake.alarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wake.alarm.data.Streaks
import com.wake.alarm.data.WakeStore
import com.wake.alarm.util.Times
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Deliberately small: a streak per alarm and a month of filled-in days. No charts, no badges. */
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val data by WakeStore.state.collectAsState()
    var month by remember { mutableStateOf(YearMonth.now()) }
    val done = remember(data.history) { Streaks.successDates(data.history) }
    val today = LocalDate.now()
    val ink = MaterialTheme.colorScheme.onBackground

    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        WakeButton("← BACK", onClick = onBack)
        Spacer(Modifier.height(16.dp))
        Text("HISTORY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)

        if (data.alarms.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Nothing here until you've set an alarm.", style = MaterialTheme.typography.bodyMedium)
        }
        data.alarms.sortedWith(compareBy({ it.hour }, { it.minute })).forEach { alarm ->
            val n = Streaks.forAlarm(alarm, data.history)
            Spacer(Modifier.height(20.dp))
            Text(
                "${Times.formatClock(context, alarm.hour, alarm.minute)} STREAK".uppercase(),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                "$n ${if (n == 1) "DAY" else "DAYS"}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp
            )
        }

        SectionTitle("Wake-ups")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SquareButton("<") { month = month.minusMonths(1) }
            Text(
                month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase() + " " + month.year,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall
            )
            SquareButton(">", enabled = month < YearMonth.now()) { month = month.plusMonths(1) }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.height(4.dp))

        val offset = month.atDay(1).dayOfWeek.value - 1
        val cells = offset + month.lengthOfMonth()
        val rows = (cells + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (c in 0 until 7) {
                    val day = r * 7 + c - offset + 1
                    if (day < 1 || day > month.lengthOfMonth()) {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = month.atDay(day)
                        val ok = date in done
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(if (ok) ink else MaterialTheme.colorScheme.background)
                                .border(if (date == today) 2.dp else 1.dp, if (date == today) ink else MaterialTheme.colorScheme.outlineVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$day",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = if (ok) MaterialTheme.colorScheme.background else ink
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Filled day = you finished the wake-up. Kept only on this phone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
