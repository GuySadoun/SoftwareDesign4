package il.ac.technion.cs.softwaredesign.services

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.AdminUserAlreadyExistsException
import il.ac.technion.cs.softwaredesign.PermissionLevel
import il.ac.technion.cs.softwaredesign.services.interfaces.user.IUserManager
import java.util.concurrent.CompletableFuture

/**
 * Admin creator - create the first user of the system
 *
 * @property mUserManager userManager of the system
 * @constructor Create empty Admin creator
 */
class AdminCreator @Inject constructor(private val mUserManager: IUserManager) {
    companion object{
        const val adminUsername = "admin"
    }

    /**
     * Create admin user if admin doesnt already exists in the system
     *
     * @param password of the new admin
     * @throws AdminUserAlreadyExistsException if admin is already exists
     * @return CompletableFuture of the action
     */
    fun createAdminUser(password: String): CompletableFuture<Unit> {
        return mUserManager.isUsernameExists(adminUsername).thenCompose { isUsernameExists ->
            if (isUsernameExists)
                throw AdminUserAlreadyExistsException()
            else
                mUserManager.addUser(adminUsername, password, PermissionLevel.ADMINISTRATOR)
        }
    }

    /**
     * Is admin exist - indicate whether admin already exists in the system
     *
     * @return CompletableFuture with the result
     */
    fun isAdminExist() : CompletableFuture<Boolean> {
        return mUserManager.isUsernameExists(adminUsername)
    }
}