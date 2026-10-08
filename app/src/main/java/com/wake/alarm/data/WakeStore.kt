package com.wake.alarm.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * All persistent state lives in one JSON file in app-private storage.
 * No network, no account, no database.
 */
object WakeStore {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val lock = Any()
    private var file: File? = null
    private val _state = MutableStateFlow(AppData())
    val state: StateFlow<AppData> = _state

    fun init(context: Context) {
        synchronized(lock) {
            if (file != null) return
            val f = File(context.applicationContext.filesDir, "wake.json")
            file = f
            _state.value = load(f)
        }
    }

    private fun load(f: File): AppData {
        if (!f.exists()) return AppData()
        return try {
            json.decodeFromString<AppData>(f.readText())
        } catch (e: Exception) {
            // Keep the unreadable file around instead of silently destroying it.
            runCatching { f.copyTo(File(f.parentFile, "wake.corrupt.json"), overwrite = true) }
            AppData()
        }
    }

    private fun persist(data: AppData) {
        val f = file ?: return
        val text = json.encodeToString(data)
        val tmp = File(f.parentFile, "wake.json.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(f)) {
            f.writeText(text)
            tmp.delete()
        }
    }

    fun mutate(change: (AppData) -> AppData) {
        synchronized(lock) {
            val next = change(_state.value)
            if (next != _state.value) {
                persist(next)
                _state.value = next
            }
        }
    }

    fun alarms(): List<Alarm> = _state.value.alarms

    fun get(id: Int): Alarm? = _state.value.alarms.firstOrNull { it.id == id }
}
