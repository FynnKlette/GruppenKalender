package de.gruppenkalender.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.gruppenkalender.app.model.AppDestination
import de.gruppenkalender.app.model.CalendarEvent
import de.gruppenkalender.app.model.CalendarGroup
import de.gruppenkalender.app.ui.theme.KinshipBlue
import de.gruppenkalender.app.ui.theme.KinshipBluePale
import de.gruppenkalender.app.ui.theme.KinshipInk
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val CardShape = RoundedCornerShape(10.dp)
val ScreenPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
val GermanDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN)
val ShortDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)
val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailingIcon: ImageVector = Icons.Outlined.SupervisorAccount,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(KinshipBlue)
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(64.dp)
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack ?: {}) {
            Icon(
                imageVector =
                    if (onBack == null) {
                        Icons.Outlined.Menu
                    } else {
                        Icons.AutoMirrored.Outlined.ArrowBack
                    },
                contentDescription = if (onBack == null) "Menü" else "Zurück",
                tint = Color.White,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        IconButton(onClick = onTrailingClick ?: {}) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = "Aktion",
                tint = Color.White,
            )
        }
    }
}

data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val destination: AppDestination,
)

private val navigationItems =
    listOf(
        NavigationItem("Home", Icons.Outlined.Home, AppDestination.Home),
        NavigationItem("Gruppen", Icons.Outlined.Groups, AppDestination.Groups),
        NavigationItem("Kalender", Icons.Outlined.CalendarMonth, AppDestination.Calendar),
        NavigationItem("Neu", Icons.Outlined.AddCircleOutline, AppDestination.EventEditor()),
        NavigationItem("Settings", Icons.Outlined.Settings, AppDestination.Settings),
    )

@Composable
fun AppBottomBar(
    current: AppDestination,
    onNavigate: (AppDestination) -> Unit,
) {
    NavigationBar(containerColor = Color.White, tonalElevation = 4.dp) {
        navigationItems.forEach { item ->
            val selected =
                when {
                    current is AppDestination.EventEditor && item.label == "Neu" -> true
                    current is AppDestination.EventDetails && item.label == "Kalender" -> true
                    else -> current::class == item.destination::class
                }
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.destination) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = KinshipInk,
                        selectedTextColor = KinshipInk,
                        indicatorColor = KinshipBluePale,
                        unselectedIconColor = Color(0xFF434751),
                        unselectedTextColor = Color(0xFF434751),
                    ),
            )
        }
    }
}

@Composable
fun KinshipCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier =
            modifier
                .then(clickableModifier)
                .border(BorderStroke(1.dp, KinshipInk), CardShape),
        shape = CardShape,
        color = Color.White,
        shadowElevation = 0.dp,
        content = content,
    )
}

@Composable
fun GroupChip(
    group: CalendarGroup,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = Color(group.accent)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                group.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        },
        leadingIcon =
            if (selected) {
                {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                }
            } else {
                {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accent),
                    )
                }
            },
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = accent,
                selectedLabelColor = Color.White,
                containerColor = Color(0xFFF0F0F3),
            ),
        border =
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = Color(0xFFC3C6D2),
                selectedBorderColor = accent,
            ),
    )
}

@Composable
fun CalendarEventCard(
    event: CalendarEvent,
    group: CalendarGroup?,
    modifier: Modifier = Modifier,
    displayDate: LocalDate = event.startDate,
    onClick: () -> Unit,
) {
    val accent = Color(group?.accent ?: 0xFF737782.toInt())
    KinshipCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row {
            Box(
                Modifier
                    .size(width = 8.dp, height = 104.dp)
                    .background(accent),
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = group?.name ?: "Ohne Gruppe",
                    color = accent,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (event.startDate == event.endDate) {
                    Text(
                        text = "${event.startTime.format(TimeFormatter)} – ${event.endTime.format(TimeFormatter)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (displayDate == event.startDate) {
                    Text(
                        text = "${event.startTime.format(TimeFormatter)} – ${event.endDate.format(ShortDateFormatter)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (displayDate == event.endDate) {
                    Text(
                        text = "${event.startDate.format(ShortDateFormatter)} – ${event.endTime.format(TimeFormatter)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "${event.startDate.format(ShortDateFormatter)} – ${event.endDate.format(ShortDateFormatter)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (event.location.isNotBlank()) {
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            EventCategoryIcon(
                event = event,
                tint = accent.copy(alpha = 0.4f),
                modifier =
                    Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 18.dp),
            )
        }
    }
}

@Composable
private fun EventCategoryIcon(
    event: CalendarEvent,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val icon =
        when (event.category) {
            de.gruppenkalender.app.model.EventCategory.SPORT -> Icons.Outlined.Groups
            de.gruppenkalender.app.model.EventCategory.FAMILY -> Icons.Outlined.Home
            de.gruppenkalender.app.model.EventCategory.WORK -> Icons.Outlined.Settings
            de.gruppenkalender.app.model.EventCategory.TRAVEL -> Icons.Outlined.CalendarMonth
            de.gruppenkalender.app.model.EventCategory.OTHER -> Icons.Outlined.AddCircleOutline
        }
    Icon(icon, contentDescription = null, tint = tint, modifier = modifier.size(30.dp))
}

@Composable
fun SectionTitle(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (action != null) {
            Text(
                text = action,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = onAction != null) { onAction?.invoke() }
                        .padding(6.dp),
                color = KinshipBlue,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}
