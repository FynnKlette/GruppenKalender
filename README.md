# GruppenKalender – Familienplan

Android-MVP für einen kollaborativen Gruppenkalender. Die App wurde in Kotlin
mit Jetpack Compose umgesetzt und orientiert sich an den bereitgestellten
Beispielscreens sowie am Ablaufdiagramm.

## Enthaltene Funktionen

- echte Anmeldung und Registrierung mit Firebase Authentication
- Startseite mit Wochenzeitraum, anstehenden Terminen und Gruppenübersicht
- Gruppen anzeigen und in Cloud Firestore neue Gruppen erstellen
- Tages-, Wochen- und Monatsfilter im Kalender
- Termine erstellen, anzeigen, bearbeiten und löschen
- Gruppenauswahl, Start-/Enddatum, Uhrzeiten, Ort, Beschreibung und Beteiligte
- Zusage/Absage in der Termindetailansicht
- Profil, Benachrichtigungseinstellungen und echter Passwort-Reset per E-Mail
- Live-Synchronisierung von Profil, Gruppen und Terminen mit Cloud Firestore
- benutzerspezifische Firestore-Sicherheitsregeln
- GitHub-Actions-Workflow für Unit Tests und Lint

Einladungslinks, echte gemeinsame Gruppenmitgliedschaften und Rollen sind als
nächste Ausbaustufe vorgesehen.

## Firebase einmalig einrichten

1. In der [Firebase Console](https://console.firebase.google.com/) ein Projekt erstellen.
2. Eine Android-App mit dem Paketnamen `de.gruppenkalender.app` hinzufügen.
3. Die heruntergeladene Datei `google-services.json` in den Ordner `app/` legen.
4. Unter **Authentication → Sign-in method** die Methode **Email/Password** aktivieren.
5. Unter **Firestore Database** eine Datenbank erstellen.
6. Die Regeln aus `firestore.rules` in der Firebase Console veröffentlichen oder mit
   `firebase deploy --only firestore:rules` deployen.

Ohne die echte `app/google-services.json` kann Firebase nicht auf dein Projekt
zugreifen. `app/google-services.json.example` dient nur dem Buildtest und enthält
keine nutzbare Firebase-Konfiguration.

## In Android Studio starten

Voraussetzungen:

- Android Studio Otter (2025.2.1) oder neuer
- JDK 17
- Android SDK 36

Vorgehen:

1. Den Ordner `GruppenKalender` in Android Studio über **Open** auswählen.
2. Den Gradle-Sync abwarten. Android Studio lädt beim ersten Start die
   Abhängigkeiten.
3. Falls nötig, ein Gerät mit Android 8.0 (API 26) oder neuer anlegen.
4. Die Run-Konfiguration `app` auswählen und **Run** starten.

Beim ersten Start erstellst du über **Jetzt registrieren** ein echtes Konto.
Danach prüft Firebase bei jeder Anmeldung die E-Mail-Adresse und das Passwort.

## Projektstruktur

```text
app/src/main/java/de/gruppenkalender/app/
├── data/                 Firebase Authentication und Cloud Firestore
├── model/                Gruppen-, Termin- und Navigationsmodelle
├── ui/
│   ├── components/       wiederverwendbare Kinship-UI-Komponenten
│   ├── screens/          Login, Home, Gruppen, Kalender, Termine, Settings
│   ├── theme/            Farben und Typografie
│   ├── AppViewModel.kt   zentraler UI-Zustand und Aktionen
│   └── GroupCalendarApp.kt
└── MainActivity.kt
```

Die Navigation ist absichtlich leichtgewichtig und typisiert. Für ein größeres
Produkt kann `AppDestination` später durch Navigation Compose ersetzt werden.

## Daten und Architektur

`AppViewModel` ist die zentrale Zustandsquelle. `FirebaseCalendarRepository`
meldet Benutzer über Firebase Authentication an und synchronisiert Firestore
über Snapshot Listener. Die Daten liegen getrennt nach Firebase-UID unter:

```text
users/{uid}
├── name, email
├── settings/default
├── groups/{groupId}
└── events/{eventId}
```

Empfohlene nächste Schritte:

1. Einladungscodes und Rollen/Rechte pro Gruppe implementieren.
2. Gemeinsame Gruppendokumente statt ausschließlich benutzerspezifischer Daten einführen.
3. WorkManager für Erinnerungen und Benachrichtigungen nutzen.
4. UI- und Repository-Tests erweitern.

## GitHub verwenden

```bash
git init
git add .
git commit -m "Initialer GruppenKalender MVP"
git branch -M main
git remote add origin https://github.com/DEIN-NAME/DEIN-REPOSITORY.git
git push -u origin main
```

Der Workflow unter `.github/workflows/android.yml` führt bei Pushes und Pull
Requests Tests und Android Lint aus.

## Referenzen

Die ausgewerteten Screens und das Ablaufdiagramm liegen zur Nachvollziehbarkeit
unter `docs/reference/`.

## Build-Versionen

- Android Gradle Plugin 8.13.2
- Gradle 8.13
- Kotlin 2.2.21
- Jetpack Compose BOM 2026.06.01
- Firebase Android BOM 34.16.0
- Google Services Gradle Plugin 4.5.0
- minSdk 26, targetSdk/compileSdk 36
