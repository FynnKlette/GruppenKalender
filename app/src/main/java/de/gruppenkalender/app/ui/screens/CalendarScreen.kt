package de.gruppenkalender.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.gruppenkalender.app.model.CalendarEvent
import de.gruppenkalender.app.model.CalendarGroup
import de.gruppenkalender.app.ui.components.AppTopBar
import de.gruppenkalender.app.ui.components.CalendarEventCard
import de.gruppenkalender.app.ui.components.GroupChip
import de.gruppenkalender.app.ui.components.KinshipCard
import de.gruppenkalender.app.ui.components.ScreenPadding
import de.gruppenkalender.app.ui.theme.KinshipBlue
import de.gruppenkalender.app.ui.theme.KinshipBluePale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale


//Kalender-Screen (Ansichten Tag/Woche/Monat)
@Composable
fun CalendarScreen(
    groups: List<CalendarGroup>,
    events: List<CalendarEvent>,
    onOpenEvent: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var selectedGroupIds by remember(groups.map { it.id }) {
        mutableStateOf(groups.map { it.id }.toSet())
    }
    var viewIndex by remember { mutableIntStateOf(1) }
    var weekOffset by remember { mutableIntStateOf(0) }
    var selectedDayIndex by remember { mutableIntStateOf(LocalDate.now().dayOfWeek.value - 1) }
    val thisMonday =
        LocalDate
            .now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val rangeStart = thisMonday.plusWeeks(weekOffset.toLong())
    val rangeEnd = rangeStart.plusDays(6)
    val selectedDate = rangeStart.plusDays(selectedDayIndex.toLong())
    val visibleEvents =
        events
            .filter { it.groupId in selectedGroupIds }
            .filter {
                when (viewIndex) {
                    0 -> it.startDate == selectedDate
                    2 ->
                        it.startDate.month == rangeStart.month &&
                            it.startDate.year == rangeStart.year
                    else -> !it.startDate.isBefore(rangeStart) && !it.startDate.isAfter(rangeEnd)
                }
            }.sortedWith(compareBy<CalendarEvent> { it.startDate }.thenBy { it.startTime })
            .groupBy { it.startDate }
    Column {
        AppTopBar(
            title = "GruppenKalender",
            subtitle = rangeStart.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)),
            onTrailingClick = onOpenSettings,
        )
        LazyColumn(
            contentPadding = ScreenPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CalendarRangeSelector(
                    viewIndex = viewIndex,
                    onViewChange = { viewIndex = it },
                    start = rangeStart,
                    end = rangeEnd,
                    onPrevious = { weekOffset-- },
                    onNext = { weekOffset++ },
                )
            }
            if (viewIndex == 0) {
                item {
                    WeekdaySelector(
                        selectedDayIndex = selectedDayIndex,
                        onDaySelected = { selectedDayIndex = it },
                    )
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(groups, key = { it.id }) { group ->
                        GroupChip(
                            group = group,
                            selected = group.id in selectedGroupIds,
                            onClick = {
                                selectedGroupIds =
                                    if (group.id in selectedGroupIds) {
                                        selectedGroupIds - group.id
                                    } else {
                                        selectedGroupIds + group.id
                                    }
                            },
                        )
                    }
                }
            }
            //Keine Termine Text
            if (visibleEvents.isEmpty()) {
                item {
                    KinshipCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Keine Termine in dieser Ansicht")
                            Text(
                                "Passe den Zeitraum oder die Gruppenfilter an.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                //Kalendereinträge
            } else {
                visibleEvents.forEach { (date, dateEvents) ->
                    item(key = "date-$date") {
                        Text(
                            date.format(
                                DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN),
                            ),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(KinshipBlue, RoundedCornerShape(5.dp))
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    items(dateEvents, key = { it.id }) { event ->
                        CalendarEventCard(
                            event = event,
                            group = groups.find { it.id == event.groupId },
                            onClick = { onOpenEvent(event.id) },
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(82.dp)) }
        }
    }
}

//UI Filter
@Composable
private fun WeekdaySelector(
    selectedDayIndex: Int,
    onDaySelected: (Int) -> Unit,
) {
    val weekdays = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

    KinshipCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            weekdays.forEachIndexed { index, label ->
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color =
                        if (selectedDayIndex == index) {
                            KinshipBlue
                        } else {
                            Color.Transparent
                        },
                    onClick = { onDaySelected(index) },
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        color =
                            if (selectedDayIndex == index) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        fontWeight =
                            if (selectedDayIndex == index) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                    )
                }
            }
        }
    }
}
@Composable
private fun CalendarRangeSelector(
    viewIndex: Int,
    onViewChange: (Int) -> Unit,
    start: LocalDate,
    end: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    KinshipCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth()) {
                listOf("Tag", "Woche", "Monat").forEachIndexed { index, label ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (viewIndex == index) KinshipBluePale else Color.Transparent,
                        onClick = { onViewChange(index) },
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(9.dp),
                            textAlign = TextAlign.Center,
                            color =
                                if (viewIndex == index) {
                                    KinshipBlue
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(KinshipBlue, RoundedCornerShape(22.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        "Vorheriger Zeitraum",
                        tint = Color.White,
                    )
                }
                Text(
                    "${start.format(DateTimeFormatter.ofPattern("dd.MM."))} – ${
                        end.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    }",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                )
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        "Nächster Zeitraum",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}
