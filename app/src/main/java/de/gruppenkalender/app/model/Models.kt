package de.gruppenkalender.app.model

import java.time.LocalDate
import java.time.LocalTime

data class CalendarGroup(
    val id: String,
    val name: String,
    val type: String,
    val memberCount: Int,
    val accent: Int,
    val isPrivate: Boolean = false,
    val memberIds: List<String> = emptyList(),
    val inviteCode: String = "",
)

data class CalendarEvent(
    val id: String,
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

enum class EventCategory {
    SPORT,
    FAMILY,
    WORK,
    TRAVEL,
    OTHER,
}

data class UserProfile(
    val name: String,
    val email: String,
)

data class NotificationSettings(
    val pushEnabled: Boolean = true,
    val emailEnabled: Boolean = false,
    val remindersEnabled: Boolean = true,
)

sealed interface AppDestination {
    data object Home : AppDestination

    data object Groups : AppDestination

    data object Calendar : AppDestination

    data object Settings : AppDestination

    data class EventDetails(
        val eventId: String,
    ) : AppDestination

    data class EventEditor(
        val eventId: String? = null,
    ) : AppDestination
}
