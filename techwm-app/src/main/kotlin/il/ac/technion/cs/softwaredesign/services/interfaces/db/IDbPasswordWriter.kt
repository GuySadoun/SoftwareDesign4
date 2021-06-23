package il.ac.technion.cs.softwaredesign.services.interfaces.db

import java.util.concurrent.CompletableFuture

interface IDbPasswordWriter {
    fun setPassword(username: String, password: String): CompletableFuture<Unit>
}