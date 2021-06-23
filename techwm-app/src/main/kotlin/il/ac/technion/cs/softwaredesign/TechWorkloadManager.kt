package il.ac.technion.cs.softwaredesign

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.authentication.Authenticator
import il.ac.technion.cs.softwaredesign.execution.GeneralResource
import java.util.concurrent.CompletableFuture
import il.ac.technion.cs.softwaredesign.services.AdminCreator

/**
 * This is the main class implementing TechWM, a Technion-made workload manager
 *
 * Currently specified:
 * + Managing users
 * + Managing permissions
 * + Queueing jobs
 */
class TechWorkloadManager @Inject constructor(private val mAuthenticator: Authenticator, private val mAdminCreator: AdminCreator) {
    /**
     * Authenticate a user identified by [username] and [password].
     *
     * If successful, this method returns a unique authentication
     * token which can be used by the user in subsequent calls to other methods. If the user previously logged in,
     * all previously valid tokens are *invalidated*
     *
     * This is a *read* operation.
     *
     * @throws IllegalArgumentException If the password does not match the username, or this user does not exist in the
     * system.
     * @return An authentication token to be used in future calls.
     */
    fun authenticate(username: String, password: String): CompletableFuture<String>{
        return mAuthenticator.authenticate(username, password).thenApply { authenticatedUser -> authenticatedUser.mToken }
    }

    /**
     * Register a user to the system, allowing him to start using it.
     *
     * A user can only be registered by another user with the [PermissionLevel.ADMINISTRATOR] permission level.
     * When a new user is registered, they are assigned a default account type [AccountType.DEFAULT].
     *
     * **Obtaining an admin user**: When the system has no user with the username "admin", a call to this method with
     * any token and an "admin" [username] succeeds, and the permission level is automatically set to
     * [PermissionLevel.ADMINISTRATOR], ignoring the actual [permissionLevel] value. This behavior enables the first
     * setup of the system to account for the case of zero users in the hierarchy.
     *
     * This is a *create* operation.
     *
     * @param token A token used to authenticate the requesting administrator
     * @param username The username to register the user under
     * @param password The password associated with the registered user.
     * @param permissionLevel The new user's permission level
     *
     * @throws PermissionException If [token] is invalid or the user associated with [token] does not have the
     * [PermissionLevel.ADMINISTRATOR] permission level.
     * @throws IllegalArgumentException If a user with the same [username] already exists
     */
    fun register(token: String, username: String, password: String, permissionLevel: PermissionLevel): CompletableFuture<Unit> {
        return mAdminCreator.isAdminExist().thenCompose { isAdminExists ->
            if (username == AdminCreator.adminUsername && !isAdminExists) {
                mAdminCreator.createAdminUser(password)
            }
            else
                mAuthenticator.authenticate(token).thenCompose { authUser ->
                    authUser.registerUser(username, password, permissionLevel)
                }.handle { _, e ->
                    if (e != null)
                    {
                        if (e.cause is TokenDoesNotExistException)
                            throw PermissionException()
                        throw e
                    }
                }
        }
    }

    /**
     * Retrieve information about a user.
     *
     * **Note**: This method can be invoked by all users to query information about other users.
     *
     * This is a *read* operation.
     *
     * @throws PermissionException If [token] is invalid
     *
     * @return If the user exists, returns a [User] object containing information about the found user. Otherwise,
     * return `null`, indicating that there is no such user
     */
    fun userInformation(token: String, username: String): CompletableFuture<User?> {
        return mAuthenticator.authenticate(token).thenCompose { authUser ->
            authUser.getUserInformationIfExists(username)
        }.handle { result, e ->
            if (e != null) {
                if (e.cause is TokenDoesNotExistException)
                    throw PermissionException()
                throw e
            }
            result
        }
    }

    /**
     * Change [username]'s permission level to [newPermissionLevel].
     *
     * A user with [token] can only change some other user's permission level if he holds an equal or higher
     * permission level himself, and can only promote him to the same level as himself.
     *
     * The following hierarchy holds:
     * * [PermissionLevel.USER] users can't invoke this method.
     * * [PermissionLevel.OPERATOR] users can only change the permissions of users with a permission level of
     * [PermissionLevel.OPERATOR] or lower, and can't make them [PermissionLevel.ADMINISTRATOR]s
     * * [PermissionLevel.ADMINISTRATOR] can change the permissions of all users
     *
     * **Note**: A user cannot change his own permissions, and this is considered an error (see below)
     *
     * This is an *update* operation.
     *
     * @throws PermissionException If [token] is invalid, or the invoking user does not have the correct permission
     * to change [username]'s permission level (see above).
     * @throws IllegalArgumentException If the [token] is associated with the same user as [username].
     */
    fun changePermissions(token: String, username: String, newPermissionLevel: PermissionLevel): CompletableFuture<Unit> {
        return mAuthenticator.authenticate(token).thenCompose { authUser ->
            authUser.changeUserPermissions(username, newPermissionLevel)
        }.handle {_ , e ->
            if (e != null) {
                val innerException = e.cause
                if (innerException is TokenDoesNotExistException)
                    throw PermissionException()
                if (innerException is CanNotChangePermissionException)
                    throw PermissionException()
                if (innerException is UserNameDoesNotExistException)
                    throw IllegalArgumentException()
                if (innerException is IllegalArgumentException)
                    throw IllegalArgumentException()
                throw e
            }
        }
    }


    /**
     * Change [username]'s account type to [accountType].
     *
     * **Note**: This method can only be invoked by users with the [PermissionLevel.OPERATOR] or higher
     * permission level, and can only be invoked on users with a [PermissionLevel.USER] permission level.
     *
     * This is an *update* operation.
     *
     * @throws PermissionException If [token] is invalid or the calling user does not hold the required permissions.
     * @throws IllegalArgumentException If [username] is not associated with a [PermissionLevel.USER] permission level.
     */
    fun changeAccountType(token: String, username: String, accountType: AccountType): CompletableFuture<Unit> {
        return mAuthenticator.authenticate(token).thenCompose { authUser ->
            authUser.getMyPermissionLevel().thenApply { permissionLevel ->
                Pair(authUser, permissionLevel)
            }
        }.thenCompose { pair ->
            val authUser = pair.first
            val permissionLevel = pair.second

            if (permissionLevel == PermissionLevel.USER)
                throw PermissionException()
            authUser.changeUserAccountType(username, accountType)
        }.handle { _, e ->
            if (e != null){
                val innerException = e.cause
                if (innerException is TokenDoesNotExistException)
                    throw PermissionException()
                if (innerException is CanNotChangeUserAccountTypeException || e is TokenDoesNotExistException)
                    throw IllegalArgumentException()
                throw e
            }
        }
    }

    /**
     * Revoke a user's permission to use the system. A revoked user is not deleted from the system, but all of his
     * permissions are dropped, and the following holds:
     *
     * * Currently valid tokens of the user, if any, are invalidated.
     * * The user is treated as non-existent for other users of the system, but can be viewed by administrators:
     *   - Subsequent calls to [authenticate] for this user fail as if he is not registered to the system.
     *
     * **Note**: This method can only be invoked by users with the [PermissionLevel.ADMINISTRATOR] permission level.
     *
     * This is an *update* operation.
     *
     * @param token A token used to authenticate the requesting administrator
     * @param username The username for which to revoke permissions
     *
     * @throws PermissionException If the [token] is invalid or the associated user does not hold the
     * [PermissionLevel.ADMINISTRATOR] permission level.
     * @throws IllegalArgumentException If no user exists with [username] (or it was previously revoked).
     */
    fun revokeUser(token: String, username: String): CompletableFuture<Unit> {
        return mAuthenticator.authenticate(token).thenCompose { authUser ->
            authUser.revokeUser(username)
        }.handle { _, e ->
            if (e != null) {
                val innerException = e.cause
                if (innerException is TokenDoesNotExistException)
                    throw PermissionException()
                if (innerException is UserNameDoesNotExistException || innerException is UserIsAlreadyRevokedException)
                    throw IllegalArgumentException()
                throw e
            }
        }
    }

    /**
     * Attach a hardware resource, making it available for jobs in the system.
     *
     * **Note**: This method can only be invoked by users with the [PermissionLevel.OPERATOR] **or higher** permission
     * level.
     *
     * This is a *create* operation.
     *
     * @param token A token used to authenticate the requesting user
     * @param id An id supplied to this hardware resource. This must be unique across all resources in the system.
     * @param name A human-readable name given to this resource. This does not have to be unique.
     *
     * @throws PermissionException If the [token] is invalid, or the associated user does not have the appropriate
     * permission level.
     * @throws IllegalArgumentException If a resource with the same [id] is already attached.
     */
    fun attachHardwareResource(token: String, id: String, name: String): CompletableFuture<Unit> {
        return mAuthenticator.authenticate(token).thenCompose { authUser ->
            authUser.attachHardwareResource(id, name)
        }.handle { _, e ->
            if (e != null) {
                val innerException = e.cause
                if (innerException is TokenDoesNotExistException)
                    throw PermissionException()
                if (innerException is IdAlreadyExistException)
                    throw IllegalArgumentException()
                throw e
            }
        }
    }

    /**
     * Get the human-readable name for an attached hardware resource.
     *
     * **Note**: This method can be invoked by all users.
     *
     * This is a *read* operation.
     *
     * @param token A token used to authenticate the requesting user
     *
     * @throws PermissionException If the [token] is invalid
     * @throws IllegalArgumentException If a resource with the given [id] is not attached to the system.
     * @return A string containing the name of the attached resource
     */
    fun getHardwareResourceName(token: String, id: String): CompletableFuture<String> {
        return mAuthenticator.authenticate(token).thenCompose { authUser ->
            authUser.getHardwareResourceName(id)
        }.handle { result, e ->
            if (e != null){
                val innerException = e.cause
                if (innerException is TokenDoesNotExistException)
                    throw PermissionException()
                if (innerException is ResourceDoesNotExistsException)
                    throw IllegalArgumentException()
                throw e
            }

            result
        }

    }

    /**
     * List the ids of the first [n] available hardware resources. Note that this method also adheres to resources
     * availability during runtime of jobs allocated via [submitJob].
     *
     * **Note**: This method can be invoked by *all* users.
     *
     * This is a *read* operation.
     *
     * @param token A token used to authenticate the requesting user
     * @throws PermissionException If the [token] is invalid.
     *
     * @return A list of ids, of size [n], sorted by date of addition (determined by a call to [attachHardwareResource]).
     * If there are less than [n] attached resources, this method returns a list of size K, where K is the total number
     * of attached resources.
     */
    fun listHardwareResources(token: String, n: Int = 10): CompletableFuture<List<String>> {
        return mAuthenticator.authenticate(token).thenCompose { authUser ->
            authUser.listHardwareResources(n)
        }.handle { result, e ->
            if (e != null){
                if (e.cause is TokenDoesNotExistException)
                    throw PermissionException()
                throw e
            }
            result
        }
    }

    /**
     * Submit a job batch to the queue with a given list of resource IDs ([resources]).
     * A job submission adheres to a FIFO queue semantic, with the following key points:
     * - A job rests at the top of the queue, until all requested [resources] are available.
     * - If a job is not at the top of the queue, it cannot be allocated until all jobs before it have been allocated
     * - The job queue is sorted by submission time. That is, the sooner a job is submitted, the sooner can be allocated
     * resources.
     * - Note: This is not a fair queue, as some resource-heavy jobs can hog the queue head while other light jobs
     * await allocation, but we assume for now that such occurrences are sparse and negligible.
     *
     * *Note: This method can be invoked by *all users.
     *
     * *Note*: Some account types are restricted from requesting specific resources. If such a conflict occurs,
     * an exception is thrown. See [AccountType] for more information. Specifically, [PermissionLevel.OPERATOR] and
     * [PermissionLevel.ADMINISTRATOR] account are implicitly of type [AccountType.ROOT], and the rest of the users
     * are determined by the [changeAccountType] method.
     *
     * This is a create operation.
     *
     * @throws PermissionException If the [token] is invalid.
     * @throws IllegalArgumentException If the requesting user does not have an appropriate account for the desired
     * resources, or at least on of the requested resources does not exist.
     *
     * @return A list of [GeneralResource] objects for usage in the job invocation.
     * Implementation notes:
     * - This method (as all others) returns immediately, but the [CompletableFuture] resolves when the job is allocated.
     * We assume here that no system failures or shutdowns
     * - [listHardwareResources] should adhere to allocation semantics of this method. That is, resources which are
     * currently unavailable, do not show up in the returned list. When a job finishes, all resources are freed
     * and are returned to the pool.
     * - Even when a job is not yet running, it should still show up in the system, so that [jobInformation] calls
     * succeed and view this job as queued.
     */
    fun submitJob(token: String, jobName: String, resources: List<String>): CompletableFuture<AllocatedJob> {
        return mAuthenticator.authenticate(token).thenCompose { authUser ->
            authUser.submitJob(jobName, resources)
        }.handle { result, e ->
            if (e != null){
                if (e.cause is TokenDoesNotExistException)
                    throw PermissionException()
                throw e
            }
            result
        }
    }

    /**
     * Return information about a specific job in the system. [id] is the allocated job id.
     *
     * *Note:    This method can be invoked by *all users.
     *
     * This is a read operation.
     *
     * @throws PermissionException If the [token] is invalid.
     * @throws IllegalArgumentException If a job with the supplied [id] does not exist in the system.
     */
    fun jobInformation(token: String, id: String): CompletableFuture<JobDescription> {
        return mAuthenticator.authenticate(token).thenCompose { authUser ->
            authUser.jobInformation(id)
        }.handle { result, e ->
            if (e != null){
                if (e.cause is TokenDoesNotExistException)
                    throw PermissionException()
                throw e
            }
            result
        }
    }

    /**
     * Cancel a currently queued/running job. If the job was running, it is interrupted and shut down.
     * The job's status becomes [JobStatus.FAILED], and all the allocated resources are returned to the resource pool.
     *
     * *Note*: This method can only be invoked by the user which is the owner of the job.
     *
     * This is a delete operation.
     *
     * @throws PermissionException If the [token] is invalid.
     * @throws IllegalArgumentException If the job associated with [jobId] does not belong to the calling user, or
     * it is not currently in a [JobStatus.QUEUED] or [JobStatus.RUNNING] state.
     */
    fun cancelJob(token: String, jobId: String): CompletableFuture<Unit> {
        return mAuthenticator.authenticate(token).thenCompose { authUser ->
            authUser.cancelJob(jobId)
        }.handle { _, e ->
            if (e != null){
                if (e.cause is TokenDoesNotExistException)
                    throw PermissionException()
                throw e
            }
        }
    }

    fun login(username: String, password: String) : CompletableFuture<Unit> {
        return mAuthenticator.authenticate(username, password).thenCompose { authUser ->
            authUser.login()
        }
    }
}