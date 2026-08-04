package de.gruppenkalender.app.model

object AuthValidator {
    fun validate(
        email: String,
        password: String,
        repeatedPassword: String? = null,
    ): String? {
        if (!email.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            return "Bitte gib eine gültige E-Mail-Adresse ein."
        }
        if (password.length < 6) {
            return "Das Passwort muss mindestens 6 Zeichen lang sein."
        }
        if (repeatedPassword != null && password != repeatedPassword) {
            return "Die Passwörter stimmen nicht überein."
        }
        return null
    }
}
