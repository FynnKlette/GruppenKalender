package de.gruppenkalender.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.gruppenkalender.app.model.CalendarEvent
import de.gruppenkalender.app.model.CalendarGroup
import de.gruppenkalender.app.ui.components.AppTopBar
import de.gruppenkalender.app.ui.components.CalendarEventCard
import de.gruppenkalender.app.ui.components.GroupChip
import de.gruppenkalender.app.ui.components.KinshipCard
import de.gruppenkalender.app.ui.components.ScreenPadding
import de.gruppenkalender.app.ui.components.SectionTitle
import de.gruppenkalender.app.ui.theme.KinshipBlue
import de.gruppenkalender.app.ui.theme.KinshipBluePale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

//Home-Screen (inkl. nächste 4 Termine, Gruppen)
@Composable
fun HomeScreen(
    groups: List<CalendarGroup>,
    events: List<CalendarEvent>,
    onOpenGroups: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val now = LocalDate.now()
    val monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    var periodIndex by remember { mutableIntStateOf(1) }

    Column {
        AppTopBar(
            title = "GruppenKalender",
            subtitle = now.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)),
            onTrailingClick = onOpenSettings,
        )
        LazyColumn(
            contentPadding = ScreenPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {/*
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(groups, key = { it.id }) { group ->
                        GroupChip(group = group, selected = group == groups.firstOrNull()) {}
                    }
                }
            }*/
            //Termine (verkürzt)
            item {
                SectionTitle("Anstehende Termine", "Alle ansehen", onOpenCalendar)
            }
            val upcoming =
                events
                    .filter { !it.startDate.isBefore(now) }
                    .sortedWith(compareBy<CalendarEvent> { it.startDate }.thenBy { it.startTime })
                    .take(4)
            //keine anstehenden Termine Text
            if (upcoming.isEmpty()) {
                item {
                    KinshipCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Keine anstehenden Termine",
                            modifier = Modifier.padding(20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                //Terminliste
            } else {
                items(upcoming, key = { it.id }) { event ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            event.startDate.format(
                                DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN),
                            ),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(KinshipBlue, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        CalendarEventCard(
                            event = event,
                            group = groups.find { it.id == event.groupId },
                            onClick = { onOpenEvent(event.id) },
                        )
                    }
                }
            }
            //Gruppen-Übersicht (vereinfacht)
            item {
                Spacer(Modifier.height(6.dp))
                SectionTitle("Meine Gruppen", "Verwalten", onOpenGroups)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(groups, key = { it.id }) { group ->
                        GroupSummaryCard(group)
                    }
                }
            }
            item { Spacer(Modifier.height(76.dp)) }
        }
    }
}


@Composable
private fun GroupSummaryCard(group: CalendarGroup) {
    val accent = Color(group.accent)
    Surface(
        modifier = Modifier.size(width = 154.dp, height = 104.dp),
        color = accent.copy(alpha = 0.18f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1A1C1E)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(Icons.Outlined.Groups, contentDescription = null, tint = accent)
            Text(group.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(
                "${group.memberCount} Mitglieder",
                color = accent,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
