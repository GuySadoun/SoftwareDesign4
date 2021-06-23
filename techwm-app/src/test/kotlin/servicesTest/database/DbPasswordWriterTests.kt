package servicesTest.database

import il.ac.technion.cs.softwaredesign.services.database.DbDirectoriesPaths
import il.ac.technion.cs.softwaredesign.services.database.DbPasswordWriter
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.verify
import library.DbFactory
import library.interfaces.IDbHandler
import library.interfaces.IDbWriter
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture


class DbPasswordWriterTests {
    private val dbWriterMock = mockk<IDbHandler>(relaxUnitFun = true)
    private val password = "123123"
    private val username = "username"

    @Test
    fun `set password to user`(){
        // Arrange
        val dbFactoryMock = mockkClass(DbFactory::class)
        every { dbFactoryMock.open(DbDirectoriesPaths.UsernameToPassword) } returns dbWriterMock
        every { dbWriterMock.write(any(), any()) } returns CompletableFuture.completedFuture(Unit)
        val dbPasswordWriter = DbPasswordWriter(dbFactoryMock)

        // Act
        dbPasswordWriter.setPassword(username, password).join()

        // Assert
        verify { dbWriterMock.write(username, password) }
    }
}