package il.ac.technion.cs.softwaredesign.services.interfaces.token

import java.util.concurrent.CompletableFuture

interface ITokenManager {
    fun createOrReplaceUserToken(username: String): CompletableFuture<String>
    fun getUsernameByTokenIfExists(token: String): CompletableFuture<String?>
    fun invalidateUsernameToken(username: String): CompletableFuture<Unit>
}