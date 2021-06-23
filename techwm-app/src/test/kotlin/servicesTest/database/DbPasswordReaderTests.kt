package servicesTest.database

import il.ac.technion.cs.softwaredesign.services.database.DbDirectoriesPaths
import il.ac.technion.cs.softwaredesign.services.database.DbPasswordReader
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import main.kotlin.Storage
import main.kotlin.StorageFactoryImpl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions
import java.util.concurrent.CompletableFuture


class DbPasswordReaderTests {
    private val expected: String = "123456"
    private val username: String = "Antonio"

    @Test
    fun `read password of existing user`() {
        // Arrange
        val dbReaderStub = mockk<Storage<String>>()
        every { dbReaderStub.read(username) } returns CompletableFuture.completedFuture(expected)
        val dbFactoryMock = mockkClass(StorageFactoryImpl::class)
        every { dbFactoryMock.open<String>(DbDirectoriesPaths.UsernameToPassword, any()) } returns CompletableFuture.completedFuture(dbReaderStub)

        val dbPasswordsReader = DbPasswordReader(dbFactoryMock)

        // Act
        val actual = dbPasswordsReader.getPassword(username).join()

        // Assert
        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun `when user does not exist, return null`() {
        // Arrange
        val dbReaderStub = mockk<Storage<String>>()
        every { dbReaderStub.read(username) } returns CompletableFuture.completedFuture(null)
        val dbFactoryMock = mockkClass(StorageFactoryImpl::class)
        every { dbFactoryMock.open<String>(DbDirectoriesPaths.UsernameToPassword, any()) } returns CompletableFuture.completedFuture(dbReaderStub)
        val dbPasswordsReader = DbPasswordReader(dbFactoryMock)

        // Act
        val actual = dbPasswordsReader.getPassword(username).join()

        // Assert
        Assertions.assertNull(actual)
    }
}