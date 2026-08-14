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
    private val eventListeners = mutableListOf<ListenerRegistration>()
    private var lastMemberNameSync:
        Pair<Set<String>, String>? = null
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
        lastMemberNameSync = null
    }

    fun updateProfile(name: String) {
        profile =
            UserProfile(
                name = name.trim(),
                email = repository.signedInEmail,
            )
        repository.saveProfile(profile)
        syncMemberName()
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
        val colors =
            listOf(
                0xFF4A76C0,
                0xFFF2994A,
                0xFF27AE60,
                0xFF8E5BB7,
            )

        val group =
            CalendarGroup(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                type = type.trim().ifEmpty { "Gruppe" },
                memberCount = 1,
                accent = colors[groups.size % colors.size].toInt(),
                isPrivate = isPrivate,
                memberIds = listOf(repository.signedInUserId),
                memberNames = mapOf(repository.signedInUserId to profile.name,),
                inviteCode = createInviteCode(),
            )

        groups += group
        repository.saveGroup(group)
    }

    fun joinGroup(
        inviteCode: String,
        onComplete: (String?) -> Unit,
    ) {
        repository.joinGroup(
            inviteCode = inviteCode,
            memberName = profile.name,
        ) { result ->
            onComplete(
                result
                    .exceptionOrNull()
                    ?.toGermanMessage(),
            )
        }
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
        val previousGroupId =
            existingId?.let { eventId ->
                events.find { it.id == eventId }?.groupId
            }
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
        if (
            previousGroupId != null &&
            previousGroupId != event.groupId
        ) {
            repository.deleteEvent(
                previousGroupId,
                event.id,
            )
        }
        repository.saveEvent(event)
        return event
    }

    fun deleteEvent(eventId: String) {
        val groupId =
            events.find { it.id == eventId }
                ?.groupId
                ?: return

        events.removeAll { it.id == eventId }

        repository.deleteEvent(
            groupId = groupId,
            eventId = eventId,
        )
    }

    fun copyEvent(
        eventId: String,
        targetGroupId: String,
    ): CalendarEvent? {
        val source = events.find { it.id == eventId } ?: return null
        if (source.groupId == targetGroupId) return null

        val copied =
            source.copy(
                id = UUID.randomUUID().toString(),
                groupId = targetGroupId,
                participants = emptyList(),
            )

        events += copied
        repository.saveEvent(copied)
        return copied
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
        profile =
            profile.copy(
                email = repository.signedInEmail,
            )

        events.clear()

        listeners +=
            repository.observeUserData(
                onProfileChanged = {
                    profile = it
                    syncMemberName()
                },
                onNotificationsChanged = {
                    notificationSettings = it
                },
                onGroupsChanged = { updatedGroups ->
                    val sortedGroups =
                        updatedGroups.sortedBy(
                            CalendarGroup::name,
                        )

                    val groupIdsChanged =
                        groups.map { it.id }.toSet() !=
                            sortedGroups.map { it.id }.toSet()

                    groups.clear()
                    groups.addAll(sortedGroups)

                    syncMemberName()

                    if (groupIdsChanged) {
                        startEventSync()
                    }
                },
                onError = {
                    dataError = it.toGermanMessage()
                },
            )
    }
    // Startet für jede Gruppe einen gemeinsamen Termin-Listener.
    private fun startEventSync() {
        eventListeners.forEach(
            ListenerRegistration::remove,
        )
        eventListeners.clear()
        events.clear()

        eventListeners +=
            repository.observeGroupEvents(
                groupIds = groups.map { it.id },
                onEventsChanged = { updatedEvents ->
                    events.clear()
                    events.addAll(
                        updatedEvents.sortedBy(
                            CalendarEvent::startDate,
                        ),
                    )
                },
                onError = {
                    dataError = it.toGermanMessage()
                },
            )
    }

    // Speichert den aktuellen Profilnamen in allen Gruppen.
    private fun syncMemberName() {
        val groupIds =
            groups.map { it.id }.toSet()

        val memberName =
            profile.name.trim()

        if (
            groupIds.isEmpty() ||
            memberName.isBlank()
        ) {
            return
        }

        val syncKey =
            groupIds to memberName

        if (syncKey == lastMemberNameSync) {
            return
        }

        lastMemberNameSync = syncKey

        repository.updateMemberName(
            groupIds = groupIds.toList(),
            memberName = memberName,
            onError = {
                lastMemberNameSync = null
                dataError = it.toGermanMessage()
            },
        )
    }

    private fun stopDataSync() {
        eventListeners.forEach(
            ListenerRegistration::remove,
        )
        eventListeners.clear()

        listeners.forEach(
            ListenerRegistration::remove,
        )
        listeners.clear()
    }

    override fun onCleared() {
        stopDataSync()
        super.onCleared()
    }

    private fun createInviteCode(): String =
        UUID
            .randomUUID()
            .toString()
            .replace("-", "")
            .take(6)
            .uppercase()

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
