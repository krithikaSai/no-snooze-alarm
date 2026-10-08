package com.wake.alarm.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wake.alarm.data.AssetCatalog
import com.wake.alarm.data.Difficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WakeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
    accent: Color? = null
) {
    val ink = accent ?: MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .background(if (filled) ink else paper)
            .border(BorderStroke(1.5.dp, ink))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (filled) paper else ink,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/** Plain "BACK" button for the alarm editor: solid blue, softly rounded, no icon. */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "BACK",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * A square checkbox. Off: an empty square outline. On: a solid blue square with a clear tick.
 * Drawn by hand (no icon library needed), flat and square to match the rest of the app.
 */
@Composable
fun SquareCheck(checked: Boolean, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.primary
    val tick = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier
            .size(26.dp)
            .background(if (checked) accent else Color.Transparent)
            .border(2.dp, if (checked) accent else ink),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Canvas(Modifier.size(26.dp)) {
                val w = size.width
                val h = size.height
                val stroke = 3.dp.toPx()
                drawLine(tick, Offset(w * 0.22f, h * 0.52f), Offset(w * 0.43f, h * 0.72f), strokeWidth = stroke, cap = StrokeCap.Square)
                drawLine(tick, Offset(w * 0.43f, h * 0.72f), Offset(w * 0.78f, h * 0.30f), strokeWidth = stroke, cap = StrokeCap.Square)
            }
        }
    }
}

/** A tappable row with a square checkbox on the left. The whole row is the tap target. */
@Composable
fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true, sub: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SquareCheck(checked)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, topSpace: Dp = 24.dp) {
    Column(modifier.fillMaxWidth().padding(top = topSpace, bottom = 8.dp)) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)
    }
}

@Composable
fun Stepper(
    value: Int,
    onChange: (Int) -> Unit,
    min: Int,
    max: Int,
    step: Int = 1,
    suffix: String = ""
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SquareButton("−", enabled = value > min) { onChange((value - step).coerceAtLeast(min)) }
        Text(
            "$value$suffix",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(88.dp)
        )
        SquareButton("+", enabled = value < max) { onChange((value + step).coerceAtMost(max)) }
    }
}

@Composable
fun SquareButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    Box(
        Modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .border(1.5.dp, ink)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}

@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    Row(Modifier.fillMaxWidth().border(1.5.dp, ink)) {
        options.forEachIndexed { i, (value, label) ->
            val on = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else ink
                )
            }
            if (i < options.lastIndex) Box(Modifier.width(1.5.dp).heightIn(min = 44.dp).background(ink))
        }
    }
}

@Composable
fun DifficultyPicker(value: Difficulty, onChange: (Difficulty) -> Unit) {
    Segmented(
        listOf(Difficulty.EASY to "EASY", Difficulty.MEDIUM to "MEDIUM", Difficulty.HARD to "HARD"),
        value, onChange
    )
}

@Composable
fun WakeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    singleLine: Boolean = false,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = label?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = minLines
    )
}

@Composable
fun WakeDialog(text: String, onOk: () -> Unit, okLabel: String = "OK") {
    AlertDialog(
        onDismissRequest = onOk,
        text = { Text(text, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { TextButton(onClick = onOk) { Text(okLabel, fontWeight = FontWeight.Bold) } }
    )
}

@Composable
fun AssetImage(
    path: String,
    modifier: Modifier = Modifier,
    maxPx: Int = 1080,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = if (path.isBlank()) null else withContext(Dispatchers.IO) { AssetCatalog.decodeImage(context, path, maxPx) }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = null, modifier = modifier, contentScale = contentScale)
    } else {
        Box(modifier)
    }
}
