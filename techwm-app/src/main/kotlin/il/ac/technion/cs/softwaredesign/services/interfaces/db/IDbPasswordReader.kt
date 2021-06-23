package il.ac.technion.cs.softwaredesign.services.interfaces.db

import java.util.concurrent.CompletableFuture

interface IDbPasswordReader {
    fun getPassword(username: String): CompletableFuture<String?>
}