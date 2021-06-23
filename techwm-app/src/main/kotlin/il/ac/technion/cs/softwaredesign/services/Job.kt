package il.ac.technion.cs.softwaredesign.services

import il.ac.technion.cs.softwaredesign.AllocatedJob
import il.ac.technion.cs.softwaredesign.JobDescription
import il.ac.technion.cs.softwaredesign.JobStatus
import il.ac.technion.cs.softwaredesign.execution.GeneralResource
import java.util.concurrent.CompletableFuture

class Job constructor (
    val mId: String,
    val mJobDescription: JobDescription,
    private val mAllocatedResources: List<GeneralResource>,
    private val mJobManager: JobManager,
    ): AllocatedJob {

    override fun id(): CompletableFuture<String> {
        return CompletableFuture.completedFuture(mId)
    }

    override fun resources(): CompletableFuture<List<GeneralResource>> {
        return CompletableFuture.completedFuture(mAllocatedResources)
    }

    override fun finishJob(): CompletableFuture<Unit> {
        return mJobManager.onJobFinish(this)
    }
}