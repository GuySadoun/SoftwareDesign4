package servicesTest

import il.ac.technion.cs.softwaredesign.PermissionLevel
import il.ac.technion.cs.softwaredesign.User
import il.ac.technion.cs.softwaredesign.services.UserManager
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbPasswordWriter
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbUserInfoHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class UserManagerTests {
    private val username = "username"
    private val password = "password"
    private val permissionLevel = PermissionLevel.USER
    private val accountType = UserManager.defaultAccountType

    @Nested
    inner class `add user tests`{
        @Test
        fun `add user add the user to the DB`() {
            // Arrange
            val dbUserInfoHandlerMock = mockk<IDbUserInfoHandler>(relaxUnitFun = true)
            val passwordWriterMock = mockk<IDbPasswordWriter>(relaxUnitFun = true)
            every { dbUserInfoHandlerMock.setUsernameToUser(any()) } returns CompletableFuture.completedFuture(Unit)
            every { dbUserInfoHandlerMock.clearNameFromRevokedList(any()) } returns CompletableFuture.completedFuture(Unit)
            every { passwordWriterMock.setPassword(any(), any()) } returns CompletableFuture.completedFuture(Unit)

            val userManager = UserManager(dbUserInfoHandlerMock, passwordWriterMock)
            val userToAdd = User(username, accountType, permissionLevel)

            // Act
            userManager.addUser(userToAdd.username, password, userToAdd.permissionLevel).join()

            //Assert
            verify { dbUserInfoHandlerMock.setUsernameToUser(userToAdd) }
            verify { passwordWriterMock.setPassword(userToAdd.username, password) }
            verify { dbUserInfoHandlerMock.clearNameFromRevokedList(userToAdd.username) }
        }
    }

    @Nested
    inner class `overrideExistingUser tests`{
        @Test
        fun `just set the override data`() {
            // Arrange
            val dbUserInfoHandlerMock = mockk<IDbUserInfoHandler>(relaxUnitFun = true)
            val passwordWriterMock = mockk<IDbPasswordWriter>(relaxUnitFun = true)

            every { dbUserInfoHandlerMock.setUsernameToUser(any()) } returns CompletableFuture.completedFuture(Unit)
            every { dbUserInfoHandlerMock.clearNameFromRevokedList(any()) } returns CompletableFuture.completedFuture(Unit)
            every { passwordWriterMock.setPassword(any(), any()) } returns CompletableFuture.completedFuture(Unit)

            val userManager = UserManager(dbUserInfoHandlerMock, passwordWriterMock)
            val overrideUser = User(username, accountType, permissionLevel)

            // Act
            userManager.overrideExistingUser(overrideUser.username, overrideUser.account, overrideUser.permissionLevel)

            //Assert
            verify { dbUserInfoHandlerMock.setUsernameToUser(overrideUser) }
        }
    }

    @Nested
    inner class `get user by username tests`{
        @Test
        fun `get user by username return the correct user`(){
            // Arrange
            val user = User(username, accountType, permissionLevel)
            val dbUserInfoHandlerMock = mockk<IDbUserInfoHandler>()
            every { dbUserInfoHandlerMock.getUserByUsername(username) } returns CompletableFuture.completedFuture(user)
            val userManager = UserManager(dbUserInfoHandlerMock, mockk())

            // Act
            val actual = userManager.getUserByUsernameIfExists(username).join()

            //Assert
            Assertions.assertEquals(user, actual)
        }

        @Test
        fun `get user by username return null when user does not exist`(){
            // Arrange
            val dbUserInfoHandlerMock = mockk<IDbUserInfoHandler>()
            val userManager = UserManager(dbUserInfoHandlerMock, mockk())
            every { dbUserInfoHandlerMock.getUserByUsername(username) } returns CompletableFuture.completedFuture(null)

            // Act
            val actual = userManager.getUserByUsernameIfExists(username).join()

            //Assert
            Assertions.assertNull(actual)
        }
    }

    @Nested
    inner class `isUsernameExists tests`{
        @Test
        fun `return true when username exists`(){
            // Arrange
            val user = User(username, accountType, permissionLevel)
            val dbUserInfoHandlerMock = mockk<IDbUserInfoHandler>(relaxed = true)
            val userManager = UserManager(dbUserInfoHandlerMock, mockk())
            every { dbUserInfoHandlerMock.getUserByUsername(username) } returns CompletableFuture.completedFuture(user)

            // Act
            val actual = userManager.isUsernameExists(username).join()

            //Assert
            Assertions.assertTrue(actual)
        }

        @Test
        fun `return false when username does not exists`(){
            // Arrange
            val dbUserInfoHandlerMock = mockk<IDbUserInfoHandler>()
            val userManager = UserManager(dbUserInfoHandlerMock, mockk())
            every { dbUserInfoHandlerMock.getUserByUsername(username) } returns CompletableFuture.completedFuture(null)

            // Act
            val actual = userManager.isUsernameExists(username).join()

            //Assert
            Assertions.assertFalse(actual)
        }
    }

    @Nested
    inner class `is user revoked tests`{
        @Test
        fun `return true if user is REVOKED`() {
            // Arrange
            val revokedUser = User("revokedName", accountType, PermissionLevel.USER)

            val dbUserInfoHandlerMock = mockk<IDbUserInfoHandler>(relaxUnitFun = true)
            val passwordWriterMock = mockk<IDbPasswordWriter>(relaxUnitFun = true)
            val userManager = UserManager(dbUserInfoHandlerMock, passwordWriterMock)

            every { dbUserInfoHandlerMock.isUserRevoked(revokedUser.username) } returns CompletableFuture.completedFuture(true)

            // Act
            val actual = userManager.isUserRevoked(revokedUser.username).join()

            //Assert
            Assertions.assertTrue(actual)
        }

        @Test
        fun `return false if user is not REVOKED`() {
            // Arrange
            val nonRevokedName = "nonRevokedName"
            val nonRevokedUser = User(nonRevokedName, accountType, PermissionLevel.USER)

            val dbUserInfoHandlerMock = mockk<IDbUserInfoHandler>(relaxUnitFun = true)
            val passwordWriterMock = mockk<IDbPasswordWriter>(relaxUnitFun = true)
            val userManager = UserManager(dbUserInfoHandlerMock, passwordWriterMock)

            every { userManager.isUserRevoked(nonRevokedName) } returns CompletableFuture.completedFuture(false)

            // Act
            val actual = userManager.isUserRevoked(nonRevokedUser.username).join()

            //Assert
            Assertions.assertFalse(actual)
        }
    }

}