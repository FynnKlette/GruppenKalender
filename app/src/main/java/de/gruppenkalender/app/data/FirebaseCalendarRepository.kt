package de.gruppenkalender.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import de.gruppenkalender.app.model.CalendarEvent
import de.gruppenkalender.app.model.CalendarGroup
import de.gruppenkalender.app.model.EventCategory
import de.gruppenkalender.app.model.NotificationSettings
import de.gruppenkalender.app.model.UserProfile
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class FirebaseCalendarRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val hasSignedInUser: Boolean
        get() = auth.currentUser != null

    val signedInEmail: String
        get() = auth.currentUser?.email.orEmpty()

    fun signIn(
        email: String,
        password: String,
        onComplete: (Result<Unit>) -> Unit,
    ) {
        auth
            .signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { ensureUserData(onComplete) }
            .addOnFailureListener { onComplete(Result.failure(it)) }
    }

    fun register(
        email: String,
        password: String,
        onComplete: (Result<Unit>) -> Unit,
    ) {
        auth
            .createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { seedUserData(onComplete) }
            .addOnFailureListener { onComplete(Result.failure(it)) }
    }

    fun ensureUserData(onComplete: (Result<Unit>) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onComplete(Result.failure(IllegalStateException("Kein Benutzer angemeldet.")))
            return
        }

        userDocument(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onComplete(Result.success(Unit))
                } else {
                    seedUserData(onComplete)
                }
            }.addOnFailureListener { onComplete(Result.failure(it)) }
    }

    fun observeUserData(
        onProfileChanged: (UserProfile) -> Unit,
        onNotificationsChanged: (NotificationSettings) -> Unit,
        onGroupsChanged: (List<CalendarGroup>) -> Unit,
        onEventsChanged: (List<CalendarEvent>) -> Unit,
        onError: (Throwable) -> Unit,
    ): List<ListenerRegistration> {
        val userId = requireUserId()
        val userRef = userDocument(userId)

        return listOf(
            userRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                snapshot?.toProfile(signedInEmail)?.let(onProfileChanged)
            },
            userRef
                .collection(COLLECTION_SETTINGS)
                .document(DOCUMENT_DEFAULT)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onError(error)
                        return@addSnapshotListener
                    }
                    snapshot?.toNotificationSettings()?.let(onNotificationsChanged)
                },
            userRef
                .collection(COLLECTION_GROUPS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onError(error)
                        return@addSnapshotListener
                    }
                    onGroupsChanged(
                        snapshot?.documents.orEmpty().mapNotNull { it.toGroup() },
                    )
                },
            userRef
                .collection(COLLECTION_EVENTS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onError(error)
                        return@addSnapshotListener
                    }
                    onEventsChanged(
                        snapshot?.documents.orEmpty().mapNotNull { it.toEvent() },
                    )
                },
        )
    }

    fun saveProfile(profile: UserProfile) {
        userDocument(requireUserId())
            .set(profile.toFirestoreMap())
    }

    fun saveNotificationSettings(settings: NotificationSettings) {
        userDocument(requireUserId())
            .collection(COLLECTION_SETTINGS)
            .document(DOCUMENT_DEFAULT)
            .set(settings.toFirestoreMap())
    }

    fun saveGroup(group: CalendarGroup) {
        userDocument(requireUserId())
            .collection(COLLECTION_GROUPS)
            .document(group.id)
            .set(group.toFirestoreMap())
    }

    fun saveEvent(event: CalendarEvent) {
        userDocument(requireUserId())
            .collection(COLLECTION_EVENTS)
            .document(event.id)
            .set(event.toFirestoreMap())
    }

    fun deleteEvent(eventId: String) {
        userDocument(requireUserId())
            .collection(COLLECTION_EVENTS)
            .document(eventId)
            .delete()
    }

    fun sendPasswordReset(
        email: String,
        onComplete: (Result<Unit>) -> Unit,
    ) {
        auth
            .sendPasswordResetEmail(email.trim())
            .addOnSuccessListener { onComplete(Result.success(Unit)) }
            .addOnFailureListener { onComplete(Result.failure(it)) }
    }

    fun logout() {
        auth.signOut()
    }

    private fun seedUserData(onComplete: (Result<Unit>) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onComplete(Result.failure(IllegalStateException("Kein Benutzer angemeldet.")))
            return
        }

        val profile =
            UserProfile(
                name =
                    "Familie ${
                        user.email
                            .orEmpty()
                            .substringBefore("@")
                            .replaceFirstChar { it.uppercase() }
                    }",
                email = user.email.orEmpty(),
            )
        val groups = defaultGroups()
        val events = defaultEvents(groups)
        val userRef = userDocument(user.uid)
        val batch = firestore.batch()

        batch.set(userRef, profile.toFirestoreMap())
        batch.set(
            userRef.collection(COLLECTION_SETTINGS).document(DOCUMENT_DEFAULT),
            NotificationSettings().toFirestoreMap(),
        )
        groups.forEach { group ->
            batch.set(
                userRef.collection(COLLECTION_GROUPS).document(group.id),
                group.toFirestoreMap(),
            )
        }
        events.forEach { event ->
            batch.set(
                userRef.collection(COLLECTION_EVENTS).document(event.id),
                event.toFirestoreMap(),
            )
        }

        batch
            .commit()
            .addOnSuccessListener { onComplete(Result.success(Unit)) }
            .addOnFailureListener { onComplete(Result.failure(it)) }
    }

    private fun userDocument(userId: String) =
        firestore.collection(COLLECTION_USERS).document(userId)

    private fun requireUserId(): String =
        requireNotNull(auth.currentUser?.uid) { "Kein Benutzer angemeldet." }

    private fun UserProfile.toFirestoreMap() =
        mapOf(
            "name" to name,
            "email" to email,
        )

    private fun NotificationSettings.toFirestoreMap() =
        mapOf(
            "pushEnabled" to pushEnabled,
            "emailEnabled" to emailEnabled,
            "remindersEnabled" to remindersEnabled,
        )

    private fun CalendarGroup.toFirestoreMap() =
        mapOf(
            "name" to name,
            "type" to type,
            "memberCount" to memberCount,
            "accent" to accent.toLong(),
            "private" to isPrivate,
        )

    private fun CalendarEvent.toFirestoreMap() =
        mapOf(
            "title" to title,
            "groupId" to groupId,
            "startDate" to startDate.toString(),
            "endDate" to endDate.toString(),
            "startTime" to startTime.toString(),
            "endTime" to endTime.toString(),
            "location" to location,
            "description" to description,
            "participants" to participants,
            "category" to category.name,
        )

    private fun DocumentSnapshot.toProfile(fallbackEmail: String): UserProfile? {
        if (!exists()) return null
        return UserProfile(
            name = getString("name").orEmpty().ifBlank { "Familie" },
            email = getString("email").orEmpty().ifBlank { fallbackEmail },
        )
    }

    private fun DocumentSnapshot.toNotificationSettings(): NotificationSettings? {
        if (!exists()) return null
        return NotificationSettings(
            pushEnabled = getBoolean("pushEnabled") ?: true,
            emailEnabled = getBoolean("emailEnabled") ?: false,
            remindersEnabled = getBoolean("remindersEnabled") ?: true,
        )
    }

    private fun DocumentSnapshot.toGroup(): CalendarGroup? =
        runCatching {
            CalendarGroup(
                id = id,
                name = requireNotNull(getString("name")),
                type = getString("type").orEmpty().ifBlank { "Gruppe" },
                memberCount = (getLong("memberCount") ?: 1L).toInt(),
                accent = (getLong("accent") ?: 0xFF4A76C0).toInt(),
                isPrivate = getBoolean("private") ?: false,
            )
        }.getOrNull()

    private fun DocumentSnapshot.toEvent(): CalendarEvent? =
        runCatching {
            CalendarEvent(
                id = id,
                title = requireNotNull(getString("title")),
                groupId = requireNotNull(getString("groupId")),
                startDate = LocalDate.parse(requireNotNull(getString("startDate"))),
                endDate = LocalDate.parse(requireNotNull(getString("endDate"))),
                startTime = LocalTime.parse(requireNotNull(getString("startTime"))),
                endTime = LocalTime.parse(requireNotNull(getString("endTime"))),
                location = getString("location").orEmpty(),
                description = getString("description").orEmpty(),
                participants = (get("participants") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                category =
                    runCatching {
                        EventCategory.valueOf(getString("category").orEmpty())
                    }.getOrDefault(EventCategory.OTHER),
            )
        }.getOrNull()

    private fun defaultGroups() =
        listOf(
            CalendarGroup("family", "Familie", "Privat", 1, 0xFF4A76C0.toInt(), true),
            CalendarGroup("sport", "Sportverein", "Sport", 1, 0xFFF2994A.toInt()),
            CalendarGroup("friends", "Freundeskreis", "Freunde", 1, 0xFF27AE60.toInt()),
        )

    private fun defaultEvents(groups: List<CalendarGroup>): List<CalendarEvent> {
        val monday = LocalDate.now().with(DayOfWeek.MONDAY)
        return listOf(
            CalendarEvent(
                id = "welcome-event",
                title = "Erster gemeinsamer Termin",
                groupId = groups.first().id,
                startDate = monday.plusDays(2),
                endDate = monday.plusDays(2),
                startTime = LocalTime.of(18, 0),
                endTime = LocalTime.of(19, 0),
                location = "Berlin",
                description = "Dieser Beispieltermin wird über Cloud Firestore synchronisiert.",
                participants = listOf("Ich"),
                category = EventCategory.FAMILY,
            ),
        )
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_SETTINGS = "settings"
        const val COLLECTION_GROUPS = "groups"
        const val COLLECTION_EVENTS = "events"
        const val DOCUMENT_DEFAULT = "default"
    }
}
