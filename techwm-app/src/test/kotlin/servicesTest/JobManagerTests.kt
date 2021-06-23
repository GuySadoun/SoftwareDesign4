package servicesTest

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import il.ac.technion.cs.softwaredesign.*
import il.ac.technion.cs.softwaredesign.execution.CPUResource
import il.ac.technion.cs.softwaredesign.execution.GPUResource
import il.ac.technion.cs.softwaredesign.execution.GeneralResource
import il.ac.technion.cs.softwaredesign.services.JobManager
import il.ac.technion.cs.softwaredesign.services.database.DbJobHandler
import il.ac.technion.cs.softwaredesign.services.interfaces.resource.IResourceManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.concurrent.thread

class JobManagerTests {
    val defaultUser = User("name", AccountType.DEFAULT, PermissionLevel.USER)
    val researcherUser = User("name", AccountType.RESEARCH, PermissionLevel.USER)
    val rootUser = User("name", AccountType.ROOT, PermissionLevel.USER)

    val resourceManagerMock = mockk<IResourceManager>()
    private val dbJobHandlerMock = mockkClass(DbJobHandler::class)

    val jobManager = JobManager(resourceManagerMock, dbJobHandlerMock)

    @Nested
    inner class `submit job tests` {
        @Test
        fun `default user pass more than 2 resources throws exception`(){
            // Arrange
            every { resourceManagerMock.verifyResource(any()) } returns CompletableFuture.completedFuture(CPUResource::class.java)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                jobManager.submitJob(defaultUser, "job name", listOf("1", "2", "3")).join()
            }
            assertThat(throwable.cause!!, isA<ResourceRequestIsIllegalException>())
        }

        @Test
        fun `default user pass gpu resource throws exception`(){
            // Arrange
            every { resourceManagerMock.verifyResource(any()) } returns CompletableFuture.completedFuture(GPUResource::class.java)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                jobManager.submitJob(defaultUser, "job name", listOf("1")).join()
            }
            assertThat(throwable.cause!!, isA<ResourceRequestIsIllegalException>())
        }

        @Test
        fun `default user pass non-exist resource by verify throws exception`(){
            // Arrange
            every { resourceManagerMock.verifyResource(any()) } returns CompletableFuture.failedFuture(IllegalArgumentException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                jobManager.submitJob(defaultUser, "job name", listOf("1")).join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }

        @Test
        fun `default user pass non-exist resource by resource manager throws exception`(){
            // Arrange
            every { resourceManagerMock.verifyResource(any()) } returns CompletableFuture.completedFuture(CPUResource::class.java)
            every { resourceManagerMock.isIdExist(any()) } returns CompletableFuture.completedFuture(false)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                jobManager.submitJob(defaultUser, "job name", listOf("1")).join()
            }
            assertThat(throwable.cause!!, isA<ResourceDoesNotExistsException>())
        }

        @Test
        fun `researcher pass more than 4 resources`(){
            // Arrange
            every { resourceManagerMock.verifyResource(any()) } returns CompletableFuture.completedFuture(CPUResource::class.java)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                jobManager.submitJob(researcherUser, "job name", listOf("1", "2", "3", "4", "5")).join()
            }
            assertThat(throwable.cause!!, isA<ResourceRequestIsIllegalException>())
        }

        @Test
        fun `researcher user pass 3 gpu resources throws exception`(){
            // Arrange
            every { resourceManagerMock.verifyResource(any()) } returns CompletableFuture.completedFuture(GPUResource::class.java)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                jobManager.submitJob(researcherUser, "job name", listOf("1", "2", "3")).join()
            }
            assertThat(throwable.cause!!, isA<ResourceRequestIsIllegalException>())
        }

        @Test
        fun `researcher pass 3 cpu resources throws exception`(){
            // Arrange
            every { resourceManagerMock.verifyResource(any()) } returns CompletableFuture.completedFuture(CPUResource::class.java)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                jobManager.submitJob(researcherUser, "job name", listOf("1", "2", "3")).join()
            }
            assertThat(throwable.cause!!, isA<ResourceRequestIsIllegalException>())
        }

        @Test
        fun `researcher pass non-exist resource by verify throws exception`(){
            // Arrange
            every { resourceManagerMock.verifyResource(any()) } returns CompletableFuture.failedFuture(IllegalArgumentException())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                jobManager.submitJob(researcherUser, "job name", listOf("1")).join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }

        @Test
        fun `researcher pass non-exist resource by resource manager throws exception`(){
            // Arrange
            every { resourceManagerMock.verifyResource(any()) } returns CompletableFuture.completedFuture(CPUResource::class.java)
            every { resourceManagerMock.isIdExist(any()) } returns CompletableFuture.completedFuture(false)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                jobManager.submitJob(researcherUser, "job name", listOf("1")).join()
            }
            assertThat(throwable.cause!!, isA<ResourceDoesNotExistsException>())
        }

        @Test
        fun `root user pass non-exist resource by verify throws exception`(){
            // Arrange
            every { resourceManagerMock.verifyResource(any()) } returns CompletableFuture.failedFuture(IllegalArgumentException())
            every { resourceManagerMock.isIdExist(any()) } returns CompletableFuture.completedFuture(true)
            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                jobManager.submitJob(rootUser, "job name", listOf("1")).join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }

        @Test
        fun `root user pass non-exist resource by resource manager throws exception`(){
            // Arrange
            every { resourceManagerMock.verifyResource(any()) } returns CompletableFuture.completedFuture(CPUResource::class.java)
            every { resourceManagerMock.isIdExist(any()) } returns CompletableFuture.completedFuture(false)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                jobManager.submitJob(rootUser, "job name", listOf("1")).join()
            }
            assertThat(throwable.cause!!, isA<ResourceDoesNotExistsException>())
        }

        @Test
        fun `submit one job work with queue policy`(){
            // Arrange
            every { resourceManagerMock.verifyResource(any()) } returns CompletableFuture.completedFuture(CPUResource::class.java)
            every { resourceManagerMock.isIdExist(any()) } returns CompletableFuture.completedFuture(true)
            every { dbJobHandlerMock.getNextId() } returns CompletableFuture.completedFuture("1")
            every { resourceManagerMock.isAvailable(any()) } returns CompletableFuture.completedFuture(true)
            val listOfGeneralResource: List<CompletableFuture<GeneralResource>> = listOf(
                CompletableFuture.completedFuture(mockk<CPUResource>()),
                CompletableFuture.completedFuture(mockk<CPUResource>()),
                CompletableFuture.completedFuture(mockk<GPUResource>()),
                CompletableFuture.completedFuture(mockk<GPUResource>()))
            every { resourceManagerMock.allocateResource(any()) } returnsMany(listOfGeneralResource)

            // Act
            val actual = jobManager.submitJob(rootUser, "someJob", listOf("1","2","3","4")).join()

            // Assert
            Assertions.assertEquals(listOfGeneralResource.map { x -> x.join() }, actual.resources().join())
        }

        @Test
        fun `submit multiple jobs work with queue policy`(){
            // Arrange
            every { resourceManagerMock.verifyResource(any()) } returns CompletableFuture.completedFuture(CPUResource::class.java)
            every { resourceManagerMock.isIdExist(any()) } returns CompletableFuture.completedFuture(true)
            every { dbJobHandlerMock.getNextId() } returnsMany(listOf(
                CompletableFuture.completedFuture("1"),
                CompletableFuture.completedFuture("2"),
                CompletableFuture.completedFuture("3"),
                CompletableFuture.completedFuture("4")))
            every { resourceManagerMock.isAvailable(any()) } returns CompletableFuture.completedFuture(true)
            val listOfGeneralResource: List<CompletableFuture<GeneralResource>> = listOf(
                CompletableFuture.completedFuture(mockk<CPUResource>()),
                CompletableFuture.completedFuture(mockk<CPUResource>()),
                CompletableFuture.completedFuture(mockk<GPUResource>()),
                CompletableFuture.completedFuture(mockk<GPUResource>()))

            every { resourceManagerMock.allocateResource(any()) } returnsMany(listOfGeneralResource)

            // Act
            val actual1 = jobManager.submitJob(rootUser, "someJob1", listOf("1","2","3","4")).join()
            jobManager.submitJob(rootUser, "someJob2", listOf("1","2")).join()
            jobManager.submitJob(rootUser, "someJob3", listOf("3","4")).join()
            jobManager.submitJob(rootUser, "someJob4", listOf("1","2","3","4")).join()

            // Assert
            Assertions.assertEquals(listOfGeneralResource.map { x -> x.join() }, actual1.resources().join())
        }
    }

    @Nested
    inner class `jobInformation tests` {
        val jobId : String = "abcdef"
        @Test
        fun `jobInformation throws exception if job does not exist`() {
            // Arrange
            every { dbJobHandlerMock.getJob(any()) } returns CompletableFuture.completedFuture(null)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                jobManager.getJobInformation(jobId).join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }

        @Test
        fun `jobInformation for queued job`() {
            // Arrange
            val expected = JobDescription("jobName", listOf("1"), defaultUser.username, JobStatus.QUEUED)
            every { resourceManagerMock.verifyResource(any()) } returns CompletableFuture.completedFuture(CPUResource::class.java)
            every { resourceManagerMock.isIdExist(any()) } returns CompletableFuture.completedFuture(true)
            every { resourceManagerMock.isAvailable(any()) } returns CompletableFuture.completedFuture(false)
            every { dbJobHandlerMock.getNextId() } returns CompletableFuture.completedFuture(jobId)

            jobManager.submitJob(defaultUser, expected.jobName, expected.allocatedResources)

            // Act
            val actual = jobManager.getJobInformation(jobId).join()

            // Assert
            Assertions.assertEquals(expected, actual)
        }

        @Test
        fun `jobInformation for running job`() {
            // Arrange
            val expected = JobDescription("jobName", listOf(), defaultUser.username, JobStatus.RUNNING)
            every { dbJobHandlerMock.getNextId() } returns CompletableFuture.completedFuture(jobId)

            jobManager.submitJob(defaultUser, expected.jobName, expected.allocatedResources).join()

            // Act
            val actual = jobManager.getJobInformation(jobId).join()

            // Assert
            Assertions.assertEquals(expected, actual)
        }

        @Test
        fun `jobInformation for canceled job`() {
            // Arrange
            val expected = JobDescription("jobName", listOf(), defaultUser.username, JobStatus.FAILED)
            every { dbJobHandlerMock.getNextId() } returns CompletableFuture.completedFuture(jobId)

            jobManager.submitJob(defaultUser, expected.jobName, expected.allocatedResources).join()
            jobManager.cancelJob(jobId, expected.ownerUserId)

            // Act
            val actual = jobManager.getJobInformation(jobId).join()

            // Assert
            Assertions.assertEquals(expected, actual)
        }

        @Test
        fun `jobInformation for finished job`() {
            // Arrange
            val expected = JobDescription("jobName", listOf(), defaultUser.username, JobStatus.FINISHED)
            every { dbJobHandlerMock.getNextId() } returns CompletableFuture.completedFuture(jobId)
            every { dbJobHandlerMock.addJob(any(),any()) } returns CompletableFuture.completedFuture(Unit)

            val allocatedJob = jobManager.submitJob(defaultUser, expected.jobName, expected.allocatedResources).join()
            allocatedJob.finishJob()

            // Act
            val actual = jobManager.getJobInformation(jobId).join()

            // Assert
            Assertions.assertEquals(expected, actual)
        }
    }

    @Nested
    inner class `cancelJob tests` {
        @Test
        fun `cancel non-exist job throws exception`(){
            // Arrange
            val jobId = "123456"
            val username = "user"

            assertThrows<IllegalArgumentException> {
                jobManager.cancelJob(jobId, username)
            }
        }

        @Test
        fun `cancel finished job throws exception`(){
            // Arrange
            val jobId = "123456"
            val jobName = "jobName"
            every { dbJobHandlerMock.getJob(any()) } returns CompletableFuture.completedFuture(null)
            every { dbJobHandlerMock.getNextId() } returns CompletableFuture.completedFuture(jobId)
            every { dbJobHandlerMock.addJob(any(), any()) } returns CompletableFuture.completedFuture(Unit)
            val allocated = jobManager.submitJob(defaultUser, jobName, listOf()).join()
            allocated.finishJob().join()
            Assertions.assertEquals(JobStatus.FINISHED, jobManager.getJobInformation(jobId).join().jobStatus)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                jobManager.cancelJob(allocated.id().join(), defaultUser.username).join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }

        @Test
        fun `cancel on cancel throws exception`(){
            // Arrange
            val jobId = "123456"
            val jobName = "jobName"
            every { dbJobHandlerMock.getJob(any()) } returns CompletableFuture.completedFuture(null)
            every { dbJobHandlerMock.getNextId() } returns CompletableFuture.completedFuture(jobId)
            every { dbJobHandlerMock.addJob(any(), any()) } returns CompletableFuture.completedFuture(Unit)
            val allocated = jobManager.submitJob(defaultUser, jobName, listOf()).join()
            jobManager.cancelJob(allocated.id().join(), defaultUser.username).join()
            Assertions.assertEquals(JobStatus.FAILED, jobManager.getJobInformation(jobId).join().jobStatus)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                jobManager.cancelJob(allocated.id().join(), defaultUser.username).join()
            }
            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }

        @Test
        fun `cancel queued job remove the job from the queue and pull the next job`(){
            // Arrange
            val firstJob = JobDescription("jobName", listOf("1"), defaultUser.username, JobStatus.RUNNING)
            val firstJobId = "id1"
            val secondJobId = "id2"
            val secondJob = JobDescription("jobName2", listOf(), defaultUser.username, JobStatus.RUNNING)
            every { resourceManagerMock.verifyResource(any()) } returns CompletableFuture.completedFuture(CPUResource::class.java)
            every { resourceManagerMock.isIdExist(any()) } returns CompletableFuture.completedFuture(true)
            every { resourceManagerMock.isAvailable(any()) } returns CompletableFuture.completedFuture(false)
            every { dbJobHandlerMock.addJob(any(), any()) } returns CompletableFuture.completedFuture(Unit)
            every { dbJobHandlerMock.getNextId() } returnsMany listOf(
                CompletableFuture.completedFuture(firstJobId),
                CompletableFuture.completedFuture(secondJobId)
            )

            jobManager.submitJob(defaultUser, firstJob.jobName, firstJob.allocatedResources)
            jobManager.submitJob(defaultUser, secondJob.jobName, secondJob.allocatedResources)

            // check the state of the manager before we act
            Assertions.assertEquals(JobStatus.QUEUED, jobManager.getJobInformation(secondJobId).join().jobStatus)

            // Act
            jobManager.cancelJob(firstJobId, firstJob.ownerUserId).join()

            // Assert
            Assertions.assertEquals(JobStatus.FAILED, jobManager.getJobInformation(firstJobId).join().jobStatus)
            Assertions.assertEquals(JobStatus.RUNNING, jobManager.getJobInformation(secondJobId).join().jobStatus)
        }

        @Test
        fun `cancel job add the job to the DB`(){
            val runningJob = JobDescription("jobName", listOf(), defaultUser.username, JobStatus.FAILED)
            val runningJobId = "id1"
            val queuedJob = JobDescription("jobName2", listOf("1"), defaultUser.username, JobStatus.FAILED)
            val queuedJobId = "id2"

            every { resourceManagerMock.verifyResource(any()) } returns CompletableFuture.completedFuture(CPUResource::class.java)
            every { resourceManagerMock.isIdExist(any()) } returns CompletableFuture.completedFuture(true)
            every { resourceManagerMock.isAvailable(any()) } returns CompletableFuture.completedFuture(false)
            every { dbJobHandlerMock.addJob(any(), any()) } returns CompletableFuture.completedFuture(Unit)
            every { dbJobHandlerMock.getNextId() } returnsMany listOf(
                CompletableFuture.completedFuture(runningJobId),
                CompletableFuture.completedFuture(queuedJobId)
            )

            jobManager.submitJob(defaultUser, runningJob.jobName, runningJob.allocatedResources)
            jobManager.submitJob(defaultUser, queuedJob.jobName, queuedJob.allocatedResources)

            // Act
            jobManager.cancelJob(runningJobId, runningJob.ownerUserId).join()
            jobManager.cancelJob(queuedJobId, queuedJob.ownerUserId).join()

            // Assert
            verify { dbJobHandlerMock.addJob(runningJobId, runningJob) }
            verify { dbJobHandlerMock.addJob(queuedJobId, queuedJob) }
        }

        @Test
        fun `cancel running job release its resources`(){
            val resourceId = "1"
            val generalResource = CompletableFuture.completedFuture(mockk<GeneralResource>())
            val runningJob = JobDescription("jobName", listOf(resourceId), defaultUser.username, JobStatus.FAILED)
            val runningJobId = "id1"
            every { resourceManagerMock.verifyResource(any()) } returns CompletableFuture.completedFuture(CPUResource::class.java)
            every { resourceManagerMock.allocateResource(any()) } returns generalResource
            every { resourceManagerMock.isIdExist(any()) } returns CompletableFuture.completedFuture(true)
            every { resourceManagerMock.isAvailable(any()) } returns CompletableFuture.completedFuture(true)
            every { resourceManagerMock.releaseResource(any()) } returns CompletableFuture.completedFuture(Unit)
            every { dbJobHandlerMock.addJob(any(), any()) } returns CompletableFuture.completedFuture(Unit)
            every { dbJobHandlerMock.getNextId() } returns CompletableFuture.completedFuture(runningJobId)

            jobManager.submitJob(defaultUser, runningJob.jobName, runningJob.allocatedResources)

            // Act
            jobManager.cancelJob(runningJobId, runningJob.ownerUserId).join()

            // Assert
            verify { resourceManagerMock.releaseResource(generalResource.join()) }
        }
    }

    @Nested
    inner class `onJobFinish tests` {
        @Test
        fun `check addJob being called`(){
            // Arrange
            val jobId = "123456"
            val jobName = "jobName"
            val expected = JobDescription(jobName, listOf(), defaultUser.username, JobStatus.FINISHED)
            every { dbJobHandlerMock.addJob(any(),any()) } returns CompletableFuture.completedFuture(Unit)
            every { dbJobHandlerMock.getNextId() } returns CompletableFuture.completedFuture(jobId)

            val allocatedJob = jobManager.submitJob(defaultUser, expected.jobName, expected.allocatedResources).join()
            verify (exactly = 0) { dbJobHandlerMock.addJob(any(),any()) }

            // Act
            allocatedJob.finishJob().join()

            // Assert
            verify { dbJobHandlerMock.addJob(any(),any()) }
        }

        @Test
        fun `check all resources are released`() {
            // Arrange
            val jobId = "123456"
            val jobName = "jobName"
            val resources = listOf("1","2")
            val generalResource = CompletableFuture.completedFuture(mockk<GeneralResource>())
            val expected = JobDescription(jobName, resources, defaultUser.username, JobStatus.FINISHED)
            every { resourceManagerMock.verifyResource(any()) } returns CompletableFuture.completedFuture(CPUResource::class.java)
            every { resourceManagerMock.isIdExist(any()) } returns CompletableFuture.completedFuture(true)
            every { dbJobHandlerMock.addJob(any(),any()) } returns CompletableFuture.completedFuture(Unit)
            every { dbJobHandlerMock.getNextId() } returns CompletableFuture.completedFuture(jobId)
            every { resourceManagerMock.allocateResource(any()) } returns generalResource
            every { resourceManagerMock.isAvailable(any()) } returns CompletableFuture.completedFuture(true)
            every { resourceManagerMock.releaseResource(any()) } returns CompletableFuture.completedFuture(Unit)

            val allocatedJob = jobManager.submitJob(defaultUser, expected.jobName, expected.allocatedResources).join()
            val generalResources = allocatedJob.resources().join()
            // Act
            allocatedJob.finishJob().join()

            // Assert
            generalResources.forEach { resource ->
                verify {
                    resourceManagerMock.releaseResource(resource)
                }
            }
        }
    }

    @Nested
    inner class `flow tests` {}
}