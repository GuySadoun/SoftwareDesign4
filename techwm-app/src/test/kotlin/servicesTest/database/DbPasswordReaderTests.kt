package servicesTest.database

import il.ac.technion.cs.softwaredesign.services.database.DbDirectoriesPaths
import il.ac.technion.cs.softwaredesign.services.database.DbPasswordReader
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import library.DbFactory
import library.interfaces.IDbHandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions
import java.util.concurrent.CompletableFuture


class DbPasswordReaderTests {
    private val expected: String = "123456"
    private val username: String = "Antonio"

    @Test
    fun `read password of existing user`() {
        // Arrange
        val dbReaderStub = mockk<IDbHandler>()
        every { dbReaderStub.read(username) } returns CompletableFuture.completedFuture(expected)
        val dbFactoryMock = mockkClass(DbFactory::class)
        every { dbFactoryMock.open(DbDirectoriesPaths.UsernameToPassword) } returns dbReaderStub

        val dbPasswordsReader = DbPasswordReader(dbFactoryMock)

        // Act
        val actual = dbPasswordsReader.getPassword(username).join()

        // Assert
        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun `when user does not exist, return null`() {
        // Arrange
        val dbReaderStub = mockk<IDbHandler>()
        every { dbReaderStub.read(username) } returns CompletableFuture.completedFuture(null)
        val dbFactoryMock = mockkClass(DbFactory::class)
        every { dbFactoryMock.open(DbDirectoriesPaths.UsernameToPassword) } returns dbReaderStub
        val dbPasswordsReader = DbPasswordReader(dbFactoryMock)

        // Act
        val actual = dbPasswordsReader.getPassword(username).join()

        // Assert
        Assertions.assertNull(actual)
    }
}