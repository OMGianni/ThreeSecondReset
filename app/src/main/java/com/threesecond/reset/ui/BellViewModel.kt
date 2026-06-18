package com.threesecond.reset.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.threesecond.reset.service.BellService
import com.threesecond.reset.service.settingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppState(
    val startHour:   Int     = 7,
    val startMinute: Int     = 0,
    val endHour:     Int     = 22,
    val endMinute:   Int     = 0,
    val isRunning:   Boolean = false,
    val isPaused:    Boolean = false,
    val inWindow:    Boolean = false,
    val nextBuzzMs:  Long    = -1L
)

class BellViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        viewModelScope.launch { loadPrefs() }
        _state.update { it.copy(isRunning = BellService.isRunning, isPaused = BellService.isPaused) }
    }

    private suspend fun loadPrefs() {
        val ctx   = getApplication<Application>()
        val prefs = ctx.settingsDataStore.data.first()
        _state.update { s -> s.copy(
            startHour   = prefs[intPreferencesKey("start_hour")]   ?: 7,
            startMinute = prefs[intPreferencesKey("start_minute")] ?: 0,
            endHour     = prefs[intPreferencesKey("end_hour")]     ?: 22,
            endMinute   = prefs[intPreferencesKey("end_minute")]   ?: 0,
            isRunning   = BellService.isRunning,
            isPaused    = BellService.isPaused,
        )}
    }

    fun setStartTime(h: Int, m: Int) { _state.update { it.copy(startHour = h, startMinute = m) }; savePrefs() }
    fun setEndTime(h: Int, m: Int)   { _state.update { it.copy(endHour   = h, endMinute   = m) }; savePrefs() }

    private fun savePrefs() = viewModelScope.launch {
        val ctx = getApplication<Application>(); val s = _state.value
        ctx.settingsDataStore.edit { p ->
            p[intPreferencesKey("start_hour")]   = s.startHour
            p[intPreferencesKey("start_minute")] = s.startMinute
            p[intPreferencesKey("end_hour")]     = s.endHour
            p[intPreferencesKey("end_minute")]   = s.endMinute
        }
    }

    fun start(ctx: Context) {
        val s = _state.value
        ctx.startForegroundService(Intent(ctx, BellService::class.java).apply {
            action = BellService.ACTION_START
            putExtra(BellService.EXTRA_START_HOUR,   s.startHour)
            putExtra(BellService.EXTRA_START_MINUTE, s.startMinute)
            putExtra(BellService.EXTRA_END_HOUR,     s.endHour)
            putExtra(BellService.EXTRA_END_MINUTE,   s.endMinute)
        })
        _state.update { it.copy(isRunning = true, isPaused = false) }
        persistRunning(true)
    }

    fun togglePause(ctx: Context) {
        ctx.startService(Intent(ctx, BellService::class.java).apply { action = BellService.ACTION_PAUSE })
        _state.update { it.copy(isPaused = !it.isPaused) }
    }

    fun stop(ctx: Context) {
        ctx.startService(Intent(ctx, BellService::class.java).apply { action = BellService.ACTION_STOP })
        _state.update { it.copy(isRunning = false, isPaused = false, nextBuzzMs = -1L) }
        persistRunning(false)
    }

    fun onTick(nextMs: Long, paused: Boolean, inWindow: Boolean) {
        _state.update { it.copy(nextBuzzMs = nextMs, isPaused = paused, inWindow = inWindow) }
    }

    private fun persistRunning(running: Boolean) = viewModelScope.launch {
        getApplication<Application>().settingsDataStore.edit {
            it[booleanPreferencesKey("was_running")] = running
        }
    }
}
