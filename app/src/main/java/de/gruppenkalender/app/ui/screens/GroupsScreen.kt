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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.gruppenkalender.app.model.CalendarEvent
import de.gruppenkalender.app.model.CalendarGroup
import de.gruppenkalender.app.ui.components.AppTopBar
import de.gruppenkalender.app.ui.components.KinshipCard
import de.gruppenkalender.app.ui.components.ScreenPadding
import de.gruppenkalender.app.ui.theme.KinshipBlue
import de.gruppenkalender.app.ui.theme.KinshipOrange
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
//Gruppenübersicht
@Composable
fun GroupsScreen(
    groups: List<CalendarGroup>,
    events: List<CalendarEvent>,
    onAddGroup: (name: String, type: String, private: Boolean) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    Column {
        AppTopBar(title = "Meine Gruppen", onTrailingClick = onOpenSettings)
        LazyColumn(
            contentPadding = ScreenPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("Verwalte deine Teams", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Hier siehst du alle Gruppen, in denen du aktiv koordinierst.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            //Gruppen-Felder
            items(groups, key = { it.id }) { group ->
                GroupManagementCard(
                    group = group,
                    nextEvent =
                        events
                            .filter { it.groupId == group.id && !it.startDate.isBefore(LocalDate.now()) }
                            .minByOrNull { it.startDate },
                    onClick = onOpenCalendar,
                )
            }
            //Gruppe hinzufügen
            item {
                ActionCard(
                    title = "Neue Gruppe erstellen",
                    icon = Icons.Outlined.Add,
                    color = KinshipBlue,
                    onClick = { showCreateDialog = true },
                )
            }
            //Gruppe beitreten
            //Funktion in Entwicklung
            item {
                ActionCard(
                    title = "Gruppe beitreten",
                    icon = Icons.Outlined.GroupAdd,
                    color = Color(0xFF904D00),
                    onClick = { showJoinDialog = true },
                )
            }
            item {
                KinshipCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier =
                            Modifier
                                .background(Color(0xFFDCE8FF))
                                .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(50.dp)
                                .background(KinshipBlue, RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Groups, null, tint = Color.White)
                        }
                        Column {
                            Text(
                                "Planung leicht gemacht",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "Synchronisiere Termine mit allen Gruppenmitgliedern.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(76.dp)) }
        }
    }
//Gruppe erstellen PopUp
    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, type, private ->
                onAddGroup(name, type, private)
                showCreateDialog = false
            },
        )
    }
    if (showJoinDialog) {
        JoinGroupDialog(onDismiss = { showJoinDialog = false })
    }
}

//Gruppen-Feld Baustein
@Composable
private fun GroupManagementCard(
    group: CalendarGroup,
    nextEvent: CalendarEvent?,
    onClick: () -> Unit,
) {
    val accent = Color(group.accent)
    KinshipCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row {
            Box(
                Modifier
                    .size(width = 7.dp, height = 128.dp)
                    .background(accent),
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(group.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        group.type,
                        modifier =
                            Modifier
                                .background(Color(0xFFEEEFF2), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .background(accent.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Groups,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text("${group.memberCount} Mitglieder")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (nextEvent == null) {
                            "Keine anstehenden Termine"
                        } else {
                            "Nächster Termin: ${
                                nextEvent.startDate.format(
                                    DateTimeFormatter.ofPattern("EEE, dd.MM.", Locale.GERMAN),
                                )
                            }"
                        },
                        modifier = Modifier.weight(1f),
                        color = accent,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Öffnen",
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(104.dp),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
//Pop-Ups Bausteine
@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, type: String, private: Boolean) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var private by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neue Gruppe") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Gruppenname") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Kategorie, z. B. Sport") },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Private Gruppe")
                    Switch(checked = private, onCheckedChange = { private = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, type, private) },
                enabled = name.isNotBlank(),
            ) { Text("ERSTELLEN") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ABBRECHEN") } },
    )
}

@Composable
private fun JoinGroupDialog(onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gruppe beitreten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Gib den Einladungscode deiner Gruppe ein.")
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Einladungscode") },
                    singleLine = true,
                )

            }
        },
        confirmButton = {
            Button(onClick = onDismiss, enabled = code.length >= 4) { Text("BEITRETEN") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ABBRECHEN") } },
    )
}
