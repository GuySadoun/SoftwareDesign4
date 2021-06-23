package servicesTest;

import il.ac.technion.cs.softwaredesign.services.UserPasswordVerifier
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbPasswordReader
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture

class UserPasswordVerifierTests {
    private val username = "username"
    private val password = "123123"
    private val wrongPassword = "1231233"

    @Test
    fun `password match returns true`() {
        // Arrange
        val passwordsReaderMock = mockk<IDbPasswordReader>()
        every { passwordsReaderMock.getPassword(username) } returns CompletableFuture.completedFuture(password)
        val userPasswordVerifier = UserPasswordVerifier(passwordsReaderMock)

        // Act
        val result = userPasswordVerifier.isUsernamePasswordMatch(username, password).join()

        // Assert
        Assertions.assertTrue(result)
    }

    @Test
    fun `password doesn't match returns false`() {
        // Arrange
        val passwordsReaderMock = mockk<IDbPasswordReader>()
        every { passwordsReaderMock.getPassword(username) } returns CompletableFuture.completedFuture(wrongPassword)
        val userPasswordVerifier = UserPasswordVerifier(passwordsReaderMock)

        // Act
        val result = userPasswordVerifier.isUsernamePasswordMatch(username, password).join()

        // Assert
        Assertions.assertFalse(result)
    }

    @Test
    fun `if username does not exist, return false`() {
        // Arrange
        val passwordsReaderMock = mockk<IDbPasswordReader>()
        every { passwordsReaderMock.getPassword(username) } returns CompletableFuture.completedFuture(null)
        val userPasswordVerifier = UserPasswordVerifier(passwordsReaderMock)

        // Act
        val result = userPasswordVerifier.isUsernamePasswordMatch(username, password).join()

        // Assert
        Assertions.assertFalse(result)
    }
}
