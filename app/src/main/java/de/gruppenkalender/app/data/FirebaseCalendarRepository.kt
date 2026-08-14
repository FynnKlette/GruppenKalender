package de.gruppenkalender.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldPath
import de.gruppenkalender.app.model.CalendarEvent
import de.gruppenkalender.app.model.CalendarGroup
import de.gruppenkalender.app.model.EventCategory
import de.gruppenkalender.app.model.NotificationSettings
import de.gruppenkalender.app.model.UserProfile
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class FirebaseCalendarRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val hasSignedInUser: Boolean
        get() = auth.currentUser != null

    val signedInUserId: String
        get() = auth.currentUser?.uid.orEmpty()

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
            onComplete(
                Result.failure(
                    IllegalStateException("Kein Benutzer angemeldet."),
                ),
            )
            return
        }

        userDocument(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    ensureSharedGroups(user.uid, onComplete)
                } else {
                    seedUserData(onComplete)
                }
            }
            .addOnFailureListener {
                onComplete(Result.failure(it))
            }
    }

    private fun ensureSharedGroups(
        userId: String,
        onComplete: (Result<Unit>) -> Unit,
    ) {
        firestore
            .collection(COLLECTION_GROUPS)
            .whereArrayContains("memberIds", userId)
            .limit(1)
            .get()
            .addOnSuccessListener sharedGroupsSuccess@{ sharedGroups ->
                if (!sharedGroups.isEmpty) {
                    migrateUserEvents(userId, onComplete)
                    return@sharedGroupsSuccess
                }

                val userRef = userDocument(userId)

                userRef
                    .collection(COLLECTION_GROUPS)
                    .get()
                    .addOnSuccessListener legacyGroupsSuccess@{ legacyGroups ->
                        if (legacyGroups.isEmpty) {
                            migrateUserEvents(userId, onComplete)
                            return@legacyGroupsSuccess
                        }

                        userRef
                            .collection(COLLECTION_EVENTS)
                            .get()
                            .addOnSuccessListener { legacyEvents ->
                                val groupIds = mutableMapOf<String, String>()
                                val batch = firestore.batch()

                                legacyGroups.documents.forEach { document ->
                                    val oldGroup =
                                        document.toGroup()
                                            ?: return@forEach

                                    val newGroupId = "$userId-${oldGroup.id}"

                                    val sharedGroup =
                                        oldGroup.copy(
                                            id = newGroupId,
                                            memberCount = 1,
                                            memberIds = listOf(userId),
                                            inviteCode = createInviteCode(),
                                        )

                                    groupIds[oldGroup.id] = newGroupId

                                    batch.set(
                                        firestore
                                            .collection(COLLECTION_GROUPS)
                                            .document(newGroupId),
                                        sharedGroup.toFirestoreMap(),
                                    )
                                }

                                legacyEvents.documents.forEach { document ->
                                    val oldEvent =
                                        document.toEvent()
                                            ?: return@forEach

                                    val newGroupId =
                                        groupIds[oldEvent.groupId]
                                            ?: return@forEach

                                    batch.update(
                                        document.reference,
                                        "groupId",
                                        newGroupId,
                                    )
                                }

                                batch
                                    .commit()
                                    .addOnSuccessListener {
                                        migrateUserEvents(userId, onComplete)
                                    }
                                    .addOnFailureListener {
                                        onComplete(Result.failure(it))
                                    }
                            }
                            .addOnFailureListener {
                                onComplete(Result.failure(it))
                            }
                    }
                    .addOnFailureListener {
                        onComplete(Result.failure(it))
                    }
            }
            .addOnFailureListener {
                onComplete(Result.failure(it))
            }
    }

    // Verschiebt alte Benutzertermine in die jeweilige Gruppe.
    private fun migrateUserEvents(
        userId: String,
        onComplete: (Result<Unit>) -> Unit,
    ) {
        userDocument(userId)
            .collection(COLLECTION_EVENTS)
            .get()
            .addOnSuccessListener { legacyEvents ->
                if (legacyEvents.isEmpty) {
                    onComplete(Result.success(Unit))
                    return@addOnSuccessListener
                }

                migrateEventChunks(
                    documents = legacyEvents.documents.chunked(20),
                    chunkIndex = 0,
                    userId = userId,
                    onComplete = onComplete,
                )
            }
            .addOnFailureListener {
                onComplete(Result.failure(it))
            }
    }

    // Migriert höchstens 20 Termine pro Firestore-Batch.
    private fun migrateEventChunks(
        documents: List<List<DocumentSnapshot>>,
        chunkIndex: Int,
        userId: String,
        onComplete: (Result<Unit>) -> Unit,
    ) {
        if (chunkIndex >= documents.size) {
            onComplete(Result.success(Unit))
            return
        }

        val batch = firestore.batch()

        documents[chunkIndex].forEach { document ->
            val oldEvent =
                document.toEvent()
                    ?: return@forEach

            val event =
                if (oldEvent.id == "welcome-event") {
                    oldEvent.copy(
                        id = "$userId-${oldEvent.id}",
                    )
                } else {
                    oldEvent
                }

            batch.set(
                groupDocument(event.groupId)
                    .collection(COLLECTION_EVENTS)
                    .document(event.id),
                event.toFirestoreMap(),
            )

            batch.delete(document.reference)
        }

        batch
            .commit()
            .addOnSuccessListener {
                migrateEventChunks(
                    documents = documents,
                    chunkIndex = chunkIndex + 1,
                    userId = userId,
                    onComplete = onComplete,
                )
            }
            .addOnFailureListener {
                onComplete(Result.failure(it))
            }
    }

    fun observeUserData(
        onProfileChanged: (UserProfile) -> Unit,
        onNotificationsChanged: (NotificationSettings) -> Unit,
        onGroupsChanged: (List<CalendarGroup>) -> Unit,
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

                snapshot
                    ?.toProfile(signedInEmail)
                    ?.let(onProfileChanged)
            },
            userRef
                .collection(COLLECTION_SETTINGS)
                .document(DOCUMENT_DEFAULT)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onError(error)
                        return@addSnapshotListener
                    }

                    snapshot
                        ?.toNotificationSettings()
                        ?.let(onNotificationsChanged)
                },
            firestore
                .collection(COLLECTION_GROUPS)
                .whereArrayContains("memberIds", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onError(error)
                        return@addSnapshotListener
                    }

                    onGroupsChanged(
                        snapshot
                            ?.documents
                            .orEmpty()
                            .mapNotNull { it.toGroup() },
                    )
                },
        )
    }

    // Beobachtet die Termine aller Gruppen des Benutzers.
    fun observeGroupEvents(
        groupIds: List<String>,
        onEventsChanged: (List<CalendarEvent>) -> Unit,
        onError: (Throwable) -> Unit,
    ): List<ListenerRegistration> {
        if (groupIds.isEmpty()) {
            onEventsChanged(emptyList())
            return emptyList()
        }

        val eventsByGroup =
            mutableMapOf<String, List<CalendarEvent>>()

        return groupIds.distinct().map { groupId ->
            groupDocument(groupId)
                .collection(COLLECTION_EVENTS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onError(error)
                        return@addSnapshotListener
                    }

                    eventsByGroup[groupId] =
                        snapshot
                            ?.documents
                            .orEmpty()
                            .mapNotNull { it.toEvent() }

                    onEventsChanged(
                        eventsByGroup.values.flatten(),
                    )
                }
        }
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
        firestore
            .collection(COLLECTION_GROUPS)
            .document(group.id)
            .set(group.toFirestoreMap())
    }

    fun joinGroup(
        inviteCode: String,
        memberName: String,
        onComplete: (Result<Unit>) -> Unit,
    ) {
        val userId = requireUserId()
        val normalizedCode = inviteCode.trim().uppercase()

        val normalizedMemberName =
            memberName
                .trim()
                .ifBlank {
                    signedInEmail.substringBefore("@")
                }

        firestore
            .collection(COLLECTION_GROUPS)
            .whereEqualTo(
                "inviteCode",
                normalizedCode,
            )
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                val groupDocument =
                    result.documents.firstOrNull()

                if (groupDocument == null) {
                    onComplete(
                        Result.failure(
                            IllegalArgumentException(
                                "Einladungscode nicht gefunden.",
                            ),
                        ),
                    )
                    return@addOnSuccessListener
                }

                firestore
                    .runTransaction { transaction ->
                        val currentGroup =
                            transaction.get(
                                groupDocument.reference,
                            )

                        val memberIds =
                            (currentGroup.get("memberIds") as? List<*>)
                                ?.filterIsInstance<String>()
                                .orEmpty()

                        val memberNames =
                            (currentGroup.get("memberNames") as? Map<*, *>)
                                ?.mapNotNull { (key, value) ->
                                    if (
                                        key is String &&
                                        value is String
                                    ) {
                                        key to value
                                    } else {
                                        null
                                    }
                                }
                                ?.toMap()
                                .orEmpty()

                        if (userId !in memberIds) {
                            val updatedMemberIds =
                                memberIds + userId

                            transaction.update(
                                groupDocument.reference,
                                mapOf(
                                    "memberIds" to updatedMemberIds,
                                    "memberCount" to updatedMemberIds.size,
                                    "memberNames" to
                                        memberNames +
                                        (userId to normalizedMemberName),
                                ),
                            )
                        }
                    }
                    .addOnSuccessListener {
                        onComplete(Result.success(Unit))
                    }
                    .addOnFailureListener {
                        onComplete(Result.failure(it))
                    }
            }
            .addOnFailureListener {
                onComplete(Result.failure(it))
            }
    }

    // Synchronisiert den Profilnamen in allen Gruppen.
    fun updateMemberName(
        groupIds: List<String>,
        memberName: String,
        onError: (Throwable) -> Unit,
    ) {
        if (
            groupIds.isEmpty() ||
            memberName.isBlank()
        ) {
            return
        }

        val userId = requireUserId()
        val batch = firestore.batch()

        groupIds.distinct().forEach { groupId ->
            batch.update(
                groupDocument(groupId),
                FieldPath.of(
                    "memberNames",
                    userId,
                ),
                memberName.trim(),
            )
        }

        batch
            .commit()
            .addOnFailureListener {
                onError(it)
            }
    }

    fun saveEvent(event: CalendarEvent) {
        groupDocument(event.groupId)
            .collection(COLLECTION_EVENTS)
            .document(event.id)
            .set(event.toFirestoreMap())
    }

    fun deleteEvent(
        groupId: String,
        eventId: String,
    ) {
        groupDocument(groupId)
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
        val groups = defaultGroups(user.uid, profile.name,)
        val events = defaultEvents(groups, profile.name)
        val userRef = userDocument(user.uid)
        val batch = firestore.batch()

        batch.set(userRef, profile.toFirestoreMap())
        batch.set(
            userRef.collection(COLLECTION_SETTINGS).document(DOCUMENT_DEFAULT),
            NotificationSettings().toFirestoreMap(),
        )
        groups.forEach { group ->
            batch.set(
                firestore
                    .collection(COLLECTION_GROUPS)
                    .document(group.id),
                group.toFirestoreMap(),
            )
        }
        events.forEach { event ->
            batch.set(
                groupDocument(event.groupId)
                    .collection(COLLECTION_EVENTS)
                    .document(event.id),
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

    private fun groupDocument(groupId: String) =
        firestore
            .collection(COLLECTION_GROUPS)
            .document(groupId)

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
            "memberIds" to memberIds,
            "memberNames" to memberNames,
            "inviteCode" to inviteCode,
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
                type =
                    getString("type")
                        .orEmpty()
                        .ifBlank { "Gruppe" },
                memberCount =
                    (getLong("memberCount") ?: 1L).toInt(),
                accent =
                    (getLong("accent") ?: 0xFF4A76C0).toInt(),
                isPrivate =
                    getBoolean("private") ?: false,
                memberIds =
                    (get("memberIds") as? List<*>)
                        ?.filterIsInstance<String>()
                        .orEmpty(),
                memberNames =
                    (get ("memberNames") as? Map<*,*>)
                        ?.mapNotNull { (key, value) ->
                            if (
                                key is String && value is String
                            ) {
                                key to value
                            } else {
                                null
                            }
                        }
                        ?.toMap()
                        .orEmpty(),
                inviteCode =
                    getString("inviteCode").orEmpty(),
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

    private fun defaultGroups(userId: String, memberName: String) =
        listOf(
            CalendarGroup(
                id = "$userId-private",
                name = "Privat",
                type = "Privat",
                memberCount = 1,
                accent = 0xFF4A76C0.toInt(),
                isPrivate = true,
                memberIds = listOf(userId),
                memberNames = mapOf(userId to memberName),
                inviteCode = createInviteCode(),
            ),

        )

    private fun createInviteCode(): String =
        UUID
            .randomUUID()
            .toString()
            .replace("-", "")
            .take(6)
            .uppercase()

    private fun defaultEvents(groups: List<CalendarGroup>, memberName: String): List<CalendarEvent> {
        val monday = LocalDate.now().with(DayOfWeek.MONDAY)
        return listOf(
            CalendarEvent(
                id = UUID.randomUUID().toString(),
                title = "Erster gemeinsamer Termin",
                groupId = groups.first().id,
                startDate = monday.plusDays(2),
                endDate = monday.plusDays(2),
                startTime = LocalTime.of(18, 0),
                endTime = LocalTime.of(19, 0),
                location = "Berlin",
                description = "Dieser Beispieltermin ist wichtig!",
                participants = listOf(memberName),
                category = EventCategory.OTHER,
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
