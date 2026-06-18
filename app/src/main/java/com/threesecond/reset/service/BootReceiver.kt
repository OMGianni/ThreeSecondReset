package com.threesecond.reset.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

val Context.settingsDataStore by preferencesDataStore(name = "3sr_prefs")

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = runBlocking { context.settingsDataStore.data.first() }
        if (prefs[booleanPreferencesKey("was_running")] != true) return

        context.startForegroundService(Intent(context, BellService::class.java).apply {
            action = BellService.ACTION_START
            putExtra(BellService.EXTRA_START_HOUR,   prefs[intPreferencesKey("start_hour")]   ?: 7)
            putExtra(BellService.EXTRA_START_MINUTE, prefs[intPreferencesKey("start_minute")] ?: 0)
            putExtra(BellService.EXTRA_END_HOUR,     prefs[intPreferencesKey("end_hour")]     ?: 22)
            putExtra(BellService.EXTRA_END_MINUTE,   prefs[intPreferencesKey("end_minute")]   ?: 0)
        })
    }
}
