package com.threesecond.reset.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.threesecond.reset.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

class BellService : Service() {

    companion object {
        const val CHANNEL_ID      = "3sr_service"
        const val NOTIFICATION_ID = 1
        const val INTERVAL_MS     = 6 * 60 * 1000L

        const val ACTION_START = "com.threesecond.reset.START"
        const val ACTION_STOP  = "com.threesecond.reset.STOP"
        const val ACTION_PAUSE = "com.threesecond.reset.PAUSE"

        const val EXTRA_START_HOUR   = "start_hour"
        const val EXTRA_START_MINUTE = "start_minute"
        const val EXTRA_END_HOUR     = "end_hour"
        const val EXTRA_END_MINUTE   = "end_minute"

        const val BROADCAST_TICK  = "com.threesecond.reset.TICK"
        const val EXTRA_NEXT_MS   = "next_ms"
        const val EXTRA_PAUSED    = "paused"
        const val EXTRA_IN_WINDOW = "in_window"

        var isRunning = false
        var isPaused  = false

        private const val PREFS_NAME = "bell_service_prefs"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var startHour   = 7
    private var startMinute = 0
    private var endHour     = 22
    private var endMinute   = 0
    private var nextVibeAt  = 0L

    private lateinit var prefs: SharedPreferences

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startHour   = intent.getIntExtra(EXTRA_START_HOUR,   prefs.getInt(EXTRA_START_HOUR,   7))
                startMinute = intent.getIntExtra(EXTRA_START_MINUTE, prefs.getInt(EXTRA_START_MINUTE, 0))
                endHour     = intent.getIntExtra(EXTRA_END_HOUR,     prefs.getInt(EXTRA_END_HOUR,     22))
                endMinute   = intent.getIntExtra(EXTRA_END_MINUTE,   prefs.getInt(EXTRA_END_MINUTE,   0))

                prefs.edit()
                    .putInt(EXTRA_START_HOUR,   startHour)
                    .putInt(EXTRA_START_MINUTE, startMinute)
                    .putInt(EXTRA_END_HOUR,     endHour)
                    .putInt(EXTRA_END_MINUTE,   endMinute)
                    .apply()

                // Toast on main thread so you can see what the service received
                val cal   = Calendar.getInstance()
                val nowH  = cal.get(Calendar.HOUR_OF_DAY)
                val nowM2 = cal.get(Calendar.MINUTE)
                mainHandler.post {
                    Toast.makeText(
                        applicationContext,
                        "Now: $nowH:${"%02d".format(nowM2)} | Window: $startHour:${"%02d".format(startMinute)}-$endHour:${"%02d".format(endMinute)}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                isRunning = true
                isPaused  = false

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, buildNotification("Active"),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIFICATION_ID, buildNotification("Active"))
                }

                scheduleNext()
                startTickLoop()
            }
            ACTION_PAUSE -> {
                isPaused = !isPaused
                updateNotification(if (isPaused) "Paused" else "Active")
                if (!isPaused) scheduleNext()
            }
            ACTION_STOP -> {
                isRunning = false
                isPaused  = false
                timerJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            null -> {
                // Android restarted us after kill — restore from prefs
                startHour   = prefs.getInt(EXTRA_START_HOUR,   7)
                startMinute = prefs.getInt(EXTRA_START_MINUTE, 0)
                endHour     = prefs.getInt(EXTRA_END_HOUR,     22)
                endMinute   = prefs.getInt(EXTRA_END_MINUTE,   0)
                isRunning   = true
                isPaused    = false

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, buildNotification("Active"),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIFICATION_ID, buildNotification("Active"))
                }

                scheduleNext()
                startTickLoop()
            }
        }
        return START_STICKY
    }

    private fun scheduleNext() {
        nextVibeAt = System.currentTimeMillis() + INTERVAL_MS
    }

    private fun startTickLoop() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isRunning) {
                val now      = System.currentTimeMillis()
                val inWindow = isInActiveWindow()

                if (!isPaused && inWindow) {
                    if (now >= nextVibeAt) {
                        vibrate()
                        scheduleNext()
                    }
                } else if (!inWindow) {
                    nextVibeAt = nextWindowStart()
                }

                val remaining = if (!isPaused && inWindow)
                    (nextVibeAt - now).coerceAtLeast(0) else -1L

                sendBroadcast(Intent(BROADCAST_TICK).apply {
                    putExtra(EXTRA_NEXT_MS,   remaining)
                    putExtra(EXTRA_PAUSED,    isPaused)
                    putExtra(EXTRA_IN_WINDOW, inWindow)
                })

                delay(1000L)
            }
        }
    }

    private fun isInActiveWindow(): Boolean {
        val cal    = Calendar.getInstance()
        val nowM   = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startM = startHour * 60 + startMinute
        val endM   = endHour   * 60 + endMinute
        val result = nowM in startM..endM
        android.util.Log.d("BellService",
            "nowM=$nowM startM=$startM endM=$endM inWindow=$result (${startHour}:${"%02d".format(startMinute)}-${endHour}:${"%02d".format(endMinute)})")
        return result
    }

    private fun nextWindowStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, startHour)
        cal.set(Calendar.MINUTE,      startMinute)
        cal.set(Calendar.SECOND,      0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun vibrate() {
        val effect = VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(effect)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "3 Second Reset", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Session indicator"
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("3 Second Reset")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        isRunning = false
        timerJob?.cancel()
        super.onDestroy()
    }
}
