package servicesTest

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import il.ac.technion.cs.softwaredesign.AdminUserAlreadyExistsException
import il.ac.technion.cs.softwaredesign.PermissionException
import il.ac.technion.cs.softwaredesign.PermissionLevel
import il.ac.technion.cs.softwaredesign.services.AdminCreator
import il.ac.technion.cs.softwaredesign.services.interfaces.user.IUserManager
import io.mockk.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class AdminCreatorTests {
    private val password = "123123"

    @Test
    fun `create admin when needed`() {
        // Arrange
        val userManagerMock = mockk<IUserManager>(relaxUnitFun = true)
        every { userManagerMock.isUsernameExists(AdminCreator.adminUsername) } returns CompletableFuture.completedFuture(false)
        val adminCreator = AdminCreator(userManagerMock)

        // Act
        adminCreator.createAdminUser(password)

        // Assert
        verify { userManagerMock.addUser(AdminCreator.adminUsername, password, PermissionLevel.ADMINISTRATOR) }
    }

    @Test
    fun `throw exception when admin already exists`() {
        // Arrange
        val userManagerMock = mockk<IUserManager>()
        every { userManagerMock.isUsernameExists(AdminCreator.adminUsername) } returns CompletableFuture.completedFuture(true)
        val adminCreator = AdminCreator(userManagerMock)

        // Act & Assert
        val throwable = assertThrows<CompletionException> {
            adminCreator.createAdminUser(password).join()
        }
        assertThat(throwable.cause!!, isA<AdminUserAlreadyExistsException>())
        verify(exactly = 0) { userManagerMock.addUser(any(), any(), any()) }
    }

    @Test
    fun `isAdminExist return true if admin exists`(){
        @Test
        fun `throw exception when admin already exists`() {
            // Arrange
            val userManagerMock = mockk<IUserManager>()
            every { userManagerMock.isUsernameExists(AdminCreator.adminUsername) } returns CompletableFuture.completedFuture(true)
            val adminCreator = AdminCreator(userManagerMock)

            // Act
            val actual = adminCreator.isAdminExist().join()

            // Assert
            Assertions.assertTrue(actual)
        }
    }

    @Test
    fun `isAdminExist return false if admin does not exists`(){
        @Test
        fun `throw exception when admin already exists`() {
            // Arrange
            val userManagerMock = mockk<IUserManager>()
            every { userManagerMock.isUsernameExists(AdminCreator.adminUsername) } returns CompletableFuture.completedFuture(false)
            val adminCreator = AdminCreator(userManagerMock)

            // Act
            val actual = adminCreator.isAdminExist().join()

            // Assert
            Assertions.assertFalse(actual)
        }
    }
}