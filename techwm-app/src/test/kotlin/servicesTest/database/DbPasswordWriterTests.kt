package servicesTest.database

import il.ac.technion.cs.softwaredesign.services.database.DbDirectoriesPaths
import il.ac.technion.cs.softwaredesign.services.database.DbPasswordWriter
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.verify
import main.kotlin.Storage
import main.kotlin.StorageFactoryImpl
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture


class DbPasswordWriterTests {
    private val dbWriterMock = mockk<Storage<String>>(relaxUnitFun = true)
    private val password = "123123"
    private val username = "username"

    @Test
    fun `set password to user`(){
        // Arrange
        val dbFactoryMock = mockkClass(StorageFactoryImpl::class)
        every { dbFactoryMock.open<String>(DbDirectoriesPaths.UsernameToPassword, any()) } returns CompletableFuture.completedFuture(dbWriterMock)
        every { dbWriterMock.write(any(), any()) } returns CompletableFuture.completedFuture(Unit)
        val dbPasswordWriter = DbPasswordWriter(dbFactoryMock)

        // Act
        dbPasswordWriter.setPassword(username, password).join()

        // Assert
        verify { dbWriterMock.write(username, password) }
    }
}