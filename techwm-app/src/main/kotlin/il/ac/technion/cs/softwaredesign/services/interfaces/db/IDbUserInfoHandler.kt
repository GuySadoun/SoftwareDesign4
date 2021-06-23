package il.ac.technion.cs.softwaredesign.services.interfaces.db

import il.ac.technion.cs.softwaredesign.PermissionLevel
import il.ac.technion.cs.softwaredesign.User
import java.util.concurrent.CompletableFuture

interface IDbUserInfoHandler {
    fun getUserByUsername(username: String): CompletableFuture<User?>

    fun setUsernameToUser(user: User): CompletableFuture<Unit>

    fun getUserPermissionLevel(username: String) : CompletableFuture<PermissionLevel?>
    fun isUserRevoked(username: String): CompletableFuture<Boolean>
    fun revokeUser(username: String): CompletableFuture<Unit>
    fun clearNameFromRevokedList(username: String): CompletableFuture<Unit>
}