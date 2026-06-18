package com.threesecond.reset

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.threesecond.reset.service.BellService
import com.threesecond.reset.ui.BellViewModel
import com.threesecond.reset.ui.MainScreen
import com.threesecond.reset.ui.theme.ThreeSecondResetTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BellViewModel by viewModels()

    private val tickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.onTick(
                nextMs   = intent.getLongExtra(BellService.EXTRA_NEXT_MS,   -1L),
                paused   = intent.getBooleanExtra(BellService.EXTRA_PAUSED,   false),
                inWindow = intent.getBooleanExtra(BellService.EXTRA_IN_WINDOW, false)
            )
        }
    }

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* proceed regardless */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            ThreeSecondResetTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(BellService.BROADCAST_TICK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(tickReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(tickReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(tickReceiver)
    }
}
