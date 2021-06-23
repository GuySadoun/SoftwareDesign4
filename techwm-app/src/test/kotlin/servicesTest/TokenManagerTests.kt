package servicesTest

import il.ac.technion.cs.softwaredesign.services.TokenManager
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbTokenHandler
import il.ac.technion.cs.softwaredesign.services.interfaces.token.ITokenGenerator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class TokenManagerTests {
    private val username = "username"
    private val token = "token"

    @Test
    fun `get username by token`() {
        // Arrange
        val dbTokenHandler = mockk<IDbTokenHandler>()
        every { dbTokenHandler.getUsernameByToken(any()) } returns CompletableFuture.completedFuture(username)
        val tokenManager = TokenManager(dbTokenHandler, mockk())

        // Act
        val actual = tokenManager.getUsernameByTokenIfExists(token).join()

        // Assert
        Assertions.assertEquals(username, actual)
    }

    @Test
    fun `get username by not existing token should return null`() {
        // Arrange
        val dbTokenHandler = mockk<IDbTokenHandler>()
        every { dbTokenHandler.getUsernameByToken(any()) } returns CompletableFuture.completedFuture(null)
        val tokenManager = TokenManager(dbTokenHandler, mockk())

        // Act
        val result = tokenManager.getUsernameByTokenIfExists(token).join()

        // Assert
        Assertions.assertNull(result)
    }

    @Test
    fun `createOrReplace token does not set deleted token to user`() {
        // Arrange
        val correctToken = "Bonzai"
        val dbTokenHandler = mockk<IDbTokenHandler>(relaxUnitFun = true)
        val tokenGenerator = mockk<ITokenGenerator>()
        every { tokenGenerator.generate() } returnsMany listOf("first", "second", "third", correctToken)
        every { dbTokenHandler.setOrReplaceTokenToUsername(any(), any()) } returns CompletableFuture.completedFuture(Unit)
        every { dbTokenHandler.getUsernameByToken(any()) } returns CompletableFuture.completedFuture(null)
        every { dbTokenHandler.isDeleted(any()) } returnsMany listOf(
            CompletableFuture.completedFuture(true),
            CompletableFuture.completedFuture(true),
            CompletableFuture.completedFuture(true),
            CompletableFuture.completedFuture(false))
        val tokenManager = TokenManager(dbTokenHandler, tokenGenerator)

        // Act
        val result = tokenManager.createOrReplaceUserToken(username).join()

        // Assert
        Assertions.assertEquals(result, correctToken)
        verify { dbTokenHandler.setOrReplaceTokenToUsername(correctToken, username) }
    }

    @Test
    fun `createOrReplace token does not set already exist token to user`() {
        // Arrange
        val correctToken = "Bonzai"
        val dbTokenHandler = mockk<IDbTokenHandler>(relaxUnitFun = true)
        val tokenGenerator = mockk<ITokenGenerator>()
        every { tokenGenerator.generate() } returnsMany listOf("first", "second", "third", correctToken)
        every { dbTokenHandler.setOrReplaceTokenToUsername(correctToken, username) } returns CompletableFuture.completedFuture(Unit)
        every { dbTokenHandler.getUsernameByToken(any()) } returnsMany listOf(
            CompletableFuture.completedFuture(username),
            CompletableFuture.completedFuture(username),
            CompletableFuture.completedFuture(username),
            CompletableFuture.completedFuture(null))
        every { dbTokenHandler.isDeleted(any()) } returns CompletableFuture.completedFuture(false)
        val tokenManager = TokenManager(dbTokenHandler, tokenGenerator)

        // Act
        val result = tokenManager.createOrReplaceUserToken(username).join()

        // Assert
        Assertions.assertEquals(result, correctToken)
        verify { dbTokenHandler.setOrReplaceTokenToUsername(correctToken, username) }
    }

    @Test
    fun `invalidateUsernameToken delete user last token`() {
        // Arrange
        val dbTokenHandler = mockk<IDbTokenHandler>(relaxUnitFun = true)
        every { dbTokenHandler.deleteUserPreviousTokenIfExist(any()) } returns CompletableFuture.completedFuture(Unit)
        val tokenManager = TokenManager(dbTokenHandler, mockk())

        // Act
        tokenManager.invalidateUsernameToken(username).join()

        // Assert
        verify { dbTokenHandler.deleteUserPreviousTokenIfExist(username) }
    }
}