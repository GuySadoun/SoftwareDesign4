package authenticationTest

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import il.ac.technion.cs.softwaredesign.*
import il.ac.technion.cs.softwaredesign.authentication.Authenticator
import il.ac.technion.cs.softwaredesign.services.interfaces.token.ITokenManager
import il.ac.technion.cs.softwaredesign.services.interfaces.user.IUserManager
import il.ac.technion.cs.softwaredesign.services.interfaces.user.IUserPasswordVerifier
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class AuthenticatorTests {
    private val username = "username"
    private val password = "password"
    private val token = "token"

    val userPasswordVerifierMock = mockk<IUserPasswordVerifier>(relaxUnitFun = true)
    val userManagerMock = mockk<IUserManager>()
    val tokenManagerMock = mockk<ITokenManager>(relaxUnitFun = true)

    @AfterEach
    fun clearMocksConfiguration() {
        clearMocks(userPasswordVerifierMock, userManagerMock, tokenManagerMock)
    }

    @Nested
    inner class `authenticate with username and password tests` {
        @Test
        fun `try to authenticate with mismatch password throws IllegalArgumentException`() {
            // Arrange
            every { userPasswordVerifierMock.isUsernamePasswordMatch(username, password) } returns CompletableFuture.completedFuture(false)
            every { userManagerMock.isUserRevoked(username) } returns CompletableFuture.completedFuture(false)

            val authenticator = Authenticator(mockk(), userPasswordVerifierMock, userManagerMock, mockk(), mockk())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                authenticator.authenticate(username, password).join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }

        @Test
        fun `try to authenticate with matched password returns authenticated user with new token`() {
            // Arrange
            every { tokenManagerMock.createOrReplaceUserToken(username) } returns CompletableFuture.completedFuture(token)
            every { userPasswordVerifierMock.isUsernamePasswordMatch(username, password) } returns CompletableFuture.completedFuture(true)
            every { userManagerMock.isUserRevoked(username) } returns CompletableFuture.completedFuture(false)

            val authenticator = Authenticator(tokenManagerMock, userPasswordVerifierMock, userManagerMock, mockk(), mockk())

            // Act
            val result = authenticator.authenticate(username, password).join()

            // Assert
            Assertions.assertEquals(result.mToken, token)
        }

        @Test
        fun `when revoked user try to authenticate, throws IllegalArgumentException`() {
            // Arrange
            every { userPasswordVerifierMock.isUsernamePasswordMatch(username, password) } returns CompletableFuture.completedFuture(true)
            every { userManagerMock.isUserRevoked(username) } returns CompletableFuture.completedFuture(true)

            val authenticator = Authenticator(mockk(), userPasswordVerifierMock, userManagerMock, mockk(), mockk())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                authenticator.authenticate(username, password).join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }
    }

    @Nested
    inner class `authenticate with token tests` {
        @Test
        fun `try to authenticate by non-existing token throws exception`() {
            // Arrange
            every { tokenManagerMock.getUsernameByTokenIfExists(token) } returns CompletableFuture.completedFuture(null)
            every { userPasswordVerifierMock.isUsernamePasswordMatch(username, password) } returns CompletableFuture.completedFuture(true)
            val authenticator = Authenticator(tokenManagerMock, userPasswordVerifierMock, mockk(), mockk(), mockk())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                authenticator.authenticate(token).join()
            }
            assertThat(throwable.cause!!, isA<TokenDoesNotExistException>())
        }

        @Test
        fun `try to authenticate with existing token returns authenticated user with the same token`() {
            // Arrange
            every { tokenManagerMock.createOrReplaceUserToken(username) } returns CompletableFuture.completedFuture(token)
            every { tokenManagerMock.getUsernameByTokenIfExists(token) } returns CompletableFuture.completedFuture(username)
            every { userPasswordVerifierMock.isUsernamePasswordMatch(username, password) } returns CompletableFuture.completedFuture(true)
            val authenticator = Authenticator(tokenManagerMock, userPasswordVerifierMock, mockk(), mockk(), mockk())

            // Act
            val result = authenticator.authenticate(token).join()

            // Assert
            Assertions.assertEquals(result.mToken, token)
        }
    }
}