import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import il.ac.technion.cs.softwaredesign.*
import il.ac.technion.cs.softwaredesign.authentication.AuthenticatedUser
import il.ac.technion.cs.softwaredesign.authentication.Authenticator
import il.ac.technion.cs.softwaredesign.services.AdminCreator
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.verify
import org.junit.jupiter.api.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException


class TechWorkloadManagerTests {
    private val name = "name"
    private val token = "token"
    private val id = "id"
    val authenticatorMock = mockkClass(Authenticator::class, relaxUnitFun = true)
    val authenticatedUserMock = mockkClass(AuthenticatedUser::class, relaxUnitFun = true)
    val adminCreatorMock = mockkClass(AdminCreator::class, relaxUnitFun = true)

    private val manager = TechWorkloadManager(authenticatorMock, adminCreatorMock)

    @BeforeEach
    fun clearMocksConfiguration(){
        clearMocks(authenticatedUserMock, authenticatorMock, adminCreatorMock)

        every { adminCreatorMock.createAdminUser(any()) } returns CompletableFuture.completedFuture(Unit)
        every { adminCreatorMock.isAdminExist() } returns CompletableFuture.completedFuture(false)
        every { authenticatedUserMock.registerUser(any(), any(), any()) } returns CompletableFuture.completedFuture(Unit)

    }

    @Nested
    inner class `user registration tests` {
        @Test
        fun `legal user registration`() {
            // Arrange
            every { authenticatorMock.authenticate(any()) } returns CompletableFuture.completedFuture(authenticatedUserMock)

            val username = "username"
            val password = "password"
            val permissionLevel = PermissionLevel.USER

            // Act
            manager.register("", username, password, permissionLevel).join()

            // Assert
            verify { authenticatedUserMock.registerUser(username, password, permissionLevel) }
        }

        @Test
        fun `register admin when there are no users on the system`() {
            // Arrange
            val password = "123123"
            every { adminCreatorMock.isAdminExist() } returns CompletableFuture.completedFuture(false)

            // Act
            manager.register("", AdminCreator.adminUsername, password, PermissionLevel.ADMINISTRATOR).join()

            // Assert
            verify { adminCreatorMock.createAdminUser(password) }
        }

        @Test
        fun `register admin when admin is already exist with valid token, should throw illegalArgumentException`() {
            // Arrange
            every { adminCreatorMock.isAdminExist() } returns CompletableFuture.completedFuture(true)
            every { authenticatedUserMock.registerUser(any(), any(), any()) } returns CompletableFuture.failedFuture(IllegalArgumentException())
            every { authenticatorMock.authenticate(any()) } returns CompletableFuture.completedFuture(authenticatedUserMock)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.register("", AdminCreator.adminUsername, "", PermissionLevel.ADMINISTRATOR).join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }

        @Test
        fun `register admin when admin is already exist with invalid token, should throw PermissionException`() {
            // Arrange
            every { adminCreatorMock.isAdminExist() } returns CompletableFuture.completedFuture(true)
            every { authenticatorMock.authenticate(any()) } returns CompletableFuture.failedFuture(TokenDoesNotExistException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.register("", AdminCreator.adminUsername, "", PermissionLevel.ADMINISTRATOR).join()
            }
            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `try to register user with invalid token, should throw PermissionException`() {
            // Arrange
            every { authenticatorMock.authenticate(any()) } returns CompletableFuture.failedFuture(TokenDoesNotExistException())
            every { adminCreatorMock.isAdminExist() } returns CompletableFuture.completedFuture(false)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.register("", "", "", PermissionLevel.ADMINISTRATOR).join()
            }
            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `try to register user without admin permissions, should throw PermissionException`() {
            // Arrange
            every { authenticatedUserMock.registerUser(any(), any(), any()) } returns CompletableFuture.failedFuture(PermissionException())
            every { authenticatorMock.authenticate(any()) } returns CompletableFuture.completedFuture(authenticatedUserMock)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.register("", "", "", PermissionLevel.ADMINISTRATOR).join()
            }
            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `try to register user when user with the same username is already exist, should throw IllegalArgumentException`() {
            // Arrange
            every { authenticatedUserMock.registerUser(any(), any(), any()) } returns CompletableFuture.failedFuture(IllegalArgumentException())
            every { authenticatorMock.authenticate(any()) } returns CompletableFuture.completedFuture(authenticatedUserMock)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.register("", "", "", PermissionLevel.ADMINISTRATOR).join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }
    }

    @Nested
    inner class `get user information tests` {
        @Test
        fun `try to get information with invalid token, should throw PermissionException`() {
            // Arrange
            every { authenticatorMock.authenticate(any()) } returns CompletableFuture.failedFuture(TokenDoesNotExistException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.userInformation("", "").join()
            }
            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `try to get information about not existing username, should return null`() {
            // Arrange
            every { authenticatedUserMock.getUserInformationIfExists(any()) } returns CompletableFuture.completedFuture(null)
            every { authenticatorMock.authenticate(any()) } returns CompletableFuture.completedFuture(authenticatedUserMock)

            // Act
            val actual = manager.userInformation("", "").join()

            // Assert
            Assertions.assertNull(actual)
        }
    }

    @Nested
    inner class `change permissions tests` {
        @Test
        fun `try to change permissions with invalid token throws exception`() {
            // Arrange
            every { authenticatorMock.authenticate(any()) } returns CompletableFuture.failedFuture(TokenDoesNotExistException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.changePermissions(token, name, PermissionLevel.OPERATOR).join()
            }
            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `try to change permissions when not allowed throws exception`() {
            // Arrange
            every { authenticatorMock.authenticate(token) } returns  CompletableFuture.completedFuture(authenticatedUserMock)
            every { authenticatedUserMock.mUsername } returns "different name"
            every { authenticatedUserMock.changeUserPermissions(name, PermissionLevel.OPERATOR) } returns CompletableFuture.failedFuture(CanNotChangePermissionException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.changePermissions(token, name, PermissionLevel.OPERATOR).join()
            }
            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `try to change permissions when allowed - success`() {
            // Arrange
            every { authenticatorMock.authenticate(token) } returns  CompletableFuture.completedFuture(authenticatedUserMock)
            every { authenticatedUserMock.mUsername } returns "different name"
            every { authenticatedUserMock.changeUserPermissions(any(), any()) } returns CompletableFuture.completedFuture(Unit)

            // Act
            manager.changePermissions(token, name, PermissionLevel.OPERATOR).join()

            // Assert
            verify { authenticatedUserMock.changeUserPermissions(name, PermissionLevel.OPERATOR) }
        }
    }

    @Nested
    inner class `change account type` {
        @Test
        fun `try to change account type with permission level user - throws exception`() {
            // Arrange
            every { authenticatorMock.authenticate(token) } returns  CompletableFuture.completedFuture(authenticatedUserMock)
            every { authenticatedUserMock.getMyPermissionLevel() } returns CompletableFuture.completedFuture(PermissionLevel.USER)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.changeAccountType(token, name, AccountType.DEFAULT).join()
            }
            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `try to change account type when token is invalid - throws exception`() {
            // Arrange
            every { authenticatorMock.authenticate(any()) } returns CompletableFuture.failedFuture(TokenDoesNotExistException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.changeAccountType(token, name, AccountType.DEFAULT).join()
            }
            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `try to change account type when not allowed - throws exception`() {
            // Arrange
            every { authenticatorMock.authenticate(token) } returns CompletableFuture.completedFuture(authenticatedUserMock)
            every { authenticatedUserMock.getMyPermissionLevel() } returns CompletableFuture.completedFuture(PermissionLevel.OPERATOR)
            every { authenticatedUserMock.changeUserAccountType(name, AccountType.RESEARCH) } returns CompletableFuture.failedFuture(CanNotChangeUserAccountTypeException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.changeAccountType(token, name, AccountType.RESEARCH).join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }

        @Test
        fun `try to change account type when allowed - success`() {
            // Arrange
            every { authenticatorMock.authenticate(token) } returns  CompletableFuture.completedFuture(authenticatedUserMock)
            every { authenticatedUserMock.getMyPermissionLevel() } returns CompletableFuture.completedFuture(PermissionLevel.ADMINISTRATOR)
            every { authenticatedUserMock.changeUserAccountType(any(), any()) } returns CompletableFuture.completedFuture(Unit)

            // Act
            manager.changeAccountType(token, name, AccountType.RESEARCH).join()

            // Assert
            verify { authenticatedUserMock.changeUserAccountType(name, AccountType.RESEARCH) }
        }
    }

    @Nested
    inner class `revoke user` {
        @Test
        fun `try to revoke user with invalid token - throws exception`() {
            // Arrange
            every { authenticatorMock.authenticate(token) } returns CompletableFuture.failedFuture(TokenDoesNotExistException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.revokeUser(token, name).join()
            }
            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `try to revoke non exist user - throws exception`() {
            // Arrange
            every { authenticatorMock.authenticate(token) } returns CompletableFuture.completedFuture(authenticatedUserMock)
            every { authenticatedUserMock.revokeUser(name) } returns CompletableFuture.failedFuture(UserNameDoesNotExistException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.revokeUser(token, name).join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }

        @Test
        fun `try to revoke user when allowed - success`() {
            // Arrange
            every { authenticatorMock.authenticate(token) } returns CompletableFuture.completedFuture(authenticatedUserMock)
            every { authenticatedUserMock.revokeUser(name) } returns CompletableFuture.completedFuture(Unit)

            // Act
            manager.revokeUser(token, name).join()

            // Assert
            verify { authenticatedUserMock.revokeUser(name) }
        }

        @Test
        fun `try to revoke user on revoked user rises exception`() {
            // Arrange
            every { authenticatorMock.authenticate(token) } returns CompletableFuture.completedFuture(authenticatedUserMock)
            every { authenticatedUserMock.revokeUser(name) } returns CompletableFuture.failedFuture(UserIsAlreadyRevokedException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.revokeUser(token, name).join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }
    }

    @Nested
    inner class `attach hardware resource` {
        @Test
        fun `try to attach hardware resource with invalid token - throws exception`() {
            // Arrange
            every { authenticatorMock.authenticate(token) } returns CompletableFuture.failedFuture(TokenDoesNotExistException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.attachHardwareResource(token, id, name).join()
            }
            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `try to revoke non exist user - throws exception`() {
            // Arrange
            every { authenticatorMock.authenticate(token) } returns CompletableFuture.completedFuture(authenticatedUserMock)
            every { authenticatedUserMock.attachHardwareResource(id, name) } returns CompletableFuture.failedFuture(IdAlreadyExistException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.attachHardwareResource(token, id, name).join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }

        @Test
        fun `try to attach hardware user when allowed - success`() {
            // Arrange
            every { authenticatorMock.authenticate(token) } returns CompletableFuture.completedFuture(authenticatedUserMock)
            every { authenticatedUserMock.attachHardwareResource(any(), any()) } returns CompletableFuture.completedFuture(Unit)

            // Act
            manager.attachHardwareResource(token, id, name).join()

            // Assert
            verify { authenticatedUserMock.attachHardwareResource(id, name) }
        }
    }

    @Nested
    inner class `get hardware resource name` {
        @Test
        fun `try to get hardware resource name with invalid token - throws exception`() {
            // Arrange
            every { authenticatorMock.authenticate(token) } returns CompletableFuture.failedFuture(TokenDoesNotExistException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.getHardwareResourceName(token, id).join()
            }
            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `try to get hardware resource name for non exist id - throws exception`() {
            // Arrange
            every { authenticatorMock.authenticate(token) } returns CompletableFuture.completedFuture(authenticatedUserMock)
            every { authenticatedUserMock.getHardwareResourceName(id) } returns CompletableFuture.failedFuture(ResourceDoesNotExistsException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.getHardwareResourceName(token, id).join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }

        @Test
        fun `try to get hardware resource name when allowed - success`() {
            // Arrange
            val expected = "success"
            every { authenticatorMock.authenticate(token) } returns CompletableFuture.completedFuture(authenticatedUserMock)
            every { authenticatedUserMock.getHardwareResourceName(id) } returns CompletableFuture.completedFuture(expected)

            // Act
            val actual = manager.getHardwareResourceName(token, id).join()

            // Assert
            Assertions.assertEquals(expected, actual)
        }
    }

    @Nested
    inner class `list hardware resources` {
        @Test
        fun `try to list hardware resources with invalid token - throws exception`() {
            // Arrange
            every { authenticatorMock.authenticate(token) } returns CompletableFuture.failedFuture(TokenDoesNotExistException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.listHardwareResources(token, 50).join()
            }
            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `try to list hardware resources when allowed - success`() {
            // Arrange
            val n = 10
            val expected = listOf("1", "2", "3")
            every { authenticatorMock.authenticate(token) } returns CompletableFuture.completedFuture(authenticatedUserMock)
            every { authenticatedUserMock.listHardwareResources(n) } returns CompletableFuture.completedFuture(expected)

            // Act
            val actual = manager.listHardwareResources(token).join()

            // Assert
            Assertions.assertEquals(expected, actual)
        }
    }

    @Nested
    inner class `submit job tests` {
        @Test
        fun `when token is invalid, throws permission exception`(){
            // Arrange
            every { authenticatorMock.authenticate(any()) } returns CompletableFuture.failedFuture(TokenDoesNotExistException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.submitJob("", "", listOf()).join()
            }
            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `when authenticatedUser throws IllegalArgsException, throws the same exception`(){
            // Arrange
            every { authenticatorMock.authenticate(any()) } returns CompletableFuture.completedFuture(authenticatedUserMock)
            every { authenticatedUserMock.submitJob(any(), any()) } returns CompletableFuture.failedFuture(IllegalArgumentException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.submitJob("", "", listOf()).join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }
    }

    @Nested
    inner class `job information tests` {
        @Test
        fun `when token is invalid, throws permission exception`(){
            // Arrange
            every { authenticatorMock.authenticate(any()) } returns CompletableFuture.failedFuture(TokenDoesNotExistException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.jobInformation("", "").join()
            }
            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `when authenticatedUser throws IllegalArgsException, throws the same exception`(){
            // Arrange
            every { authenticatorMock.authenticate(any()) } returns CompletableFuture.completedFuture(authenticatedUserMock)
            every { authenticatedUserMock.jobInformation(any()) } returns CompletableFuture.failedFuture(IllegalArgumentException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.jobInformation("", "").join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }
    }

    @Nested
    inner class `cancel job tests` {
        @Test
        fun `when token is invalid, throws permission exception`(){
            // Arrange
            every { authenticatorMock.authenticate(any()) } returns CompletableFuture.failedFuture(TokenDoesNotExistException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.cancelJob("", "").join()
            }
            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `when authenticatedUser throws IllegalArgsException, throws the same exception`(){
            // Arrange
            every { authenticatorMock.authenticate(any()) } returns CompletableFuture.completedFuture(authenticatedUserMock)
            every {authenticatedUserMock.cancelJob(any()) } returns CompletableFuture.failedFuture(IllegalArgumentException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                manager.cancelJob("", "").join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }
    }
}
