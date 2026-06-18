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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.threesecond.reset.service.BellService
import com.threesecond.reset.ui.BellViewModel
import com.threesecond.reset.ui.MainScreen
import com.threesecond.reset.ui.theme.ThreeSecondResetTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BellViewModel by viewModels()

    private val tickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.onTick(
                nextMs   = intent.getLongExtra(BellService.EXTRA_NEXT_MS,    -1L),
                paused   = intent.getBooleanExtra(BellService.EXTRA_PAUSED,   false),
                inWindow = intent.getBooleanExtra(BellService.EXTRA_IN_WINDOW, false)
            )
        }
    }

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
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
        // Use LocalBroadcastManager — works reliably for same-app broadcasts
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(tickReceiver, IntentFilter(BellService.BROADCAST_TICK))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(tickReceiver)
    }
}
