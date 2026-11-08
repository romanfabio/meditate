package io.github.romanfabio.meditate.ui.timer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.romanfabio.meditate.MeditateApp
import io.github.romanfabio.meditate.R
import io.github.romanfabio.meditate.data.SessionRepository
import io.github.romanfabio.meditate.session.SessionState
import io.github.romanfabio.meditate.util.formatClock
import io.github.romanfabio.meditate.util.formatDuration

@Composable
fun TimerScreen(modifier: Modifier = Modifier, viewModel: TimerViewModel = viewModel()) {
    val context = LocalContext.current
    val app = context.applicationContext as MeditateApp
    val manager = app.sessionManager
    val state by manager.state.collectAsStateWithLifecycle()

    Box(modifier.fillMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        when (val s = state) {
            SessionState.Idle -> IdleContent(viewModel, app)
            is SessionState.Countdown -> CountdownContent(s, onCancel = manager::stop)
            is SessionState.Running -> SessionContent(
                remainingSeconds = s.remainingSeconds,
                progress = s.progress,
                paused = false,
                onPauseResume = manager::pause,
                onStop = manager::stop,
            )
            is SessionState.Paused -> SessionContent(
                remainingSeconds = s.remainingSeconds,
                progress = s.progress,
                paused = true,
                onPauseResume = manager::resume,
                onStop = manager::stop,
            )
            is SessionState.Finished -> FinishedContent(s, onDone = manager::dismissFinished)
        }
    }
}

@Composable
private fun IdleContent(viewModel: TimerViewModel, app: MeditateApp) {
    val context = LocalContext.current
    val duration = viewModel.durationSeconds
    var dndEnabled by remember { mutableStateOf(app.prefs.dndEnabled) }
    var showDndDialog by remember { mutableStateOf(false) }

    fun startNow() = app.sessionManager.start(viewModel.durationSeconds)

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Start whether or not it was granted; without it the session just has no visible notification.
        startNow()
    }

    fun askNotificationsThenStart() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else startNow()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WheelColumn(
                value = duration / 3600,
                range = 0..23,
                label = stringResource(R.string.unit_hours),
                onChange = { viewModel.setPart(hours = it) },
            )
            Separator()
            WheelColumn(
                value = duration % 3600 / 60,
                range = 0..59,
                label = stringResource(R.string.unit_minutes),
                onChange = { viewModel.setPart(minutes = it) },
            )
            Separator()
            WheelColumn(
                value = duration % 60,
                range = 0..59,
                label = stringResource(R.string.unit_seconds),
                onChange = { viewModel.setPart(seconds = it) },
            )
        }

        Spacer(Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f).padding(end = 16.dp)) {
                Text(stringResource(R.string.dnd_switch_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.dnd_switch_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = dndEnabled,
                onCheckedChange = {
                    dndEnabled = it
                    app.prefs.dndEnabled = it
                },
            )
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                if (dndEnabled && !app.dnd.hasAccess()) showDndDialog = true else askNotificationsThenStart()
            },
            enabled = duration > 0,
            modifier = Modifier.fillMaxWidth().height(60.dp),
        ) {
            Text(stringResource(R.string.start), fontSize = 20.sp)
        }
    }

    if (showDndDialog) {
        AlertDialog(
            onDismissRequest = { showDndDialog = false },
            title = { Text(stringResource(R.string.dnd_dialog_title)) },
            text = { Text(stringResource(R.string.dnd_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDndDialog = false
                    context.startActivity(app.dnd.settingsIntent())
                }) { Text(stringResource(R.string.dnd_dialog_open)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDndDialog = false }) { Text(stringResource(R.string.dnd_dialog_cancel)) }
                    TextButton(onClick = {
                        showDndDialog = false
                        dndEnabled = false
                        app.prefs.dndEnabled = false
                        askNotificationsThenStart()
                    }) { Text(stringResource(R.string.dnd_dialog_skip)) }
                }
            },
        )
    }
}

@Composable
private fun WheelColumn(value: Int, range: IntRange, label: String, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        WheelPicker(value = value, range = range, onValueChange = onChange, description = label)
    }
}

@Composable
private fun Separator() {
    Text(
        ":",
        fontSize = 32.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp).padding(top = 28.dp),
    )
}

@Composable
private fun CountdownContent(state: SessionState.Countdown, onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.countdown_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            state.secondsLeft.toString(),
            style = TextStyle(fontSize = 120.sp, fontWeight = FontWeight.Light),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(40.dp))
        OutlinedButton(onClick = onCancel, modifier = Modifier.width(200.dp).height(52.dp)) {
            Text(stringResource(R.string.action_cancel))
        }
    }
}

@Composable
private fun SessionContent(
    remainingSeconds: Int,
    progress: Float,
    paused: Boolean,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val arc = MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 10.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                drawArc(
                    arc, -90f, 360f * (1f - progress), false, Offset(inset, inset), arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatClock(remainingSeconds),
                    style = TextStyle(fontSize = 52.sp, fontWeight = FontWeight.Light, fontFeatureSettings = "tnum"),
                )
                if (paused) {
                    Text(
                        stringResource(R.string.paused_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Spacer(Modifier.height(40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onPauseResume, modifier = Modifier.width(150.dp).height(56.dp)) {
                Text(stringResource(if (paused) R.string.action_resume else R.string.action_pause))
            }
            OutlinedButton(onClick = onStop, modifier = Modifier.width(150.dp).height(56.dp)) {
                Text(stringResource(R.string.action_stop))
            }
        }
    }
}

@Composable
private fun FinishedContent(state: SessionState.Finished, onDone: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(if (state.completed) R.string.finished_completed else R.string.finished_stopped),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.finished_meditated, formatDuration(state.meditatedSeconds)),
            style = MaterialTheme.typography.titleLarge,
        )
        if (state.meditatedSeconds < SessionRepository.MIN_RECORD_SECONDS) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.finished_not_saved, formatDuration(SessionRepository.MIN_RECORD_SECONDS)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(40.dp))
        Button(onClick = onDone, modifier = Modifier.width(200.dp).height(56.dp)) {
            Text(stringResource(R.string.done))
        }
    }
}
