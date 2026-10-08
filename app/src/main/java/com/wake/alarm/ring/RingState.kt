package com.wake.alarm.ring

import com.wake.alarm.data.Alarm
import kotlinx.coroutines.flow.MutableStateFlow

/** What the service is doing right now. The service writes it; RingActivity observes it. */
data class RingUiState(
    val alarm: Alarm? = null,
    val isTest: Boolean = false,
    val soundOn: Boolean = false,
    /** True once the wake-up ended (snoozed or completed). The activity closes itself. */
    val finished: Boolean = false
)

object RingState {
    val flow = MutableStateFlow(RingUiState())
}
