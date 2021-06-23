package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.execution.GeneralResource
import il.ac.technion.cs.softwaredesign.execution.CPUResource
import il.ac.technion.cs.softwaredesign.execution.GPUResource
import java.lang.StringBuilder
import java.util.concurrent.CompletableFuture

/**
 * Represents a job status in the system:
 * * [QUEUED] The job is queued and waiting for resources.
 * * [RUNNING] The job was allocated resources and is currently running.
 * * [FINISHED] The job finished and is no longer in the queue.
 * * [FAILED] The job failed for some reason and the user should manually requeue it when possible.
 */
enum class JobStatus {
    QUEUED,
    RUNNING,
    FINISHED,
    FAILED
}

/**
 * A description of a job in the system.
 * * [allocatedResources] - A list of allocated resource IDs.
 * * [jobName] - The submitted job name, as specified by the user.
 * * [ownerUserId] - The user who owns this job.
 * * [jobStatus] - A flag indicating this job's status in the system.
 */
data class JobDescription(val jobName: String,
                          val allocatedResources: List<String>,
                          val ownerUserId: String,
                          val jobStatus: JobStatus) {
    override fun toString(): String {
        val res = StringBuilder()
        res.append("$jobName^")
        res.append("$allocatedResources^")
        res.append("$ownerUserId^")
        res.append("${jobStatus.ordinal}")
        return res.toString()
    }
}


/**
 * An [AllocatedJob] represented a job which was allocated resources by the system. Using this object we
 * obtain information about the allocated job, the resources allocated, and we can finish, returning
 * resources to the system's resource pool.
 */
interface AllocatedJob {
    /**
     * Returns a unique ID for this job.
     */
    fun id(): CompletableFuture<String>

    /**
     * Returns a list of allocated resources for this job. Resources can be either [CPUResource]s or [GPUResource],
     * depending on the job request.
     */
    fun resources(): CompletableFuture<List<GeneralResource>>

    /**
     * Marks the job as finished, returning all allocated resources to the resource pool, marking them as "available"
     */
    fun finishJob(): CompletableFuture<Unit>
}