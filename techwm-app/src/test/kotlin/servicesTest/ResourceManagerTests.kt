package servicesTest

import il.ac.technion.cs.softwaredesign.execution.ExecutionService
import il.ac.technion.cs.softwaredesign.services.ResourceManager
import il.ac.technion.cs.softwaredesign.services.database.DbResourceHandler
import il.ac.technion.cs.softwaredesign.services.database.ResourceInfo
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class ResourceManagerTests {
    private val name = "Menashe"
    private val id = "0120340hjfXvlEW10"

    private val dbResourceHandlerMock = mockkClass(DbResourceHandler::class, relaxUnitFun = true)
    private val executionServiceMock = mockk<ExecutionService>(relaxUnitFun = true)

    @AfterEach
    fun clearFakesDatabasesAndMocks() {
        clearMocks(dbResourceHandlerMock, executionServiceMock)
    }

    @Nested
    inner class `check is id exist`{
        @Test
        fun `isIdExist return false when no resource`() {
            // Arrange
            every { dbResourceHandlerMock.getResourceById(id) } returns CompletableFuture.completedFuture(null)
            val rm = ResourceManager(dbResourceHandlerMock, executionServiceMock)

            // Act
            val actual = rm.isIdExist(id).join()

            // Assert
            Assertions.assertFalse(actual)
        }

        @Test
        fun `isIdExist return true when resource exist for id`() {
            // Arrange
            every { dbResourceHandlerMock.getResourceById(id) } returns CompletableFuture.completedFuture(ResourceInfo(true, 0, "no null"))
            val rm = ResourceManager(dbResourceHandlerMock, executionServiceMock)

            // Act
            val actual = rm.isIdExist(id).join()

            // Assert
            Assertions.assertTrue(actual)
        }
    }

    @Nested
    inner class `get attached resources`{
        @Test
        fun `getAttachedResources stops at last object if n is bigger then size`() {
            // Arrange
            for (i in 0 until 4)
                every { dbResourceHandlerMock.getResourceIdBySerialNumber(i) } returns CompletableFuture.completedFuture("str$i")
            every { dbResourceHandlerMock.getResourcesSize() } returns CompletableFuture.completedFuture(4)
            every { dbResourceHandlerMock.getResourceIdBySerialNumber(4) } returns CompletableFuture.completedFuture(null)
            val rm = ResourceManager(dbResourceHandlerMock, executionServiceMock)

            // Act
            val actual = rm.getAttachedResources(6).join()

            // Assert
            Assertions.assertEquals(listOf("str0", "str1", "str2", "str3"), actual)
        }

        @Test
        fun `getAttachedResources return smaller list if asked to`() {
            // Arrange
            for (i in 0 until 30)
                every { dbResourceHandlerMock.getResourceIdBySerialNumber(i) } returns CompletableFuture.completedFuture("str$i")
            every { dbResourceHandlerMock.getResourcesSize() } returns CompletableFuture.completedFuture(30)
            every { dbResourceHandlerMock.getResourceIdBySerialNumber(31) } returns CompletableFuture.completedFuture(null)
            val rm = ResourceManager(dbResourceHandlerMock, executionServiceMock)

            // Act
            val actual = rm.getAttachedResources(4).join()

            // Assert
            Assertions.assertEquals(listOf("str0", "str1", "str2", "str3"), actual)
        }
    }

    @Nested
    inner class `attach hardware resource`{
        @Test
        fun `attachHardwareResource call addHardwareResource`() {
            // Arrange
            every { dbResourceHandlerMock.addHardwareResource(any(), any()) } returns CompletableFuture.completedFuture(Unit)
            val rm = ResourceManager(dbResourceHandlerMock, executionServiceMock)

            // Act
            rm.attachHardwareResource(id, name).join()

            // Assert
            verify { dbResourceHandlerMock.addHardwareResource(id, name) }
        }
    }
}