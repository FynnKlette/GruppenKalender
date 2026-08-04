package de.gruppenkalender.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import de.gruppenkalender.app.model.AppDestination
import de.gruppenkalender.app.ui.components.AppBottomBar
import de.gruppenkalender.app.ui.screens.AuthScreen
import de.gruppenkalender.app.ui.screens.CalendarScreen
import de.gruppenkalender.app.ui.screens.EventDetailsScreen
import de.gruppenkalender.app.ui.screens.EventDraft
import de.gruppenkalender.app.ui.screens.EventEditorScreen
import de.gruppenkalender.app.ui.screens.GroupsScreen
import de.gruppenkalender.app.ui.screens.HomeScreen
import de.gruppenkalender.app.ui.screens.SettingsScreen
import de.gruppenkalender.app.ui.theme.GroupCalendarTheme

@Composable
fun GroupCalendarApp(appViewModel: AppViewModel = viewModel()) {
    GroupCalendarTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            var destination by remember {
                mutableStateOf<AppDestination>(AppDestination.Home)
            }

            if (appViewModel.isCheckingAuth) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (!appViewModel.isAuthenticated) {
                AuthScreen(
                    isLoading = appViewModel.authInProgress,
                    firebaseError = appViewModel.authError,
                    onAuthenticate = appViewModel::authenticate,
                    onClearError = appViewModel::clearAuthError,
                )
            } else {
                GroupCalendarShell(
                    destination = destination,
                    onNavigate = { destination = it },
                    appViewModel = appViewModel,
                )
            }
        }
    }
}

@Composable
private fun GroupCalendarShell(
    destination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    appViewModel: AppViewModel,
) {
    val showFab =
        destination is AppDestination.Home ||
            destination is AppDestination.Groups ||
            destination is AppDestination.Calendar

    Scaffold(
        bottomBar = {
            AppBottomBar(current = destination, onNavigate = onNavigate)
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { onNavigate(AppDestination.EventEditor()) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Neuer Eintrag")
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            when (destination) {
                AppDestination.Home ->
                    HomeScreen(
                        groups = appViewModel.groups,
                        events = appViewModel.events,
                        onOpenGroups = { onNavigate(AppDestination.Groups) },
                        onOpenCalendar = { onNavigate(AppDestination.Calendar) },
                        onOpenEvent = { onNavigate(AppDestination.EventDetails(it)) },
                        onOpenSettings = { onNavigate(AppDestination.Settings) },
                    )

                AppDestination.Groups ->
                    GroupsScreen(
                        groups = appViewModel.groups,
                        events = appViewModel.events,
                        onAddGroup = appViewModel::addGroup,
                        onOpenCalendar = { onNavigate(AppDestination.Calendar) },
                        onOpenSettings = { onNavigate(AppDestination.Settings) },
                    )

                AppDestination.Calendar ->
                    CalendarScreen(
                        groups = appViewModel.groups,
                        events = appViewModel.events,
                        onOpenEvent = { onNavigate(AppDestination.EventDetails(it)) },
                        onOpenSettings = { onNavigate(AppDestination.Settings) },
                    )

                AppDestination.Settings ->
                    SettingsScreen(
                        profile = appViewModel.profile,
                        notificationSettings = appViewModel.notificationSettings,
                        onUpdateProfile = appViewModel::updateProfile,
                        onUpdateNotifications = appViewModel::updateNotifications,
                        onSendPasswordReset = appViewModel::sendPasswordReset,
                        onLogout = {
                            appViewModel.logout()
                            onNavigate(AppDestination.Home)
                        },
                    )

                is AppDestination.EventDetails -> {
                    val event = appViewModel.events.find { it.id == destination.eventId }
                    if (event == null) {
                        LaunchedEffect(destination.eventId) {
                            onNavigate(AppDestination.Calendar)
                        }
                    } else {
                        EventDetailsScreen(
                            event = event,
                            group = appViewModel.groups.find { it.id == event.groupId },
                            onBack = { onNavigate(AppDestination.Calendar) },
                            onEdit = {
                                onNavigate(AppDestination.EventEditor(destination.eventId))
                            },
                            onDelete = {
                                appViewModel.deleteEvent(destination.eventId)
                                onNavigate(AppDestination.Calendar)
                            },
                        )
                    }
                }

                is AppDestination.EventEditor -> {
                    val existing =
                        destination.eventId?.let { id ->
                            appViewModel.events.find { it.id == id }
                        }
                    EventEditorScreen(
                        event = existing,
                        groups = appViewModel.groups,
                        onBack = {
                            if (existing == null) {
                                onNavigate(AppDestination.Calendar)
                            } else {
                                onNavigate(AppDestination.EventDetails(existing.id))
                            }
                        },
                        onSave = { draft ->
                            val saved = saveDraft(appViewModel, destination.eventId, draft)
                            onNavigate(AppDestination.EventDetails(saved.id))
                        },
                    )
                }
            }
        }
    }
}

private fun saveDraft(
    viewModel: AppViewModel,
    existingId: String?,
    draft: EventDraft,
) = viewModel.saveEvent(
    existingId = existingId,
    title = draft.title,
    groupId = draft.groupId,
    startDate = draft.startDate,
    endDate = draft.endDate,
    startTime = draft.startTime,
    endTime = draft.endTime,
    location = draft.location,
    description = draft.description,
    participants = draft.participants,
    category = draft.category,
)
