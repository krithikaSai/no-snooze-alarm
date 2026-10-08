package com.wake.alarm.ui.missions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.wake.alarm.mission.MissionSpec
import com.wake.alarm.ui.FlatBar
import com.wake.alarm.ui.WakeButton
import kotlin.math.sqrt

/** Counts real shakes from the accelerometer (no permission needed). */
@Composable
fun ShakeMission(spec: MissionSpec, onInteract: (Long) -> Unit, onComplete: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    var count by remember { mutableStateOf(0) }

    if (sensor == null) {
        SensorFallback("This phone has no motion sensor, so shaking can't be counted.", onInteract, onComplete)
        return
    }

    DisposableEffect(Unit) {
        var lastShake = 0L
        var done = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                if (done) return
                val g = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]) /
                    SensorManager.GRAVITY_EARTH
                val now = SystemClock.elapsedRealtime()
                if (g > 2.3f && now - lastShake > 250) {
                    lastShake = now
                    count += 1
                    onInteract(0L)
                    if (count >= spec.count) {
                        done = true
                        onComplete()
                    }
                }
            }

            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    Column(Modifier.fillMaxWidth()) {
        Text("SHAKE IT", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(24.dp))
        Text("Hold the phone firmly and shake it.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Text(
            "$count / ${spec.count}",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp
        )
        Spacer(Modifier.height(16.dp))
        FlatBar(count.toFloat() / spec.count)
    }
}

/** Counts steps with the hardware step counter. Needs the activity-recognition permission on Android 10+. */
@Composable
fun WalkMission(spec: MissionSpec, onInteract: (Long) -> Unit, onComplete: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }

    if (sensor == null) {
        SensorFallback("This phone has no step counter, so steps and jumps can't be counted.", onInteract, onComplete)
        return
    }

    fun permitted() = Build.VERSION.SDK_INT < 29 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    var allowed by remember { mutableStateOf(permitted()) }
    var denied by remember { mutableStateOf(false) }
    var steps by remember { mutableStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        allowed = ok
        denied = !ok
    }

    if (denied) {
        SensorFallback("Step counting was not allowed, so your steps can't be counted.", onInteract, onComplete)
        return
    }

    if (!allowed) {
        Column(Modifier.fillMaxWidth()) {
            Text("STEPS / JUMPS", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(24.dp))
            Text(
                "To count your steps and jumps, Android needs your OK for physical activity. " +
                    "It is only used to count them for this wake-up. Nothing leaves your phone.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(24.dp))
            WakeButton(
                "ALLOW STEP COUNTING",
                onClick = {
                    onInteract(120_000L)
                    launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                },
                modifier = Modifier.fillMaxWidth(),
                filled = true
            )
        }
        return
    }

    DisposableEffect(Unit) {
        var base = -1f
        var done = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                if (done) return
                val total = e.values[0]
                if (base < 0f) base = total
                val s = (total - base).toInt().coerceAtLeast(0)
                if (s != steps) {
                    steps = s
                    if (s > 0) onInteract(0L)
                    if (s >= spec.count) {
                        done = true
                        onComplete()
                    }
                }
            }

            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    Column(Modifier.fillMaxWidth()) {
        Text("STEPS / JUMPS", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(24.dp))
        Text(
            "Get moving. Take steps or do jumps until you've reached the target.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "$steps / ${spec.count}",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp
        )
        Spacer(Modifier.height(16.dp))
        FlatBar(steps.toFloat() / spec.count)
        Spacer(Modifier.height(16.dp))
        Text(
            "Steps and jumps can take a few seconds to show up.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
