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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.gruppenkalender.app.model.NotificationSettings
import de.gruppenkalender.app.model.UserProfile
import de.gruppenkalender.app.ui.components.AppTopBar
import de.gruppenkalender.app.ui.components.KinshipCard
import de.gruppenkalender.app.ui.components.ScreenPadding
import de.gruppenkalender.app.ui.theme.KinshipBlue
import de.gruppenkalender.app.ui.theme.KinshipRed
//Settings&Profile-Screen
@Composable
fun SettingsScreen(
    profile: UserProfile,
    notificationSettings: NotificationSettings,
    onUpdateProfile: (name: String) -> Unit,
    onUpdateNotifications: (NotificationSettings) -> Unit,
    onSendPasswordReset: (email: String, onComplete: (String?) -> Unit) -> Unit,
    onLogout: () -> Unit,
) {
    var name by rememberSaveable(profile.name) { mutableStateOf(profile.name) }
    var email by rememberSaveable(profile.email) { mutableStateOf(profile.email) }
    var editingProfile by rememberSaveable { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showPasswordMessage by remember { mutableStateOf(false) }
    var passwordResetError by remember { mutableStateOf<String?>(null) }
    var passwordResetInProgress by remember { mutableStateOf(false) }

    Column {
        AppTopBar(title = "Mein Profil", trailingIcon = Icons.Outlined.SupervisorAccount)
        LazyColumn(
            contentPadding = ScreenPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                KinshipCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier
                                .size(84.dp)
                                .background(KinshipBlue.copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                profile.name
                                    .split(" ")
                                    .mapNotNull { it.firstOrNull()?.uppercase() }
                                    .take(2)
                                    .joinToString(""),
                                color = KinshipBlue,
                                style = MaterialTheme.typography.headlineLarge,
                            )
                            IconButton(
                                onClick = { editingProfile = !editingProfile },
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(36.dp)
                                        .background(KinshipBlue, CircleShape),
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = "Profil bearbeiten",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            enabled = editingProfile,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("NAME") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("E-MAIL") },
                            leadingIcon = { Icon(Icons.Outlined.AlternateEmail, null) },
                            singleLine = true,
                        )
                        if (editingProfile) {
                            Button(
                                onClick = {
                                    onUpdateProfile(name)
                                    editingProfile = false
                                },
                                enabled = name.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("PROFIL SPEICHERN") }
                        }
                    }
                }
            }
            item { SettingsLabel("BENACHRICHTIGUNGEN") }
            item {
                KinshipCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SettingsToggleRow(
                            icon = Icons.Outlined.Notifications,
                            title = "Push-Benachrichtigungen",
                            checked = notificationSettings.pushEnabled,
                            onCheckedChange = {
                                onUpdateNotifications(
                                    notificationSettings.copy(pushEnabled = it),
                                )
                            },
                        )
                        DividerLine()
                        SettingsToggleRow(
                            icon = Icons.Outlined.Email,
                            title = "E-Mail-Updates",
                            checked = notificationSettings.emailEnabled,
                            onCheckedChange = {
                                onUpdateNotifications(
                                    notificationSettings.copy(emailEnabled = it),
                                )
                            },
                        )
                        DividerLine()
                        SettingsToggleRow(
                            icon = Icons.Outlined.Schedule,
                            title = "Erinnerungen",
                            checked = notificationSettings.remindersEnabled,
                            onCheckedChange = {
                                onUpdateNotifications(
                                    notificationSettings.copy(remindersEnabled = it),
                                )
                            },
                        )
                    }
                }
            }
            item { SettingsLabel("PRIVATSPHÄRE") }
            item {
                KinshipCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SettingsLinkRow(
                            icon = Icons.Outlined.RemoveRedEye,
                            title = "Sichtbarkeit der Gruppe",
                            subtitle = "Nur Mitglieder können Termine sehen",
                        )
                        DividerLine()
                        SettingsLinkRow(
                            icon = Icons.Outlined.Share,
                            title = "Datenfreigabe",
                            subtitle = "Analyse-Daten und Fehlerberichte",
                        )
                    }
                }
            }
            item { SettingsLabel("KONTO-VERWALTUNG") }
            item {
                KinshipCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SettingsLinkRow(
                            icon = Icons.Outlined.Lock,
                            title = "Passwort ändern",
                            onClick = { showPasswordDialog = true },
                        )
                        DividerLine()
                        SettingsLinkRow(
                            icon = Icons.Outlined.CreditCard,
                            title = "Abonnement verwalten",
                        )
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = onLogout,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KinshipRed),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = KinshipRed,
                            containerColor = Color(0xFFFFE7E4),
                        ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ExitToApp, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Abmelden")
                }
            }
            item {
                Text(
                    "Version 1.0.0 (MVP)",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(72.dp))
            }
        }
    }

    if (showPasswordDialog) {
        var resetEmail by remember { mutableStateOf(profile.email) }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Passwort zurücksetzen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Wir senden einen Reset-Link an deine E-Mail-Adresse.")
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("E-Mail") },
                        singleLine = true,
                    )
                    if (passwordResetError != null) {
                        Text(
                            passwordResetError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        passwordResetInProgress = true
                        passwordResetError = null
                        onSendPasswordReset(resetEmail) { error ->
                            passwordResetInProgress = false
                            if (error == null) {
                                showPasswordDialog = false
                                showPasswordMessage = true
                            } else {
                                passwordResetError = error
                            }
                        }
                    },
                    enabled = resetEmail.contains("@") && !passwordResetInProgress,
                ) {
                    if (passwordResetInProgress) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("LINK SENDEN")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text("ABBRECHEN") }
            },
        )
    }
    if (showPasswordMessage) {
        AlertDialog(
            onDismissRequest = { showPasswordMessage = false },
            title = { Text("E-Mail vorbereitet") },
            text = {
                Text(
                    "Link zum Zurücksetzen des Passworts versendet.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showPasswordMessage = false }) { Text("OK") }
            },
        )
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = KinshipBlue)
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {},
) {
    androidx.compose.material3.Surface(onClick = onClick, color = Color.White) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, null, tint = KinshipBlue)
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Öffnen")
        }
    }
}

@Composable
private fun DividerLine() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFD8DAE0)),
    )
}
