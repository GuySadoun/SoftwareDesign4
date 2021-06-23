package servicesTest.database

import il.ac.technion.cs.softwaredesign.AccountType
import il.ac.technion.cs.softwaredesign.PermissionLevel
import il.ac.technion.cs.softwaredesign.User
import il.ac.technion.cs.softwaredesign.services.database.DbDirectoriesPaths
import il.ac.technion.cs.softwaredesign.services.database.DbUserInfoHandler
import il.ac.technion.cs.softwaredesign.services.database.DbUserInfoHandler.Companion.accountSuffix
import il.ac.technion.cs.softwaredesign.services.database.DbUserInfoHandler.Companion.permissionSuffix
import il.ac.technion.cs.softwaredesign.services.database.DbUserInfoHandler.Companion.usernameSuffix
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.verify
import library.DbFactory
import library.interfaces.IDbHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import testDoubles.DbHandlerFake
import java.util.concurrent.CompletableFuture

class DbUserInfoTests {
    private val username = "username"
    private val user = User(username, AccountType.DEFAULT, PermissionLevel.USER)

    private val dbHandlerFake = DbHandlerFake()

    @AfterEach
    fun clearFakeDatabase(){
        dbHandlerFake.clearDatabase()
    }

    @Test
    fun `check write function is called`(){
        // Arrange
        val dbHandlerMock = mockk<IDbHandler>(relaxUnitFun = true)
        val dbFactoryMock = mockkClass(DbFactory::class)
        every { dbFactoryMock.open(any()) } returns dbHandlerMock
        every { dbHandlerMock.write(any(), any()) } returns CompletableFuture.completedFuture(Unit)

        val dbUserInfoHandler = DbUserInfoHandler(dbFactoryMock)

        // Act
        dbUserInfoHandler.setUsernameToUser(user).join()

        // Assert
        verify { dbHandlerMock.write(username+usernameSuffix, username) }
        verify { dbHandlerMock.write(username+accountSuffix, user.account.ordinal.toString()) }
        verify { dbHandlerMock.write(username+permissionSuffix, user.permissionLevel.ordinal.toString()) }
    }

    @Test
    fun `get user by username for existing user`(){
        // Arrange
        val dbFactoryMock = mockkClass(DbFactory::class)
        every { dbFactoryMock.open(any()) } returns dbHandlerFake

        val dbUserInfoHandler = DbUserInfoHandler(dbFactoryMock)
        dbUserInfoHandler.setUsernameToUser(user).join()

        // Act
        val actual = dbUserInfoHandler.getUserByUsername(username).join()

        // Assert
        Assertions.assertEquals(user, actual)
    }

    @Test
    fun `when username does not exist, return null`(){
        // Arrange
        val dbFactoryMock = mockkClass(DbFactory::class)
        every { dbFactoryMock.open(any()) } returns dbHandlerFake

        val dbUserInfoHandler = DbUserInfoHandler(dbFactoryMock)

        // Act
        val actual = dbUserInfoHandler.getUserByUsername(username).join()

        // Assert
        Assertions.assertNull(actual)
    }

    @Test
    fun `isUserRevoked on non-revoked user returns false`() {
        // Arrange
        val dbFactoryMock = mockkClass(DbFactory::class)
        every { dbFactoryMock.open(any()) } returns dbHandlerFake
        val dbUserInfoHandler = DbUserInfoHandler(dbFactoryMock)

        // Act & Assert
        Assertions.assertFalse(dbUserInfoHandler.isUserRevoked(username).join())
    }

    @Test
    fun `after revoking user isUserRevoked returns true`() {
        // Arrange
        val dbFactoryMock = mockkClass(DbFactory::class)
        every { dbFactoryMock.open(any()) } returns dbHandlerFake
        val dbUserInfoHandler = DbUserInfoHandler(dbFactoryMock)
        dbUserInfoHandler.revokeUser(username).join()

        // Act
        val actual = dbUserInfoHandler.isUserRevoked(username).join()

        // Assert
        Assertions.assertTrue(actual)
    }

    @Test
    fun `clearNameFromRevokedList calls dbIsUsernameRevokedHandler write`() {
        // Arrange
        val dbFactoryMock = mockkClass(DbFactory::class)
        val dbHandlerMock = mockk<IDbHandler>(relaxUnitFun = true)
        every { dbFactoryMock.open(DbDirectoriesPaths.UsernameIsRevoked) } returns dbHandlerMock
        every { dbHandlerMock.write(any(), any()) } returns CompletableFuture.completedFuture(Unit)
        val dbUserInfoHandler = DbUserInfoHandler(dbFactoryMock)

        // Act
        dbUserInfoHandler.clearNameFromRevokedList(username).join()

        // Assert
        verify { dbHandlerMock.write(username, "0") }
    }
}