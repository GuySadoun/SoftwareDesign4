package il.ac.technion.cs.softwaredesign

import java.util.concurrent.CompletableFuture

class AccessRequestImpl(override val requestingUsername: String, override val reason: String, private val password: String) : AccessRequest  {
    override fun approve(): CompletableFuture<Unit> {
        TODO("Not yet implemented")
    }

    override fun decline(): CompletableFuture<Unit> {
        TODO("Not yet implemented")
    }
}