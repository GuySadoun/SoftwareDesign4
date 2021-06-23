package il.ac.technion.cs.softwaredesign.services.interfaces.db

import java.util.concurrent.CompletableFuture

interface IDbTokenHandler{
    fun getUsernameByToken(token: String): CompletableFuture<String?>
    fun setOrReplaceTokenToUsername(token: String, username: String): CompletableFuture<Unit>
    fun isDeleted(token: String) : CompletableFuture<Boolean>
    fun deleteUserPreviousTokenIfExist(username: String) : CompletableFuture<Unit>
}