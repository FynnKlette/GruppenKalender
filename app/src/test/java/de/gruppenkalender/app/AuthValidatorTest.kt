package de.gruppenkalender.app

import de.gruppenkalender.app.model.AuthValidator
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {
    @Test
    fun validCredentialsPass() {
        assertNull(AuthValidator.validate("test@example.org", "secret7"))
    }

    @Test
    fun mismatchingPasswordsFail() {
        val result = AuthValidator.validate("test@example.org", "secret7", "secret8")
        assertTrue(result?.contains("stimmen nicht") == true)
    }
}
