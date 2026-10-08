package com.wake.alarm.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private val ROW_HEIGHT = 48.dp

/** 12-hour display text, e.g. "9:30 PM". Display only: the stored value stays a 24-hour hour and a minute. */
fun format12(hour: Int, minute: Int): String {
    val h = if (hour % 12 == 0) 12 else hour % 12
    return "%d:%02d %s".format(h, minute, if (hour < 12) "AM" else "PM")
}

/**
 * Scrolling 12-hour time picker: hour 1-12, minute 00-59, AM/PM. [hour24] and [minute] are the starting value;
 * [onChange] reports the new value as a 24-hour hour (0-23) and a minute, so nothing downstream changes.
 */
@Composable
fun WheelTimePicker(hour24: Int, minute: Int, onChange: (hour24: Int, minute: Int) -> Unit) {
    var h12 by remember { mutableIntStateOf(if (hour24 % 12 == 0) 12 else hour24 % 12) }
    var min by remember { mutableIntStateOf(minute) }
    var pm by remember { mutableIntStateOf(if (hour24 >= 12) 1 else 0) }

    fun report() = onChange((h12 % 12) + if (pm == 1) 12 else 0, min)

    Box(Modifier.fillMaxWidth().height(ROW_HEIGHT * 3), contentAlignment = Alignment.Center) {
        // The band marking the selected row.
        Box(
            Modifier
                .fillMaxWidth()
                .height(ROW_HEIGHT)
                .border(1.5.dp, MaterialTheme.colorScheme.primary)
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Wheel(12, h12 - 1, { (it + 1).toString() }, Modifier.weight(1f)) { h12 = it + 1; report() }
            Text(":", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Wheel(60, min, { "%02d".format(it) }, Modifier.weight(1f)) { min = it; report() }
            Wheel(2, pm, { if (it == 0) "AM" else "PM" }, Modifier.weight(1f)) { pm = it; report() }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Wheel(
    count: Int,
    initial: Int,
    label: (Int) -> String,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initial)
    val fling = rememberSnapFlingBehavior(lazyListState = state)

    // The row nearest the middle of the wheel.
    val centered by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - middle) }?.index ?: initial
        }
    }
    // Report only once the wheel has come to rest.
    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress) onSelect(centered)
    }

    LazyColumn(
        state = state,
        flingBehavior = fling,
        modifier = modifier.height(ROW_HEIGHT * 3),
        contentPadding = PaddingValues(vertical = ROW_HEIGHT),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(List(count) { it }) { i, _ ->
            Box(Modifier.height(ROW_HEIGHT).width(72.dp), contentAlignment = Alignment.Center) {
                Text(
                    label(i),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (i == centered) FontWeight.Bold else FontWeight.Normal,
                    fontSize = if (i == centered) 24.sp else 18.sp,
                    color = if (i == centered) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
