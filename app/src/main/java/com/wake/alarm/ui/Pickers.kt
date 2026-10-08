package com.wake.alarm.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wake.alarm.data.AssetCatalog

/** Short in-app preview of an alarm sound. Uses the alarm stream so you hear it at alarm volume. */
class SoundPreview {
    private var player: MediaPlayer? = null

    fun play(context: android.content.Context, path: String, onDone: () -> Unit): Boolean {
        stop()
        val mp = MediaPlayer()
        return try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            context.assets.openFd(path).use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            mp.setOnCompletionListener { stop(); onDone() }
            mp.prepare()
            mp.start()
            player = mp
            true
        } catch (e: Exception) {
            mp.release()
            false
        }
    }

    fun stop() {
        player?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        player = null
    }
}

@Composable
fun SoundPicker(current: String, onPick: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val sounds = remember { AssetCatalog.sounds(context) }
    val preview = remember { SoundPreview() }
    var playing by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { preview.stop() } }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        WakeButton("← BACK", onClick = onBack)
        Spacer(Modifier.height(16.dp))
        Text("ALARM SOUND", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Text("Tap PLAY to hear one. Tap the name to choose it.", style = MaterialTheme.typography.bodyMedium)
        if (failed) Text("That file couldn't be played.", style = MaterialTheme.typography.bodyMedium)

        if (sounds.values.all { it.isEmpty() }) {
            Spacer(Modifier.height(16.dp))
            Text(
                "No sounds are bundled yet. Add files under assets/audio/<category>/ and rebuild. " +
                    "Until then the phone's default alarm tone is used.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(16.dp))
        CheckRow("System default alarm tone", checked = current.isBlank(), onChange = { onPick("") })

        sounds.forEach { (category, files) ->
            if (files.isEmpty()) return@forEach
            SectionTitle(AssetCatalog.prettyCategory(category), topSpace = 40.dp)
            Spacer(Modifier.height(6.dp))
            files.forEach { path ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    val isPlaying = playing == path
                    WakeButton(
                        if (isPlaying) "STOP" else "PLAY",
                        onClick = {
                            if (isPlaying) {
                                preview.stop()
                                playing = null
                            } else {
                                failed = !preview.play(context, path) { playing = null }
                                playing = if (failed) null else path
                            }
                        },
                        modifier = Modifier.width(92.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Box(Modifier.weight(1f)) {
                        CheckRow(AssetCatalog.prettyName(path), checked = current == path, onChange = { onPick(path) })
                    }
                }
            }
        }
    }
}

@Composable
fun BackgroundPicker(current: String, onPick: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val images = remember { AssetCatalog.backgrounds(context) }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        WakeButton("← BACK", onClick = onBack)
        Spacer(Modifier.height(16.dp))
        Text("BACKGROUND", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        if (images.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "No images are bundled yet. Add files under assets/backgrounds/ and rebuild. Until then the screen is plain black.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        CheckRow("None (plain black)", checked = current.isBlank(), onChange = { onPick("") })
        images.forEach { path ->
            Row(
                Modifier.fillMaxWidth().clickable { onPick(path) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssetImage(
                    path,
                    Modifier.size(width = 112.dp, height = 72.dp).border(1.dp, MaterialTheme.colorScheme.onBackground),
                    maxPx = 320
                )
                Spacer(Modifier.width(12.dp))
                Text(AssetCatalog.prettyName(path), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                SquareCheck(current == path)
            }
        }
    }
}
