package de.gruppenkalender.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.ListenerRegistration
import de.gruppenkalender.app.data.FirebaseCalendarRepository
import de.gruppenkalender.app.model.AuthValidator
import de.gruppenkalender.app.model.CalendarEvent
import de.gruppenkalender.app.model.CalendarGroup
import de.gruppenkalender.app.model.EventCategory
import de.gruppenkalender.app.model.NotificationSettings
import de.gruppenkalender.app.model.UserProfile
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class AppViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FirebaseCalendarRepository()
    private val listeners = mutableListOf<ListenerRegistration>()

    var isCheckingAuth by mutableStateOf(true)
        private set

    var isAuthenticated by mutableStateOf(false)
        private set

    var authInProgress by mutableStateOf(false)
        private set

    var authError by mutableStateOf<String?>(null)
        private set

    var dataError by mutableStateOf<String?>(null)
        private set

    var profile by
        mutableStateOf(
            UserProfile(
                name = "Familie",
                email = repository.signedInEmail,
            ),
        )
        private set

    var notificationSettings by mutableStateOf(NotificationSettings())
        private set

    val groups = mutableStateListOf<CalendarGroup>()
    val events = mutableStateListOf<CalendarEvent>()

    init {
        if (repository.hasSignedInUser) {
            repository.ensureUserData { result ->
                result.fold(
                    onSuccess = { startDataSync() },
                    onFailure = {
                        authError = it.toGermanMessage()
                        isCheckingAuth = false
                    },
                )
            }
        } else {
            isCheckingAuth = false
        }
    }

    fun authenticate(
        email: String,
        password: String,
        repeatedPassword: String? = null,
    ) {
        val validationError = AuthValidator.validate(email, password, repeatedPassword)
        if (validationError != null) {
            authError = validationError
            return
        }

        authInProgress = true
        authError = null
        val onComplete: (Result<Unit>) -> Unit = { result ->
            authInProgress = false
            result.fold(
                onSuccess = { startDataSync() },
                onFailure = { authError = it.toGermanMessage() },
            )
        }

        if (repeatedPassword == null) {
            repository.signIn(email, password, onComplete)
        } else {
            repository.register(email, password, onComplete)
        }
    }

    fun clearAuthError() {
        authError = null
    }

    fun logout() {
        stopDataSync()
        repository.logout()
        groups.clear()
        events.clear()
        isAuthenticated = false
        authError = null
        dataError = null
    }

    fun updateProfile(name: String) {
        profile =
            UserProfile(
                name = name.trim(),
                email = repository.signedInEmail,
            )
        repository.saveProfile(profile)
    }

    fun updateNotifications(settings: NotificationSettings) {
        notificationSettings = settings
        repository.saveNotificationSettings(settings)
    }

    fun addGroup(
        name: String,
        type: String,
        isPrivate: Boolean,
    ) {
        val colors = listOf(0xFF4A76C0, 0xFFF2994A, 0xFF27AE60, 0xFF8E5BB7)
        val group =
            CalendarGroup(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                type = type.trim().ifEmpty { "Gruppe" },
                memberCount = 1,
                accent = colors[groups.size % colors.size].toInt(),
                isPrivate = isPrivate,
            )
        groups += group
        repository.saveGroup(group)
    }

    fun saveEvent(
        existingId: String?,
        title: String,
        groupId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        location: String,
        description: String,
        participants: List<String>,
        category: EventCategory,
    ): CalendarEvent {
        val event =
            CalendarEvent(
                id = existingId ?: UUID.randomUUID().toString(),
                title = title.trim(),
                groupId = groupId,
                startDate = startDate,
                endDate = endDate,
                startTime = startTime,
                endTime = endTime,
                location = location.trim(),
                description = description.trim(),
                participants = participants,
                category = category,
            )
        val index = events.indexOfFirst { it.id == event.id }
        if (index >= 0) events[index] = event else events += event
        repository.saveEvent(event)
        return event
    }

    fun deleteEvent(eventId: String) {
        events.removeAll { it.id == eventId }
        repository.deleteEvent(eventId)
    }

    fun sendPasswordReset(
        email: String,
        onComplete: (String?) -> Unit,
    ) {
        repository.sendPasswordReset(email) { result ->
            onComplete(result.exceptionOrNull()?.toGermanMessage())
        }
    }

    private fun startDataSync() {
        stopDataSync()
        isAuthenticated = true
        isCheckingAuth = false
        dataError = null
        profile = profile.copy(email = repository.signedInEmail)

        listeners +=
            repository.observeUserData(
                onProfileChanged = { profile = it },
                onNotificationsChanged = { notificationSettings = it },
                onGroupsChanged = {
                    groups.clear()
                    groups.addAll(it.sortedBy(CalendarGroup::name))
                },
                onEventsChanged = {
                    events.clear()
                    events.addAll(it.sortedBy(CalendarEvent::startDate))
                },
                onError = { dataError = it.toGermanMessage() },
            )
    }

    private fun stopDataSync() {
        listeners.forEach(ListenerRegistration::remove)
        listeners.clear()
    }

    override fun onCleared() {
        stopDataSync()
        super.onCleared()
    }

    private fun Throwable.toGermanMessage(): String =
        when (this) {
            is FirebaseAuthUserCollisionException ->
                "Für diese E-Mail-Adresse existiert bereits ein Konto."

            is FirebaseAuthInvalidUserException ->
                "Für diese E-Mail-Adresse wurde kein Konto gefunden."

            is FirebaseAuthInvalidCredentialsException ->
                "E-Mail-Adresse oder Passwort ist falsch."

            is FirebaseTooManyRequestsException ->
                "Zu viele Versuche. Bitte probiere es später erneut."

            is FirebaseNetworkException ->
                "Keine Verbindung zu Firebase. Prüfe deine Internetverbindung."

            else -> localizedMessage ?: "Firebase konnte die Anfrage nicht ausführen."
        }
}
