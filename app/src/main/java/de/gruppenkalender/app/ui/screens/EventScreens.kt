package de.gruppenkalender.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.gruppenkalender.app.model.CalendarEvent
import de.gruppenkalender.app.model.CalendarGroup
import de.gruppenkalender.app.model.EventCategory
import de.gruppenkalender.app.ui.components.AppTopBar
import de.gruppenkalender.app.ui.components.CardShape
import de.gruppenkalender.app.ui.components.GermanDateFormatter
import de.gruppenkalender.app.ui.components.KinshipCard
import de.gruppenkalender.app.ui.components.ScreenPadding
import de.gruppenkalender.app.ui.components.ShortDateFormatter
import de.gruppenkalender.app.ui.components.TimeFormatter
import de.gruppenkalender.app.ui.theme.KinshipBlue
import de.gruppenkalender.app.ui.theme.KinshipGreen
import de.gruppenkalender.app.ui.theme.KinshipInk
import de.gruppenkalender.app.ui.theme.KinshipRed
import java.time.LocalDate
import java.time.LocalTime
//Termin hinzufügen/bearbeiten
data class EventDraft(
    val title: String,
    val groupId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val location: String,
    val description: String,
    val participants: List<String>,
    val category: EventCategory,
)

@Composable
fun EventEditorScreen(
    event: CalendarEvent?,
    groups: List<CalendarGroup>,
    onBack: () -> Unit,
    onSave: (EventDraft) -> Unit,
) {
    val today = LocalDate.now()
    var title by rememberSaveable(event?.id) { mutableStateOf(event?.title.orEmpty()) }
    var selectedGroupId by rememberSaveable(event?.id) {
        mutableStateOf(event?.groupId ?: groups.firstOrNull()?.id.orEmpty())
    }
    var startDateText by rememberSaveable(event?.id) {
        mutableStateOf((event?.startDate ?: today).format(ShortDateFormatter))
    }
    var endDateText by rememberSaveable(event?.id) {
        mutableStateOf((event?.endDate ?: today).format(ShortDateFormatter))
    }
    var startTimeText by rememberSaveable(event?.id) {
        mutableStateOf((event?.startTime ?: LocalTime.of(19, 0)).format(TimeFormatter))
    }
    var endTimeText by rememberSaveable(event?.id) {
        mutableStateOf((event?.endTime ?: LocalTime.of(21, 0)).format(TimeFormatter))
    }
    var location by rememberSaveable(event?.id) { mutableStateOf(event?.location.orEmpty()) }
    var description by rememberSaveable(event?.id) {
        mutableStateOf(event?.description.orEmpty())
    }
    var categoryName by rememberSaveable(event?.id) {
        mutableStateOf((event?.category ?: EventCategory.FAMILY).name)
    }
    var selectedPeople by rememberSaveable(event?.id) {
        mutableStateOf(
            event
                ?.participants
                ?.toSet()
                .orEmpty(),
        )
    }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val accent = Color(groups.find { it.id == selectedGroupId }?.accent ?: 0xFF4A76C0.toInt())
    val availablePeople =
        (
            groups
                .find {
                    it.id == selectedGroupId
                }
                ?.memberNames
                ?.values
                .orEmpty() +
                selectedPeople
            )
            .distinct()
            .sorted()

    Column {
        AppTopBar(
            title = if (event == null) "Neuer Eintrag" else "Eintrag bearbeiten",
            onBack = onBack,
            trailingIcon = Icons.Outlined.Close,
            onTrailingClick = onBack,
        )
        LazyColumn(
            contentPadding = ScreenPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent),
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.25f),
                        modifier =
                            Modifier
                                .size(110.dp)
                                .align(Alignment.CenterEnd)
                                .padding(end = 18.dp),
                    )
                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                    ) {
                        Text(
                            "PLANUNG",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            if (title.isBlank()) "Nächste Schritte festlegen" else title,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                }
            }
            //Termin erstellen
            item {
                FieldLabel("EVENT TITEL")
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("z. B. Training oder Zahnarzt") },
                    singleLine = true,
                    isError = error != null && title.isBlank(),
                )
            }
            item {
                FieldLabel("GRUPPE")
                KinshipCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        groups.forEach { group ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = selectedGroupId == group.id,
                                    onCheckedChange = {
                                        if (
                                            it &&
                                            selectedGroupId != group.id
                                        ) {
                                            selectedGroupId = group.id
                                            selectedPeople = emptySet()
                                        }
                                    },
                                )
                                Text(group.name)
                            }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel("DATUM")
                        OutlinedTextField(
                            value = startDateText,
                            onValueChange = { startDateText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        FieldLabel("ENDDATUM")
                        OutlinedTextField(
                            value = endDateText,
                            onValueChange = { endDateText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel("STARTZEIT")
                        OutlinedTextField(
                            value = startTimeText,
                            onValueChange = { startTimeText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        FieldLabel("ENDZEIT")
                        OutlinedTextField(
                            value = endTimeText,
                            onValueChange = { endTimeText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }
            }
            item {
                FieldLabel("ORT")
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, null) },
                    placeholder = { Text("Ort") },
                    singleLine = true,
                )
            }
            item {
                FieldLabel("BESCHREIBUNG")
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    placeholder = { Text("Zusätzliche Infos zur Ausrüstung oder Mitbringsel …") },
                )
            }
            item {
                FieldLabel("KATEGORIE")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EventCategory.entries, key = { it.name }) { category ->
                        val selected = category.name == categoryName
                        Surface(
                            onClick = { categoryName = category.name },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) KinshipBlue else Color(0xFFEEEFF2),
                            border =
                                androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selected) KinshipBlue else Color(0xFFC3C6D2),
                                ),
                        ) {
                            Text(
                                category.germanLabel(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = if (selected) Color.White else KinshipInk,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
            item {
                FieldLabel("GRUPPENMITGLIEDER")

                LazyRow(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp),
                ) {
                    if (availablePeople.isEmpty()) {
                        item {
                            Text(
                                "Keine Mitglieder verfügbar",
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant,
                            )
                        }
                    }

                    items(
                        items = availablePeople,
                        key = { it },
                    ) { person ->
                        PersonChip(
                            name = person,
                            selected =
                                person in selectedPeople,
                            onClick = {
                                selectedPeople =
                                    if (
                                        person in selectedPeople
                                    ) {
                                        selectedPeople - person
                                    } else {
                                        selectedPeople + person
                                    }
                            },
                        )
                    }
                }
            }
            if (error != null) {
                item {
                    Text(
                        error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            item {
                Button(
                    onClick = {
                        val parsed =
                            runCatching {
                                EventDraft(
                                    title = title,
                                    groupId = selectedGroupId,
                                    startDate = LocalDate.parse(startDateText, ShortDateFormatter),
                                    endDate = LocalDate.parse(endDateText, ShortDateFormatter),
                                    startTime = LocalTime.parse(startTimeText, TimeFormatter),
                                    endTime = LocalTime.parse(endTimeText, TimeFormatter),
                                    location = location,
                                    description = description,
                                    participants = selectedPeople.toList(),
                                    category = EventCategory.valueOf(categoryName),
                                )
                            }.getOrNull()
                        //Error-Texte
                        error =
                            when {
                                title.isBlank() -> "Bitte gib einen Titel ein."
                                selectedGroupId.isBlank() -> "Bitte wähle eine Gruppe."
                                parsed == null -> "Bitte prüfe Datum (TT.MM.JJJJ) und Uhrzeit (HH:MM)."
                                parsed.endDate.isBefore(parsed.startDate) ->
                                    "Das Enddatum darf nicht vor dem Startdatum liegen."
                                parsed.startDate == parsed.endDate &&
                                    !parsed.endTime.isAfter(parsed.startTime) ->
                                    "Die Endzeit muss nach der Startzeit liegen."
                                else -> null
                            }
                        if (error == null && parsed != null) onSave(parsed)
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        if (event == null) "Eintrag erstellen" else "Änderungen speichern",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            item { Spacer(Modifier.height(78.dp)) }
        }
    }
}
//Termin-Ansicht
@Composable
fun EventDetailsScreen(
    event: CalendarEvent,
    group: CalendarGroup?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var attending by rememberSaveable(event.id) { mutableStateOf<Boolean?>(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val accent = Color(group?.accent ?: 0xFF4A76C0.toInt())

    Column {
        AppTopBar(
            title = "Termindetails",
            onBack = onBack,
            trailingIcon = Icons.Outlined.Edit,
            onTrailingClick = onEdit,
        )
        LazyColumn(
            contentPadding = ScreenPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(174.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent),
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier =
                            Modifier
                                .size(140.dp)
                                .align(Alignment.CenterEnd),
                    )
                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            group?.name ?: "Termin",
                            modifier =
                                Modifier
                                    .background(
                                        Color.White.copy(alpha = 0.92f),
                                        RoundedCornerShape(12.dp),
                                    ).padding(horizontal = 10.dp, vertical = 4.dp),
                            color = accent,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            event.title,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text(
                            "${event.startDate.format(GermanDateFormatter)} | " +
                                "${event.startTime.format(TimeFormatter)} Uhr",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "${event.endDate.format(GermanDateFormatter)} | " +
                                "${event.endTime.format(TimeFormatter)} Uhr",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            item {
                KinshipCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            when (attending) {
                                true -> "✓ Du nimmst teil"
                                false -> "Du hast abgesagt"
                                null -> "Teilnahme offen"
                            },
                            color =
                                if (attending == true) {
                                    KinshipGreen
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { attending = true },
                                modifier = Modifier.weight(1f),
                            ) { Text("👍 Zusage") }
                            OutlinedButton(
                                onClick = { attending = false },
                                modifier = Modifier.weight(1f),
                            ) { Text("👎 Absage") }
                        }
                    }
                }
            }
            item {
                KinshipCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("ORT", style = MaterialTheme.typography.labelSmall)
                                Text(event.location.ifBlank { "Kein Ort angegeben" })
                            }
                            OutlinedButton(onClick = {}) {
                                Icon(Icons.Outlined.Route, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Route")
                            }
                        }
                        MapPlaceholder()
                    }
                }
            }
            item {
                KinshipCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("BESCHREIBUNG", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            event.description.ifBlank { "Keine Beschreibung vorhanden." },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "TEILNEHMER (${event.participants.size})",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text("Alle ansehen", color = KinshipBlue, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(event.participants) { person ->
                        PersonChip(name = person, selected = true, onClick = {})
                    }
                }
            }
            //Termin löschen
            item {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = KinshipRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KinshipRed),
                ) {
                    Icon(Icons.Outlined.Delete, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Termin löschen")
                }
            }
            item { Spacer(Modifier.height(76.dp)) }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Termin löschen?") },
            text = { Text("„${event.title}“ wird dauerhaft aus diesem Kalender entfernt.") },
            confirmButton = {
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = KinshipRed),
                ) { Text("LÖSCHEN") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("ABBRECHEN") }
            },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall)
    Spacer(Modifier.height(5.dp))
}

@Composable
private fun PersonChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val initialColor =
        when (name.firstOrNull()?.uppercaseChar()) {
            'A', 'J' -> Color(0xFF4A76C0)
            'B', 'L' -> Color(0xFF27AE60)
            else -> Color(0xFFF2994A)
        }
    Surface(
        onClick = onClick,
        color = if (selected) initialColor.copy(alpha = 0.16f) else Color.White,
        shape = RoundedCornerShape(18.dp),
        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,
                if (selected) initialColor else Color(0xFF737782),
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .size(22.dp)
                    .background(initialColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name.take(1),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(name, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MapPlaceholder() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(118.dp)
                .border(1.dp, Color(0xFFC3C6D2), CardShape)
                .background(Color(0xFFF0F2F5), CardShape),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val road = Color(0xFFD3D7DE)
            drawLine(
                road,
                start =
                    androidx.compose.ui.geometry
                        .Offset(0f, size.height * 0.7f),
                end =
                    androidx.compose.ui.geometry
                        .Offset(size.width, size.height * 0.25f),
                strokeWidth = 18f,
                cap = StrokeCap.Round,
            )
            drawLine(
                road,
                start =
                    androidx.compose.ui.geometry
                        .Offset(size.width * 0.22f, 0f),
                end =
                    androidx.compose.ui.geometry
                        .Offset(size.width * 0.72f, size.height),
                strokeWidth = 12f,
                cap = StrokeCap.Round,
            )
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .size(38.dp)
                    .background(KinshipBlue, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.LocationOn, null, tint = Color.White)
        }
    }
}

private fun EventCategory.germanLabel(): String =
    when (this) {
        EventCategory.SPORT -> "Sport"
        EventCategory.FAMILY -> "Familie"
        EventCategory.WORK -> "Arbeit"
        EventCategory.TRAVEL -> "Reise"
        EventCategory.OTHER -> "Sonstiges"
    }
