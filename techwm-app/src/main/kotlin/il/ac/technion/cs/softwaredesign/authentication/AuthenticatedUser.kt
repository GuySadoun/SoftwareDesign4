package il.ac.technion.cs.softwaredesign.authentication

import il.ac.technion.cs.softwaredesign.*
import il.ac.technion.cs.softwaredesign.services.JobManager
import il.ac.technion.cs.softwaredesign.services.interfaces.resource.IResourceManager
import il.ac.technion.cs.softwaredesign.services.interfaces.token.ITokenManager
import il.ac.technion.cs.softwaredesign.services.interfaces.user.IUserManager
import java.util.concurrent.CompletableFuture

class AuthenticatedUser constructor(val mToken: String,
                                    val mUsername: String,
                                    private val mUserManager: IUserManager,
                                    private val mTokenManager: ITokenManager,
                                    private val mResourceManager: IResourceManager,
                                    private val mJobManager: JobManager
) {
    /**
     * Register new user to the system
     *
     * @param username the username of the new user
     * @param password the password of the new user
     * @param permissionLevel the permissionLevel of the new user
     * @throws PermissionException if userPermission is not Administrator
     * @throws IllegalArgumentException if the user is already exists and is not revoked
     * @return CompletableFuture of the action
     */
    fun registerUser(username: String, password: String, permissionLevel: PermissionLevel) : CompletableFuture<Unit> {
        return getMyUser().thenApply { user ->
            if (user.permissionLevel != PermissionLevel.ADMINISTRATOR)
                throw PermissionException()
        }.thenCompose {
            mUserManager.isUsernameExists(username).thenCombine(mUserManager.isUserRevoked(username)) { isExist, isRevoked ->
                if(isExist && !isRevoked)
                    throw IllegalArgumentException()
            }
        }.thenCompose {
            mUserManager.addUser(username, password, permissionLevel)
        }
    }

    /**
     * Get user information if it exists
     *
     * @param username of the user we want information of
     * @return CompletableFuture<User?> with the user information of null if the user does not exists
     */
    fun getUserInformationIfExists(username: String): CompletableFuture<User?> {
        return getUserByUsername(username)
    }

    /**
     * Change user permissions
     *
     * @param username of the user
     * @param newPermissionLevel of the user
     * @throws UserNameDoesNotExistException if username is not exist in the system
     * @throws CanNotChangePermissionException if the user does not have the appropriate permission
     * @return CompletableFuture of the action
     */
    fun changeUserPermissions(username: String, newPermissionLevel: PermissionLevel) : CompletableFuture<Unit> {
        return getUserInformationIfExists(username).thenCombine(getMyUser()) { userToChangePermission, myUser ->
            if(userToChangePermission == null) {
                throw UserNameDoesNotExistException()
            } else if((myUser.permissionLevel == PermissionLevel.USER) ||
                (myUser.permissionLevel < userToChangePermission.permissionLevel) ||
                (myUser.permissionLevel == PermissionLevel.OPERATOR && newPermissionLevel == PermissionLevel.ADMINISTRATOR)) {
                throw CanNotChangePermissionException()
            } else if (mUsername == username) {
                throw IllegalArgumentException()
            }
            userToChangePermission
        }.thenCompose { userToChangePermission -> // TODO: check this line works well
            mUserManager.overrideExistingUser(username, userToChangePermission.account, newPermissionLevel)
        }

    }

    /**
     * Change user account type
     *
     * @param username of the user
     * @param newAccountType of the user
     * @throws UserNameDoesNotExistException if username is not exist in the system
     * @throws CanNotChangePermissionException if the user does not have the appropriate permission
     * @return CompletableFuture of the action
     */
    fun changeUserAccountType(username: String, newAccountType: AccountType) : CompletableFuture<Unit> {
    // getUserInformationIfExists(username) throws exception if username is revoked and auth user is not an admin
        return getUserInformationIfExists(username).thenCompose { userToChange ->
            when {
                userToChange == null -> throw UserNameDoesNotExistException()
                userToChange.permissionLevel != PermissionLevel.USER -> throw CanNotChangeUserAccountTypeException()
                else -> mUserManager.overrideExistingUser(username, newAccountType, userToChange.permissionLevel)
            }
        }
    }

    /**
     * Get the permission level of the authenticated user
     *
     * @return CompletableFuture with the current's user permissionLevel
     */
    fun getMyPermissionLevel() : CompletableFuture<PermissionLevel> {
        return getMyUser().thenApply { it.permissionLevel }
    }

    /**
     * Revoke user
     *
     * @param username of the user we want to revoke
     * @throws PermissionException if the authenticatedUser is not administrator
     * @throws UserNameDoesNotExistException if the username does not exists in the system
     * @return CompletableFuture of the action
     */
    fun revokeUser(username: String) : CompletableFuture<Unit> {
        return getMyPermissionLevel().thenApply { permission ->
            if (permission != PermissionLevel.ADMINISTRATOR)
                throw PermissionException()
        }.thenCompose {
            mUserManager.isUsernameExists(username).thenApply { isExist ->
                if (!isExist)
                    throw UserNameDoesNotExistException()
            }
        }.thenCompose {
            mUserManager.isUserRevoked(username).thenApply { revoked ->
                if (revoked)
                    throw UserIsAlreadyRevokedException()
            }
        }.thenCompose {
            mUserManager.revokeUser(username).thenCompose {
                mTokenManager.invalidateUsernameToken(username)
            }
        }
    }

    /**
     * Attach hardware resource
     *
     * @param id of the resource
     * @param name of the resource
     * @throws PermissionException if the authenticated user is a regular user
     * @throws IdAlreadyExistException if there is already a resource with the same id in the system
     * @return completableFuture of the action
     */
    fun attachHardwareResource(id: String, name: String) : CompletableFuture<Unit> {
        return getMyPermissionLevel().thenApply { myPermission ->
            if(myPermission < PermissionLevel.OPERATOR)
                throw PermissionException()
        }.thenCompose {
            mResourceManager.isIdExist(id).thenApply { isIdExist ->
                if(isIdExist)
                    throw IdAlreadyExistException()
            }
        }.thenCompose {
            mResourceManager.attachHardwareResource(id, name)
        }
    }

    /**
     * Get hardware resource name
     *
     * @param id of the resource
     * @throws ResourceDoesNotExistsException if there is no resource with this id
     * @return CompletableFuture with the resource name
     */
    fun getHardwareResourceName(id: String): CompletableFuture<String> {
        return mResourceManager.getResourceName(id).thenApply { resourceName ->
            resourceName ?: throw ResourceDoesNotExistsException()
        }
    }

    /**
     * return list of the n first resources ids
     *
     * @param n - number of resources to list
     * @return completableFuture with the list
     */
    fun listHardwareResources(n: Int) : CompletableFuture<List<String>> {
        return mResourceManager.getAttachedResources(n)
    }

    private fun getMyUser(): CompletableFuture<User> {
        // the user can't be null as there is an AuthenticatedUser creation
        return mUserManager.getUserByUsernameIfExists(mUsername).thenApply { user ->
            user!!
        }
    }

    private fun getUserByUsername(username: String) : CompletableFuture<User?> {
        return mUserManager.getUserByUsernameIfExists(username).thenCombine(getMyPermissionLevel()) { user, myPermissionLevel ->
            Pair(user, myPermissionLevel)
        }.thenCombine(mUserManager.isUserRevoked(username)) { condPair, isRevoked ->
            if(condPair.first == null || (condPair.second != PermissionLevel.ADMINISTRATOR && isRevoked))
                null
            else
                condPair.first
        }
    }

    /**
     * Submit job to the system
     *
     * @param jobName of the job
     * @param resources the job need to allocate
     * @throws IllegalArgumentException if job manager fails
     * @return [CompletableFuture] of the [AllocatedJob] object
     */
    fun submitJob(jobName: String, resources: List<String>) : CompletableFuture<AllocatedJob> {
        return getUserByUsername(mUsername).thenCompose { user ->
            mJobManager.submitJob(user!!, jobName, resources)
        }.handle { result, exception ->
            if (exception != null)
                throw IllegalArgumentException()
            else
                result
        }
    }

    /**
     * get Job information
     *
     * @param jobId of the job
     * @return CompletableFuture of the [JobDescription]
     */
    fun jobInformation(jobId: String): CompletableFuture<JobDescription> {
        return mJobManager.getJobInformation(jobId)
    }

    /**
     * Cancel job
     *
     * @param jobId
     * @return CompletableFuture of the action
     */
    fun cancelJob(jobId: String): CompletableFuture<Unit>{
        return try {
            mJobManager.cancelJob(jobId, mUsername)
        } catch (e: IllegalArgumentException) {
            CompletableFuture.failedFuture(e)
        }
    }

    fun login() : CompletableFuture<Unit> {
        return mUserManager.setUserLoginState(mUsername, true)
    }
}