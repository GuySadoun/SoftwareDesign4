package il.ac.technion.cs.softwaredesign.services.interfaces.user

import java.util.concurrent.CompletableFuture

interface IUserPasswordVerifier {
    fun isUsernamePasswordMatch(username: String, password: String): CompletableFuture<Boolean>
}