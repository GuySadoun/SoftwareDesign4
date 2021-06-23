package il.ac.technion.cs.softwaredesign.services

import com.google.common.collect.ImmutableList
import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.*
import il.ac.technion.cs.softwaredesign.execution.CPUResource
import il.ac.technion.cs.softwaredesign.execution.GPUResource
import il.ac.technion.cs.softwaredesign.execution.GeneralResource
import il.ac.technion.cs.softwaredesign.services.database.DbJobHandler
import il.ac.technion.cs.softwaredesign.services.interfaces.resource.IResourceManager
import java.util.*
import java.util.concurrent.CompletableFuture

var dbg = false

class JobManager @Inject constructor(
    private val mResourceManager: IResourceManager,
    private val mDbJobHandler: DbJobHandler
) {
    private var queue: LinkedHashMap<String, Pair<JobDescription, CompletableFuture<AllocatedJob>>> = LinkedHashMap()
    private val idJobsDict = mutableMapOf<String, Pair<JobDescription, Job?>>()

    fun submitJob(user: User, jobName: String, resources: List<String>) : CompletableFuture<AllocatedJob> {
        var jobCompletableFuture = CompletableFuture<AllocatedJob>()

        if (dbg) println("submitJob - user: " + user.username + " jobName: " + jobName + " resources: " + resources)
        val accountType = if (user.permissionLevel != PermissionLevel.USER) AccountType.ROOT else user.account

        if (dbg) println("accountType:$accountType")
        val unitCompletable = when (accountType) {
            AccountType.DEFAULT -> {
                isResourceRequestLegalForDefaultType(resources)
            }
            AccountType.RESEARCH -> {
                isResourceRequestLegalForResearchType(resources)
            }
            else -> {
                allResourceExists(resources).thenCompose {
                    verifyAllResources(resources)
                }
            }
        }


        if (dbg) println("jobCompletableFuture created")
        jobCompletableFuture = unitCompletable.thenCompose {
            if (dbg) println("in first thenCompose")
            val jobDescription = JobDescription(jobName, resources, user.username, JobStatus.QUEUED)
            mDbJobHandler.getNextId().thenApply { jobId ->
                if (dbg) println("nextId: $jobId")
                if (dbg) println("Q size: " + queue.size)
                idJobsDict[jobId] = Pair(jobDescription, null)
                if (queue.size == 0){
                    queue[jobId] = Pair(jobDescription, jobCompletableFuture)
                    pullJobIfPossible()
                } else {
                    queue[jobId] = Pair(jobDescription, jobCompletableFuture)
                }
            }
        }.thenCompose { jobCompletableFuture }

        return jobCompletableFuture
    }

    fun onJobFinish(finishedJob: Job): CompletableFuture<Unit> {
        val oldJobDescription = finishedJob.mJobDescription
        val newJobDescription = JobDescription(oldJobDescription.jobName, oldJobDescription.allocatedResources, oldJobDescription.ownerUserId, JobStatus.FINISHED)
        idJobsDict[finishedJob.mId] = Pair(newJobDescription, finishedJob)

        return mDbJobHandler.addJob(finishedJob.mId, newJobDescription)
            .thenCompose {
                finishedJob.resources().thenCompose { resources ->
                    releaseJobResources(resources)
                }
            }.thenCompose {
                pullJobIfPossible().thenApply {  }
            }
    }

    fun getJobInformation(jobId: String): CompletableFuture<JobDescription> {
        val jobPair = idJobsDict[jobId] ?: return mDbJobHandler.getJob(jobId)
            .thenApply { jobDescription ->
                jobDescription ?: throw IllegalArgumentException()
            }
        return CompletableFuture.completedFuture(jobPair.first)
    }

    fun cancelJob(jobId: String, username: String) : CompletableFuture<Unit> {
        val completable = CompletableFuture.completedFuture(Unit)
        val descriptionJobPair = idJobsDict[jobId] ?: throw IllegalArgumentException()
        val jobDescription = descriptionJobPair.first
        val allocatedJob = descriptionJobPair.second
        return completable.thenApply {
            if ((jobDescription.jobStatus != JobStatus.RUNNING && jobDescription.jobStatus != JobStatus.QUEUED) ||
                jobDescription.ownerUserId != username)
                throw IllegalArgumentException()
        }.thenCompose {
            if (jobDescription.jobStatus == JobStatus.RUNNING) cancelRunningJob(allocatedJob!!, jobId)
            else cancelQueuedJob(jobDescription, jobId)
        }
    }

    private fun areAllResourcesAvailable(resources: List<String>): CompletableFuture<Boolean> {
        var booleanCompletable = CompletableFuture.completedFuture(true)
        var result = true
        for (resource in resources) {
            booleanCompletable = booleanCompletable.thenCompose {
                mResourceManager.isAvailable(resource).thenApply { isAvailable ->
                    result = result && isAvailable
                    result
                }
            }
        }
        return booleanCompletable
    }

    private fun pullJobIfPossible(): CompletableFuture<Boolean>{
        if (queue.isEmpty())
            return CompletableFuture.completedFuture(false)

        val jobId = queue.entries.first().key
        if (dbg) println("try to pull: job:$jobId")
        val jobDescription = queue.entries.first().value.first
        val jobCompletable = queue.entries.first().value.second

        return areAllResourcesAvailable(jobDescription.allocatedResources).thenApply { areAllResourcesAvailable ->
            if (dbg) println("areAllResourcesAvailable: $areAllResourcesAvailable")
            if (areAllResourcesAvailable) {
                allocateJobResources(jobDescription.allocatedResources).thenApply { generalResourcesList ->
                    if (dbg) println("generalResourcesList: $generalResourcesList")

                    val newJobDescription = JobDescription(jobDescription.jobName, jobDescription.allocatedResources, jobDescription.ownerUserId, JobStatus.RUNNING)
                    val allocatedJob = Job(jobId, newJobDescription, generalResourcesList, this)
                    idJobsDict[jobId] = Pair(newJobDescription, allocatedJob)
                    jobCompletable.complete(allocatedJob)
                    queue.remove(queue.entries.first().key)
                    pullJobIfPossible()
                }
            }
            areAllResourcesAvailable
        }
    }

    private fun allocateJobResources(resourcesToAllocate: List<String>): CompletableFuture<List<GeneralResource>> {
        val list = mutableListOf<GeneralResource>()
        var completable = CompletableFuture.completedFuture(list)

        for (resource in resourcesToAllocate) {
            completable = completable.thenCompose {
                mResourceManager.allocateResource(resource).thenApply { generalResource ->
                    list.add(generalResource)
                    list
                }
            }
        }
        if (dbg) println("allocateJobResources - HERE!!!")
        return completable.thenApply {
                mutableList -> ImmutableList.copyOf(mutableList)
        }
    }

    private fun releaseJobResources(resourcesToAllocate: List<GeneralResource>): CompletableFuture<Unit> {
        var completable = CompletableFuture.completedFuture(Unit)

        for (resource in resourcesToAllocate) {
            completable = completable.thenCompose { mResourceManager.releaseResource(resource) }
        }

        return completable
    }

    private fun verifyAllResources(allocatedResources: List<String>): CompletableFuture<Unit> {
        var generalResourceCompletable = CompletableFuture.completedFuture<Class<out GeneralResource>>(GeneralResource::class.java)
        for (resource in allocatedResources) {
            generalResourceCompletable = generalResourceCompletable.thenCompose { mResourceManager.verifyResource(resource) }
        }

        var completableFuture = CompletableFuture.completedFuture(Unit)
        generalResourceCompletable.handle { _ , e ->
            completableFuture = CompletableFuture.failedFuture(e)
        }

    return completableFuture
    }

    private fun isResourceRequestLegalForResearchType(
        resources: List<String>
    ) : CompletableFuture<Unit>{
        var unitCompletable = CompletableFuture.completedFuture(Unit)

        unitCompletable = unitCompletable.thenApply {
            if (resources.size > 4)
                throw ResourceRequestIsIllegalException()
        }
        var GPUCount = 0
        var CPUCount = 0
        for (resource in resources) {
            unitCompletable = unitCompletable.thenCompose {
                mResourceManager.verifyResource(resource).thenApply { type ->
                    if (type == CPUResource::class.java) {
                        CPUCount += 1
                    } else {
                        GPUCount += 1
                    }
                }
            }
        }
        unitCompletable = unitCompletable.thenApply {
            if (GPUCount > 2 || CPUCount > 2) {
                throw ResourceRequestIsIllegalException()
            }
        }
        unitCompletable = unitCompletable.thenCompose { allResourceExists(resources) }
        return unitCompletable
    }

    private fun isResourceRequestLegalForDefaultType(
        resources: List<String>
    ) : CompletableFuture<Unit>{
        var unitCompletable = CompletableFuture.completedFuture(Unit)
        unitCompletable = unitCompletable.thenApply {
            if (resources.size > 2)
                throw ResourceRequestIsIllegalException()
        }
        for (resource in resources) {
            unitCompletable = unitCompletable.thenCompose {
                    mResourceManager.verifyResource(resource).thenApply { type ->
                        if (type == GPUResource::class.java) {
                            throw ResourceRequestIsIllegalException()
                        }
                    }
                }
        }
        unitCompletable = unitCompletable.thenCompose { allResourceExists(resources) }
        return unitCompletable
    }

    private fun allResourceExists(
        resources: List<String>): CompletableFuture<Unit> {
        var unitCompletable1 = CompletableFuture.completedFuture(Unit)
        for (resource in resources) {
            unitCompletable1 = unitCompletable1.thenCompose {
                mResourceManager.isIdExist(resource).thenApply { isExist ->
                    if (!isExist) throw ResourceDoesNotExistsException()
                }
            }
        }
        return unitCompletable1
    }

    private fun cancelQueuedJob(jobDescription: JobDescription, jobId: String): CompletableFuture<Unit> {
        val newJobDescription = updateToFailedJobDescription(jobDescription, jobId)
        return pullJobIfPossible().thenCompose { mDbJobHandler.addJob(jobId, newJobDescription) }
    }

    private fun cancelRunningJob(allocatedJob: Job, jobId: String): CompletableFuture<Unit> {
        val newJobDescription = updateToFailedJobDescription(allocatedJob.mJobDescription, jobId)
        return allocatedJob.resources().thenCompose {  resources ->
            releaseJobResources(resources).thenCompose {
                pullJobIfPossible().thenCompose {
                    mDbJobHandler.addJob(jobId, newJobDescription)
                }
            }
        }
    }

    private fun updateToFailedJobDescription(jobDescription: JobDescription, jobId: String): JobDescription {
        val newJobDescription = JobDescription(jobDescription.jobName, jobDescription.allocatedResources, jobDescription.ownerUserId, JobStatus.FAILED)
        idJobsDict[jobId] = Pair(newJobDescription, null)
        queue.remove(jobId)
        return newJobDescription
    }
}
