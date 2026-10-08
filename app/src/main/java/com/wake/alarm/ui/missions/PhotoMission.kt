package com.wake.alarm.ui.missions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.wake.alarm.mission.MissionSpec
import com.wake.alarm.mission.PhotoPrompts
import com.wake.alarm.ui.WakeButton

/**
 * The photo itself doesn't matter and is never stored or analysed. It only exists to get you
 * out of bed. Success = the system camera returned RESULT_OK.
 */
@Composable
fun PhotoMission(spec: MissionSpec, onInteract: (Long) -> Unit, onComplete: () -> Unit) {
    val context = LocalContext.current
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    val prompt = remember { PhotoPrompts.pick(spec.instruction) }
    var message by remember { mutableStateOf("") }
    var unavailable by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onComplete()
        } else {
            message = "No photo yet. Give it another go once you're in position."
        }
    }

    if (!hasCamera || unavailable) {
        SensorFallback("No camera is available on this phone.", onInteract, onComplete)
        return
    }

    Column(Modifier.fillMaxWidth()) {
        Text("PHOTO", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(24.dp))
        Text(prompt, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(32.dp))
        WakeButton(
            "OK",
            onClick = {
                // The camera app takes over the screen; give the alarm service a few minutes of slack.
                onInteract(300_000L)
                message = ""
                try {
                    launcher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                } catch (e: ActivityNotFoundException) {
                    unavailable = true
                } catch (e: SecurityException) {
                    unavailable = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            filled = true
        )
        if (message.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
