package servicesTest.database

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import il.ac.technion.cs.softwaredesign.PermissionException
import il.ac.technion.cs.softwaredesign.TokenWasDeletedAndCantBeReusedException
import il.ac.technion.cs.softwaredesign.services.database.DbDirectoriesPaths
import il.ac.technion.cs.softwaredesign.services.database.DbTokenHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.verify
import library.DbFactory
import library.interfaces.IDbHandler
import org.junit.jupiter.api.*
import testDoubles.DbHandlerFake
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class DbTokenHandlerTests {
    private val username = "username"
    private val token = "token"

    private val dbTokenToUsernameHandlerFake = DbHandlerFake()
    private val dbUsernameToTokenHandlerFake = DbHandlerFake()
    private val dbDeletedTokensHandlerFake = DbHandlerFake()

    @BeforeEach
    fun clearFakesDatabases(){
        dbTokenToUsernameHandlerFake.clearDatabase()
        dbUsernameToTokenHandlerFake.clearDatabase()
        dbDeletedTokensHandlerFake.clearDatabase()
    }

    @Nested
    inner class `set token to user` {
        @Test
        fun `check write function is called when we set a token to user`(){
            // Arrange
            val dbTokenToUsernameHandlerMock = mockk<IDbHandler>(relaxUnitFun = true)
            val dbUsernameToTokenHandlerMock = mockk<IDbHandler>(relaxUnitFun = true)
            val dbDeletedTokensHandlerMock = mockk<IDbHandler>(relaxUnitFun = true)
            val dbFactoryMock = mockkClass(DbFactory::class)
            every { dbDeletedTokensHandlerMock.read(any()) } returns CompletableFuture.completedFuture(null)
            every { dbUsernameToTokenHandlerMock.read(any()) } returns CompletableFuture.completedFuture(null)

            every { dbFactoryMock.open(DbDirectoriesPaths.TokenToUsername) } returns dbTokenToUsernameHandlerMock
            every { dbFactoryMock.open(DbDirectoriesPaths.UsernameToToken) } returns dbUsernameToTokenHandlerMock
            every { dbFactoryMock.open(DbDirectoriesPaths.DeletedTokens) } returns dbDeletedTokensHandlerMock

            every { dbTokenToUsernameHandlerMock.write(any(), any()) } returns CompletableFuture.completedFuture(Unit)
            every { dbUsernameToTokenHandlerMock.write(any(), any()) } returns CompletableFuture.completedFuture(Unit)
            every { dbDeletedTokensHandlerMock.write(any(), any()) } returns CompletableFuture.completedFuture(Unit)

            val dbTokenHandler = DbTokenHandler(dbFactoryMock)

            // Act
            dbTokenHandler.setOrReplaceTokenToUsername(token, username).join()

            // Assert
            verify { dbTokenToUsernameHandlerMock.write(token, username) }
            verify { dbUsernameToTokenHandlerMock.write(username, token) }
        }

        @Test
        fun `set new token, should delete previous one`(){
            // Arrange
            val oldToken = token
            val newToken = "newToken"

            val dbFactoryMock = mockkClass(DbFactory::class)
            every { dbFactoryMock.open(DbDirectoriesPaths.TokenToUsername) } returns dbTokenToUsernameHandlerFake
            every { dbFactoryMock.open(DbDirectoriesPaths.UsernameToToken) } returns dbUsernameToTokenHandlerFake
            every { dbFactoryMock.open(DbDirectoriesPaths.DeletedTokens) } returns dbDeletedTokensHandlerFake

            val dbTokenHandler = DbTokenHandler(dbFactoryMock)
            dbTokenHandler.setOrReplaceTokenToUsername(token, username).join()

            // Act
            dbTokenHandler.setOrReplaceTokenToUsername(newToken, username).join()

            // Assert
            Assertions.assertNotNull(dbDeletedTokensHandlerFake.read(oldToken))
            Assertions.assertEquals(newToken, dbUsernameToTokenHandlerFake.read(username).join())
            Assertions.assertEquals(username, dbTokenToUsernameHandlerFake.read(newToken).join())
        }

        @Test
        fun `try to set a deleted token to user, should throw exception`(){
            // Arrange
            val dbDeletedTokensHandlerStub = mockk<IDbHandler>()
            every { dbDeletedTokensHandlerStub.read(token) } returns CompletableFuture.completedFuture(DbTokenHandler.deletedTokenDbValue)

            val dbFactoryMock = mockkClass(DbFactory::class)
            every { dbFactoryMock.open(any()) } returns mockk()
            every { dbFactoryMock.open(DbDirectoriesPaths.DeletedTokens) } returns dbDeletedTokensHandlerStub

            val dbTokenHandler = DbTokenHandler(dbFactoryMock)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                dbTokenHandler.setOrReplaceTokenToUsername(token, username).join()
            }

            assertThat(throwable.cause!!, isA<TokenWasDeletedAndCantBeReusedException>())

        }
    }

    @Test
    fun `is deleted check if the token is in the deleted token hash`(){
        // Arrange
        val dbDeletedTokensHandlerStub = mockk<IDbHandler>()
        every { dbDeletedTokensHandlerStub.read(token) } returns CompletableFuture.completedFuture(DbTokenHandler.deletedTokenDbValue)

        val dbFactoryMock = mockkClass(DbFactory::class)
        every { dbFactoryMock.open(any()) } returns mockk()
        every { dbFactoryMock.open(DbDirectoriesPaths.DeletedTokens) } returns dbDeletedTokensHandlerStub

        val dbTokenHandler = DbTokenHandler(dbFactoryMock)

        // Act
        val actual = dbTokenHandler.isDeleted(token).join()

        // Assert
        Assertions.assertTrue(actual)
    }

    @Nested
    inner class `read username by token` {
        @Test
        fun `get username by token for existing user`(){
            // Arrange
            val dbTokenToUsernameHandlerMock = mockk<IDbHandler>()
            val dbDeletedTokensHandlerMock = mockk<IDbHandler>()
            every { dbTokenToUsernameHandlerMock.read(token) } returns CompletableFuture.completedFuture(username)
            every { dbDeletedTokensHandlerMock.read(token) } returns CompletableFuture.completedFuture(null)

            val dbFactoryMock = mockkClass(DbFactory::class)
            every { dbFactoryMock.open(DbDirectoriesPaths.TokenToUsername) } returns dbTokenToUsernameHandlerMock
            every { dbFactoryMock.open(DbDirectoriesPaths.UsernameToToken) } returns mockk()
            every { dbFactoryMock.open(DbDirectoriesPaths.DeletedTokens) } returns dbDeletedTokensHandlerMock

            val dbTokenHandler = DbTokenHandler(dbFactoryMock)

            // Act
            val actual = dbTokenHandler.getUsernameByToken(token).join()

            // Assert
            Assertions.assertEquals(username, actual)
        }

        @Test
        fun `when token does not exist, get username by token return null`(){
            // Arrange
            val dbTokenToUsernameHandlerMock = mockk<IDbHandler>()
            val dbDeletedTokensHandlerMock = mockk<IDbHandler>()
            every { dbTokenToUsernameHandlerMock.read(token) } returns CompletableFuture.completedFuture(null)
            every { dbDeletedTokensHandlerMock.read(token) } returns CompletableFuture.completedFuture(null)

            val dbFactoryMock = mockkClass(DbFactory::class)
            every { dbFactoryMock.open(DbDirectoriesPaths.TokenToUsername) } returns dbTokenToUsernameHandlerMock
            every { dbFactoryMock.open(DbDirectoriesPaths.UsernameToToken) } returns mockk()
            every { dbFactoryMock.open(DbDirectoriesPaths.DeletedTokens) } returns dbDeletedTokensHandlerMock

            val dbTokenHandler = DbTokenHandler(dbFactoryMock)

            // Act
            val actual = dbTokenHandler.getUsernameByToken(token).join()

            // Assert
            Assertions.assertNull(actual)
        }

        @Test
        fun `get username by a deleted token should return null`(){
            // Arrange
            val dbDeletedTokensHandlerMock = mockk<IDbHandler>()
            every { dbDeletedTokensHandlerMock.read(token) } returns CompletableFuture.completedFuture(DbTokenHandler.deletedTokenDbValue)

            val dbFactoryMock = mockkClass(DbFactory::class)
            every { dbFactoryMock.open(any()) } returns mockk()
            every { dbFactoryMock.open(DbDirectoriesPaths.DeletedTokens) } returns dbDeletedTokensHandlerMock

            val dbTokenHandler = DbTokenHandler(dbFactoryMock)

            // Act
            val actual = dbTokenHandler.getUsernameByToken(token).join()

            // Assert
            Assertions.assertNull(actual)
        }
    }
}