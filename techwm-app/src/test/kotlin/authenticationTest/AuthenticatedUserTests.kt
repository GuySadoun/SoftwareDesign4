package authenticationTest

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import il.ac.technion.cs.softwaredesign.*
import il.ac.technion.cs.softwaredesign.authentication.AuthenticatedUser
import il.ac.technion.cs.softwaredesign.services.JobManager
import il.ac.technion.cs.softwaredesign.services.UserManager
import il.ac.technion.cs.softwaredesign.services.interfaces.resource.IResourceManager
import il.ac.technion.cs.softwaredesign.services.interfaces.token.ITokenManager
import il.ac.technion.cs.softwaredesign.services.interfaces.user.IUserManager
import io.mockk.*
import org.junit.jupiter.api.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class AuthenticatedUserTests {
    private val username = "username"
    private val userToAdd = "userToAdd"
    private val password = "password"
    private val token = "token"
    private val accountType = UserManager.defaultAccountType

    val userManagerMock = mockk<IUserManager>(relaxUnitFun = true)
    val tokenManagerMock = mockk<ITokenManager>(relaxUnitFun = true)
    val resourceManagerMock = mockk<IResourceManager>(relaxUnitFun = true)
    private val jobManagerMock = mockkClass(JobManager::class)

    val authenticatedUser =
        AuthenticatedUser(token, username, userManagerMock, tokenManagerMock, resourceManagerMock, jobManagerMock)

    @AfterEach
    fun clearMocksConfiguration() {
        clearMocks(userManagerMock, tokenManagerMock, resourceManagerMock, jobManagerMock)
    }

    @Nested
    inner class `user registration tests` {
        @Test
        fun `non-admin tries to register user throws permission exception`() {
            // Arrange
            val user = User(username, accountType, PermissionLevel.USER)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(user)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                authenticatedUser.registerUser(userToAdd, password, PermissionLevel.USER).join()
            }
            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `admin tries to register existing user throws illegal argument exception`() {
            // Arrange
            val adminUser = User(username, accountType, PermissionLevel.ADMINISTRATOR)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(adminUser)
            every { userManagerMock.isUsernameExists(userToAdd) } returns CompletableFuture.completedFuture(true)
            every { userManagerMock.isUserRevoked(userToAdd) } returns CompletableFuture.completedFuture(false)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                authenticatedUser.registerUser(userToAdd, password, PermissionLevel.USER).join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
            verify(exactly = 0) { userManagerMock.addUser(any(), any(), any()) }
        }

        @Test
        fun `admin tries to register non existing user call 'addUser'`() {
            // Arrange
            val adminUser = User(username, accountType, PermissionLevel.ADMINISTRATOR)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(adminUser)
            every { userManagerMock.isUserRevoked(userToAdd) } returns CompletableFuture.completedFuture(false)
            every { userManagerMock.addUser(userToAdd, password, PermissionLevel.USER) } returns CompletableFuture.completedFuture(Unit)
            every { userManagerMock.isUsernameExists(userToAdd) } returns CompletableFuture.completedFuture(false)

            // Act
            authenticatedUser.registerUser(userToAdd, password, PermissionLevel.USER).join()

            // Assert
            verify { userManagerMock.addUser(userToAdd, password, PermissionLevel.USER) }
        }

        @Test
        fun `admin can add new user with the same username as revoked user`() {
            // Arrange
            val adminUser = User(username, accountType, PermissionLevel.ADMINISTRATOR)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(adminUser)
            every { userManagerMock.isUsernameExists(userToAdd) } returns CompletableFuture.completedFuture(true)
            every { userManagerMock.isUserRevoked(userToAdd) } returns CompletableFuture.completedFuture(true)

            // Act & Assert
            assertDoesNotThrow {
                authenticatedUser.registerUser(userToAdd, password, PermissionLevel.USER)
            }
        }
    }

    @Nested
    inner class `get user information tests` {
        @Test
        fun `get user information when user exists`() {
            // Arrange
            val userToGetHisInformation = User("name", accountType, PermissionLevel.USER)
            every { userManagerMock.getUserByUsernameIfExists(userToGetHisInformation.username) } returns CompletableFuture.completedFuture(userToGetHisInformation)
            every { userManagerMock.isUserRevoked(userToGetHisInformation.username) } returns CompletableFuture.completedFuture(false)
            val adminUser =User(username, accountType, PermissionLevel.ADMINISTRATOR)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(adminUser)

            // Act
            val result = authenticatedUser.getUserInformationIfExists(userToGetHisInformation.username).join()

            // Assert
            Assertions.assertEquals(userToGetHisInformation, result)
        }

        @Test
        fun `null is returned when trying to get non-existing user`() {
            // Arrange
            val nonExistingUser = "notExist"
            val someUser = User(username, accountType, PermissionLevel.USER)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(someUser)
            every { userManagerMock.getUserByUsernameIfExists(nonExistingUser) } returns CompletableFuture.completedFuture(null)
            every { userManagerMock.isUserRevoked(nonExistingUser) } returns CompletableFuture.completedFuture(false)

            // Act
            val result = authenticatedUser.getUserInformationIfExists(nonExistingUser).join()

            // Assert
            Assertions.assertNull(result)
        }

        @Test
        fun `when non-admin try to get revoked user information, null returned`() {
            // Arrange
            val revokeUsername = "revokedUsername"
            val revokedUser = User(revokeUsername, accountType, PermissionLevel.USER)
            val someUser = User(username, accountType, PermissionLevel.USER)
            every { userManagerMock.getUserByUsernameIfExists(revokeUsername) } returns CompletableFuture.completedFuture(revokedUser)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(someUser)
            every { userManagerMock.isUserRevoked(revokeUsername) } returns CompletableFuture.completedFuture(true)

            // Act
            val result = authenticatedUser.getUserInformationIfExists(revokeUsername).join()

            // Assert
            Assertions.assertNull(result)
        }

        @Test
        fun `admin can get revoked user information`() {
            // Arrange
            val revokeUsername = "revokedUsername"
            val revokedUser = User(revokeUsername, accountType, PermissionLevel.USER)
            val adminUser = User(username, accountType, PermissionLevel.ADMINISTRATOR)
            every { userManagerMock.getUserByUsernameIfExists(revokeUsername) } returns CompletableFuture.completedFuture(revokedUser)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(adminUser)
            every { userManagerMock.isUserRevoked(revokeUsername) } returns CompletableFuture.completedFuture(true)

            // Act
            val result = authenticatedUser.getUserInformationIfExists(revokeUsername).join()

            // Assert
            Assertions.assertEquals(revokedUser, result)
        }
    }

    @Nested
    inner class `change user permissions tests` {
        @Test
        fun `regular user cannot change permission of another user`() {
            // Arrange
            val userToChangeUsername = "toChange"
            val userToChange = User(userToChangeUsername, accountType, PermissionLevel.USER)

            val user = User(username, accountType, PermissionLevel.USER)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(user)
            every { userManagerMock.getUserByUsernameIfExists(userToChangeUsername) } returns CompletableFuture.completedFuture(userToChange)
            every { userManagerMock.isUserRevoked(any()) } returns CompletableFuture.completedFuture(false)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                authenticatedUser.changeUserPermissions(userToChangeUsername, PermissionLevel.USER).join()
            }
            assertThat(throwable.cause!!, isA<CanNotChangePermissionException>())
        }
        @Test
        fun `operator can change user permission`() {
            // Arrange
            val userToChangeUsername = "toChange"
            val userToChange = User(userToChangeUsername, accountType, PermissionLevel.USER)
            val permissionToChangeTo = PermissionLevel.OPERATOR

            val operator = User(username, accountType, PermissionLevel.OPERATOR)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(operator)
            every { userManagerMock.getUserByUsernameIfExists(userToChangeUsername) } returns CompletableFuture.completedFuture(userToChange)
            every { userManagerMock.isUserRevoked(any()) } returns CompletableFuture.completedFuture(false)

            // Act
            authenticatedUser.changeUserPermissions(userToChangeUsername, permissionToChangeTo)

            // Assert
            verify { userManagerMock.overrideExistingUser(userToChangeUsername, userToChange.account, permissionToChangeTo)  }
        }

        @Test
        fun `operator cant give user an admin permissions`() {
            // Arrange
            val userToChangeUsername = "toChange"
            val userToChange = User(userToChangeUsername, accountType, PermissionLevel.USER)

            val operator = User(username, accountType, PermissionLevel.OPERATOR)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(operator)
            every { userManagerMock.getUserByUsernameIfExists(userToChangeUsername) } returns CompletableFuture.completedFuture(userToChange)
            every { userManagerMock.isUserRevoked(any()) } returns CompletableFuture.completedFuture(false)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                authenticatedUser.changeUserPermissions(userToChangeUsername, PermissionLevel.ADMINISTRATOR).join()
            }
            assertThat(throwable.cause!!, isA<CanNotChangePermissionException>())
        }

        @Test
        fun `operator cant change admin permissions`() {
            // Arrange
            val userToChangeUsername = "toChange"
            val userToChange = User(userToChangeUsername, accountType, PermissionLevel.ADMINISTRATOR)

            val operator = User(username, accountType, PermissionLevel.OPERATOR)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(operator)
            every { userManagerMock.getUserByUsernameIfExists(userToChangeUsername) } returns CompletableFuture.completedFuture(userToChange)
            every { userManagerMock.isUserRevoked(any()) } returns CompletableFuture.completedFuture(false)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                authenticatedUser.changeUserPermissions(userToChangeUsername, PermissionLevel.USER).join()
            }
            assertThat(throwable.cause!!, isA<CanNotChangePermissionException>())
        }

        @Test
        fun `admin can change any user permission level`(){
            // Arrange
            val userToChangeUsername = "toChange"
            val userToChange = User(userToChangeUsername, accountType, PermissionLevel.ADMINISTRATOR)
            val permissionToChangeTo = PermissionLevel.ADMINISTRATOR

            val admin = User(username, accountType, PermissionLevel.ADMINISTRATOR)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(admin)
            every { userManagerMock.getUserByUsernameIfExists(userToChangeUsername) } returns CompletableFuture.completedFuture(userToChange)
            every { userManagerMock.isUserRevoked(any()) } returns CompletableFuture.completedFuture(false)

            // Act
            authenticatedUser.changeUserPermissions(userToChangeUsername, permissionToChangeTo)

            // Assert
            verify { userManagerMock.overrideExistingUser(userToChangeUsername, userToChange.account, permissionToChangeTo)  }
        }
    }

    @Nested
    inner class `change user account type tests`{
        @Test
        fun `cant change user with permission level higher than PermissionLevel-User`(){
            // Arrange
            val userToChangeUsername = "toChange"
            val userToChange = User(userToChangeUsername, accountType, PermissionLevel.OPERATOR)

            val admin = User(username, accountType, PermissionLevel.ADMINISTRATOR)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(admin)
            every { userManagerMock.getUserByUsernameIfExists(userToChangeUsername) } returns CompletableFuture.completedFuture(userToChange)
            every { userManagerMock.isUserRevoked(any()) } returns CompletableFuture.completedFuture(false)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                authenticatedUser.changeUserAccountType(userToChangeUsername, AccountType.DEFAULT).join()
            }
            assertThat(throwable.cause!!, isA<CanNotChangeUserAccountTypeException>())
        }

        @Test
        fun `can change user with PermissionLevel-User`(){
            // Arrange
            val userToChangeUsername = "toChange"
            val userToChange = User(userToChangeUsername, accountType, PermissionLevel.USER)
            val accountTypeToChangeTo = AccountType.DEFAULT

            val admin = User(username, accountType, PermissionLevel.ADMINISTRATOR)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(admin)
            every { userManagerMock.getUserByUsernameIfExists(userToChangeUsername) } returns CompletableFuture.completedFuture(userToChange)
            every { userManagerMock.isUserRevoked(any()) } returns CompletableFuture.completedFuture(false)

            // Act
            authenticatedUser.changeUserAccountType(userToChangeUsername, accountTypeToChangeTo)

            // Assert
            verify { userManagerMock.overrideExistingUser(userToChangeUsername, accountTypeToChangeTo, userToChange.permissionLevel)  }
        }
    }

    @Nested
    inner class `revoke user tests`{
        @Test
        fun `cant invoked by non admin user`(){
            // Arrange
            val operator = User(username, accountType, PermissionLevel.OPERATOR)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(operator)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                authenticatedUser.revokeUser("").join()
            }
            assertThat(throwable.cause!!, isA<PermissionException>())
        }
        @Test
        fun `revoking user is invalidating his token`(){
            // Arrange
            val userToRevokeUsername = "toChange"
            val userToRevoke = User(userToRevokeUsername, accountType, PermissionLevel.OPERATOR)

            val admin = User(username, accountType, PermissionLevel.ADMINISTRATOR)
            every { tokenManagerMock.invalidateUsernameToken(userToRevokeUsername) } returns CompletableFuture.completedFuture(Unit)
            every { userManagerMock.isUsernameExists(any()) } returns CompletableFuture.completedFuture(true)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(admin)
            every { userManagerMock.revokeUser(userToRevokeUsername) } returns CompletableFuture.completedFuture(Unit)
            every { userManagerMock.getUserByUsernameIfExists(userToRevokeUsername) } returns CompletableFuture.completedFuture(userToRevoke)
            every { userManagerMock.isUserRevoked(any()) } returns CompletableFuture.completedFuture(false)

            // Act
            authenticatedUser.revokeUser(userToRevokeUsername).join()

            // Assert
            verify { userManagerMock.revokeUser(userToRevokeUsername)  }
            verify { tokenManagerMock.invalidateUsernameToken(userToRevokeUsername) }
        }
        @Test
        fun `try to revoke a revoked user throws an exception`() {
            // Arrange
            val revokedUserToRevokeUsername = "shouldThrowException"
            every { userManagerMock.isUserRevoked(revokedUserToRevokeUsername) } returns CompletableFuture.completedFuture(true)
            every { userManagerMock.isUsernameExists(revokedUserToRevokeUsername) } returns CompletableFuture.completedFuture(true)
            val admin = User(username, accountType, PermissionLevel.ADMINISTRATOR)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(admin)

            //Act & Assert
            val throwable = assertThrows<CompletionException> {
                authenticatedUser.revokeUser(revokedUserToRevokeUsername).join()
            }
            assertThat(throwable.cause!!, isA<UserIsAlreadyRevokedException>())
        }
    }

    @Nested
    inner class `attach hardware resource tests`(){
        @Test
        fun `operator or higher can attach hardware resource`(){
            // Arrange
            val firstResourceId = "id1"
            val secondResourceId = "id2"
            val admin = User("admin", accountType, PermissionLevel.OPERATOR)
            val operator = User("operator", accountType, PermissionLevel.ADMINISTRATOR)
            every { userManagerMock.getUserByUsernameIfExists(admin.username) } returns CompletableFuture.completedFuture(admin)
            every { userManagerMock.getUserByUsernameIfExists(operator.username) } returns CompletableFuture.completedFuture(operator)
            every { resourceManagerMock.isIdExist(any()) } returns CompletableFuture.completedFuture(false)

            val adminAuthenticatedUser =
                AuthenticatedUser(token, admin.username, userManagerMock, tokenManagerMock, resourceManagerMock, jobManagerMock)
            val operatorAuthenticatedUser =
                AuthenticatedUser(token, operator.username, userManagerMock, tokenManagerMock, resourceManagerMock, jobManagerMock)

            // Act
            adminAuthenticatedUser.attachHardwareResource(firstResourceId, "name")
            operatorAuthenticatedUser.attachHardwareResource(secondResourceId, "name")

            // Assert
            verify { resourceManagerMock.attachHardwareResource(firstResourceId, any()) }
            verify { resourceManagerMock.attachHardwareResource(secondResourceId, any()) }
        }

        @Test
        fun `regular user cant attach hardware resource`(){
            // Arrange
            val user = User(username, accountType, PermissionLevel.USER)
            every { userManagerMock.getUserByUsernameIfExists(username) } returns CompletableFuture.completedFuture(user)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                authenticatedUser.attachHardwareResource("", "").join()
            }
            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `if id is already exist, should throw IdAlreadyExistException`(){
            // Arrange
            val resourceId = "id"
            val operator = User("operator", accountType, PermissionLevel.ADMINISTRATOR)
            every { userManagerMock.getUserByUsernameIfExists(operator.username) } returns CompletableFuture.completedFuture(operator)
            every { resourceManagerMock.isIdExist(resourceId) } returns CompletableFuture.completedFuture(true)

            val authenticatedUser =
                AuthenticatedUser(token, operator.username, userManagerMock, tokenManagerMock, resourceManagerMock, jobManagerMock)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                authenticatedUser.attachHardwareResource(resourceId, "").join()
            }
            assertThat(throwable.cause!!, isA<IdAlreadyExistException>())
        }
    }

    @Nested
    inner class `get hardware resource name tests`{
        @Test
        fun `when resource does not exist, throw IllegalArgumentException`() {
            // Arrange
            every { resourceManagerMock.getResourceName(any()) } returns CompletableFuture.completedFuture(null)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                authenticatedUser.getHardwareResourceName("").join()
            }
            assertThat(throwable.cause!!, isA<ResourceDoesNotExistsException>())
        }

        @Test
        fun `when resource exists, return its name`(){
            // Arrange
            val resourceName = "name"
            every { resourceManagerMock.getResourceName(any()) } returns CompletableFuture.completedFuture(resourceName)

            // Act
            val actual = authenticatedUser.getHardwareResourceName("").join()

            // Assert
            Assertions.assertEquals(resourceName, actual)
        }
    }

    @Nested
    inner class `list hardwarer resources tests`{
        @Test
        fun `return the list as resource manager return`(){
            // Arrange
            val expected = listOf("id1", "id2", "id3")
            val n = 5
            every { resourceManagerMock.getAttachedResources(n) } returns CompletableFuture.completedFuture(expected)

            // Act
            val actual = authenticatedUser.listHardwareResources(n).join()

            //Assert
            Assertions.assertEquals(expected, actual)
        }
    }

    @Nested
    inner class `submit job tests` {
        @Test
        fun `when submit job throws Exception, throws the same IllegalArgumentException`() {
            // Arrange
            val user = User("", AccountType.DEFAULT, PermissionLevel.ADMINISTRATOR)
            every { jobManagerMock.submitJob(any(), any(), any()) } returns CompletableFuture.failedFuture(Exception())
            every { userManagerMock.getUserByUsernameIfExists(any()) } returns CompletableFuture.completedFuture(user)
            every { userManagerMock.isUserRevoked(any()) } returns CompletableFuture.completedFuture(false)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                authenticatedUser.submitJob("", listOf()).join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }
    }

    @Nested
    inner class `job information tests` {
        @Test
        fun `when jobManager throws IllegalArgException, we throw the same exception`() {
            // Arrange
            val user = User("", AccountType.DEFAULT, PermissionLevel.ADMINISTRATOR)
            every { jobManagerMock.getJobInformation(any()) } returns CompletableFuture.failedFuture(IllegalArgumentException())
            every { userManagerMock.getUserByUsernameIfExists(any()) } returns CompletableFuture.completedFuture(user)
            every { userManagerMock.isUserRevoked(any()) } returns CompletableFuture.completedFuture(false)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                authenticatedUser.jobInformation("").join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }
    }

    @Nested
    inner class `cancel job tests` {
        @Test
        fun `when jobManager throws IllegalArgException inside a completable, we throw the same exception`() {
            // Arrange
            val user = User("", AccountType.DEFAULT, PermissionLevel.ADMINISTRATOR)
            every { jobManagerMock.cancelJob(any(), any()) } returns CompletableFuture.failedFuture(IllegalArgumentException())
            every { userManagerMock.getUserByUsernameIfExists(any()) } returns CompletableFuture.completedFuture(user)
            every { userManagerMock.isUserRevoked(any()) } returns CompletableFuture.completedFuture(false)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                authenticatedUser.cancelJob("").join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }

        @Test
        fun `when jobManager throws IllegalArgException outside of a completable, we throw the same exception in a completable`() {
            // Arrange
            val user = User("", AccountType.DEFAULT, PermissionLevel.ADMINISTRATOR)
            every { jobManagerMock.cancelJob(any(), any()) } throws IllegalArgumentException()
            every { userManagerMock.getUserByUsernameIfExists(any()) } returns CompletableFuture.completedFuture(user)
            every { userManagerMock.isUserRevoked(any()) } returns CompletableFuture.completedFuture(false)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                authenticatedUser.cancelJob("").join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }
    }


}