package il.ac.technion.cs.softwaredesign.services.database

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.JobDescription
import il.ac.technion.cs.softwaredesign.JobStatus
import main.kotlin.SerializerImpl
import main.kotlin.StorageFactoryImpl
import java.util.concurrent.CompletableFuture

class DbJobHandler @Inject constructor(databaseFactory: StorageFactoryImpl) {
    companion object {
        const val counterKey = "counter"
    }
    private val dbJobIdToJobInfo by lazy { databaseFactory.open(DbDirectoriesPaths.JobIdToJobInfo, SerializerImpl()) }
    private val jobStatusArray = mapOf<Int?, JobStatus>(
        JobStatus.QUEUED.ordinal to JobStatus.QUEUED,
        JobStatus.FAILED.ordinal to JobStatus.FAILED,
        JobStatus.FINISHED.ordinal to JobStatus.FINISHED,
        JobStatus.RUNNING.ordinal to JobStatus.RUNNING
    )

    /**
     * Get unique id for job
     *
     * @return next id
     */
    fun getNextId(): CompletableFuture<String> {
        return dbJobIdToJobInfo.thenCompose {
            it.read(counterKey).thenCompose { value ->
                var id = 0
                if (!value.isNullOrEmpty()) {
                    id = value.toInt()
                }

                it.write(counterKey, (id + 1).toString()).thenApply { id.toString() }
            }
        }
    }

    /**
     * write job to DB: jobId -> jobDescription
     *
     * @param jobId
     * @param jobDescription
     * @return CompletableFuture of action
     */
    fun addJob(jobId: String, jobDescription: JobDescription): CompletableFuture<Unit> {
        return dbJobIdToJobInfo.thenCompose {
            it.write(jobId, jobDescription.toString())
        }
    }

    /**
     * Get job description from DB by ID
     *
     * @param jobId
     * @return CompletableFuture of JobDescription if jobId exists in DB, else CompletableFuture<null>
     */
    fun getJob(jobId: String): CompletableFuture<JobDescription?> {
        return dbJobIdToJobInfo.thenCompose {
            it.read(jobId).thenApply { jobAsString ->
                if (jobAsString == null)
                    jobAsString
                else
                    stringToJobDescription(jobAsString)
            }
        }
    }

    private fun stringToJobDescription(str: String): JobDescription {
        val separatedStr = str.split('^')
        val jobName = separatedStr[0]
        val ownerUserId = separatedStr[2]
        val jobStatus = separatedStr[3]
        val listAsString = separatedStr[1]
        val allocatedResources = listAsString.drop(1).dropLast(1).split(", ")
        val ordinalJobStatus = jobStatusArray[jobStatus.toInt()] ?: throw Exception("job status should not be null!!!!")

        return JobDescription(jobName, allocatedResources, ownerUserId, ordinalJobStatus)
    }
}