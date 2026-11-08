package io.github.romanfabio.meditate

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.romanfabio.meditate.ui.calendar.CalendarScreen
import io.github.romanfabio.meditate.ui.theme.MeditateTheme
import io.github.romanfabio.meditate.ui.timer.TimerScreen

private enum class Tab(@StringRes val label: Int, @DrawableRes val icon: Int) {
    Timer(R.string.tab_timer, R.drawable.ic_tab_timer),
    Calendar(R.string.tab_calendar, R.drawable.ic_tab_calendar),
    About(R.string.tab_about, R.drawable.ic_tab_info),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The bells play on the alarm stream, so the volume keys should control that stream.
        volumeControlStream = AudioManager.STREAM_ALARM
        setContent {
            MeditateTheme {
                MeditateRoot()
            }
        }
    }
}

@Composable
private fun MeditateRoot() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { padding ->
        val content = Modifier.padding(padding)
        when (Tab.entries[selected]) {
            Tab.Timer -> TimerScreen(content)
            Tab.Calendar -> CalendarScreen(content)
            Tab.About -> Box(content.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.tab_about)) // replaced in the About milestone
            }
        }
    }
}
