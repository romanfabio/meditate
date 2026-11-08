package io.github.romanfabio.meditate.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.romanfabio.meditate.R
import io.github.romanfabio.meditate.data.DayTotal
import io.github.romanfabio.meditate.data.SessionEntity
import io.github.romanfabio.meditate.util.formatCompact
import io.github.romanfabio.meditate.util.formatDuration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun CalendarScreen(modifier: Modifier = Modifier, viewModel: CalendarViewModel = viewModel()) {
    val month by viewModel.month.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val totals by viewModel.dayTotals.collectAsStateWithLifecycle()
    val sessions by viewModel.selectedSessions.collectAsStateWithLifecycle()
    val locale = Locale.getDefault()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item { MonthHeader(month, totals, onPrev = { viewModel.showMonth(month.minusMonths(1)) }, onNext = { viewModel.showMonth(month.plusMonths(1)) }, locale = locale) }
        item { WeekdayRow(locale) }
        item { MonthGrid(month, totals, selected, locale, onSelect = viewModel::select) }
        item { Spacer(Modifier.height(20.dp)) }
        item { DayHeader(selected, totals[selected.toString()], locale) }
        if (sessions.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.calendar_day_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            items(sessions, key = { it.id }) { SessionRow(it) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    totals: Map<String, DayTotal>,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    locale: Locale,
) {
    val title = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        .replaceFirstChar { it.titlecase(locale) } + " " + month.year
    val totalSeconds = totals.values.sumOf { it.totalSeconds }
    val count = totals.values.sumOf { it.sessionCount }
    Column(Modifier.padding(top = 16.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPrev) { Text("‹", fontSize = 28.sp) }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onNext) { Text("›", fontSize = 28.sp) }
        }
        Text(
            if (count == 0) {
                stringResource(R.string.calendar_month_empty)
            } else {
                stringResource(R.string.calendar_month_total, formatDuration(totalSeconds)) + " · " +
                    pluralStringResource(R.plurals.sessions_count, count, count)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WeekdayRow(locale: Locale) {
    val first = WeekFields.of(locale).firstDayOfWeek
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        for (i in 0 until 7) {
            Text(
                first.plus(i.toLong()).getDisplayName(TextStyle.NARROW, locale),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    totals: Map<String, DayTotal>,
    selected: LocalDate,
    locale: Locale,
    onSelect: (LocalDate) -> Unit,
) {
    val first = WeekFields.of(locale).firstDayOfWeek
    val leading = (month.atDay(1).dayOfWeek.value - first.value + 7) % 7
    val days = month.lengthOfMonth()
    val rows = (leading + days + 6) / 7
    val today = LocalDate.now()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 7) {
                    val dayNumber = row * 7 + col - leading + 1
                    Box(Modifier.weight(1f).aspectRatio(0.85f)) {
                        if (dayNumber in 1..days) {
                            val date = month.atDay(dayNumber)
                            DayCell(
                                date = date,
                                total = totals[date.toString()],
                                isSelected = date == selected,
                                isToday = date == today,
                                onClick = { onSelect(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, total: DayTotal?, isSelected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    val background = if (total != null) {
        // Tint grows with the meditated time, saturating at one hour.
        val intensity = 0.35f + 0.65f * (total.totalSeconds / 3600f).coerceIn(0f, 1f)
        scheme.primaryContainer.copy(alpha = intensity)
    } else {
        Color.Transparent
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background, shape)
            .border(
                if (isSelected) BorderStroke(2.dp, scheme.primary) else BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
                shape,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            date.dayOfMonth.toString(),
            fontSize = 15.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isToday) scheme.primary else if (total != null) scheme.onPrimaryContainer else scheme.onSurface,
        )
        Text(
            if (total != null) formatCompact(total.totalSeconds) else "",
            fontSize = 10.sp,
            maxLines = 1,
            color = scheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun DayHeader(date: LocalDate, total: DayTotal?, locale: Locale) {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    Column(Modifier.padding(bottom = 8.dp)) {
        Text(date.format(formatter), style = MaterialTheme.typography.titleMedium)
        if (total != null) {
            Text(
                stringResource(R.string.calendar_day_total, formatDuration(total.totalSeconds)) + " · " +
                    pluralStringResource(R.plurals.sessions_count, total.sessionCount, total.sessionCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SessionRow(session: SessionEntity) {
    val locale = Locale.getDefault()
    val time = Instant.ofEpochMilli(session.startedAtEpochMs)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(time, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(formatDuration(session.durationSeconds), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(if (session.completed) R.string.session_completed else R.string.session_stopped),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
