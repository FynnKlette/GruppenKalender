package de.gruppenkalender.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.gruppenkalender.app.ui.components.KinshipCard
import de.gruppenkalender.app.ui.theme.KinshipBackground
import de.gruppenkalender.app.ui.theme.KinshipBlue
import de.gruppenkalender.app.ui.theme.KinshipBluePale

//Anmelde/Register-screen
@Composable
fun AuthScreen(
    isLoading: Boolean,
    firebaseError: String?,
    onAuthenticate: (email: String, password: String, repeatedPassword: String?) -> Unit,
    onClearError: () -> Unit,
) {
    var registerMode by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var repeatedPassword by rememberSaveable { mutableStateOf("") }

 //bg
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(KinshipBackground),
    ) {
 //head
        Box(
            Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(KinshipBlue),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Box(
                modifier =
                    Modifier
                        .size(76.dp)
                        .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = KinshipBlue,
                    modifier = Modifier.size(42.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "GruppenKalender",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                "Gemeinsam organisiert. Jederzeit im Blick.",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(36.dp))
            //Login/Register Block
            KinshipCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        if (registerMode) "Konto erstellen" else "Willkommen zurück",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        if (registerMode) {
                            "Registriere dich für deinen Gruppenkalender."
                        } else {
                            "Melde dich an, um deine Termine zu koordinieren."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            onClearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("E-Mail") },
                        leadingIcon = { Icon(Icons.Outlined.Mail, contentDescription = null) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            onClearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Passwort") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                    )
                    //zusätzliche Passwort Feld im Registermode
                    if (registerMode) {
                        OutlinedTextField(
                            value = repeatedPassword,
                            onValueChange = {
                                repeatedPassword = it
                                onClearError()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Passwort bestätigen") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Lock, contentDescription = null)
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                        )
                    }
                    //Error-Text
                    if (firebaseError != null) {
                        Text(
                            firebaseError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    //BE Register/Login
                    Button(
                        onClick = {
                            onAuthenticate(
                                email,
                                password,
                                repeatedPassword.takeIf { registerMode },
                            )
                        },
                        enabled = !isLoading,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                if (registerMode) "REGISTRIEREN" else "ANMELDEN",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            registerMode = !registerMode
                            onClearError()
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = KinshipBluePale.copy(alpha = 0.35f),
                            ),
                    ) {
                        Text(
                            if (registerMode) {
                                "Schon registriert? Jetzt anmelden"
                            } else {
                                "Noch kein Konto? Jetzt registrieren"
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row {
            }
        }
    }
}
