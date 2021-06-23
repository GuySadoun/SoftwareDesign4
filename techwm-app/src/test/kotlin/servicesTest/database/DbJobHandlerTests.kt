package servicesTest.database

import il.ac.technion.cs.softwaredesign.JobDescription
import il.ac.technion.cs.softwaredesign.JobStatus
import il.ac.technion.cs.softwaredesign.services.database.DbDirectoriesPaths
import il.ac.technion.cs.softwaredesign.services.database.DbJobHandler
import io.mockk.every
import io.mockk.mockkClass
import main.kotlin.StorageFactoryImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import testDoubles.StorageFake
import java.util.concurrent.CompletableFuture

class DbJobHandlerTests {
    private val jobName = "Menashe"
    private val username: String = "Antonio"
    private val resources = listOf("_1","_2")
    private val id0 = "0"
    private val id1 = "1"
    val jobDescription: JobDescription = JobDescription(jobName, resources, username, JobStatus.QUEUED)

    private val dbJobIdToJobInfoFake = StorageFake()
    private val dbFactoryMock = mockkClass(StorageFactoryImpl::class)
    private val dbJobHandler : DbJobHandler = DbJobHandler(dbFactoryMock)

    @BeforeEach
    fun clearFakesDatabasesAndMocks() {
        every { dbFactoryMock.open<String>(DbDirectoriesPaths.JobIdToJobInfo,any()) } returns CompletableFuture.completedFuture(dbJobIdToJobInfoFake)
        dbJobIdToJobInfoFake.clearDatabase()
    }

    @Test
    fun `getNextId returns val next id`() {
        for (i in 0..10) {
            // Act
            val actual = dbJobHandler.getNextId().join()

            // Assert
            Assertions.assertEquals(i.toString(), actual)
        }
    }

    @Nested
    inner class `addJob and getJob` {
        @Test
        fun `add job allows us to get the job later`() {
            // Act
            dbJobHandler.addJob(id0, jobDescription).join()
            val actual = dbJobHandler.getJob(id0).join()

            // Assert
            Assertions.assertEquals(jobDescription, actual)
        }

        @Test
        fun `get job for non-exist id returns null`() {
            // Act
            val actual = dbJobHandler.getJob(id1).join()

            // Assert
            Assertions.assertEquals(null, actual)
        }

        @Test
        fun `add job with no resources is ok`() {
            // Arrange
            val jobDescEmpty = JobDescription(jobName, listOf(), username, JobStatus.QUEUED)

            // Act
            dbJobHandler.addJob(id0, jobDescEmpty).join()
            val actual = dbJobHandler.getJob(id0).join()

            // Assert
            Assertions.assertEquals(jobDescEmpty.toString(), actual.toString())
        }
    }

}