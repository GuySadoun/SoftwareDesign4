package servicesTest.database

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import il.ac.technion.cs.softwaredesign.IdAlreadyExistException
import il.ac.technion.cs.softwaredesign.services.database.DbDirectoriesPaths
import il.ac.technion.cs.softwaredesign.services.database.DbResourceHandler
import io.mockk.*
import library.DbFactory
import library.interfaces.IDbHandler
import org.junit.jupiter.api.*
import testDoubles.DbHandlerFake
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class DbResourceHandlerTests {
    private val name = "Menashe"
    private val id0 = "0120340hjfXvlEW10"
    private val id1 = "0120340hjfXvlEW21"
    private val id2 = "0120340hjfXvlEW32"
    private val id3 = "0120340hjfXvlEW43"

    private val dbSerialNumberToIdHandlerFake = DbHandlerFake()
    private val dbIdToResourceNameHandlerFake = DbHandlerFake()
    private val dbFactoryMock = mockkClass(DbFactory::class)
    private val dbSerialNumberToIdHandlerMock = mockk<IDbHandler>(relaxUnitFun = true)
    private val dbIdToResourceNameHandlerMock = mockk<IDbHandler>(relaxUnitFun = true)

    @BeforeEach
    fun clearFakesDatabasesAndMocks() {
        dbSerialNumberToIdHandlerFake.clearDatabase()
        dbIdToResourceNameHandlerFake.clearDatabase()

        clearMocks(dbFactoryMock, dbSerialNumberToIdHandlerMock, dbIdToResourceNameHandlerMock)

        every { dbSerialNumberToIdHandlerMock.write(any(), any()) } returns CompletableFuture.completedFuture(Unit)
        every { dbIdToResourceNameHandlerMock.write(any(), any()) } returns CompletableFuture.completedFuture(Unit)
    }

    @Nested
    inner class `add hardware resource` {
        @Test
        fun `check first hardware insertion writes to db and initialize size`() {
            // Arrange
            every { dbFactoryMock.open(DbDirectoriesPaths.SerialNumberToId) } returns dbSerialNumberToIdHandlerMock
            every { dbFactoryMock.open(DbDirectoriesPaths.IdToResourceName) } returns dbIdToResourceNameHandlerMock
            every { dbSerialNumberToIdHandlerMock.read("size") } returns CompletableFuture.completedFuture(null)
            every { dbIdToResourceNameHandlerMock.read(id0) } returns CompletableFuture.completedFuture(null)
            val dbResourceHandler = DbResourceHandler(dbFactoryMock)

            // Act
            dbResourceHandler.addHardwareResource(id0, name).join()

            // Assert
            verify { dbSerialNumberToIdHandlerMock.write("size", "1") }
            verify { dbIdToResourceNameHandlerMock.write(id0, "10-$name") } // 1 for availability and zero for serial number
        }

        @Test
        fun `check hardware insertion keep serial number`() {
            // Arrange
            every { dbFactoryMock.open(DbDirectoriesPaths.SerialNumberToId) } returns dbSerialNumberToIdHandlerMock
            every { dbFactoryMock.open(DbDirectoriesPaths.IdToResourceName) } returns dbIdToResourceNameHandlerMock
            every { dbSerialNumberToIdHandlerMock.read("size") } returns CompletableFuture.completedFuture("379")
            every { dbIdToResourceNameHandlerMock.read(id0) } returns CompletableFuture.completedFuture(null)
            val dbResourceHandler = DbResourceHandler(dbFactoryMock)

            // Act
            dbResourceHandler.addHardwareResource(id0, name).join()

            // Assert
            verify { dbSerialNumberToIdHandlerMock.write("379", id0) }
            verify { dbSerialNumberToIdHandlerMock.write("size", "380") }
        }

        @Test
        fun `check hardware insertion with existing resource id throws exception`() {
            // Arrange
            every { dbFactoryMock.open(DbDirectoriesPaths.SerialNumberToId) } returns dbSerialNumberToIdHandlerMock
            every { dbFactoryMock.open(DbDirectoriesPaths.IdToResourceName) } returns dbIdToResourceNameHandlerMock
            every { dbSerialNumberToIdHandlerMock.read("size") } returns CompletableFuture.completedFuture("379")
            every { dbIdToResourceNameHandlerMock.read(id0) } returns CompletableFuture.completedFuture("11-name")
            val dbResourceHandler = DbResourceHandler(dbFactoryMock)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                dbResourceHandler.addHardwareResource(id0, name).join()
            }

            assertThat(throwable.cause!!, isA<IdAlreadyExistException>())
        }
    }

    @Nested
    inner class `get resource by id` {
        @Test
        fun `getResourceById after insertion returns resource`() {
            // Arrange
            every { dbFactoryMock.open(DbDirectoriesPaths.SerialNumberToId) } returns dbSerialNumberToIdHandlerFake
            every { dbFactoryMock.open(DbDirectoriesPaths.IdToResourceName) } returns dbIdToResourceNameHandlerFake
            val dbResourceHandler = DbResourceHandler(dbFactoryMock)
            dbResourceHandler.addHardwareResource(id0, name).join()

            // Act
            val actual = dbResourceHandler.getResourceById(id0).join()
            val shouldBeNull = dbResourceHandler.getResourceById(id1).join()

            // Assert
            Assertions.assertEquals(name, actual?.name)
            Assertions.assertEquals(true, actual?.isAvailable)
            Assertions.assertNull(shouldBeNull)
        }

        @Test
        fun `getResourceId keep order of serial numbers`(){
            // Arrange
            every { dbFactoryMock.open(DbDirectoriesPaths.SerialNumberToId) } returns dbSerialNumberToIdHandlerFake
            every { dbFactoryMock.open(DbDirectoriesPaths.IdToResourceName) } returns dbIdToResourceNameHandlerFake
            val dbResourceHandler = DbResourceHandler(dbFactoryMock)

            dbResourceHandler.addHardwareResource(id0, name).join()
            dbResourceHandler.addHardwareResource(id1, name).join()
            dbResourceHandler.addHardwareResource(id2, name).join()

            // Act & Assert
            Assertions.assertEquals(0, dbResourceHandler.getResourceById(id0).join()?.serialNumber)
            Assertions.assertEquals(1, dbResourceHandler.getResourceById(id1).join()?.serialNumber)
            Assertions.assertEquals(2, dbResourceHandler.getResourceById(id2).join()?.serialNumber)
        }
    }

    @Nested
    inner class `get resource id` {
        @Test
        fun `getResourceId keeps the order`() {
            // Arrange
            every { dbFactoryMock.open(DbDirectoriesPaths.SerialNumberToId) } returns dbSerialNumberToIdHandlerFake
            every { dbFactoryMock.open(DbDirectoriesPaths.IdToResourceName) } returns dbIdToResourceNameHandlerFake
            val dbResourceHandler = DbResourceHandler(dbFactoryMock)

            dbResourceHandler.addHardwareResource(id0, name).join()
            dbResourceHandler.addHardwareResource(id1, name).join()
            dbResourceHandler.addHardwareResource(id2, name).join()
            dbResourceHandler.addHardwareResource(id3, name).join()

            // Act & Assert
            Assertions.assertEquals(dbResourceHandler.getResourceIdBySerialNumber(0).join(), id0)
            Assertions.assertEquals(dbResourceHandler.getResourceIdBySerialNumber(1).join(), id1)
            Assertions.assertEquals(dbResourceHandler.getResourceIdBySerialNumber(2).join(), id2)
            Assertions.assertEquals(dbResourceHandler.getResourceIdBySerialNumber(3).join(), id3)
        }
    }
}