package com.threesecond.reset.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.threesecond.reset.ui.theme.Gold
import com.threesecond.reset.ui.theme.GoldDim
import com.threesecond.reset.ui.theme.GoldFaint
import com.threesecond.reset.ui.theme.SurfaceCard

@Composable
fun MainScreen(viewModel: BellViewModel) {
    val state by viewModel.state.collectAsState()
    val ctx = LocalContext.current

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker   by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        // Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "3 Second Reset",
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                color = Gold
            )
            Text(
                text = "One vibration every 6 minutes",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

        // Active hours section
        Text(
            text = "Active hours",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeButton(
                label = "From",
                value = formatTime(state.startHour, state.startMinute),
                enabled = !state.isRunning,
                modifier = Modifier.weight(1f),
                onClick = { showStartPicker = true }
            )
            TimeButton(
                label = "Until",
                value = formatTime(state.endHour, state.endMinute),
                enabled = !state.isRunning,
                modifier = Modifier.weight(1f),
                onClick = { showEndPicker = true }
            )
        }

        Spacer(Modifier.height(4.dp))

        // Status card — only visible when running
        if (state.isRunning) {
            StatusCard(state)
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.weight(1f))

        // Action buttons
        if (!state.isRunning) {
            GoldButton(
                text = "Start session",
                onClick = { viewModel.start(ctx) }
            )
        } else {
            GoldButton(
                text = if (state.isPaused) "Resume" else "Pause",
                outlined = state.isPaused,
                onClick = { viewModel.togglePause(ctx) }
            )
            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = { viewModel.stop(ctx) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Stop session",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }

    if (showStartPicker) {
        GoldTimePickerDialog(
            title = "From",
            initialHour = state.startHour,
            initialMinute = state.startMinute,
            onConfirm = { h, m -> viewModel.setStartTime(h, m); showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showEndPicker) {
        GoldTimePickerDialog(
            title = "Until",
            initialHour = state.endHour,
            initialMinute = state.endMinute,
            onConfirm = { h, m -> viewModel.setEndTime(h, m); showEndPicker = false },
            onDismiss = { showEndPicker = false }
        )
    }
}

@Composable
fun TimeButton(label: String, value: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceCard,
            disabledContainerColor = SurfaceCard.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Medium,
                color = if (enabled) Gold else GoldDim)
        }
    }
}

@Composable
fun StatusCard(state: AppState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GoldFaint)
            .border(0.5.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val statusText = when {
                state.isPaused    -> "Paused"
                state.inWindow    -> "Next reset in"
                else              -> "Waiting for active window"
            }
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = GoldDim,
                letterSpacing = 0.5.sp
            )

            if (state.inWindow && !state.isPaused && state.nextBuzzMs > 0) {
                AnimatedContent(
                    targetState = formatCountdown(state.nextBuzzMs),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "countdown"
                ) { label ->
                    Text(
                        text = label,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Medium,
                        color = Gold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun GoldButton(text: String, outlined: Boolean = false, onClick: () -> Unit) {
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Gold),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
        ) {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Gold,
                contentColor   = Color(0xFF1A1400)
            )
        ) {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerState = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceCard,
        titleContentColor = Gold,
        title = { Text(title) },
        text  = { TimePicker(state = pickerState) },
        confirmButton = { TextButton(onClick = { onConfirm(pickerState.hour, pickerState.minute) }) { Text("OK", color = Gold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    )
}

fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)
fun formatCountdown(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
