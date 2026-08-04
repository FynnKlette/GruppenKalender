# Architektur und App-Flows

## Schichten

| Bereich | Verantwortung |
| --- | --- |
| `model` | Unabhängige Datenmodelle für Gruppen, Termine, Profil und Navigation |
| `data` | Firebase Authentication, Firestore-Synchronisierung und initiale Beispieldaten |
| `ui/AppViewModel` | Zustandsverwaltung, Validierung und CRUD-Aktionen |
| `ui/screens` | Zustandslose beziehungsweise lokal zustandsbehaftete Compose-Screens |
| `ui/components` | Einheitliche Top-/Bottom-Navigation, Karten, Chips und Terminkarten |
| `ui/theme` | Kinship-Farbsystem und Typografie |

## Umgesetzte Hauptflüsse

1. App-Start → Login oder Registrierung → Home
2. Home → Gruppenübersicht oder Kalender
3. Kalender → Termindetails → Bearbeiten oder Löschen
4. Bottom Navigation „Neu“ → Termin erstellen → Termindetails
5. Gruppen → Gruppe erstellen → Gruppenliste
6. Settings → Profil ändern, Benachrichtigungen setzen, Passwort-Reset-E-Mail
7. Settings → Abmelden → Login

## Firebase-Anbindung

`FirebaseCalendarRepository` verwendet Firebase Authentication für Registrierung,
Anmeldung, Sitzungsverwaltung und Passwort-Reset. Cloud Firestore speichert die
Daten jedes Kontos unter `users/{uid}`:

```text
users/{uid}
├── Profilfelder
├── settings/default
├── groups/{groupId}
└── events/{eventId}
```

Snapshot Listener übertragen Änderungen in Echtzeit in den Compose-Zustand.
Die Regeln in `firestore.rules` erlauben einem angemeldeten Konto ausschließlich
den Zugriff auf sein eigenes Benutzerdokument und dessen Unterkollektionen.

## Designentscheidungen

- Konturierte, weiße Karten statt starker Schatten
- Gruppenfarbe als linke 8-dp-Akzentleiste
- Blau als primäre Navigations- und Aktionsfarbe
- Monospace-Stil für Zeit-, Meta- und Systemlabels
- 8-dp-Abstandsraster und mindestens 16-dp-Seitenränder
- Feste Bottom Navigation auf allen eingeloggten Screens
