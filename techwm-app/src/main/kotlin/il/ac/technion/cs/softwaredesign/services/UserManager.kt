package il.ac.technion.cs.softwaredesign.services

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.AccountType
import il.ac.technion.cs.softwaredesign.PermissionLevel
import il.ac.technion.cs.softwaredesign.User
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbPasswordWriter
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbUserInfoHandler
import il.ac.technion.cs.softwaredesign.services.interfaces.user.IUserManager
import java.util.Objects.isNull
import java.util.concurrent.CompletableFuture

class UserManager @Inject constructor (private val mDbUserInfoHandler: IDbUserInfoHandler, private val mPasswordSetter: IDbPasswordWriter):
    IUserManager {
    companion object{
        internal val defaultAccountType = AccountType.DEFAULT
    }

    /**
     * Add user to the system
     *
     * @param username of the new user
     * @param password of the new user
     * @param permissionLevel of the new user
     * @return CompletableFuture of the action
     */
    override fun addUser(username: String, password: String, permissionLevel: PermissionLevel): CompletableFuture<Unit> {
        val userToAdd = User(username, defaultAccountType, permissionLevel)
        return mDbUserInfoHandler.setUsernameToUser(userToAdd)
            .thenCompose { mDbUserInfoHandler.clearNameFromRevokedList(username) }
            .thenCompose { mPasswordSetter.setPassword(username, password) }
    }

    /**
     * Override existing user's details in the system
     *
     * @param username new username of the user
     * @param accountType new accountType of the user
     * @param permissionLevel new permissionLevel of the user
     * @return CompletableFuture of the action
     */
    override fun overrideExistingUser(username: String, accountType: AccountType, permissionLevel: PermissionLevel): CompletableFuture<Unit> {
        val updatedUser = User(username, accountType, permissionLevel)
        return mDbUserInfoHandler.setUsernameToUser(updatedUser)
    }

    /**
     * Get user by username if exists
     *
     * @param username of the user
     * @return CompletableFuture of the user information or of null if user does not exist
     */
    override fun getUserByUsernameIfExists(username: String): CompletableFuture<User?> {
        return mDbUserInfoHandler.getUserByUsername(username)
    }

    /**
     * return whether user with this username is exists
     *
     * @param username
     * @return CompletableFuture of the result
     */
    override fun isUsernameExists(username: String): CompletableFuture<Boolean> {
        return mDbUserInfoHandler.getUserByUsername(username).thenApply {
            !isNull(it)
        }
    }

    /**
     * return whether user with this username is revoked
     *
     * @param username
     * @return CompletableFuture of the result
     */
    override fun isUserRevoked(username: String): CompletableFuture<Boolean> {
        return mDbUserInfoHandler.isUserRevoked(username)
    }

    /**
     * Revoke user
     *
     * @param username
     * @return CompletableFuture of the action
     */
    override fun revokeUser(username: String): CompletableFuture<Unit> {
        return mDbUserInfoHandler.revokeUser(username)
    }
}