package il.ac.technion.cs.softwaredesign.services.interfaces.user

import il.ac.technion.cs.softwaredesign.AccountType
import il.ac.technion.cs.softwaredesign.PermissionLevel
import il.ac.technion.cs.softwaredesign.User
import java.util.concurrent.CompletableFuture

interface IUserManager {

    fun addUser(username: String, password: String, permissionLevel: PermissionLevel): CompletableFuture<Unit>
    fun overrideExistingUser(username: String, accountType: AccountType, permissionLevel: PermissionLevel): CompletableFuture<Unit>
    fun getUserByUsernameIfExists(username: String): CompletableFuture<User?>
    fun isUsernameExists(username: String): CompletableFuture<Boolean>
    fun isUserRevoked(username: String): CompletableFuture<Boolean>
    fun revokeUser(username: String): CompletableFuture<Unit>
}